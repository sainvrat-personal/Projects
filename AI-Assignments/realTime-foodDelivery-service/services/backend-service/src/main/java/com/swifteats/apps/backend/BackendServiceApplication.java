package com.swifteats.apps.backend;

import com.swifteats.common.runtime.SwiftEatsServiceApplication;
import org.springframework.boot.SpringApplication;

@SwiftEatsServiceApplication
public class BackendServiceApplication {

    public static void main(String[] args) {
        System.setProperty("swifteats.service.name", "BACKEND");
        SpringApplication.run(BackendServiceApplication.class, args);
    }
}
