package com.ecom.inventory.service;

import com.ecom.inventory.dto.CategoryCreateDTO;
import com.ecom.inventory.dto.CategoryDTO;
import com.ecom.inventory.exception.ResourceNotFoundException;
import com.ecom.inventory.mapper.CategoryMapper;
import com.ecom.inventory.model.Category;
import com.ecom.inventory.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing categories.
 * Handles business logic related to category operations and interacts with the
 * repository.
 */
@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * Retrieves all categories.
     *
     * @return a list of all {@link CategoryDTO} entities.
     */
    public List<CategoryDTO> getAllCategories() {
        logger.info("Fetching all categories from database");
        return categoryRepository.findAll().stream().map(categoryMapper::toDto).collect(Collectors.toList());
    }

    /**
     * Retrieves a single category by its ID.
     *
     * @param id The UUID of the category to retrieve.
     * @return The found {@link CategoryDTO}.
     * @throws ResourceNotFoundException if no category with the given ID is found.
     */
    public CategoryDTO getCategoryById(UUID id) {
        logger.info("Fetching category with id: {}", id);
        return categoryRepository.findById(id).map(categoryMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Category with specified id not found"));
    }

    /**
     * Creates a new category.
     *
     * @param categoryDTO The DTO containing the details for the new category.
     * @return The newly created {@link CategoryDTO}.
     */
    public CategoryDTO createCategory(CategoryCreateDTO categoryDTO) {
        logger.info("Creating new category with data: {}", categoryDTO);
        Category category = new Category();
        category.setName(categoryDTO.getName());
        Category savedCategory = categoryRepository.save(category);
        logger.info("Saved new category with id: {}", savedCategory.getId());
        return categoryMapper.toDto(savedCategory);
    }

    /**
     * Updates an existing category.
     *
     * @param id              The UUID of the category to update.
     * @param categoryDetails The new details for the category.
     * @return The updated {@link CategoryDTO}.
     * @throws ResourceNotFoundException if no category with the given ID is found.
     */
    public CategoryDTO updateCategory(UUID id, CategoryCreateDTO categoryDetails) {
        logger.info("Updating category with id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(categoryDetails.getName());
        Category updatedCategory = categoryRepository.save(category);
        logger.info("Updated category with id: {}", updatedCategory.getId());
        return categoryMapper.toDto(updatedCategory);
    }

    /**
     * Deletes a category by its ID.
     *
     * @param id The UUID of the category to delete.
     * @throws ResourceNotFoundException if no category with the given ID is found.
     */
    public void deleteCategory(UUID id) {
        logger.info("Deleting category with id: {}", id);
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category with specified id not found");
        }
        categoryRepository.deleteById(id);
        logger.info("Deleted category with id: {}", id);
    }
}