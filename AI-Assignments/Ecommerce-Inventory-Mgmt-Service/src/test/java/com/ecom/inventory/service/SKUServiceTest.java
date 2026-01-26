package com.ecom.inventory.service;

import com.ecom.inventory.dto.SKUCreateDTO;
import com.ecom.inventory.dto.SKUDTO;
import com.ecom.inventory.exception.ResourceNotFoundException;
import com.ecom.inventory.mapper.SKUMapper;
import com.ecom.inventory.model.Product;
import com.ecom.inventory.model.SKU;
import com.ecom.inventory.repository.ProductRepository;
import com.ecom.inventory.repository.SKURepository;
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

public class SKUServiceTest {

    @InjectMocks
    private SKUService skuService;
    @Mock
    private SKURepository skuRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SKUMapper skuMapper;

    private SKU sku;
    private SKUDTO skuDTO;
    private UUID skuId;
    private Product product;
    private UUID productId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        productId = UUID.randomUUID();
        product = new Product();
        product.setId(productId);
        product.setName("Laptop");

        skuId = UUID.randomUUID();
        sku = new SKU();
        sku.setId(skuId);
        sku.setSku("LP-123");
        sku.setProduct(product);
        product.setSkus(Collections.singletonList(sku));

        skuDTO = new SKUDTO();
        skuDTO.setId(skuId);
        skuDTO.setSku("LP-123");
        skuDTO.setProductId(productId);
    }

    @Test
    void testGetSkuById_Success() {
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));
        when(skuMapper.toDto(any(SKU.class))).thenReturn(skuDTO);
        SKUDTO found = skuService.getSkuById(skuId);
        assertNotNull(found);
        assertEquals("LP-123", found.getSku());
    }

    @Test
    void testGetSkuById_NotFound() {
        when(skuRepository.findById(skuId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> skuService.getSkuById(skuId));
    }

    @Test
    void testGetSkusByProductId_Success() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(skuMapper.toDto(any(SKU.class))).thenReturn(skuDTO);
        assertEquals(1, skuService.getSkusByProductId(productId).size());
    }

    @Test
    void testGetSkusByProductId_ProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> skuService.getSkusByProductId(productId));
    }

    @Test
    void testCreateSku_Success() {
        SKUCreateDTO createDTO = new SKUCreateDTO();
        createDTO.setSku("LP-456");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(skuRepository.save(any(SKU.class))).thenReturn(sku);
        when(skuMapper.toDto(any(SKU.class))).thenReturn(skuDTO);

        SKUDTO result = skuService.createSku(productId, createDTO);
        assertNotNull(result);
    }

    @Test
    void testCreateSku_ProductNotFound() {
        SKUCreateDTO createDTO = new SKUCreateDTO();
        createDTO.setSku("LP-456");
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> skuService.createSku(productId, createDTO));
    }

    @Test
    void testUpdateSku_Success() {
        SKUCreateDTO updateDTO = new SKUCreateDTO();
        updateDTO.setSku("LP-789");

        SKUDTO updatedDTO = new SKUDTO();
        updatedDTO.setSku("LP-789");

        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));
        when(skuRepository.save(any(SKU.class))).thenReturn(sku);
        when(skuMapper.toDto(any(SKU.class))).thenReturn(updatedDTO);

        SKUDTO result = skuService.updateSku(skuId, updateDTO);
        assertEquals("LP-789", result.getSku());
    }

    @Test
    void testUpdateSku_NotFound() {
        SKUCreateDTO updateDTO = new SKUCreateDTO();
        updateDTO.setSku("LP-789");
        when(skuRepository.findById(skuId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> skuService.updateSku(skuId, updateDTO));
    }

    @Test
    void testDeleteSku_Success() {
        when(skuRepository.existsById(skuId)).thenReturn(true);
        doNothing().when(skuRepository).deleteById(skuId);
        skuService.deleteSku(skuId);
        verify(skuRepository, times(1)).deleteById(skuId);
    }

    @Test
    void testDeleteSku_NotFound() {
        when(skuRepository.existsById(skuId)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> skuService.deleteSku(skuId));
    }
}