package com.twitter.captainahabrunner.configuration;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.esotericsoftware.yamlbeans.YamlReader;

public class ConfigurationReader {

  private ExecutionConfiguration configuration;

  public ConfigurationReader(String fileName) {

    YamlReader reader = null;
    try {
      reader = new YamlReader(new FileReader(fileName));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    try {
      configuration = reader.read(ExecutionConfiguration.class);

      for (ClientData client: configuration.getClients()) {
        List<String> validOperations = Arrays.asList("read", "write",
            "sync_read", "CAS", "reconfig");

        for (String operation: client.getOperations()) {
          if (!validOperations.contains(operation)) {
            throw new IllegalArgumentException("The client is performing an illegal operation: " +
                operation);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getExecutionName() {
    return configuration.getExecutionName();
  }

  public Set<ServerTriple> getServers() {
    Set<ServerTriple> set = new HashSet<>();
    for (ServerTriple server: configuration.getServers()) {
      set.add(server);
    }
    return set;
  }

  public int getPartitionEveryInSeconds() {
    return configuration.getPartitionEveryInSeconds();
  }

  public int getPartitionDurationInSeconds() {
    return configuration.getPartitionDurationInSeconds();
  }

  public String getUsername() {
    return configuration.getUsername();
  }

  public List<ClientData> getClients() {
    return configuration.getClients();
  }

  public int getTotalOperationsPerClient() {
    return configuration.getTotalOperationsPerClient();
  }

}
