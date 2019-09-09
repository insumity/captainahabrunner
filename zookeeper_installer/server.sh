#!/usr/bin/env bash

mkdir /tmp/zookeeper/data

echo ${id} > /tmp/zookeeper/data/myid
sudo pkill -9 java
/tmp/zookeeper/bin/zkServer.sh start /tmp/zookeeper/zoo.cfg
