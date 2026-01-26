package com.ecom.inventory.repository;

import com.ecom.inventory.model.SKU;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link SKU} entity.
 * Provides standard CRUD operations and query capabilities for SKUs.
 */
@Repository
public interface SKURepository extends JpaRepository<SKU, UUID> {
}