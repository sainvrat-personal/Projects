package com.ecom.inventory.mapper;

import com.ecom.inventory.dto.CategoryDTO;
import com.ecom.inventory.model.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public CategoryDTO toDto(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        return dto;
    }
}