package com.twitter.captainahabrunner.zookeeper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.twitter.captainahabrunner.configuration.ServerTriple;
import com.twitter.captainahabrunner.utilities.Utilities;

import static com.twitter.captainahabrunner.utilities.ToEdnUtilities.getCASEdn;
import static com.twitter.captainahabrunner.utilities.ToEdnUtilities.getCASEdnINFO;
import static com.twitter.captainahabrunner.utilities.ToEdnUtilities.getReadEdn;
import static com.twitter.captainahabrunner.utilities.ToEdnUtilities.getReadEdnINFO;
import static com.twitter.captainahabrunner.utilities.ToEdnUtilities.getWriteEdn;
import static com.twitter.captainahabrunner.utilities.ToEdnUtilities.getWriteEdnINFO;
import static com.twitter.captainahabrunner.utilities.Utilities.ObserversParticipantsPair;
import static com.twitter.captainahabrunner.utilities.Utilities.getRandomServersToAdd;
import static com.twitter.captainahabrunner.utilities.Utilities.getRandomServersToRemove;
import static com.twitter.captainahabrunner.utilities.Utilities.getServers;
import static com.twitter.captainahabrunner.utilities.Utilities.getVersion;

public class ZooKeeperClient implements Runnable {

  // contains the strings as needed for the reconfigure operations
  // (e.g., (key = 4, value = "server.4=localhost:2222:2223:participant;localhost:2791")
  private Map<Integer, String> serverIdToZKString;

  private int totalClients;
  private ZooKeeper zk;
  private ZooKeeperConnection connection;
  private int clientId;
  private List<ServerTriple> hosts;
  private List<Integer> servers;
  private List<String> clientOperations;
  private volatile boolean isCompleted;
  private int totalOps;
  private boolean connectToOne;

  private BufferedWriter bufferedWriter;

  public ZooKeeperClient(BufferedWriter bufferedWriter, int totalClients, int totalOps,
                         int clientId, List<ServerTriple> hosts, List<String> clientOperations,
                         boolean connectToOne) {
    this.hosts = hosts;
    this.clientId = clientId;
    this.totalClients = totalClients;
    this.totalOps = totalOps;
    this.bufferedWriter = bufferedWriter;
    this.clientOperations = clientOperations;
    this.isCompleted = false;
    this.connectToOne = connectToOne;
    this.servers = new LinkedList<>();
  }

  public boolean isCompleted() {
    return isCompleted;
  }

  // UNKNOWN in case we get a ConnectionLossException or a SessionExpiredException
  public enum OperationResult {
    SUCCESS, FAILURE, UNKNOWN
  };

  public OperationResult write(String path, int value) {
    try {
      // always succeeds if no exception is thrown
      zk.setData(path, String.valueOf(value).getBytes(), -1);
      return OperationResult.SUCCESS;
    } catch (KeeperException.SessionExpiredException e) {
      connect();
      return OperationResult.UNKNOWN;
    } catch (KeeperException.ConnectionLossException e) {
      return OperationResult.UNKNOWN;
    } catch (Exception e) {
      e.printStackTrace();
      return OperationResult.UNKNOWN;
    }
  }

  private boolean performWrite(String key, int value) {
    info(getWriteEdn(System.nanoTime(), true, clientId, value));

    OperationResult res = write(key, value);
    if (res == OperationResult.SUCCESS) {
      info(getWriteEdn(System.nanoTime(), false, clientId, value));
      return true;
    }
    else {
      info(getWriteEdnINFO(System.nanoTime(), clientId, 0));
      clientId += totalClients;
    }

    return false;
  }

  private String addServers(String serversToAdd, long previousConfig) {
    try {
      byte[] res = zk.reconfig(serversToAdd, null, null, previousConfig, new Stat());
      return new String(res);
    } catch (KeeperException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return "error";
  }

  private String removeServers(String serversToRemove, long previousConfig) {
    try {
      byte[] res = zk.reconfig(null, serversToRemove, null, previousConfig , new Stat());
      return new String(res);
    } catch (KeeperException e) {
      e.printStackTrace();

    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return "error";
  }

  public boolean reconfigure() {

    try {
      Random r = new Random();
      byte[] configBytes = zk.getConfig(false, new Stat());
      String config = new String(configBytes);
      long configVersion = getVersion(config);
      List<Integer> participants = getServers(config, true);
      List<Integer> observers = getServers(config, false);

      // initialize once
      if (serverIdToZKString == null) {
        serverIdToZKString = new HashMap<>();
        for (ServerTriple triple: hosts) {
          String ip = triple.getHost();
          int port1 = 2222;
          int port2 = 2223;
          int clientPort = 2791;
          int id = triple.getId();
          servers.add(id);
          boolean isParticipant = participants.contains(id);
          serverIdToZKString.put(id, String.format("server.%d=%s:%s:%s:%s;%s:%s",
              id, ip, port1, port2, isParticipant ? "participant" : "observer", ip, clientPort));
        }
        System.out.println("\n\n" + serverIdToZKString);
      }

      System.out.println("Current config [participants: " +  participants + "] [observers: " + observers + "]");

      boolean toAdd = r.nextInt(2) == 1;
      if (toAdd) {
        ObserversParticipantsPair addPair = getRandomServersToAdd(observers, participants, servers);
        if (addPair.getObservers().isEmpty() && addPair.getParticipants().isEmpty()) {
          return true;
        }
        String serversToAddStr = Utilities.serversToAddToString(addPair, serverIdToZKString);
        addServers(serversToAddStr, configVersion);
      }
      else {
        ObserversParticipantsPair removePair = getRandomServersToRemove(observers, participants);
        if (removePair.getObservers().isEmpty() && removePair.getParticipants().isEmpty()) {
          return true;
        }
        String serversToRemoveStr = Utilities.serversToRemoveToString(removePair);
        removeServers(serversToRemoveStr, configVersion);
      }
      return true;
    } catch (KeeperException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return false;
  }

  private String getFullConnectionString() {
    StringBuffer connectionString = new StringBuffer();

    for (int i = 0; i < hosts.size(); ++i) {
      String hostAndPort = String.format("%s:%d", hosts.get(i).getHost(), hosts.get(i).getPort());
      connectionString.append(hostAndPort);

      if (i < hosts.size() - 1) {
        connectionString.append(",");
      }
    }

    return connectionString.toString();
  }

  private String getOneRandomServerConnectionString() {
    StringBuffer connectionString = new StringBuffer();
    Random r = new Random();
    int serverIndex = r.nextInt(hosts.size());

    connectionString.append(String.format("%s:%d", hosts.get(serverIndex).getHost(),
        hosts.get(serverIndex).getPort()));
    return connectionString.toString();
  }

  private void connect() {
    connection = new ZooKeeperConnection();

    String connectionString;
    if (connectToOne) {
      connectionString = getOneRandomServerConnectionString();
    }
    else {
      connectionString = getFullConnectionString();
    }

    System.out.println("ZooKeeperClient (" + clientId + ") about to connect to (" + connectionString + ")");
    try {
      zk = connection.connect(connectionString);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println("Client could not be started");
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println("Client successfully started!");
  }


  /**
   * @param path
   * @param stat
   * @return -1 corresponds to a failed read.
   */
  public ReadResult read(String path, Stat stat) {
    try {
      byte[] result = zk.getData(path, false, stat);
      return new ReadResult(OperationResult.SUCCESS, stat.getVersion(), Integer.valueOf(new String(result)));
    } catch (KeeperException.SessionExpiredException e) {
      connect();
      return new ReadResult(OperationResult.UNKNOWN, -1, -1);
    } catch (KeeperException.ConnectionLossException e) {
      return new ReadResult(OperationResult.UNKNOWN, -1, -1);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return new ReadResult(OperationResult.UNKNOWN, -1, -1);
  }

  private class ReadResult {
    OperationResult result;
    int latestVersion;
    int latestReadValue;

    public ReadResult(OperationResult result, int latestVersion, int latestReadValue) {
      this.result = result;
      this.latestVersion = latestVersion;
      this.latestReadValue = latestReadValue;
    }
  }

  private ReadResult performRead(String key) {
    info(getReadEdn(System.nanoTime(), true, clientId, -1));

    Stat stat = new Stat();
    ReadResult res = read(key, stat);
    int latestReadValue = -1;
    int latestVersion = -1;

    if (res.result == OperationResult.UNKNOWN) {
      // we are not sure what value was read
      info(getReadEdnINFO(System.nanoTime(), clientId, -1));
      clientId += totalClients;
    }
    else {
      info(getReadEdn(System.nanoTime(), false, clientId, res.latestReadValue));
      if (res.result != OperationResult.UNKNOWN) {
        latestVersion = stat.getVersion();
        latestReadValue = res.latestReadValue;
      }
    }
    return new ReadResult(res.result, latestVersion, latestReadValue);
  }

  private ReadResult performSyncRead(String key) {
    info(getReadEdn(System.nanoTime(), true, clientId, -1));

    zk.sync(key, null, null); // FIXME (refactor with performRead -- same code stuff)
    Stat stat = new Stat();
    ReadResult res = read(key, stat);
    int latestReadValue = -1;
    int latestVersion = -1;

    if (res.result == OperationResult.UNKNOWN) {
      // we are not sure what value was read
      info(getReadEdnINFO(System.nanoTime(), clientId, -1));
      clientId += totalClients;
    }
    else {
      info(getReadEdn(System.nanoTime(), false, clientId, res.latestReadValue));
      if (res.result != OperationResult.UNKNOWN) {
        latestVersion = stat.getVersion();
        latestReadValue = res.latestReadValue;
      }
    }
    return new ReadResult(res.result, latestVersion, latestReadValue);
  }

  public boolean create(String path) {
    try {
      byte[] initialData = "0".getBytes();
      zk.create(path, initialData, ZooDefs.Ids.OPEN_ACL_UNSAFE,
          CreateMode.PERSISTENT);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  private void info(String s) {
    try {
      bufferedWriter.write(s + "\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   *
   * @param path
   * @param value
   * @param version
   * @return boolean array[2], if array[0] then the CAS isCompleted, and array[1] shows whether it was
   * successful or not
   */
  public OperationResult[] CAS(String path, int value, int version) {
    try {
      zk.setData(path, String.valueOf(value).getBytes(), version);
      return new OperationResult[] {OperationResult.SUCCESS, OperationResult.SUCCESS};
    } catch (KeeperException.BadVersionException e) {
      // CAS itself was isCompleted, but failed to set the value
      return new OperationResult[] {OperationResult.SUCCESS, OperationResult.FAILURE};
    } catch (KeeperException.SessionExpiredException e) {
      connect();
      return new OperationResult[] {OperationResult.UNKNOWN, OperationResult.UNKNOWN};
    } catch (KeeperException.ConnectionLossException e) {
      return new OperationResult[] {OperationResult.UNKNOWN, OperationResult.UNKNOWN};
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return new OperationResult[] {OperationResult.UNKNOWN, OperationResult.FAILURE};
  }

  private boolean performCAS(String key, int value, int latestReadValue, int latestVersion) {
    info(getCASEdn(System.nanoTime(), true, clientId,false,
        value, latestReadValue));

    OperationResult[] res = CAS(key, value, latestVersion);
    if (res[0] == OperationResult.SUCCESS) {
      info(getCASEdn(System.nanoTime(), false, clientId, res[1] == OperationResult.SUCCESS,
          value, latestReadValue));
      return res[1] == OperationResult.SUCCESS;
    }
    else {
      info(getCASEdnINFO(System.nanoTime(), clientId, value, latestReadValue));
      clientId += totalClients;
      return false;
    }

  }

  @Override
  public void run() {
    connect();

    String key = "/key";
    System.out.println(String.format("About to start running ZK client %d", clientId));
    create(key);

    Random r = new Random();
    int latestVersion = -1;
    int latestReadValue = -1;
    int successfulOps = 0;

    // initialize value to 0 before we start
    performWrite(key, 0);

    long start = System.currentTimeMillis();
    for (int i = 0; i < totalOps; ++i) {
      int value = r.nextInt(20);


      int possibleOperations = clientOperations.size();
      int operationIndex = r.nextInt(possibleOperations);

      switch (clientOperations.get(operationIndex)) {
        case "write": {
          boolean res = performWrite(key, value);
          if (i % 2000 == 0) {
            System.out.println(String.format("Client %d performed a WRITE %s [%d operations]",
                clientId, zk.getState(), i));
          }
          successfulOps += (res ? 1 : 0);
          break;
        }
        case "read": {
          ReadResult ret = performRead(key);
          if (i % 2000 == 0) {
            System.out.println(String.format("Client %d performed a READ %s [%d operations]",
                clientId, zk.getState(), i));
          }
          latestVersion = ret.latestVersion;
          latestReadValue = ret.latestReadValue;
          successfulOps += (ret.result == OperationResult.SUCCESS ? 1 : 0);
          break;
        }
        case "sync_read": {
          ReadResult ret = performSyncRead(key);
          if (i % 2000 == 0) {
            System.out.println(String.format("Client %d performed a SYNC_READ %s [%d operations]",
                clientId, zk.getState(), i));
          }
          latestVersion = ret.latestVersion;
          latestReadValue = ret.latestReadValue;
          successfulOps += (ret.result == OperationResult.SUCCESS ? 1 : 0);
          break;
        }
        case "CAS": {
          // CAS only if you read something before
          if (latestReadValue != -1) {
            if (i % 2000 == 0) {
              System.out.println(String.format("Client %d performed a CAS %s [%d operations]",
                  clientId, zk.getState(), i));
            }
            boolean res = performCAS(key, value, latestReadValue, latestVersion);
            successfulOps += (res ? 1 : 0);
          }
          break;
        }
        case "reconfig": {
          // avoid reconfiguring too often
          if (r.nextInt(500) <= 450) {
            break;
          }
          if (i % 2000 == 0) {
            System.out.println(String.format("Client %d performed a RECONFIG %s [%d operations]",
                clientId, zk.getState(), i));
          }
          boolean res = reconfigure();
          successfulOps += (res ? 1 : 0);
          break;
        }
      }
    }
    long end = System.currentTimeMillis();

    long totalTimeInSec = (long) ((end - start) / 1000.0);
    System.out.println("<< Total time in sec: " + totalTimeInSec);
    System.out.println("Percentage of successful operations " + ((float) successfulOps) / totalOps);
    long opsPerSec = totalTimeInSec != 0? totalOps / totalTimeInSec: 0;
    System.out.println(opsPerSec);

    close();
  }

  public void close() {
    try {
      bufferedWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    isCompleted = true;
    connection.close();
  }
}
