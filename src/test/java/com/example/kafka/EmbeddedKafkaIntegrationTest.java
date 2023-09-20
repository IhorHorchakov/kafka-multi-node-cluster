package com.example.kafka;

import com.example.kafka.consumer.KafkaConsumer;
import com.example.kafka.producer.KafkaProducer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 2, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
class EmbeddedKafkaIntegrationTest {

  @Autowired
  private KafkaConsumer consumer;

  @Autowired
  private KafkaProducer producer;

  @Value("${test.topic}")
  private String topic;


  @Test
  public void givenEmbeddedKafkaBroker_whenSendingWithSimpleProducer_thenMessageReceived() throws Exception {
    String data = "Sending with our own simple KafkaProducer";
    producer.send(topic, data);
  }
}