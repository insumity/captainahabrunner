package com.twitter.captainahabrunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.twitter.captainahab.main.CaptainAhabMain;
import com.twitter.captainahab.main.ServerPortPair;
import com.twitter.captainahab.nemesis.NetworkPartitionManager;
import com.twitter.captainahabrunner.configuration.ClientData;
import com.twitter.captainahabrunner.configuration.ConfigurationReader;
import com.twitter.captainahabrunner.configuration.ServerTriple;
import com.twitter.captainahabrunner.zookeeper.ZooKeeperClient;

import static com.twitter.captainahab.utilities.Utilities.executeCommandLocally;
import static com.twitter.captainahab.utilities.Utilities.executeCommandLocallyNonRoot;

public class Main {

  public static void main(String[] args) throws Exception {
    String configurationFile = args[0];

    ConfigurationReader conf = new ConfigurationReader(configurationFile);
    Set<ServerTriple> servers = conf.getServers();
    Set<ServerPortPair> serverPairs = toPairs(servers);

    String username = conf.getUsername();
    CaptainAhabMain captain = new CaptainAhabMain(serverPairs, username);

    System.out.println("Oh captain! My captain!");
    System.out.println(">> Starting servers and clients");
    captain.start();

    System.out.println(">> Flushing iptables.");
    flushIpTables(captain, serverPairs);
    System.out.println(">> Staring ZK servers.");
    startZKServers(captain, serverPairs);


    // create ZooKeeper servers
    List<ServerTriple> zkServers = new LinkedList<>();
    for (ServerTriple server: servers) {
      zkServers.add(new ServerTriple(server.getHost(), 2791, server.getId()));
    }


    List<ClientData> clients = conf.getClients();
    int totalClients = clients.size();
    Thread[] threads = new Thread[totalClients];
    ZooKeeperClient[] zkThreads = new ZooKeeperClient[totalClients];

    String history = String.format("/tmp/%s", conf.getExecutionName());
    createLogDirectory(history, username);

    for (int i = 0; i < totalClients; ++i) {
      BufferedWriter bufferedWriter =
          new BufferedWriter(new FileWriter(new File(history + "/log_results_client_" + i)));

      ClientData client = clients.get(i);
      int totalOps = conf.getTotalOperationsPerClient();
      zkThreads[i] = new ZooKeeperClient(bufferedWriter, totalClients, totalOps, i, zkServers,
          client.getOperations(), client.isConnectToOne());
      threads[i] = new Thread(zkThreads[i]);
      threads[i].start();
    }
    System.out.println(">>> Started ZK client threads");


    NetworkPartitionManager manager = captain.getNetworkPartitionManager();

    long startTime = System.currentTimeMillis();
    while (!allThreadsCompleted(zkThreads)) {
      System.out.println(String.format("Changing topology to: %s",
          topologyToString(Arrays.asList(serverPairs), servers)));
      manager.changeTopology(Arrays.asList(serverPairs));


      int fullyConnectedTimeInSeconds = conf.getPartitionEveryInSeconds();
      System.out.println(String.format("wait for %d seconds", fullyConnectedTimeInSeconds));
      Thread.sleep(fullyConnectedTimeInSeconds * 1000);


      List<Set<ServerPortPair>> topology = getRandomTopology(serverPairs);
      System.out.println(String.format("Changing topology to: %s",
          topologyToString(topology, servers)));
      manager.changeTopology(topology);

      int partitionedTimeInSeconds = conf.getPartitionDurationInSeconds();
      System.out.println(String.format("wait for %d seconds", partitionedTimeInSeconds));
      Thread.sleep(partitionedTimeInSeconds * 1000);
    }
    long endTime = System.currentTimeMillis();
    System.out.println("Total time (s): " + (endTime - startTime) / 1000.0);

    for (int i = 0; i < totalClients; ++i) {
      zkThreads[i].close();
    }

    gatherLogFiles(history, serverPairs, username);
    captain.stop();
  }

  private static String topologyToString(List<Set<ServerPortPair>> servers, Set<ServerTriple> triples) {
    List<List<Integer>> topology = new LinkedList<>();

    for (Set<ServerPortPair> set: servers) {
      List<Integer> component = new ArrayList<>();
      for (ServerPortPair pair: set) {
        for (ServerTriple triple: triples) {
          if (pair.getHost().equals(triple.getHost())) {
            component.add(triple.getId());
            break;
          }
        }
      }
      Collections.sort(component);
      topology.add(component);
    }
    return topology.toString();
  }

  private static Set<ServerPortPair> toPairs(Set<ServerTriple> servers) {
    Set<ServerPortPair> set = new HashSet<>();
    for (ServerTriple triple: servers) {
      set.add(new ServerPortPair(triple.getHost(), triple.getPort()));
    }
    return set;
  }

  private static void flushIpTables(CaptainAhabMain captain, Set<ServerPortPair> servers) {
    captain.executeCommandToAllServers(servers, "sudo iptables -F");
    try {
      Thread.sleep(2 * 1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static void startZKServers(CaptainAhabMain captain, Set<ServerPortPair> servers) {
    // First kill any running server, so we can get the logs.
    captain.executeCommandToAllServers(servers, "sudo pkill -f zookeeper");
    captain.executeCommandToAllServers(servers, "sudo rm -f /tmp/zookeeper*.log");

    captain.executeCommandToAllServers(servers, "sudo rm -f /tmp/zookeeper/*.cfg");
    captain.executeCommandToAllServers(servers, "sudo rm -f /tmp/zookeeper/*.cfg.dynamic.*");

    for (ServerPortPair server: servers) {
      try {
        executeCommandLocallyNonRoot("scp /tmp/zoo.cfg " + server.getHost() + ":/tmp/zookeeper", true);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    captain.executeCommandToAllServers(servers, "/tmp/zookeeper/bin/zkServer.sh " +
        "start-foreground " +
        "/tmp/zookeeper/zoo.cfg " +
        "1>/tmp/zookeeper_stdout.log " +
        "2>/tmp/zookeeper_stderr.log");
    try {
      Thread.sleep(5 * 1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static void createLogDirectory(String history, String username) {
    try {
      executeCommandLocally("mkdir " + history, true);
      executeCommandLocally(String.format("sudo chown %s %s", username, history), true);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void gatherLogFiles(String history, Set<ServerPortPair> servers, String username) throws IOException {

    for (ServerPortPair server: servers) {
      String localLogsDirectory = String.format("%s/%s_%s", history, server.getHost(), "logs");
      executeCommandLocally("mkdir " + localLogsDirectory, true);
      executeCommandLocally(String.format("sudo chown %s %s", username, localLogsDirectory), true);
      System.out.println("scp " + server.getHost() + ":/tmp/*.log " + localLogsDirectory);
      executeCommandLocallyNonRoot("scp " + server.getHost() + ":/tmp/*.log " + localLogsDirectory, true);
    }
  }


  private static List<Set<ServerPortPair>> getRandomTopology(Set<ServerPortPair> servers) {
    ServerPortPair[] serversArray = servers.toArray(new ServerPortPair[servers.size()]);
    Collections.shuffle(Arrays.asList(servers));
    List<Set<ServerPortPair>> lst = new LinkedList<>();
    Random r = new Random();
    int components = r.nextInt(4) + 2; // from 2 to 5 components
    int currentComponents = components;
    int totalNodes = serversArray.length;
    int actualTotalNodes = serversArray.length;
    int start = 0;

    for (int i = 0; i < components; ++i) {
      Set<ServerPortPair> component = new HashSet<>();
      if (i != components - 1) {
        int maxNodesInComponent = totalNodes - (currentComponents - 1);
        int actualNodesInComponent = r.nextInt(maxNodesInComponent) + 1; // at least one node is needed
        totalNodes -= actualNodesInComponent;
        for (int j = start; j < start + actualNodesInComponent; ++j) {
          component.add(serversArray[j]);
        }
        start = start + actualNodesInComponent;
        currentComponents--;
      }
      else {
        for (int j = start; j < actualTotalNodes; ++j) {
          component.add(serversArray[j]);
        }
      }
      lst.add(component);
    }
    return lst;
  }

  private static boolean allThreadsCompleted(ZooKeeperClient[] zkThreads) {
    for (ZooKeeperClient c: zkThreads) {
      if (!c.isCompleted()) {
        return false;
      }
    }
    return true;
  }
}
