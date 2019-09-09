package com.twitter.captainahabrunner.configuration;

import java.util.List;

public class ExecutionConfiguration {
  private String executionName;
  private List<ClientData> clients;
  private int partitionEveryInSeconds;
  private int totalOperationsPerClient;
  private int partitionDurationInSeconds;
  private List<ServerTriple> servers;
  private String username;


  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public List<ServerTriple> getServers() {
    return servers;
  }

  public void setServers(List<ServerTriple> servers) {
    this.servers = servers;
  }

  public int getTotalOperationsPerClient() {
    return totalOperationsPerClient;
  }

  public void setTotalOperationsPerClient(int totalOperationsPerClient) {
    this.totalOperationsPerClient = totalOperationsPerClient;
  }


  public int getPartitionEveryInSeconds() {
    return partitionEveryInSeconds;
  }

  public void setPartitionEveryInSeconds(int partitionEveryInSeconds) {
    this.partitionEveryInSeconds = partitionEveryInSeconds;
  }

  public List<ClientData> getClients() {
    return clients;
  }

  public void setClients(List<ClientData> clients) {
    this.clients = clients;
  }

  public int getPartitionDurationInSeconds() {
    return partitionDurationInSeconds;
  }

  public void setPartitionDurationInSeconds(int partitionDurationInSeconds) {
    this.partitionDurationInSeconds = partitionDurationInSeconds;
  }

  public void setExecutionName(String executionName) {
    this.executionName = executionName;
  }

  public String getExecutionName() {
    return executionName;
  }
}
