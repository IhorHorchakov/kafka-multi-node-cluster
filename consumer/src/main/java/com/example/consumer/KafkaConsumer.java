package com.example.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaConsumer {
    private static final String TOPIC = "test-topic";

    @KafkaListener(topics = TOPIC, groupId = KafkaConsumerConfig.CONSUMER_GROUP, containerFactory = "kafkaListenerContainerFactory")
    public void receive(ConsumerRecord<?, ?> consumerRecord) {
        log.info("Received a record '{}'", consumerRecord);
    }
}
