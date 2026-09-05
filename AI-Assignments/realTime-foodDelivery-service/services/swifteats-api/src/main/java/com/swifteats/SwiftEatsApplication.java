package com.swifteats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "com.swifteats")
@EnableScheduling
public class SwiftEatsApplication {

    private static final Logger log = LoggerFactory.getLogger(SwiftEatsApplication.class);

    public static void main(String[] args) {
        log.info("Starting SwiftEats service...");
        SpringApplication application = new SpringApplication(SwiftEatsApplication.class);
        application.addListeners((ApplicationListener<ApplicationReadyEvent>) event ->
                log.info("SwiftEats service started successfully"));
        application.run(args);
    }
}
