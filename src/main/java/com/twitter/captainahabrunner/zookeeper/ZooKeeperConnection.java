package com.twitter.captainahabrunner.zookeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class ZooKeeperConnection {

  private ZooKeeper zk;

  public ZooKeeper connect(String host) throws IOException,InterruptedException {

    CountDownLatch connectedSignal = new CountDownLatch(1);

    zk = new ZooKeeper(host,10000, we -> {
      if (we.getState() == Watcher.Event.KeeperState.SyncConnected) {
        connectedSignal.countDown();
      }
    });

    connectedSignal.await();
    return zk;
  }

  public void close() {
    try {
      System.err.println("The ZooKeeper connection was closed!");
      zk.close();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
