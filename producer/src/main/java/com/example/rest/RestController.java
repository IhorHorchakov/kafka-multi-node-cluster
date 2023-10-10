package com.example.rest;

import com.example.producer.KafkaProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/producer")
@Slf4j
public class RestController {
    @Autowired
    private KafkaProducer kafkaProducer;

    @PostMapping(path = "/publish", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ModelMap> post(@RequestBody ModelMap modelMap) {
        log.info("Received model '{}'", modelMap);

        if (modelMap == null || !modelMap.containsAttribute("payload")) {
            log.error("Request doesn't has the 'payload' property");
            return ResponseEntity.unprocessableEntity().body(modelMap);
        }

        Object payload = modelMap.getAttribute("payload");
        kafkaProducer.publish(payload);
        return ResponseEntity.ok(modelMap);
    }
}
