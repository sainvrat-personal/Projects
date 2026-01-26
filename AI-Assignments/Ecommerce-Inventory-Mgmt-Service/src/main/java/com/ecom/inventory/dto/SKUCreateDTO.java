package com.ecom.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data Transfer Object for creating a new SKU.
 * This class encapsulates the data required to create a new SKU.
 */
@Data
public class SKUCreateDTO {
    /**
     * The unique identifier string for the SKU (e.g., "TSHIRT-RED-L").
     */
    @NotBlank(message = "SKU identifier cannot be blank")
    private String sku;
    /**
     * The price of the SKU.
     */
    @NotNull(message = "Price cannot be null")
    @Min(value = 0, message = "Price must be a positive value")
    private Double price;
    /**
     * The available quantity of the SKU.
     */
    @NotNull(message = "Quantity cannot be null")
    @Min(value = 0, message = "Quantity must be a non-negative value")
    private Integer quantity;
}