package com.ecom.inventory.controller;

import com.ecom.inventory.dto.ProductCreateDTO;
import com.ecom.inventory.dto.ProductDTO;
import com.ecom.inventory.model.Product;
import com.ecom.inventory.service.ProductService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing products.
 * Provides endpoints for all CRUD operations on
 * {@link com.ecom.inventory.model.Product} entities.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "Endpoints for managing individual products.")
public class ProductController {

        private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

        @Autowired
        private ProductService productService;

        @GetMapping
        @Operation(summary = "Get all products with optional filters and pagination", description = "Retrieves a list of products with optional search by name, filtering by category, and pagination.")
        public Page<ProductDTO> getAllProducts(
                        @Parameter(description = "Search by product name (case-insensitive).") @RequestParam(required = false) String name,
                        @Parameter(description = "Filter by category name.") @RequestParam(required = false) String categoryName,
                        @Parameter(description = "The page number to retrieve (0-indexed).") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "The number of items per page.") @RequestParam(defaultValue = "10") int pageSize) {
                logger.info("Request to get all products with filters: name='{}', category='{}', page={}, pageSize={}",
                                name,
                                categoryName, page, pageSize);
                return productService.getAllProducts(name, categoryName, PageRequest.of(page, pageSize));
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get a product by ID", description = "Retrieves a single product by its unique ID.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved the product."),
                        @ApiResponse(responseCode = "404", description = "Product with the specified ID not found.", content = @Content(schema = @Schema(implementation = String.class))) })
        public ResponseEntity<ProductDTO> getProductById(
                        @Parameter(description = "Unique ID of the product to retrieve.") @PathVariable UUID id) {
                logger.info("Request to get product with id: {}", id);
                return ResponseEntity.ok(productService.getProductById(id));
        }

        @PostMapping
        @Operation(summary = "Create a new product", description = "Creates a new product and associates it with a category.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully created the product."),
                        @ApiResponse(responseCode = "404", description = "The specified category ID was not found.", content = @Content(schema = @Schema(implementation = String.class))) })
        public ProductDTO createProduct(
                        @Parameter(description = "The ID of the category to which this product belongs.") @RequestParam UUID categoryId,
                        @Parameter(description = "The details of the product to create.") @Valid @RequestBody ProductCreateDTO productDTO) {
                logger.info("Request to create product in category {} with data: {}", categoryId, productDTO);
                return productService.createProduct(categoryId, productDTO);
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update an existing product", description = "Updates the details of an existing product by its unique ID.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully updated the product."),
                        @ApiResponse(responseCode = "404", description = "Product with the specified ID not found.", content = @Content(schema = @Schema(implementation = String.class))) })
        public ResponseEntity<ProductDTO> updateProduct(
                        @Parameter(description = "Unique ID of the product to update.") @PathVariable UUID id,
                        @Parameter(description = "The updated details for the product.") @Valid @RequestBody ProductCreateDTO productDetails) {
                logger.info("Request to update product with id {} with data: {}", id, productDetails);
                return ResponseEntity.ok(productService.updateProduct(id, productDetails));
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete a product", description = "Deletes a product by its unique ID. This will also delete all associated SKUs.", responses = {
                        @ApiResponse(responseCode = "200", description = "Successfully deleted the product.", content = @Content(schema = @Schema(implementation = String.class))),
                        @ApiResponse(responseCode = "404", description = "Product with the specified ID not found.", content = @Content(schema = @Schema(implementation = String.class))) })
        public ResponseEntity<String> deleteProduct(
                        @Parameter(description = "Unique ID of the product to delete.") @PathVariable UUID id) {
                logger.info("Request to delete product with id: {}", id);
                productService.deleteProduct(id);
                return ResponseEntity.ok("product deleted with id - " + id);
        }
}