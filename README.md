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

Environment properties used in the configuration:

KAFKA_BROKER_ID – The broker.id property is the unique and permanent name of each node in the cluster.

KAFKA_AUTO_CREATE_TOPICS_ENABLE – If the value is true then it allows brokers to create topics when they’re first referenced by the producer or consumer. If the value is set to false, the topic should be first created using the Kafka command and then used.

KAFKA_ZOOKEEPER_CONNECT – instructs Kafka how to contact Zookeeper.

KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR – is required when we are running with a single-node cluster. If you have three or more nodes, we can use the default.

KAFKA_LISTENER_SECURITY_PROTOCOL_MAP – defines key/value pairs for the security protocol to use per listener name.

KAFKA_ADVERTISED_LISTENERS – makes Kafka accessible from outside the container by advertising its location on the Docker host.


It is essential to ensure that service names and the KAFKA_BROKER_ID are distinct for each service. Additionally, each service should have a unique port exposed to the host machine. For instance, while zookeeper listens port 2181, it is exposed to the host through ports 22181, respectively. Similarly, the broker-1, broker-2, and broker-3 services are listening ports 19092, 19093, and 19094, respectively.

-----
Useful links:

https://howtodoinjava.com/kafka/apache-kafka-tutorial/

https://www.confluent.io/blog/kafka-client-cannot-connect-to-broker-on-aws-on-docker-etc/