**Kafka multi-node cluster setup using docker compose**

For proof-of-concept or non-critical development work, a single-node cluster works fine. However, having multi-node has many benefits:

* **Scalability**: Kafka is designed to handle large volumes of data, and a single broker may not be enough to handle the load. By adding more brokers to the cluster, we can distribute the load across multiple machines and increase the overall capacity of the system.
* **High availability**: Having multiple brokers in a Kafka cluster provides fault tolerance and high availability. If one broker goes down, the other brokers can continue to service requests and maintain the continuity of the data stream.
* **Replication**: Kafka replicates data across multiple brokers to ensure that data is not lost in the event of a broker failure. With multiple brokers, we can set a replication factor of more than one, which means that multiple copies of each message are stored across different brokers. This way, if one broker goes down, the data can still be retrieved from another broker.
* **Geographical distribution**: If we want to have Kafka brokers in different geographical locations, we can set up a multi-broker cluster to handle data replication and ensure that data is available even if one location goes down.

In real-world scenarios we will need more resilient setup to have redundancy for both the zookeeper servers and the Kafka brokers. 
In this project I've created setup with 1 Zookeeper and 3 Kafka Brokers:

![The topology of cluster](https://github.com/IhorHorchakov/kafka-multi-node-cluster/blob/main/img/kafka-cluster.png?raw=true)

-----
Useful links:

https://howtodoinjava.com/kafka/apache-kafka-tutorial/

https://www.confluent.io/blog/kafka-client-cannot-connect-to-broker-on-aws-on-docker-etc/