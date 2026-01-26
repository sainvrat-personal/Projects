package com.ecom.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object for creating a new Category.
 * This class encapsulates the data required to create a new category.
 */
@Data
public class CategoryCreateDTO {
    /**
     * The name of the category.
     */
    @NotBlank(message = "Category name cannot be blank")
    private String name;
}