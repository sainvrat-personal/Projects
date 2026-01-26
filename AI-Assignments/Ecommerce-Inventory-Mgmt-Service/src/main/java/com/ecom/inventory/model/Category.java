package com.ecom.inventory.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
import java.io.Serializable;
import java.util.UUID;

/**
 * Represents a product category in the inventory.
 * A category can have multiple products.
 */
@Entity
@Data
public class Category implements Serializable {
    /**
     * The unique identifier for the category.
     * It is generated automatically.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * The name of the category (e.g., "Electronics", "Books").
     */
    private String name;

    /**
     * The list of products belonging to this category.
     * This is a one-to-many relationship, and the products are managed through the
     * 'category' field in the Product entity.
     * It is ignored during JSON serialization to prevent infinite recursion and
     * lazy loading issues.
     */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @JsonIgnore
    private List<Product> products;
}