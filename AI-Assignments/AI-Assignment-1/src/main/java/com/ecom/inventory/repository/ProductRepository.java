package com.ecom.inventory.repository;

import com.ecom.inventory.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Product} entity.
 * Provides standard CRUD operations and query capabilities for products.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Product> findByCategoryName(String categoryName, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndCategoryName(String name, String categoryName, Pageable pageable);
}