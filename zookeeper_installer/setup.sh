#!/usr/bin/env bash

USERNAME=''
FILE_TO_COPY=./zookeeper.tar

# get zookeeper
wget https://archive.apache.org/dist/zookeeper/zookeeper-3.5.5/apache-zookeeper-3.5.5.tar.gz
gunzip apache-zookeeper-3.5.5.tar.gz
tar -xvf apache-zookeeper-3.5.5.tar
rm apache-zookeeper-3.5.5.tar
mv apache-zookeeper-3.5.5 zookeeper
tar -cvf zookeeper.tar zookeeper
rm -rf zookeeper


# transfer zookeeper directory to servers
j=0
l=1
for i do
  if [ $j -eq 0 ]; then
    USERNAME=$i
    j=$((j+1))
    continue
  fi
  echo $i
  scp -r -C $FILE_TO_COPY $USERNAME@$i:/tmp/
  
  # install java
  ssh $USERNAME@$i 'sudo apt-get update'
  ssh $USERNAME@$i 'sudo apt-get -y upgrade'
  ssh $USERNAME@$i 'sudo apt install -y openjdk-11-jdk openjdk-11-jre-headless ant'

  # start the server
  ssh $USERNAME@$i 'sudo rm -rf /tmp/zookeeper'
  ssh $USERNAME@$i 'sudo rm -rf /tmp/comman*'
  ssh $USERNAME@$i 'sudo pkill -9 java'
  ssh $USERNAME@$i 'tar -xvf /tmp/zookeeper.tar -C /tmp'
  ssh $USERNAME@$i 'mkdir /tmp/zookeeper/data'

  echo $l > /tmp/myid
  scp /tmp/myid $USERNAME@$i:/tmp/zookeeper/data
  
  scp zoo.cfg $USERNAME@$i:/tmp/zookeeper
  ssh $USERNAME@$i 'sudo pkill -9 java'
  ssh $USERNAME@$i '(cd /tmp/zookeeper; ant compile)'
  ssh $USERNAME@$i '/tmp/zookeeper/bin/zkServer.sh start /tmp/zookeeper/zoo.cfg >/tmp/zookeeper.log &'

  sed -e "s/\${id}/$l/" server.sh > server_c.sh
  scp server_c.sh $USERNAME@$i:/tmp
  ssh $USERNAME@$i 'chmod +x /tmp/server_c.sh'
  ssh $USERNAME@$i 'bash -s' < server_c.sh
  rm server_c.sh
  l=$((l+1))
done

