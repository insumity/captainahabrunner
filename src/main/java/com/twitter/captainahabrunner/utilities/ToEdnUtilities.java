package com.twitter.captainahabrunner.utilities;

public class ToEdnUtilities {

  public static String getWriteEdn(long time, boolean isInvocation, int client,  int value) {
    if (isInvocation) {
      return String.format("%d, {:process %d, :type :invoke, :f :write, :value %d}",
          time, client, value);
    }
    else {
      return String.format("%d, {:process %d, :type :ok, :f :write, :value %d}",
          time, client, value);
    }
  }

  public static String getWriteEdnINFO(long time, int client, int value) {
      return String.format("%d, {:process %d, :type :info, :f :write, :value %d}",
          time, client, value);
  }

  public static String getReadEdn(long time, boolean isInvocation, int client, int value) {
    if (isInvocation) {
      return String.format("%d, {:process %d, :type :invoke, :f :read, :value nil}", time, client);
    }
    else {
      return String.format("%d, {:process %d, :type :ok, :f :read, :value %d}",
          time, client, value);
    }
  }

  public static String getReadEdnINFO(long time, int client, int value) {
    return String.format("%d, {:process %d, :type :info, :f :read, :value %d}",
        time, client, value);
  }

  public static String getCASEdn(long time, boolean isInvocation, int client, boolean result,
                                 int value, int oldValue) {
    if (isInvocation) {
      return String.format("%d, {:process %d, :type :invoke, :f :cas, :value [%d %d]}", time,
          client, oldValue, value);
    }
    else {
      return String.format("%d, {:process %d, :type :%s, :f :cas, :value [%d %d]}",
          time, client, result? "ok": "fail", oldValue, value);
    }
  }


  public static String getCASEdnINFO(long time, int client, int value, int oldValue) {
      return String.format("%d, {:process %d, :type :info, :f :cas, :value [%d %d]}", time, client,
          oldValue, value);

  }
}
