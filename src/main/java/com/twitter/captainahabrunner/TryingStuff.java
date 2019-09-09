//package com.twitter.captainahabrunner;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.InputStreamReader;
//import java.util.Arrays;
//
//import com.twitter.captainahab.main.ServerPortPair;
//
//public class TryingStuff {
//
//  private static String executeCommandSync(String command) {
//    Process p;
//    StringBuffer line = new StringBuffer();
//
//    try {
//      p = Runtime.getRuntime().exec(command);
//      BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
//
//      String tmpLine = input.readLine();
//      while (tmpLine != null) {
//        line.append(tmpLine);
//        line.append('\n');
//        tmpLine = input.readLine();
//      }
//
//      p.waitFor();
//    } catch (Exception e) {
//      return String.format("Error: %s for command: %s", e, command);
//    }
//
//    return line.toString();
//  }
//
//  public static void main(String[] args) throws Exception {
//    ZooKeeperClient[] zkThreads = new ZooKeeperClient[2];
//    Thread[] threads = new Thread[2];
//
//    ServerPortPair[] servers = new ServerPortPair[5];
//    for (int j = 0; j < 5; ++j) {
//      servers[j] = new ServerPortPair("127.0.0.1", 2791 + j);
//    }
//
//    for (int i = 0; i < 2; ++i) {
//      BufferedWriter bufferedWriter =
//          new BufferedWriter(new FileWriter(new File("NEWlog_results_client_" + i)));
//      zkThreads[i] = new ZooKeeperClient(bufferedWriter, 2, 50000, i,
//          Arrays.asList(servers), i == 0,
//          i == 0 ? 0 : 1);
//      threads[i] = new Thread(zkThreads[i]);
//      threads[i].start();
//    }
//
//    Thread.sleep(5 * 1000);
//    System.out.println("JEY THERE");
//    Thread.sleep(20 * 1000);
//    System.out.println("JEY THERE2");
//
//
//    for (int i = 0; i < 2; ++i) {
//      threads[i].join();
//    }
//  }
//}
