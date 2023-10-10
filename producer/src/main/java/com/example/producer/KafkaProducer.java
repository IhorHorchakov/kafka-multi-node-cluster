package com.example.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaProducer {
    private final static String TOPIC = "test-topic";
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(Object payload) {
        log.info("Sending payload='{}' to topic='{}'", payload, TOPIC);
        kafkaTemplate.send(TOPIC, payload);
    }
}