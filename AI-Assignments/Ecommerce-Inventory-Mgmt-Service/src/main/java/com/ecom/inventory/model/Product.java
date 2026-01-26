package com.ecom.inventory.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Represents a product in the inventory.
 * A product belongs to a single category and can have multiple SKUs.
 */
@Entity
@Data
public class Product implements Serializable {
    /**
     * The unique identifier for the product.
     * It is generated automatically.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * The name of the product (e.g., "Laptop", "T-Shirt").
     */
    private String name;

    /**
     * The category to which this product belongs.
     * This is a many-to-one relationship.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonBackReference
    private Category category;

    /**
     * The list of SKUs (Stock Keeping Units) for this product.
     * This is a one-to-many relationship.
     * It is ignored during JSON serialization to prevent infinite recursion and
     * lazy loading issues.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @JsonIgnore
    private List<SKU> skus;
}