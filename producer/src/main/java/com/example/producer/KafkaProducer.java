package com.example.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class KafkaProducer {
    public final static String TOPIC = "test-topic";
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(Object event) {
        log.info("Sending event='{}' to topic='{}'", event, TOPIC);

        /**
         * The send API returns a CompletableFuture object. If we want to block the sending thread and get the result
         * about the sent message, we can call the get API of the CompletableFuture object. The thread will wait for
         * the result, but it will slow down the producer.
         *
         * Kafka is a fast-stream processing platform. Therefore, it’s better to handle the results asynchronously
         * so that the subsequent messages do not wait for the result of the previous message.
         */
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(TOPIC, event);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent message with offset=[{}]", result.getRecordMetadata().offset());
            } else {
                log.warn("Unable to send message {}", ex.getMessage());
            }
        });
    }
}
