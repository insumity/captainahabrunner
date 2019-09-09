# CaptainAhab Runner

CaptainAhab runner checks the consistency guarantees of ZooKeeper.
To run in a cluster of servers `S`, first ZooKeeper must be installed on all the servers in `S`. This can be done by using the scripts in [zookeeper_installer](zookeeper_installer).
Make sure to change the IPs in [start.sh](zookeeper_installer/start.sh), as well as in [zoo.cfg](zookeeper_installer/zoo.cfg). You can also add more servers depending on how many servers are in your cluster `S`. Also note that you need to be able to SSH to the servers in `S` just by using `ssh username@host`. For this, you most likely would have to use `ssh-add`. 

Then you need to create the executable JAR for the captain ahab runner. This can be easily done by using IntelliJ IDEA. 
- Go to `Project Structure` - `Modules` - `+` - `JAR or directories...` and add the non-executable [CaptainAhab](https://github.com/insumity/captainahab) JAR.
- Go to `Project Structure` - `Artifacts` - `+` - `JAR` - `From module with dependencies...` and do as seen below:
![creating_artifact](../media/creating_artifact.png)
- Go `Build` - `Build Artifacts...` - `Build`

The above steps generate the `captainahabrunner.jar`. You can then just say `java -jar captainahabrunner.jar configuration.yaml` from a server `s'` not in `S`, to start a ZooKeeper cluster with clients, while at the same time CaptainAhab is injecting network partitions. The configuration [file](configuration.yaml) can be used to configure how many clients we want to access the ZooKeeper cluster at the sasme time and how many network partitions are going to take place, as well as how often.

For example, for the configuration of clients below:

```clients:
  - operations: [read]
    connectToOne: true
  - operations: [read, write, sync_read, CAS, reconfig]
    connectToOne: true
```
we have 2 clients. The 2 clients apply different operations. The first client only applies `read` operations, while the second client performs as well `write`, `sync_read`, `CAS`, and `reconfig` operations. 
Operation `sync_read` corresponds to a combination of a `sync` followed a by a `read`. A `reconfig` operation will randomly reconfigure the ZooKeeper cluster by transforming observers to participants or vice versa, as well as by removing observers or participants. 

Naturally, the IPs in the configuration file need to be changed as well. 
At the end of the execution, ZooKeeper log files as well as the clients' logs are accumulated in the `/tmp/` directory, under `executionName` directory. The generated `log_results_client_*` files can be used to test the consistency guarantees of ZooKeeper. They are in appropriate format to be passed in [Knossos](https://github.com/jepsen-io/knossos) for this.

