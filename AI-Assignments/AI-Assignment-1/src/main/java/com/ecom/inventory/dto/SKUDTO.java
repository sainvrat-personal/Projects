package com.ecom.inventory.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class SKUDTO {
    private UUID id;
    private String sku;
    private Double price;
    private Integer quantity;
    private UUID productId;
}