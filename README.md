**Kafka multi-node cluster setup using docker compose**

For proof-of-concept or non-critical development work, a single-node cluster works fine. However, having multi-node has many benefits:

* **Scalability**: Kafka is designed to handle large volumes of data, and a single broker may not be enough to handle the load. By adding more brokers to the cluster, we can distribute the load across multiple machines and increase the overall capacity of the system.
* **High availability**: Having multiple brokers in a Kafka cluster provides fault tolerance and high availability. If one broker goes down, the other brokers can continue to service requests and maintain the continuity of the data stream.
* **Replication**: Kafka replicates data across multiple brokers to ensure that data is not lost in the event of a broker failure. With multiple brokers, we can set a replication factor of more than one, which means that multiple copies of each message are stored across different brokers. This way, if one broker goes down, the data can still be retrieved from another broker.
* **Geographical distribution**: If we want to have Kafka brokers in different geographical locations, we can set up a multi-broker cluster to handle data replication and ensure that data is available even if one location goes down.

Management of the brokers in the cluster is performed by Zookeeper. There may be multiple Zookeepers in a cluster, in fact the recommendation is **three to five**, keeping an odd number so that there is always a majority and the number as low as possible to conserve overhead resources.

In this project I've created setup with 1 Zookeeper and 3 Kafka Brokers:

![The topology of cluster](https://github.com/IhorHorchakov/kafka-multi-node-cluster/blob/main/img/kafka-cluster.png?raw=true)

-----
**Kafka Topic**

A Topic is distributed commit log to which records append and stored. Kafka topics are multi-subscriber. Records published to the cluster stay in the cluster(topic) until a configurable _retention period_ has passed by.

Kafka stores records in the topic, making the consumers responsible for tracking the position in the log, known as the “offset”. Typically, a consumer advances the offset in a linear manner as messages are read. However, the position is actually controlled by the consumer, which can consume messages in any order. For example, a consumer can reset to an older offset when reprocessing records.

-----
**Failover, Parallel processing**

Kafka breaks topic into partitions. A record is stored on a partition usually by record key if the key is present and round-robin if the key is missing (default behavior). The record key, by default, determines which partition a producer sends the record.

Kafka uses partitions to scale a topic across many servers for producer writes. Also, Kafka uses partitions to facilitate **parallel consumers**. Consumers consume records in parallel up to the number of partitions.

The order guaranteed per partition. If partitioning by key then all records for the key will be on the same partition which is useful if you ever have to replay the log. Kafka can replicate partitions to multiple brokers for **failover**.

![Topic Partition Layout and Offsets](https://github.com/IhorHorchakov/kafka-multi-node-cluster/blob/main/img/kafka-topic-partition-layout-offsets.png?raw=true)


-----
**Replication, Fault tolerance, In-Sync Replicas**

In Kafka, replication is implemented at the partition level. The redundant unit of a partition is called a replica. 

Kafka can replicate partitions across a configurable number of Kafka servers which is used for **fault tolerance**. Fault tolerance is a property of a system to make data available even in the case of some failures. 

Each partition has a leader broker and zero or more follower brokers. Leaders handle all read and write requests for a partition.
If the lead broker fails, one of the follower ISR brokers becomes the leader by default. You should strive to have a good balance of leaders so each broker is a leader of an equal amount of partitions to distribute the load.
We can specify a number of all partition replicas (leader replica + follower replicas) that has to be created for the topic using the **Replication factor** property.

Followers replicate data from the leader to themselves by sending Fetch Requests periodically, by default every 500ms. That's why some replicas are fully caught up / synchronized with the leader(**in-sync replicas**), and some replicas are not synchronized. In-sync replicas(ISR) will consist of the leader replica and any additional follower replicas that are also considered in-sync.
In other words, the ISR is a group of stable replicas that didn't have lags and were in-sync with leader replica during some fixed period of time `replica.lag.time.max.ms` (10 seconds by default)

The ISR acts as a tradeoff between availability and latency. As a producer, if we don't want to lose a message, we'd make sure that the message has been replicated to all replicas before receiving an acknowledgment. But this is problematic as the loss or slowdown of a single replica could cause a partition to become unavailable or add extremely high latencies. So the goal to be able to tolerate one or more replicas being lost or being very slow.

-----
**Producers, ack-value**

Kafka uses an asynchronous publish/subscribe model. When your producer calls the send() command, the result returned is a future. If you do not use a future, you could get just one record, wait for the result, and then send a response. Latency is very low, but so is throughput.
When you use Producer.send(), you fill up buffers on the producer. When a buffer is full, the producer sends the buffer to the Kafka broker and begins to refill the buffer.

When a producer publishes a record to a topic, it is published to its leader. The leader appends the record to its commit log and increments its record offset. Kafka only exposes a record to a consumer after it has been “committed”. And the record is considered “committed” depending on the `ACK` property.

A producer must know which partition to write to, this is not up to the broker. It's possible for the producer to attach a key to the record dictating the partition the record should go to. All records with the same key will arrive at the same partition. Before a producer can send any records, it has to request metadata about the cluster from the broker. The metadata contains information on which broker is the leader for each partition and a producer always writes to the partition leader. The producer then uses the key to know which partition to write to, the default implementation is to use the hash of the key to calculate partition, you can also skip this step and specify partition yourself.

A common error when publishing records is setting the same key or null key for all records, which results in all records ending up in the same partition and you get an unbalanced topic.

An acknowledgment (`ACK`) is a signal passed between communicating processes to signify acknowledgment, i.e., receipt of the message sent. The ack-value is a producer configuration parameter in Apache Kafka and can be set to the following values:

* acks=0 The producer never waits for an acknowledgment from the broker. No guarantee can be made that the broker has received the message. This setting provides lower latency and higher throughput at the cost of much higher risk of message loss.
* acks=1 The producer gets an ack after the leader has received the record and respond without awaiting a full acknowledgment from all followers. The message will be lost only if the leader fails immediately after acknowledging the record, but before the followers have replicated it. This setting is the middle ground for latency, throughput, and durability. It is slower but more durable than acks=0.
* acks=all The producer gets an ack when all in-sync replicas have received the record. The leader will wait for the full set of in-sync replicas to acknowledge the record. This means that it takes a longer time to send a message with ack value all, but it gives the strongest message durability.

-----
**Consumers, consumer groups, fail-over**
A consumer group is a group of consumers that share the same group id. When a topic is consumed by consumers in the same group, every record will be delivered to only one consumer. This way you can ensure parallel processing of records from a topic.
When a new consumer is started it will join a consumer group (this happens under the hood) and Kafka will then ensure that each partition is consumed by only one consumer from that group.

So, if you have a topic with two partitions and only one consumer in a group, that consumer would consume records from both partitions.
After another consumer joins the same group, each consumer would continue consuming only one partition.

If you have more consumers in a group than you have partitions, extra consumers will sit idle, since all the partitions are taken. If you know that you will need many consumers to parallelize the processing, then plan accordingly with the number of partitions.

If there is a need to consume the same record from multiple consumers, it is possible as long as that consumers have different groups.

fail-over:

Consumers notify the Kafka broker when they have successfully processed a record, which advances the offset.

If a consumer fails before sending commit offset to Kafka broker, then a different consumer can continue from the last committed offset.

If a consumer fails after processing the record but before sending the commit to the broker, then some Kafka records could be reprocessed. In this scenario, Kafka implements the at least once behavior, and you should make sure the messages (record deliveries ) are idempotent.

-----
Tuning performance - latency and throughput 

https://docs.cloudera.com/documentation/kafka/1-4-x/topics/kafka_performance.html

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
https://www.cloudkarafka.com/blog/part1-kafka-for-beginners-what-is-apache-kafka.html

http://cloudurable.com/blog/kafka-architecture-topics/index.html

https://howtodoinjava.com/kafka/apache-kafka-tutorial/

https://www.confluent.io/blog/kafka-client-cannot-connect-to-broker-on-aws-on-docker-etc/