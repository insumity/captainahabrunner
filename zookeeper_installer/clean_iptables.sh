#!/usr/bin/env bash

USERNAME=''

j=0
for i do
  if [ $j -eq 0 ]; then
    USERNAME=$i
    j=$((j+1))
    continue
  fi
  echo $i
  
  ssh $USERNAME@$i 'sudo iptables -F'
done

