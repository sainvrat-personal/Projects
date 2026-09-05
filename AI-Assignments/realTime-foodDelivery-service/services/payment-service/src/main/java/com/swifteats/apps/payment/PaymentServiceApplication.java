package com.swifteats.apps.payment;

import com.swifteats.common.runtime.SwiftEatsServiceApplication;
import org.springframework.boot.SpringApplication;

@SwiftEatsServiceApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        System.setProperty("swifteats.service.name", "PAYMENT");
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
