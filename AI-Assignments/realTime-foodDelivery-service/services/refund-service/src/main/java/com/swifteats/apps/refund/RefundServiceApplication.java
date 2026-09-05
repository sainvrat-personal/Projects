package com.swifteats.apps.refund;

import com.swifteats.common.runtime.SwiftEatsServiceApplication;
import org.springframework.boot.SpringApplication;

@SwiftEatsServiceApplication
public class RefundServiceApplication {

    public static void main(String[] args) {
        System.setProperty("swifteats.service.name", "REFUND");
        SpringApplication.run(RefundServiceApplication.class, args);
    }
}
