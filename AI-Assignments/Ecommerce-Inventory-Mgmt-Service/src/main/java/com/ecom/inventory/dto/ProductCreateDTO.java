package com.ecom.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object for creating a new Product.
 * This class encapsulates the data required to create a new product.
 */
@Data
public class ProductCreateDTO {
    /**
     * The name of the product.
     */
    @NotBlank(message = "Product name cannot be blank")
    private String name;
}