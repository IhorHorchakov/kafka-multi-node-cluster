**Kafka multi-node cluster setup using docker compose**

## Intro
This project is created to play with Kafka and Docker configuration, debug and learn more about Kafka internals.


For proof-of-concept or non-critical development work a single-node cluster works fine. However, having multi-node has many benefits:

* **Scalability**: Kafka is designed to handle large volumes of data, and a single broker may not be enough to handle the load. By adding more brokers to the cluster, we can distribute the load across multiple machines and increase the overall capacity of the system.
* **High availability**: Having multiple brokers in a Kafka cluster provides fault tolerance and high availability. If one broker goes down, the other brokers can continue to service requests and maintain the continuity of the data stream.
* **Replication**: Kafka replicates data across multiple brokers to ensure that data is not lost in the event of a broker failure. With multiple brokers, we can set a replication factor of more than one, which means that multiple copies of each message are stored across different brokers. This way, if one broker goes down, the data can still be retrieved from another broker.
* **Geographical distribution**: If we want to have Kafka brokers in different geographical locations, we can set up a multi-broker cluster to handle data replication and ensure that data is available even if one location goes down.

Management of the brokers in the cluster is performed by Zookeeper. There may be multiple Zookeepers in a cluster, in fact the recommendation is **three to five**, keeping an odd number so that there is always a majority and the number as low as possible to conserve overhead resources.

In this project I've created setup with 1 Zookeeper and 3 Kafka Brokers. A single topic `test-topic` is created with 4 partitions and 3 replicas.

![The topology of cluster](https://github.com/IhorHorchakov/kafka-multi-node-cluster/blob/main/img/kafka-cluster.png?raw=true)

## Theory

**Kafka Topic**

A stream of messages that are a part of a specific category or feed name is referred to as a Kafka topic. In Kafka, data is stored in the form of topics. Producers write their data to topics, and consumers read the data from these topics.

The topic is a distributed commit log to which records append and stored. Kafka topics are multi-subscriber. Records published to the cluster stay in the cluster(topic) until a configurable _retention period_ has passed by.
Kafka stores records in the topic making the consumers responsible for tracking the position in the log, known as the “offset”. Typically, a consumer advances the offset in a linear manner as messages are read. However, the position is actually controlled by the consumer, which can consume messages in any order. For example, a consumer can reset to an older offset when reprocessing records.


**Parallel processing and Fail-over**

Kafka breaks topic into partitions. A record is stored on a partition usually by record key if the key is present and round-robin if the key is missing (default behavior). The record key, by default, determines which partition a producer sends the record.

Kafka uses partitions to scale a topic across many servers for producer writes. Also, Kafka uses partitions to facilitate **parallel consumers**. Consumers consume records in parallel up to the number of partitions.

The order guaranteed per partition. If partitioning by key then all records for the key will be on the same partition which is useful if you ever have to replay the log. Kafka can replicate partitions to multiple brokers for **fail-over**.

![Topic Partition Layout and Offsets](https://github.com/IhorHorchakov/kafka-multi-node-cluster/blob/main/img/kafka-topic-partition-layout-offsets.png?raw=true)

**Replication, Fault tolerance, In-Sync Replicas**

In Kafka, replication is implemented at the partition level. The redundant unit of a partition is called a replica. 

Kafka can replicate partitions across a configurable number of Kafka servers which is used for **fault tolerance**. Fault tolerance is a property of a system to make data available even in the case of some failures. 

Each partition has a leader broker and zero or more follower brokers. Leaders handle all read and write requests for a partition.
If the lead broker fails, one of the follower ISR brokers becomes the leader by default. You should strive to have a good balance of leaders so each broker is a leader of an equal amount of partitions to distribute the load.
We can specify a number of all partition replicas (leader replica + follower replicas) that has to be created for the topic using the **Replication factor** property.

Followers replicate data from the leader to themselves by sending Fetch Requests periodically, by default every 500ms. That's why some replicas are fully caught up / synchronized with the leader(**in-sync replicas**), and some replicas are not synchronized. In-sync replicas(ISR) will consist of the leader replica and any additional follower replicas that are also considered in-sync.
In other words, the ISR is a group of stable replicas that didn't have lags and were in-sync with leader replica during some fixed period of time `replica.lag.time.max.ms` (10 seconds by default)

The ISR acts as a tradeoff between availability and latency. As a producer, if we don't want to lose a message, we'd make sure that the message has been replicated to all replicas before receiving an acknowledgment. But this is problematic as the loss or slowdown of a single replica could cause a partition to become unavailable or add extremely high latencies. So the goal to be able to tolerate one or more replicas being lost or being very slow.

![Topic metadata by Kafdrop](https://github.com/IhorHorchakov/kafka-multi-node-cluster/blob/main/img/kafdrop-topic-metadata.png?raw=true)


**Producers, acknowledgments**

The producers send data directly to the broker that plays the role of leader for a given partition. In order to help the producer send the messages directly, 
the nodes of the Kafka cluster answer requests for metadata on which servers are alive and the current status of the leaders of partitions of a topic so that the producer can direct its requests accordingly. 
The client decides which partition it publishes its messages to. This can either be done arbitrarily or by making use of a partitioning key,
where all messages containing the same partition key will be sent to the same partition.

Messages in Kafka are sent in the form of batches, known as record batches. The producers accumulate messages in memory and send them in batches either after a 
fixed number of messages are accumulated or before a fixed latency bound period of time has elapsed.

Kafka uses an asynchronous publish/subscribe model. When your producer calls the send() command, the result returned is a future. If you do not use a future, you could get just one record, wait for the result, and then send a response. Latency is very low, but so is throughput.

When you use Producer.send():
* the producer passes the message to a configured list of **Interceptors**,
* **Serializers** convert record key and value to byte arrays,
* default or configured **Partitioner** calculates topic partition if none is specified,
* the **Record Accumulator** appends the message to producer batches using a configured compression algorithm. Batching is mainly controlled by two producer settings - `linger.ms` and `batch.size`. If a batch reaches its maximum size before the end of the linger.ms period, it will be sent to Kafka right away!

At this point, if a message size is less then `batch.size`, the message is still in memory and not sent to the Kafka broker. When a buffer is full, the sender thread publishes the buffer to the Kafka broker and begins to refill the buffer.

![Kafka producer internals](https://github.com/IhorHorchakov/kafka-multi-node-cluster/blob/main/img/kafka-producer-internals.png?raw=true)


A common error when publishing records is setting the same key or null key for all records, which results in all records ending up in the same partition and you get an unbalanced topic.

An acknowledgment (`acks`) is a signal passed between communicating processes to signify acknowledgment, i.e., receipt of the message sent. The ack-value is a producer configuration parameter in Apache Kafka and can be set to the following values:

* acks=0 The producer never waits for an acknowledgment from the broker. No guarantee can be made that the broker has received the message. This setting provides lower latency and higher throughput at the cost of much higher risk of message loss.
* acks=1 The producer gets an ack after the leader has received the record and respond without awaiting a full acknowledgment from all followers. The message will be lost only if the leader fails immediately after acknowledging the record, but before the followers have replicated it. This setting is the middle ground for latency, throughput, and durability. It is slower but more durable than acks=0.
* acks=all The producer gets an ack when all in-sync replicas have received the record. The leader will wait for the full set of in-sync replicas to acknowledge the record. This means that it takes a longer time to send a message with ack value all, but it gives the strongest message durability.


**Consumers, consumer groups, fail-over**

The consumer has to send requests to the brokers indicating the partitions it wants to consume. The consumer is required to specify its offset in the request 
and receives a chunk of log beginning from the offset position from the broker. Since the consumer has control over this position, it can re-consume data if required. 
Records remain in the log for a configurable time period which is known as the retention period. The consumer may re-consume the data as long as the data is present in the log.

In Kafka, the consumers work on a pull-based approach. This means that data is not immediately pushed onto the consumers from the brokers. The consumers have to send requests to the brokers to indicate 
that they are ready to consume the data. A pull-based system ensures that the consumer does not get overwhelmed with messages and can fall behind and catch up when it can. 
A pull-based system can also allow aggressive batching of data sent to the consumer since the consumer will pull all available messages after its current position in the log. 
In this manner, batching is performed without any unnecessary latency.

A consumer group is a group of consumers that share the same group id. Consumers in the same consumer group split the partitions among them. This way you can ensure parallel processing of records from a topic.
When a new consumer is started it will join a consumer group (this happens under the hood) and Kafka will then ensure that each partition is consumed by only one consumer from that group.
So, if you have a topic with two partitions and only one consumer in a group, that consumer would consume records from both partitions.
After another consumer joins the same group, each consumer would continue consuming only one partition.

If you have more consumers in a group than you have partitions, extra consumers will sit idle, since all the partitions are taken. If you know that you will need many consumers to parallelize the processing, then plan accordingly with the number of partitions.

If there is a need to consume the same record from multiple consumers, it is possible as long as that consumers have different groups.

_Fail-over behaviour:_

Consumers notify the Kafka broker when they have successfully processed a record, which advances the offset.

If a consumer fails before sending commit offset to Kafka broker, then a different consumer can continue from the last committed offset.

If a consumer fails after processing the record but before sending the commit to the broker, then some Kafka records could be reprocessed. In this scenario, Kafka implements the at least once behavior, and you should make sure the messages (record deliveries ) are idempotent.


### When Kafka looses data ?
* When asks = 1 and a broker with leader replica is getting break down before record-commit
* When asks = all and broker with leader replica fails and no in-sync replicas present to take a leadership

### Useful links

https://www.cloudkarafka.com/blog/part1-kafka-for-beginners-what-is-apache-kafka.html

http://cloudurable.com/blog/kafka-architecture-topics/index.html

https://howtodoinjava.com/kafka/apache-kafka-tutorial

https://blog.developer.adobe.com/exploring-kafka-producers-internals-37411b647d0f

https://www.confluent.io/blog/kafka-client-cannot-connect-to-broker-on-aws-on-docker-etc

https://docs.cloudera.com/documentation/kafka/1-4-x/topics/kafka_performance.html