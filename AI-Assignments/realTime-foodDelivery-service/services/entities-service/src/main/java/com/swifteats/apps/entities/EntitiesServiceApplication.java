package com.swifteats.apps.entities;

import com.swifteats.common.runtime.SwiftEatsServiceApplication;
import org.springframework.boot.SpringApplication;

@SwiftEatsServiceApplication
public class EntitiesServiceApplication {

    public static void main(String[] args) {
        System.setProperty("swifteats.service.name", "ENTITIES");
        SpringApplication.run(EntitiesServiceApplication.class, args);
    }
}
