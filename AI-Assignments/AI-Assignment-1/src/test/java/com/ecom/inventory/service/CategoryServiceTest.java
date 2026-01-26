package com.ecom.inventory.service;

import com.ecom.inventory.dto.CategoryCreateDTO;
import com.ecom.inventory.dto.CategoryDTO;
import com.ecom.inventory.exception.ResourceNotFoundException;
import com.ecom.inventory.mapper.CategoryMapper;
import com.ecom.inventory.model.Category;
import com.ecom.inventory.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;

    private Category category;
    private CategoryDTO categoryDTO;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        categoryId = UUID.randomUUID();

        category = new Category();
        category.setId(categoryId);
        category.setName("Electronics");

        categoryDTO = new CategoryDTO();
        categoryDTO.setId(categoryId);
        categoryDTO.setName("Electronics");
    }

    @Test
    void testGetCategoryById_Success() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(any(Category.class))).thenReturn(categoryDTO);
        CategoryDTO found = categoryService.getCategoryById(categoryId);
        assertNotNull(found);
        assertEquals("Electronics", found.getName());
    }

    @Test
    void testGetCategoryById_NotFound() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> categoryService.getCategoryById(categoryId));
    }

    @Test
    void testGetAllCategories() {
        when(categoryRepository.findAll()).thenReturn(Collections.singletonList(category));
        when(categoryMapper.toDto(any(Category.class))).thenReturn(categoryDTO);
        assertEquals(1, categoryService.getAllCategories().size());
    }

    @Test
    void testCreateCategory() {
        CategoryCreateDTO createDTO = new CategoryCreateDTO();
        createDTO.setName("Books");

        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toDto(any(Category.class))).thenReturn(categoryDTO);

        CategoryDTO result = categoryService.createCategory(createDTO);
        assertNotNull(result.getId());
    }

    @Test
    void testUpdateCategory_Success() {
        CategoryCreateDTO updateDTO = new CategoryCreateDTO();
        updateDTO.setName("Home Appliances");

        CategoryDTO updatedDTO = new CategoryDTO();
        updatedDTO.setId(categoryId);
        updatedDTO.setName("Home Appliances");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryMapper.toDto(any(Category.class))).thenReturn(updatedDTO);

        CategoryDTO result = categoryService.updateCategory(categoryId, updateDTO);
        assertEquals("Home Appliances", result.getName());
    }

    @Test
    void testUpdateCategory_NotFound() {
        CategoryCreateDTO updateDTO = new CategoryCreateDTO();
        updateDTO.setName("Home Appliances");
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> categoryService.updateCategory(categoryId, updateDTO));
    }

    @Test
    void testDeleteCategory_Success() {
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        doNothing().when(categoryRepository).deleteById(categoryId);
        categoryService.deleteCategory(categoryId);
        verify(categoryRepository, times(1)).deleteById(categoryId);
    }

    @Test
    void testDeleteCategory_NotFound() {
        when(categoryRepository.existsById(categoryId)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCategory(categoryId));
    }
}