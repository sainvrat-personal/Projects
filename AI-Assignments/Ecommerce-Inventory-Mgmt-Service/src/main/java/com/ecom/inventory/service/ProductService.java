package com.ecom.inventory.service;

import com.ecom.inventory.dto.ProductCreateDTO;
import com.ecom.inventory.dto.ProductDTO;
import com.ecom.inventory.exception.ResourceNotFoundException;
import com.ecom.inventory.mapper.ProductMapper;
import com.ecom.inventory.model.Category;
import com.ecom.inventory.model.Product;
import com.ecom.inventory.repository.CategoryRepository;
import com.ecom.inventory.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing products.
 * Handles business logic related to product operations and interacts with
 * repositories.
 */
@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductMapper productMapper;

    /**
     * Retrieves a paginated and filtered list of products.
     *
     * @param name         Optional name to search for (case-insensitive).
     * @param categoryName Optional category name to filter by.
     * @param pageable     Pagination information.
     * @return A {@link Page} of {@link ProductDTO} entities.
     */
    public Page<ProductDTO> getAllProducts(String name, String categoryName, Pageable pageable) {
        logger.info("Fetching products with filters: name='{}', category='{}', pageable={}", name, categoryName,
                pageable);

        if (StringUtils.hasText(name) && StringUtils.hasText(categoryName)) {
            return productRepository.findByNameContainingIgnoreCaseAndCategoryName(name, categoryName, pageable)
                    .map(productMapper::toDto);
        } else if (StringUtils.hasText(name)) {
            return productRepository.findByNameContainingIgnoreCase(name, pageable).map(productMapper::toDto);
        } else if (StringUtils.hasText(categoryName)) {
            return productRepository.findByCategoryName(categoryName, pageable).map(productMapper::toDto);
        } else {
            return productRepository.findAll(pageable).map(productMapper::toDto);
        }
    }

    /**
     * Retrieves a single product by its ID.
     *
     * @param id The UUID of the product to retrieve.
     * @return The found {@link ProductDTO}.
     * @throws ResourceNotFoundException if no product with the given ID is found.
     */
    public ProductDTO getProductById(UUID id) {
        logger.info("Fetching product with id: {}", id);
        return productRepository.findById(id).map(productMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Product with specified id not found"));
    }

    /**
     * Creates a new product and associates it with a category.
     *
     * @param categoryId The UUID of the category to associate the product with.
     * @param productDTO The DTO containing the details for the new product.
     * @return The newly created {@link ProductDTO}.
     * @throws ResourceNotFoundException if no category with the given ID is found.
     */
    public ProductDTO createProduct(UUID categoryId, ProductCreateDTO productDTO) {
        logger.info("Creating product in category {} with data: {}", categoryId, productDTO);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    logger.error("Category not found with id: {}", categoryId);
                    return new ResourceNotFoundException("Category not found");
                });
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setCategory(category);
        Product savedProduct = productRepository.save(product);
        logger.info("Saved new product with id: {}", savedProduct.getId());
        return productMapper.toDto(savedProduct);
    }

    /**
     * Updates an existing product.
     *
     * @param id             The UUID of the product to update.
     * @param productDetails The new details for the product.
     * @return The updated {@link ProductDTO}.
     * @throws ResourceNotFoundException if no product with the given ID is found.
     */
    public ProductDTO updateProduct(UUID id, ProductCreateDTO productDetails) {
        logger.info("Updating product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setName(productDetails.getName());
        Product updatedProduct = productRepository.save(product);
        logger.info("Updated product with id: {}", updatedProduct.getId());
        return productMapper.toDto(updatedProduct);
    }

    /**
     * Deletes a product by its ID.
     *
     * @param id The UUID of the product to delete.
     * @throws ResourceNotFoundException if no product with the given ID is found.
     */
    public void deleteProduct(UUID id) {
        logger.info("Deleting product with id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product with specified id not found");
        }
        productRepository.deleteById(id);
        logger.info("Deleted product with id: {}", id);
    }
}