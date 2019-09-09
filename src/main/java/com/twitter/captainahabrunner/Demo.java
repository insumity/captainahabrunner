package com.twitter.captainahabrunner;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import com.twitter.captainahab.main.ServerPortPair;
import com.twitter.captainahabrunner.configuration.ConfigurationReader;
import com.twitter.captainahabrunner.configuration.ServerTriple;


//  public static void main(String[] args) throws InterruptedException, IOException {
//
//    ServerPortPair[] servers = new ServerPortPair[]{
//        new ServerPortPair("10.247.0.99", 5005),
//        new ServerPortPair("10.247.5.10", 5005),
//        new ServerPortPair("10.247.5.24", 5005),
//        new ServerPortPair("10.247.1.42", 5005),
//        new ServerPortPair("10.247.5.179", 5005),
//        new ServerPortPair("10.247.1.53", 5005),
//        new ServerPortPair("10.247.5.51", 5005),
//        new ServerPortPair("10.247.5.181", 5005),
//        new ServerPortPair("10.247.5.53", 5005),
//        new ServerPortPair("10.247.5.48", 5005),
//        new ServerPortPair("10.247.5.33", 5005)};
//
//    Set<ServerPortPair> allSet = new HashSet<>();
//    for (ServerPortPair server : servers) {
//      allSet.add(server);
//    }
//
//
//    CaptainAhabMain captain = new CaptainAhabMain(allSet, "kantoniadis");
//
//    System.out.println("Oh captain! My captain!");
//    captain.start();
//
//
//    for (int i = 0; i < servers.length; ++i) {
//      ClientDemo client = new ClientDemo();
//      client.start(servers[i].getHost(), servers[i].getPort());
//
//      System.out.println("----");
//      System.out.println(client.applyCommand("sleep 5; uname -a", true));
//      System.out.println(client.applyCommand("sudo pkill -f captainahab", false));
//
//
//      System.out.println("Started all the servers ... now time to kill them");
//    }
//
//    captain.stop();
//  }

//
//  public static void main2(String[] args) {
//    System.out.println("HELLO");
//    CaptainAhabClient client = new CaptainAhabClient();
//    client.start(args[0], Integer.parseInt(args[1]));
//
//    Scanner input = new Scanner(System.in);
//
//    int i = 0;
//    while (input.hasNext()) {
//      String cmd = input.nextLine();
//
//      if (cmd.equals("stop")) {
//        break;
//      }
//
//      String res = new String();
//      if (i % 2 == 0) {
//        System.out.println("About to apply sync command: " + cmd);
//        res = client.applyCommand(cmd, true);
//      } else {
//        System.out.println("About to apply async command: " + cmd);
//        res = client.applyCommand(cmd, false);
//      }
//      i++;
//      System.out.println("The result was: " + res);
//    }
//
//    client.stop();
//  }
//

public class Demo {
//
  public static void main(String[] args) throws IOException, InterruptedException, IllegalAccessException {
    ConfigurationReader conf = new ConfigurationReader("configuration.yaml");
    Set<ServerTriple> servers = conf.getServers();
    System.out.println(conf.getClients());

    System.exit(1);


    ZooKeeper o = new ZooKeeper("localhost",10000, event -> System.out.println(event));

    Class c = o.getClass();

    for (Field f: c.getDeclaredFields()) {
      System.out.println(f.get(o));
    }
//    BufferedWriter bufferedWriter =
//        new BufferedWriter(new FileWriter(new File("foobarisios")));
//
//    List<ServerPortPair> pairs = Arrays.asList(new ServerPortPair[] {new ServerPortPair("localhost", 2791),
//    new ServerPortPair("localhost", 2792), new ServerPortPair("localhost", 2793)});
//    ZooKeeperClient zk = new ZooKeeperClient(bufferedWriter, 1, 100000, 1, pairs);
//
//    Thread t = new Thread(zk);
//    t.start();
//
//    t.join();
//    zk.close();
  }



  private static void randomRemoveFromConfig(List<Integer> currentObservers, List<Integer> currentParticipants) {
    int participantsNumber = currentParticipants.size();
    int maxParticipantsToRemove = (participantsNumber - 1) / 2;
    Random r = new Random();

    // in case of 2 participants, do not remove any
    if (maxParticipantsToRemove != 0) {
      int participantsToRemove = r.nextInt(maxParticipantsToRemove) + 1;
      for (int i = 0; i < participantsToRemove; ++i) {
        int part = currentParticipants.remove(0);
        System.out.println("removed participant: " + part);
      }
    }

    // remove observers only if we have at least 1
    if (currentObservers.size() > 1) {
      int observersToRemove = r.nextInt(currentObservers.size());
      for (int i = 0; i < observersToRemove; ++i) {
        int obs = currentObservers.remove(0);
        System.out.println("removed observer: " + obs);
      }
    }
  }

  private static void randomAddToConfig(List<Integer> currentObservers, List<Integer> currentParticipants,
                                        List<Integer> allServers) {
    LinkedList<Integer> currentServers = new LinkedList<>(currentObservers);
    currentServers.addAll(currentParticipants);

    Set<Integer> notUsedServers = new HashSet<>(allServers);
    notUsedServers.removeAll(new HashSet<>(currentServers));
    System.out.println(">>> " + notUsedServers);

    List<Integer> notUsedServersList = new LinkedList<>(notUsedServers);

    Random r = new Random();
    if (notUsedServers.size() == 0) {
      return;
    }

    int participantsToAdd = r.nextInt(notUsedServers.size());
    for (int i = 0; i < participantsToAdd; ++i) {
      int part = notUsedServersList.remove(0);
      currentParticipants.add(part);
      System.out.println("added participant: " + part);
    }

    if (notUsedServersList.size() != 0) {
      int observersToAdd = r.nextInt(notUsedServersList.size());

      for (int i = 0; i < observersToAdd; ++i) {
        int obs = notUsedServersList.remove(0);
        currentObservers.add(obs);
        System.out.println("added observer: " + obs);
      }
    }
  }


  public static void main2(String[] args) throws InterruptedException {

    List<Integer> currentParticipants = new LinkedList<>(Arrays.asList(new Integer[] {1, 2, 3}));
    List<Integer> currentObservers = new LinkedList<>(Arrays.asList(new Integer[] {4, 5, 6, 7, 8, 9, 10, 11}));
    List<Integer> all = new LinkedList<>(Arrays.asList(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}));

    while (true) {
      randomRemoveFromConfig(currentObservers, currentParticipants);
      System.out.println("(observers: " + currentObservers + "), (participants: " + currentParticipants);

      randomAddToConfig(currentObservers, currentParticipants, all);
      System.out.println("(observers: " + currentObservers + "), (participants: " + currentParticipants);
      Thread.sleep(5 * 1000);
    }

//
//    String[] servers = new String[11];
//    for (int i = 0; i < 11; ++i) {
//      servers[i] = new String("" + i);
//    }
//
//    Collections.shuffle(Arrays.asList(servers));
//    System.out.println(Arrays.toString(servers));
//
//    List<Set<String>> lst = new LinkedList<>();
//    Random r = new Random();
//    int components = r.nextInt(4) + 2; // from 2 to 5 components
//    int currentComponents = components;
//    int totalNodes = 11;
//    int start = 0;
//    for (int i = 0; i < components; ++i) {
//      Set<String> component = new HashSet<>();
//      if (i != components - 1) {
//        int maxNodesInComponent = totalNodes - (currentComponents - 1);
//        int actualNodesInComponent = r.nextInt(maxNodesInComponent) + 1; // at least one node is needed
//        totalNodes -= actualNodesInComponent;
//        for (int j = start; j < start + actualNodesInComponent; ++j) {
//          component.add(servers[j]);
//        }
//        start = start + actualNodesInComponent;
//        currentComponents--;
//      }
//      else {
//        for (int j = start; j < 11; ++j) {
//          component.add(servers[j]);
//        }
//      }
//      lst.add(component);
//    }

  }

}
