package com.ecom.inventory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main entry point for the Inventory Management Service application.
 * This class initializes the Spring Boot application and enables API
 * documentation.
 */
@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "Inventory Management API", version = "1.0", description = "API for managing e-commerce inventory, including categories, products, and SKUs."))
public class InventoryMgmtServiceApplication {

    /**
     * The main method which serves as the entry point for the Spring Boot
     * application.
     *
     * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        SpringApplication.run(InventoryMgmtServiceApplication.class, args);
    }

}