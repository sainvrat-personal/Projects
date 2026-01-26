package com.ecom.inventory.mapper;

import com.ecom.inventory.dto.SKUDTO;
import com.ecom.inventory.model.SKU;
import org.springframework.stereotype.Component;

@Component
public class SKUMapper {
    public SKUDTO toDto(SKU sku) {
        SKUDTO dto = new SKUDTO();
        dto.setId(sku.getId());
        dto.setSku(sku.getSku());
        dto.setPrice(sku.getPrice());
        dto.setQuantity(sku.getQuantity());
        if (sku.getProduct() != null) {
            dto.setProductId(sku.getProduct().getId());
        }
        return dto;
    }
}