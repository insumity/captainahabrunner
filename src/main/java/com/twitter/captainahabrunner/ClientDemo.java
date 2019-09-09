package com.twitter.captainahabrunner;

import java.util.Scanner;

import com.twitter.captainahab.CaptainAhabServiceGrpc;
import com.twitter.captainahab.Request;
import com.twitter.captainahab.Response;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class ClientDemo {
  private ManagedChannel channel;
  private CaptainAhabServiceGrpc.CaptainAhabServiceBlockingStub stub;

  private String host;
  private int port;

  public void start(String host, int port) {
    this.host = host;
    this.port = port;

    System.out.println("About to start at " + host + ", " + port);

    try {
      channel = ManagedChannelBuilder.forAddress(host, port)
          .usePlaintext()
          .build();

      System.out.println("Channel: " + channel);

      stub = CaptainAhabServiceGrpc.newBlockingStub(channel);
      System.out.println("Stub + ... " + stub);
    } catch (Exception e) {
      System.err.println(e + " something went wrong!");
    }
  }

  public String applyCommand(String command, boolean wait) {
    try {
      Response response = stub.applyCommand(Request.newBuilder()
          .setWait(wait)
          .setCommand(command)
          .build());
      System.out.println("The resonse was: " + response);
      return response.getResult();
    } catch (StatusRuntimeException e) {
      return "Could not get a response for command: " + command + ", wait: " + wait;
    }
  }

  public void stop() {
    channel.shutdown();
  }

  @Override
  public String toString() {
    return String.format("Client (host: %s, port: %s)", host, port);
  }


  public static void main(String[] args) {

    com.twitter.captainahab.client.CaptainAhabClient client = new com.twitter.captainahab.client.CaptainAhabClient();
    client.start(args[0], Integer.parseInt(args[1]));

    Scanner input = new Scanner(System.in);

    int i = 0;
    while (input.hasNext()) {
      String cmd = input.nextLine();

      if (cmd.equals("stop")) {
        break;
      }

      String res;
      if (i % 2 == 0) {
        System.out.println("About to apply sync command: " + cmd);
        res = client.applyCommand(cmd, true);
      } else {
        System.out.println("About to apply async command: " + cmd);
        res = client.applyCommand(cmd, false);
      }
      i++;
      System.out.println("The result was: " + res);
    }

    client.stop();
  }
}
