package com.example.producer;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaProducer {
    private final static String TOPIC = "test-topic";
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void send(Object payload) {
        log.info("Sending payload='{}' to topic='{}'", payload, TOPIC);
        kafkaTemplate.send(TOPIC, payload);
    }
}