package com.example.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.*;

class OrderReturnsManagementApplicationTest {

    @Test
    void main_runsWithoutExceptions_withEmptyArgs() {
        assertDoesNotThrow(() -> OrderReturnsManagementApplication.main(new String[]{}));
    }

    @Test
    void main_throwsIllegalArgumentException_withNullArgs() {
        assertThrows(IllegalArgumentException.class,
                () -> OrderReturnsManagementApplication.main(null));
    }

    @Test
    void applicationClass_hasRequiredSpringAnnotations() {
        Class<OrderReturnsManagementApplication> clazz = OrderReturnsManagementApplication.class;

        assertNotNull(clazz.getAnnotation(SpringBootApplication.class));
        assertNotNull(clazz.getAnnotation(EnableAsync.class));
        assertNotNull(clazz.getAnnotation(EnableRetry.class));
        assertNotNull(clazz.getAnnotation(EnableScheduling.class));
    }
}

