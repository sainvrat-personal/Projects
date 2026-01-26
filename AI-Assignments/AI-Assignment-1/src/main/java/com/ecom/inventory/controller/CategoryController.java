package com.ecom.inventory.controller;

import com.ecom.inventory.dto.CategoryCreateDTO;
import com.ecom.inventory.dto.CategoryDTO;
import com.ecom.inventory.model.Category;
import com.ecom.inventory.service.CategoryService;
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
 * REST controller for managing categories.
 * Provides endpoints for all CRUD operations on
 * {@link com.ecom.inventory.model.Category} entities.
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category Management", description = "Endpoints for creating, reading, updating, and deleting product categories.")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories", description = "Retrieves a list of all product categories.")
    public List<CategoryDTO> getAllCategories() {
        logger.info("Request to get all categories");
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by ID", description = "Retrieves a single category by its unique ID.", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the category."),
            @ApiResponse(responseCode = "404", description = "Category with the specified ID not found.", content = @Content(schema = @Schema(implementation = String.class))) })
    public ResponseEntity<CategoryDTO> getCategoryById(
            @Parameter(description = "Unique ID of the category to retrieve.") @PathVariable UUID id) {
        logger.info("Request to get category with id: {}", id);
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new category", description = "Creates a new product category.")
    public CategoryDTO createCategory(
            @Parameter(description = "The details of the category to create.") @Valid @RequestBody CategoryCreateDTO categoryDTO) {
        logger.info("Request to create category with data: {}", categoryDTO);
        return categoryService.createCategory(categoryDTO);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing category", description = "Updates the details of an existing category by its unique ID.", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully updated the category."),
            @ApiResponse(responseCode = "404", description = "Category with the specified ID not found.", content = @Content(schema = @Schema(implementation = String.class))) })
    public ResponseEntity<CategoryDTO> updateCategory(
            @Parameter(description = "Unique ID of the category to update.") @PathVariable UUID id,
            @Parameter(description = "The updated details for the category.") @Valid @RequestBody CategoryCreateDTO categoryDetails) {
        logger.info("Request to update category with id {} with data: {}", id, categoryDetails);
        return ResponseEntity.ok(categoryService.updateCategory(id, categoryDetails));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category", description = "Deletes a category by its unique ID.", responses = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted the category.", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "Category with the specified ID not found.", content = @Content(schema = @Schema(implementation = String.class))) })
    public ResponseEntity<String> deleteCategory(
            @Parameter(description = "Unique ID of the category to delete.") @PathVariable UUID id) {
        logger.info("Request to delete category with id: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("category deleted with id - " + id);
    }
}