package com.example.rest;

import com.example.producer.KafkaProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

@Controller("/producer")
@Slf4j
public class ProducerController {

    @Autowired
    private KafkaProducer kafkaProducer;

    @PostMapping("/send")
    public ResponseEntity<Model> post(Model model) {
        log.info("Received model '{}'", model);

        if (model == null || !model.containsAttribute("payload")) {
            log.error("Request doesn't has the 'payload' property");
            return ResponseEntity.unprocessableEntity().body(model);
        }

        Object payload = model.getAttribute("payload");
        kafkaProducer.send(payload);
        return ResponseEntity.ok(model);
    }
}
