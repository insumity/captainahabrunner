package com.twitter.captainahabrunner.utilities;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class Utilities {

  public static class ObserversParticipantsPair {
    private List<Integer> observers, participants;

    public ObserversParticipantsPair() {
      observers = new LinkedList<>();
      participants = new LinkedList<>();
    }

    public void addObserver(int observer) {
      observers.add(observer);
    }

    public void addParticipant(int participant) {
      participants.add(participant);
    }

    public List<Integer> getObservers() {
      return observers;
    }

    public List<Integer> getParticipants() {
      return participants;
    }

    @Override
    public String toString() {
      return String.format("[pair: (observers: %s), (participants: %s)]", observers, participants);
    }
  }

  public static String serversToAddToString(ObserversParticipantsPair addPair,
                                            Map<Integer, String> idToZKString) {
//    // FIXME to
//    StringBuffer serversToAddStr1 = new StringBuffer();
//    for (Integer server: addPair.getParticipants()) {
//      serversToAddStr1.append(idToZKString.get(server));
//      serversToAddStr1.append(",");
//    }
//    // remove last comma
//    serversToAddStr1.deleteCharAt(serversToAddStr1.length() - 1);
//    serversToAddStr1.toString().replaceAll("observer", "participant");
//
//    StringBuffer serversToAddStr2 = new StringBuffer();
//    for (Integer server: addPair.getObservers()) {
//      serversToAddStr2.append(idToZKString.get(server));
//      serversToAddStr2.append(",");
//    }
//    // remove last comma
//    serversToAddStr2.deleteCharAt(serversToAddStr2.length() - 1);
//    serversToAddStr2.toString().replaceAll("participant", "observer");
//
//    if (!serversToAddStr1.toString().isEmpty() && !serversToAddStr2.toString().isEmpty()) {
//      return serversToAddStr1.append(',').append(serversToAddStr2).toString();
//    }
//    else if (!serversToAddStr1.toString().isEmpty() && serversToAddStr2.toString().isEmpty()) {
//      return serversToAddStr1.toString();
//    }
//    else if (serversToAddStr1.toString().isEmpty() && !serversToAddStr2.toString().isEmpty()) {
//      return serversToAddStr2.toString();
//    }
//    else {
//      return "";
//    }


    List<Integer> serversToAdd = new LinkedList<>();
    serversToAdd.addAll(addPair.getParticipants()); // FIXME
    serversToAdd.addAll(addPair.getObservers());


    StringBuffer serversToAddStr = new StringBuffer();
    for (Integer server: serversToAdd) {
      serversToAddStr.append(idToZKString.get(server));
      serversToAddStr.append(",");
    }
    // remove last comma
    serversToAddStr.deleteCharAt(serversToAddStr.length() - 1);

    return serversToAddStr.toString().replaceAll("observer", "participant");
  }

  public static String serversToRemoveToString(ObserversParticipantsPair removePair) {
    List<Integer> serversToRemove = new LinkedList<>();
    serversToRemove.addAll(removePair.getParticipants());
    serversToRemove.addAll(removePair.getObservers());

    StringBuffer serversToRemoveStr = new StringBuffer();
    for (Integer server: serversToRemove) {
      serversToRemoveStr.append(server);
      serversToRemoveStr.append(",");
    }
    // remove last comma
    serversToRemoveStr.deleteCharAt(serversToRemoveStr.length() - 1);

    return serversToRemoveStr.toString();
  }

  private static Properties load(String config) {
    final Properties p = new Properties();
    try {
      p.load(new StringReader(config));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return p;
  }

  public static long getVersion(String config) {
    Properties p = load(config);
    String s = p.getProperty("version");
    return Long.decode(String.format("0x%s", s));
  }

  public static List<Integer> getServers(String config, boolean areParticipants) {

    List<Integer> servers = new LinkedList<>();

    Properties p = load(config);
    Enumeration<String> enums = (Enumeration<String>) p.propertyNames();
    while (enums.hasMoreElements()) {
      String key = enums.nextElement();
      if (key.contains("server.")) {
        String[] components = key.split("\\.");
        int id = Integer.valueOf(components[1]);

        if (areParticipants && p.getProperty(key).contains("participant")) {
            servers.add(id);
        }
        else if (!areParticipants && p.getProperty(key).contains("observer")) {
          servers.add(id);
        }
      }
    }

    return servers;
  }

  public static ObserversParticipantsPair getRandomServersToRemove(List<Integer> observers,
                                                                   List<Integer> participants) {
    ObserversParticipantsPair result = new ObserversParticipantsPair();

    int participantsNumber = participants.size();
    int maxParticipantsToRemove = (participantsNumber - 1) / 2;
    Random r = new Random();

    // in case of 2 participants, do not remove any
    if (maxParticipantsToRemove != 0) {
      int participantsToRemove = r.nextInt(maxParticipantsToRemove) + 1;
      for (int i = 0; i < participantsToRemove; ++i) {
        int part = participants.get(i);
        result.addParticipant(part);
      }
    }

    // remove observers only if we have at least 1
    if (observers.size() >= 1) {
      int observersToRemove = r.nextInt(observers.size());
      for (int i = 0; i < observersToRemove; ++i) {
        int obs = observers.get(i);
        result.addObserver(obs);
      }
    }

    return result;
  }

  public static void main(String[] args) {
    List<Integer> observers = Arrays.asList(new Integer[]{4, 5, 6});
    List<Integer> participants = Arrays.asList(new Integer[]{1, 2, 3});
    List<Integer> allServers = Arrays.asList(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11});

    System.out.println(getRandomServersToAdd(observers, participants, allServers));
  }

  public static ObserversParticipantsPair getRandomServersToAdd(List<Integer> observers,
                                                                List<Integer> participants,
                                                                List<Integer> allServers) {

    ObserversParticipantsPair result = new ObserversParticipantsPair();


    LinkedList<Integer> currentServers = new LinkedList<>(observers);
    currentServers.addAll(participants);
    Set<Integer> notUsedServers = new HashSet<>(allServers);
    notUsedServers.removeAll(new HashSet<>(currentServers));
    List<Integer> notUsedServersList = new LinkedList<>(notUsedServers);

    Random r = new Random();
    if (notUsedServers.isEmpty()) {
      return result;
    }

    int participantsToAdd = r.nextInt(notUsedServers.size());
    for (int i = 0; i < participantsToAdd; ++i) {
      int part = notUsedServersList.get(i); // FIXME: should be remove!
      result.addParticipant(part);
    }

    if (notUsedServersList.size() != 0) {
      int observersToAdd = r.nextInt(notUsedServersList.size());

      for (int i = 0; i < observersToAdd; ++i) {
        int obs = notUsedServersList.remove(0);
        result.addObserver(obs);
      }
    }

    return result;
  }

}
