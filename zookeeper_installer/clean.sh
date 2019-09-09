#!/usr/bin/env bash

USERNAME=''

j=0
for i do
  if [ $j -eq 0 ]; then
    USERNAME=$i
    j=$((j + 1))
    continue
  fi

  # start the server
  ssh $USERNAME@$i 'sudo rm -rf /tmp/command*.sh'
  ssh $USERNAME@$i 'sudo rm -rf /tmp/zookeeper2'
  ssh $USERNAME@$i 'sudo pkill -9 java'
done

