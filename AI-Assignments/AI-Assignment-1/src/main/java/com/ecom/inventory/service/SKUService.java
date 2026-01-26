package com.ecom.inventory.service;

import com.ecom.inventory.dto.SKUCreateDTO;
import com.ecom.inventory.dto.SKUDTO;
import com.ecom.inventory.exception.ResourceNotFoundException;
import com.ecom.inventory.mapper.SKUMapper;
import com.ecom.inventory.model.Product;
import com.ecom.inventory.model.SKU;
import com.ecom.inventory.repository.ProductRepository;
import com.ecom.inventory.repository.SKURepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing SKUs (Stock Keeping Units).
 * Handles business logic related to SKU operations and interacts with
 * repositories.
 */
@Service
public class SKUService {

    private static final Logger logger = LoggerFactory.getLogger(SKUService.class);

    @Autowired
    private SKURepository skuRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SKUMapper skuMapper;

    /**
     * Retrieves all SKUs for a given product.
     *
     * @param productId The UUID of the product.
     * @return A list of {@link SKUDTO} entities belonging to the product.
     * @throws ResourceNotFoundException if no product with the given ID is found.
     */
    public List<SKUDTO> getSkusByProductId(UUID productId) {
        logger.info("Fetching SKUs for product with id: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    logger.error("Product not found with id: {}", productId);
                    return new ResourceNotFoundException("Product with specified id not found");
                });
        return product.getSkus().stream().map(skuMapper::toDto).collect(Collectors.toList());
    }

    /**
     * Retrieves a single SKU by its ID.
     *
     * @param id The UUID of the SKU to retrieve.
     * @return The found {@link SKUDTO}.
     * @throws ResourceNotFoundException if no SKU with the given ID is found.
     */
    public SKUDTO getSkuById(UUID id) {
        logger.info("Fetching SKU with id: {}", id);
        return skuRepository.findById(id).map(skuMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("SKU with specified id not found"));
    }

    /**
     * Creates a new SKU for a given product.
     *
     * @param productId The UUID of the product to associate the SKU with.
     * @param skuDTO    The DTO containing the details for the new SKU.
     * @return The newly created {@link SKUDTO}.
     * @throws ResourceNotFoundException if no product with the given ID is found.
     */
    public SKUDTO createSku(UUID productId, SKUCreateDTO skuDTO) {
        logger.info("Creating SKU for product {} with data: {}", productId, skuDTO);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    logger.error("Product not found with id: {}", productId);
                    return new ResourceNotFoundException("Product not found");
                });
        SKU sku = new SKU();
        sku.setSku(skuDTO.getSku());
        sku.setPrice(skuDTO.getPrice());
        sku.setQuantity(skuDTO.getQuantity());
        sku.setProduct(product);
        SKU savedSku = skuRepository.save(sku);
        logger.info("Saved new SKU with id: {}", savedSku.getId());
        return skuMapper.toDto(savedSku);
    }

    /**
     * Updates an existing SKU.
     *
     * @param id         The UUID of the SKU to update.
     * @param skuDetails The new details for the SKU.
     * @return The updated {@link SKUDTO}.
     * @throws ResourceNotFoundException if no SKU with the given ID is found.
     */
    public SKUDTO updateSku(UUID id, SKUCreateDTO skuDetails) {
        logger.info("Updating SKU with id: {}", id);
        SKU sku = skuRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("SKU not found with id: {}", id);
                    return new ResourceNotFoundException("SKU not found");
                });
        sku.setSku(skuDetails.getSku());
        sku.setPrice(skuDetails.getPrice());
        sku.setQuantity(skuDetails.getQuantity());
        SKU updatedSku = skuRepository.save(sku);
        logger.info("Updated SKU with id: {}", updatedSku.getId());
        return skuMapper.toDto(updatedSku);
    }

    /**
     * Deletes an SKU by its ID.
     *
     * @param id The UUID of the SKU to delete.
     * @throws ResourceNotFoundException if no SKU with the given ID is found.
     */
    public void deleteSku(UUID id) {
        logger.info("Deleting SKU with id: {}", id);
        if (!skuRepository.existsById(id)) {
            throw new ResourceNotFoundException("SKU with specified id not found");
        }
        skuRepository.deleteById(id);
        logger.info("Deleted SKU with id: {}", id);
    }
}