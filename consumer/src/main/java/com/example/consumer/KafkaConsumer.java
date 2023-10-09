package com.example.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaConsumer {

    @KafkaListener(topics = "test-topic")
    public void receive(ConsumerRecord<?, ?> consumerRecord) {
        log.info("Received payload='{}'", consumerRecord.toString());
        Object payload = consumerRecord.value();
    }
}