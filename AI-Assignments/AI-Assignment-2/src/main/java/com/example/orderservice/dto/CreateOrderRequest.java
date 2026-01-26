
package com.example.orderservice.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "customerId is required")
    private UUID customerId;

    @NotNull(message = "item is required")
    @Valid
    private Item item;

    @NotBlank(message = "shippingAddress is required")
    private String shippingAddress;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a well-formed email address")
    private String email;

    // Optional client-supplied transaction id for idempotent order creation
    private UUID transactionId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @NotNull(message = "item.productId is required")
        private UUID productId;

        @NotNull(message = "item.quantity is required")
        @Positive(message = "item.quantity must be greater than zero")
        private Integer quantity;

        @NotNull(message = "item.price is required")
        @Positive(message = "item.price must be greater than zero")
        private Double price;
    }
}
