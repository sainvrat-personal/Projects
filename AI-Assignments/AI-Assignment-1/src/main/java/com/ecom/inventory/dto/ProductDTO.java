package com.ecom.inventory.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ProductDTO {
    private UUID id;
    private String name;
    private UUID categoryId;
}