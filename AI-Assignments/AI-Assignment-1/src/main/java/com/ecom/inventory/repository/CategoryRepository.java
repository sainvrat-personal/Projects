package com.ecom.inventory.repository;

import com.ecom.inventory.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Category} entity.
 * Provides standard CRUD operations and query capabilities for categories.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
}