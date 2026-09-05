package com.swifteats.apps.order;

import com.swifteats.common.runtime.SwiftEatsServiceApplication;
import org.springframework.boot.SpringApplication;

@SwiftEatsServiceApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        System.setProperty("swifteats.service.name", "ORDER");
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
