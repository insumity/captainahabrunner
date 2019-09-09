package com.twitter.captainahabrunner.configuration;

public class ServerTriple {
  private String host;
  private int port;
  private int id;

  public ServerTriple() {

  }

  public ServerTriple(String host, int port, int id) {
    this.host = host;
    this.port = port;
    this.id = id;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return String.format("%d", id);
  }
}
