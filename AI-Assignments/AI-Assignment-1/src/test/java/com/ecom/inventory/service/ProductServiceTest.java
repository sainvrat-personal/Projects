package com.ecom.inventory.service;

import com.ecom.inventory.dto.ProductCreateDTO;
import com.ecom.inventory.dto.ProductDTO;
import com.ecom.inventory.exception.ResourceNotFoundException;
import com.ecom.inventory.mapper.ProductMapper;
import com.ecom.inventory.model.Category;
import com.ecom.inventory.model.Product;
import com.ecom.inventory.repository.CategoryRepository;
import com.ecom.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ProductServiceTest {

    @InjectMocks
    private ProductService productService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductMapper productMapper;

    private Product product;
    private ProductDTO productDTO;
    private UUID productId;
    private Category category;
    private UUID categoryId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        categoryId = UUID.randomUUID();
        category = new Category();
        category.setId(categoryId);
        category.setName("Electronics");

        productId = UUID.randomUUID();
        product = new Product();
        product.setId(productId);
        product.setName("Laptop");
        product.setCategory(category);

        productDTO = new ProductDTO();
        productDTO.setId(productId);
        productDTO.setName("Laptop");
        productDTO.setCategoryId(categoryId);
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void testGetProductById_Success() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productMapper.toDto(any(Product.class))).thenReturn(productDTO);
        ProductDTO found = productService.getProductById(productId);
        assertNotNull(found);
        assertEquals("Laptop", found.getName());
    }

    @Test
    void testGetProductById_NotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(productId));
    }

    @Test
    void testGetAllProducts_NoFilters() {
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);
        when(productMapper.toDto(any(Product.class))).thenReturn(productDTO);

        Page<ProductDTO> result = productService.getAllProducts(null, null, pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void testGetAllProducts_ByName() {
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.findByNameContainingIgnoreCase(anyString(), any(Pageable.class)))
                .thenReturn(productPage);
        when(productMapper.toDto(any(Product.class))).thenReturn(productDTO);

        Page<ProductDTO> result = productService.getAllProducts("Laptop", null, pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findByNameContainingIgnoreCase(eq("Laptop"), any(Pageable.class));
    }

    @Test
    void testGetAllProducts_ByCategory() {
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.findByCategoryName(anyString(), any(Pageable.class))).thenReturn(productPage);
        when(productMapper.toDto(any(Product.class))).thenReturn(productDTO);

        Page<ProductDTO> result = productService.getAllProducts(null, "Electronics", pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findByCategoryName(eq("Electronics"), any(Pageable.class));
    }

    @Test
    void testGetAllProducts_ByNameAndCategory() {
        Page<Product> productPage = new PageImpl<>(Collections.singletonList(product));
        when(productRepository.findByNameContainingIgnoreCaseAndCategoryName(anyString(), anyString(),
                any(Pageable.class))).thenReturn(productPage);
        when(productMapper.toDto(any(Product.class))).thenReturn(productDTO);

        Page<ProductDTO> result = productService.getAllProducts("Laptop", "Electronics", pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findByNameContainingIgnoreCaseAndCategoryName(eq("Laptop"),
                eq("Electronics"), any(Pageable.class));
    }

    @Test
    void testCreateProduct_Success() {
        ProductCreateDTO createDTO = new ProductCreateDTO();
        createDTO.setName("Smartphone");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toDto(any(Product.class))).thenReturn(productDTO);

        ProductDTO result = productService.createProduct(categoryId, createDTO);
        assertNotNull(result);
    }

    @Test
    void testCreateProduct_CategoryNotFound() {
        ProductCreateDTO createDTO = new ProductCreateDTO();
        createDTO.setName("Smartphone");
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(categoryId, createDTO));
    }

    @Test
    void testUpdateProduct_Success() {
        ProductCreateDTO updateDTO = new ProductCreateDTO();
        updateDTO.setName("Gaming Laptop");

        ProductDTO updatedDTO = new ProductDTO();
        updatedDTO.setName("Gaming Laptop");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toDto(any(Product.class))).thenReturn(updatedDTO);

        ProductDTO result = productService.updateProduct(productId, updateDTO);
        assertEquals("Gaming Laptop", result.getName());
    }

    @Test
    void testUpdateProduct_NotFound() {
        ProductCreateDTO updateDTO = new ProductCreateDTO();
        updateDTO.setName("Gaming Laptop");
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productService.updateProduct(productId, updateDTO));
    }

    @Test
    void testDeleteProduct_Success() {
        when(productRepository.existsById(productId)).thenReturn(true);
        doNothing().when(productRepository).deleteById(productId);
        productService.deleteProduct(productId);
        verify(productRepository, times(1)).deleteById(productId);
    }

    @Test
    void testDeleteProduct_NotFound() {
        when(productRepository.existsById(productId)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> productService.deleteProduct(productId));
    }
}