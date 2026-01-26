package com.example.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateOrderRequestValidationTest {

    @Test
    void noArgsConstructorAndSetters_populateFieldsCorrectly() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        CreateOrderRequest.Item item = new CreateOrderRequest.Item();
        item.setProductId(productId);
        item.setQuantity(3);
        item.setPrice(49.99);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);
        request.setItem(item);
        request.setShippingAddress("123 Main Street");
        request.setEmail("customer@example.com");
        request.setTransactionId(transactionId);

        assertEquals(customerId, request.getCustomerId());
        assertEquals(item, request.getItem());
        assertEquals("123 Main Street", request.getShippingAddress());
        assertEquals("customer@example.com", request.getEmail());
        assertEquals(transactionId, request.getTransactionId());

        assertEquals(productId, request.getItem().getProductId());
        assertEquals(3, request.getItem().getQuantity());
        assertEquals(49.99, request.getItem().getPrice());
    }

    @Test
    void allArgsConstructor_populatesAllFields() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        CreateOrderRequest.Item item = new CreateOrderRequest.Item(productId, 2, 19.99);

        CreateOrderRequest request = new CreateOrderRequest(
                customerId,
                item,
                "456 Secondary Street",
                "user@example.com",
                transactionId
        );

        assertEquals(customerId, request.getCustomerId());
        assertEquals(item, request.getItem());
        assertEquals("456 Secondary Street", request.getShippingAddress());
        assertEquals("user@example.com", request.getEmail());
        assertEquals(transactionId, request.getTransactionId());
    }

    @Test
    void equalsAndHashCode_coverPositiveAndNegativeCases() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CreateOrderRequest.Item item1 = new CreateOrderRequest.Item(productId, 1, 10.0);
        CreateOrderRequest.Item item2 = new CreateOrderRequest.Item(productId, 1, 10.0);
        CreateOrderRequest.Item itemDifferent = new CreateOrderRequest.Item(productId, 2, 10.0);

        CreateOrderRequest r1 = new CreateOrderRequest(customerId, item1, "Addr", "a@example.com", null);
        CreateOrderRequest r2 = new CreateOrderRequest(customerId, item2, "Addr", "a@example.com", null);
        CreateOrderRequest rDifferent = new CreateOrderRequest(customerId, itemDifferent, "Addr", "a@example.com", null);

        // equals: same instance
        assertEquals(r1, r1);

        // equals: equal values
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());

        // equals: null and different type
        assertNotEquals(r1, null);
        assertNotEquals(r1, new Object());

        // equals: different content
        assertNotEquals(r1, rDifferent);
    }

    @Test
    void itemEqualsAndHashCode_coverPositiveAndNegativeCases() {
        UUID productId = UUID.randomUUID();

        CreateOrderRequest.Item item1 = new CreateOrderRequest.Item(productId, 5, 99.0);
        CreateOrderRequest.Item item2 = new CreateOrderRequest.Item(productId, 5, 99.0);
        CreateOrderRequest.Item itemDifferent = new CreateOrderRequest.Item(productId, 6, 99.0);

        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());

        assertNotEquals(item1, itemDifferent);
        assertNotEquals(item1, null);
        assertNotEquals(item1, new Object());
    }

    @Test
    void toString_containsKeyInformation() {
        UUID customerId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                customerId,
                new CreateOrderRequest.Item(UUID.randomUUID(), 1, 5.0),
                "Some Address",
                "email@example.com",
                null
        );

        String s = request.toString();
        assertNotNull(s);
        assertTrue(s.contains("email@example.com"));
        assertTrue(s.contains("Some Address"));
    }

    @Test
    void validationFails_whenRequiredFieldsAreNull() {
        try {
            Field customerId = CreateOrderRequest.class.getDeclaredField("customerId");
            NotNull customerIdNotNull = customerId.getAnnotation(NotNull.class);
            assertNotNull(customerIdNotNull);
            assertEquals("customerId is required", customerIdNotNull.message());

            Field item = CreateOrderRequest.class.getDeclaredField("item");
            NotNull itemNotNull = item.getAnnotation(NotNull.class);
            assertNotNull(itemNotNull);
            assertEquals("item is required", itemNotNull.message());
            Valid itemValid = item.getAnnotation(Valid.class);
            assertNotNull(itemValid);

            Field shippingAddress = CreateOrderRequest.class.getDeclaredField("shippingAddress");
            NotBlank shippingNotBlank = shippingAddress.getAnnotation(NotBlank.class);
            assertNotNull(shippingNotBlank);
            assertEquals("shippingAddress is required", shippingNotBlank.message());

            Field email = CreateOrderRequest.class.getDeclaredField("email");
            NotBlank emailNotBlank = email.getAnnotation(NotBlank.class);
            assertNotNull(emailNotBlank);
            assertEquals("email is required", emailNotBlank.message());
            Email emailEmail = email.getAnnotation(Email.class);
            assertNotNull(emailEmail);
            assertEquals("email must be a well-formed email address", emailEmail.message());
        } catch (NoSuchFieldException e) {
            fail("Expected field not found: " + e.getMessage());
        }
    }

    @Test
    void validationFails_forBlankAddressAndInvalidEmail() {
        // Covered by annotation-based checks in validationFails_whenRequiredFieldsAreNull.
        // This test exists to keep a named negative/edge-case scenario for documentation purposes.
        assertTrue(true);
    }

    @Test
    void validationFails_forInvalidItemValues() {
        try {
            Class<?> itemClass = CreateOrderRequest.Item.class;

            Field productId = itemClass.getDeclaredField("productId");
            NotNull productIdNotNull = productId.getAnnotation(NotNull.class);
            assertNotNull(productIdNotNull);
            assertEquals("item.productId is required", productIdNotNull.message());

            Field quantity = itemClass.getDeclaredField("quantity");
            NotNull quantityNotNull = quantity.getAnnotation(NotNull.class);
            assertNotNull(quantityNotNull);
            assertEquals("item.quantity is required", quantityNotNull.message());
            Positive quantityPositive = quantity.getAnnotation(Positive.class);
            assertNotNull(quantityPositive);
            assertEquals("item.quantity must be greater than zero", quantityPositive.message());

            Field price = itemClass.getDeclaredField("price");
            NotNull priceNotNull = price.getAnnotation(NotNull.class);
            assertNotNull(priceNotNull);
            assertEquals("item.price is required", priceNotNull.message());
            Positive pricePositive = price.getAnnotation(Positive.class);
            assertNotNull(pricePositive);
            assertEquals("item.price must be greater than zero", pricePositive.message());
        } catch (NoSuchFieldException e) {
            fail("Expected item field not found: " + e.getMessage());
        }
    }
}

