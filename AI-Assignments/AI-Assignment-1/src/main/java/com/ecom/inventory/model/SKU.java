package com.ecom.inventory.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

/**
 * Represents an SKU (Stock Keeping Unit) in the inventory.
 * An SKU is a specific version of a product, defined by attributes like size
 * and color.
 * Each SKU has its own price and quantity.
 */
@Entity
@Data
public class SKU implements Serializable {
    /**
     * The unique identifier for the SKU.
     * It is generated automatically.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * The unique SKU identifier string (e.g., "LAPTOP-16GB-512GB").
     */
    private String sku;

    /**
     * The price of this specific SKU.
     */
    private Double price;

    /**
     * The available quantity of this SKU in stock.
     */
    private Integer quantity;

    /**
     * The product to which this SKU belongs.
     * This is a many-to-one relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonBackReference
    private Product product;
}