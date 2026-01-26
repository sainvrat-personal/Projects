package com.ecom.inventory;

import com.ecom.inventory.dto.CategoryCreateDTO;
import com.ecom.inventory.dto.ProductCreateDTO;
import com.ecom.inventory.dto.SKUCreateDTO;
import com.ecom.inventory.model.Category;
import com.ecom.inventory.model.Product;
import com.ecom.inventory.model.SKU;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InventoryMgmtIntegrationTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13");

    @Container
    public static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:6.2"))
            .withExposedPorts(6379);

    @Autowired
    private TestRestTemplate restTemplate;

    private static UUID categoryId;
    private static UUID productId;
    private static UUID skuId;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    // --- CREATE ---
    @Test
    @Order(1)
    void testCreateCategory() {
        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setName("Electronics");
        ResponseEntity<Category> response = restTemplate.postForEntity("/api/categories", dto, Category.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        categoryId = response.getBody().getId();
        assertThat(categoryId).isNotNull();
    }

    @Test
    @Order(2)
    void testCreateCategory_Invalid() {
        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setName(""); // Blank name
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/categories", dto, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("name");
        assertThat(response.getBody().get("name")).isEqualTo("Category name cannot be blank");
    }

    @Test
    @Order(3)
    void testCreateProduct() {
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName("Laptop");
        String url = String.format("/api/products?categoryId=%s", categoryId);
        ResponseEntity<Product> response = restTemplate.postForEntity(url, dto, Product.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        productId = response.getBody().getId();
        assertThat(productId).isNotNull();
    }

    @Test
    @Order(4)
    void testCreateProduct_Invalid() {
        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName(""); // Blank name
        String url = String.format("/api/products?categoryId=%s", categoryId);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, dto, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("name");
        assertThat(response.getBody().get("name")).isEqualTo("Product name cannot be blank");
    }

    @Test
    @Order(5)
    void testCreateSku() {
        SKUCreateDTO dto = new SKUCreateDTO();
        dto.setSku("LP-123");
        dto.setPrice(1200.00);
        dto.setQuantity(10);
        String url = String.format("/api/products/%s/skus", productId);
        ResponseEntity<SKU> response = restTemplate.postForEntity(url, dto, SKU.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        skuId = response.getBody().getId();
        assertThat(skuId).isNotNull();
    }

    @Test
    @Order(6)
    void testCreateSku_Invalid() {
        SKUCreateDTO dto = new SKUCreateDTO();
        dto.setSku(""); // Blank SKU
        dto.setPrice(-10.0); // Negative price
        dto.setQuantity(-5); // Negative quantity
        String url = String.format("/api/products/%s/skus", productId);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, dto, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("sku");
        assertThat(response.getBody()).containsKey("price");
        assertThat(response.getBody()).containsKey("quantity");
    }

    // --- READ ---
    @Test
    @Order(7)
    void testGetAllCategories() {
        ResponseEntity<List<Category>> response = restTemplate.exchange("/api/categories", HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(1);
    }

    @Test
    @Order(8)
    void testGetAllProducts() {
        ResponseEntity<RestPage<Product>> response = restTemplate.exchange(
                "/api/products",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<RestPage<Product>>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    @Order(9)
    void testGetSkusForProduct() {
        String url = String.format("/api/products/%s/skus", productId);
        ResponseEntity<List<SKU>> response = restTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<>() {
                });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(1);
    }

    @Test
    @Order(10)
    void testGetByIdEndpoints() {
        // Category
        ResponseEntity<Category> catResponse = restTemplate.getForEntity("/api/categories/" + categoryId,
                Category.class);
        assertThat(catResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(catResponse.getBody()).isNotNull();
        assertThat(catResponse.getBody().getName()).isEqualTo("Electronics");

        // Product
        ResponseEntity<Product> prodResponse = restTemplate.getForEntity("/api/products/" + productId, Product.class);
        assertThat(prodResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(prodResponse.getBody()).isNotNull();
        assertThat(prodResponse.getBody().getName()).isEqualTo("Laptop");

        // SKU
        String skuUrl = String.format("/api/products/%s/skus/%s", productId, skuId);
        ResponseEntity<SKU> skuResponse = restTemplate.getForEntity(skuUrl, SKU.class);
        assertThat(skuResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(skuResponse.getBody()).isNotNull();
        assertThat(skuResponse.getBody().getSku()).isEqualTo("LP-123");
    }

    // --- UPDATE ---
    @Test
    @Order(11)
    void testUpdateCategory() {
        Category updatedDetails = new Category();
        updatedDetails.setName("Consumer Electronics");
        restTemplate.put("/api/categories/" + categoryId, updatedDetails);
        ResponseEntity<Category> response = restTemplate.getForEntity("/api/categories/" + categoryId, Category.class);
        assertThat(response.getBody().getName()).isEqualTo("Consumer Electronics");
    }

    @Test
    @Order(12)
    void testUpdateProduct() {
        Product updatedDetails = new Product();
        updatedDetails.setName("Gaming Laptop");
        restTemplate.put("/api/products/" + productId, updatedDetails);
        ResponseEntity<Product> response = restTemplate.getForEntity("/api/products/" + productId, Product.class);
        assertThat(response.getBody().getName()).isEqualTo("Gaming Laptop");
    }

    @Test
    @Order(13)
    void testUpdateSku() {
        SKU updatedDetails = new SKU();
        updatedDetails.setSku("LP-456");
        updatedDetails.setPrice(1250.00);
        updatedDetails.setQuantity(8);
        String url = String.format("/api/products/%s/skus/%s", productId, skuId);
        restTemplate.put(url, updatedDetails);
        ResponseEntity<SKU> response = restTemplate.getForEntity(url, SKU.class);
        assertThat(response.getBody().getSku()).isEqualTo("LP-456");
        assertThat(response.getBody().getPrice()).isEqualTo(1250.00);
    }

    // --- DELETE ---
    @Test
    @Order(14)
    void testDeleteSku() {
        String url = String.format("/api/products/%s/skus/%s", productId, skuId);
        ResponseEntity<String> deleteResponse = restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteResponse.getBody()).isEqualTo("sku deleted with id - " + skuId);

        ResponseEntity<Map<String, String>> getResponse = restTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, String>>() {
                });
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getResponse.getBody().get("error")).isEqualTo("SKU with specified id not found");
    }

    @Test
    @Order(15)
    void testDeleteProduct() {
        String productUrl = "/api/products/" + productId;
        ResponseEntity<String> deleteResponse = restTemplate.exchange(productUrl, HttpMethod.DELETE, null,
                String.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteResponse.getBody()).isEqualTo("product deleted with id - " + productId);

        ResponseEntity<Map<String, String>> getProductResponse = restTemplate.exchange(productUrl, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, String>>() {
                });
        assertThat(getProductResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getProductResponse.getBody().get("error")).isEqualTo("Product with specified id not found");

        String skusUrl = String.format("/api/products/%s/skus", productId);
        ResponseEntity<Map<String, String>> getSkusResponse = restTemplate.exchange(skusUrl, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, String>>() {
                });
        assertThat(getSkusResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getSkusResponse.getBody().get("error")).isEqualTo("Product with specified id not found");
    }

    @Test
    @Order(16)
    void testDeleteCategory() {
        String url = "/api/categories/" + categoryId;
        ResponseEntity<String> deleteResponse = restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleteResponse.getBody()).isEqualTo("category deleted with id - " + categoryId);

        ResponseEntity<Map<String, String>> getResponse = restTemplate.exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, String>>() {
                });
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(getResponse.getBody().get("error")).isEqualTo("Category with specified id not found");
    }
}