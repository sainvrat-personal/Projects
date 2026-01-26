package com.ecom.inventory.controller;

import com.ecom.inventory.dto.SKUCreateDTO;
import com.ecom.inventory.dto.SKUDTO;
import com.ecom.inventory.model.SKU;
import com.ecom.inventory.service.SKUService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing SKUs.
 * Provides endpoints for all CRUD operations on
 * {@link com.ecom.inventory.model.SKU} entities,
 * scoped to a specific product.
 */
@RestController
@RequestMapping("/api/products/{productId}/skus")
@Tag(name = "SKU Management", description = "Endpoints for managing SKUs (Stock Keeping Units) for a specific product.")
public class SKUController {

        private static final Logger logger = LoggerFactory.getLogger(SKUController.class);

        @Autowired
        private SKUService skuService;

        @GetMapping
        @Operation(summary = "Get all SKUs for a product", description = "Retrieves a list of all SKUs associated with a specific product ID.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of SKUs."),
                        @ApiResponse(responseCode = "404", description = "Product with the specified ID not found.", content = @Content(schema = @Schema(implementation = String.class))) })
        public List<SKUDTO> getSkusByProductId(
                        @Parameter(description = "The unique ID of the product.") @PathVariable UUID productId) {
                logger.info("Request to get SKUs for product with id: {}", productId);
                return skuService.getSkusByProductId(productId);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get a specific SKU by ID", description = "Retrieves a single SKU by its unique ID, within the context of a product.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the SKU."),
                        @ApiResponse(responseCode = "404", description = "SKU or Product with the specified ID not found.", content = @Content(schema = @Schema(implementation = String.class))) })
        public ResponseEntity<SKUDTO> getSkuById(
                        @Parameter(description = "The unique ID of the product.") @PathVariable UUID productId,
                        @Parameter(description = "Unique ID of the SKU to retrieve.") @PathVariable UUID id) {
                logger.info("Request to get SKU {} for product {}", id, productId);
                return ResponseEntity.ok(skuService.getSkuById(id));
        }

        @PostMapping
        @Operation(summary = "Create a new SKU for a product", description = "Creates a new SKU and associates it with an existing product.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully created the SKU."),
                        @ApiResponse(responseCode = "404", description = "Product with the specified ID not found.", content = @Content(schema = @Schema(implementation = String.class))) })
        public SKUDTO createSku(
                        @Parameter(description = "The unique ID of the product.") @PathVariable UUID productId,
                        @Parameter(description = "The details of the SKU to create.") @Valid @RequestBody SKUCreateDTO skuDTO) {
                logger.info("Request to create SKU for product {} with data: {}", productId, skuDTO);
                return skuService.createSku(productId, skuDTO);
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update an existing SKU", description = "Updates the details of an existing SKU by its unique ID.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully updated the SKU."),
                        @ApiResponse(responseCode = "404", description = "SKU or Product with the specified ID not found.", content = @Content(schema = @Schema(implementation = String.class))) })
        public ResponseEntity<SKUDTO> updateSku(
                        @Parameter(description = "The unique ID of the product.") @PathVariable UUID productId,
                        @Parameter(description = "Unique ID of the SKU to update.") @PathVariable UUID id,
                        @Parameter(description = "The updated details for the SKU.") @Valid @RequestBody SKUCreateDTO skuDetails) {
                logger.info("Request to update SKU {} for product {} with data: {}", id, productId, skuDetails);
                return ResponseEntity.ok(skuService.updateSku(id, skuDetails));
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete an SKU", description = "Deletes an SKU by its unique ID.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully deleted the SKU.", content = @Content(schema = @Schema(implementation = String.class))),
                        @ApiResponse(responseCode = "404", description = "SKU or Product with the specified ID not found.", content = @Content(schema = @Schema(implementation = String.class))) })
        public ResponseEntity<String> deleteSku(
                        @Parameter(description = "The unique ID of the product.") @PathVariable UUID productId,
                        @Parameter(description = "Unique ID of the SKU to delete.") @PathVariable UUID id) {
                logger.info("Request to delete SKU {} for product {}", id, productId);
                skuService.deleteSku(id);
                return ResponseEntity.ok("sku deleted with id - " + id);
        }
}