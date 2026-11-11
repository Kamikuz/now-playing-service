package com.widdit.nowplaying.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/embedded")
@ConditionalOnProperty(name = "now-playing.embedded", havingValue = "true")
public class EmbeddedController {

    private final ConfigurableApplicationContext applicationContext;

    public EmbeddedController(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "ok");
    }

    @PostMapping("/shutdown")
    public ResponseEntity<Void> shutdown() {
        Thread shutdownThread = new Thread(() -> {
            int exitCode = SpringApplication.exit(applicationContext);
            System.exit(exitCode);
        }, "embedded-shutdown");
        shutdownThread.setDaemon(false);
        shutdownThread.start();
        return ResponseEntity.accepted().build();
    }
}
