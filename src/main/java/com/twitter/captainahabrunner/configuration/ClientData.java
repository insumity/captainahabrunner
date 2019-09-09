package com.twitter.captainahabrunner.configuration;

import java.util.List;

public class ClientData {
  private List<String> operations;
  private boolean connectToOne;

  public ClientData() {

  }

  public List<String> getOperations() {
    return operations;
  }

  public void setOperations(List<String> operations) {
    this.operations = operations;
  }

  public boolean isConnectToOne() {
    return connectToOne;
  }

  public void setConnectToOne(boolean connectToOne) {
    this.connectToOne = connectToOne;
  }

  @Override
  public String toString() {
    return String.format("operations: %s, connectToOne: %b", operations, connectToOne);
  }
}
