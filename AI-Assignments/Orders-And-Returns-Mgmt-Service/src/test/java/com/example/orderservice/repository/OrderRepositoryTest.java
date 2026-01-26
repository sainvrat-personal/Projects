package com.example.orderservice.repository;

import com.example.orderservice.dto.OrderStatus;
import com.example.orderservice.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private Order order1;
    private Order order2;
    private Order order3;
    private UUID customerId1;
    private UUID customerId2;

    @BeforeEach
    void setUp() {
        customerId1 = UUID.randomUUID();
        customerId2 = UUID.randomUUID();

        order1 = new Order();
        order1.setId(UUID.randomUUID());
        order1.setTransactionId(UUID.randomUUID());
        order1.setCustomerId(customerId1);
        order1.setProductId(UUID.randomUUID());
        order1.setQuantity(2);
        order1.setPrice(99.99);
        order1.setStatus(OrderStatus.PENDING_PAYMENT);
        order1.setCreatedAt(Instant.now());
        order1.setShippingAddress("123 Test St, Test City, TC 12345");
        order1.setEmail("test1@gmail.com");

        order2 = new Order();
        order2.setId(UUID.randomUUID());
        order2.setTransactionId(UUID.randomUUID());
        order2.setCustomerId(customerId1); // Same customer
        order2.setProductId(UUID.randomUUID());
        order2.setQuantity(1);
        order2.setPrice(149.99);
        order2.setStatus(OrderStatus.PAID);
        order2.setCreatedAt(Instant.now());
        order2.setShippingAddress("456 Another St, Another City, AC 67890");
        order2.setEmail("test1@gmail.com");

        order3 = new Order();
        order3.setId(UUID.randomUUID());
        order3.setTransactionId(UUID.randomUUID());
        order3.setCustomerId(customerId2); // Different customer
        order3.setProductId(UUID.randomUUID());
        order3.setQuantity(3);
        order3.setPrice(29.99);
        order3.setStatus(OrderStatus.DELIVERED);
        order3.setCreatedAt(Instant.now());
        order3.setShippingAddress("789 Third St, Third City, TC 13579");
        order3.setEmail("test2@gmail.com");
    }

    @Test
    void testSaveAndFindById() {
        // When
        Order savedOrder = orderRepository.save(order1);
        Optional<Order> foundOrder = orderRepository.findById(order1.getId());

        // Then
        assertNotNull(savedOrder);
        assertTrue(foundOrder.isPresent());
        assertEquals(order1.getId(), foundOrder.get().getId());
        assertEquals(order1.getCustomerId(), foundOrder.get().getCustomerId());
        assertEquals(order1.getStatus(), foundOrder.get().getStatus());
        assertEquals(order1.getEmail(), foundOrder.get().getEmail());
        assertEquals(order1.getShippingAddress(), foundOrder.get().getShippingAddress());
    }

    @Test
    void testFindById_NotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<Order> foundOrder = orderRepository.findById(nonExistentId);

        // Then
        assertFalse(foundOrder.isPresent());
    }

    @Test
    void testFindAll() {
        // Given
        orderRepository.save(order1);
        orderRepository.save(order2);
        orderRepository.save(order3);

        // When
        List<Order> allOrders = orderRepository.findAll();

        // Then
        assertEquals(3, allOrders.size());
    }

    @Test
    void testUpdateOrder() {
        // Given
        Order savedOrder = orderRepository.save(order1);
        assertEquals(OrderStatus.PENDING_PAYMENT, savedOrder.getStatus());

        // When - Update the status
        savedOrder.setStatus(OrderStatus.PAID);
        Order updatedOrder = orderRepository.save(savedOrder);

        // Then
        assertEquals(OrderStatus.PAID, updatedOrder.getStatus());
        
        // Verify in database
        Optional<Order> foundOrder = orderRepository.findById(order1.getId());
        assertTrue(foundOrder.isPresent());
        assertEquals(OrderStatus.PAID, foundOrder.get().getStatus());
    }

    @Test
    void testDeleteOrder() {
        // Given
        Order savedOrder = orderRepository.save(order1);
        assertTrue(orderRepository.findById(order1.getId()).isPresent());

        // When
        orderRepository.delete(savedOrder);

        // Then
        assertFalse(orderRepository.findById(order1.getId()).isPresent());
    }

    @Test
    void testDeleteById() {
        // Given
        orderRepository.save(order1);
        assertTrue(orderRepository.findById(order1.getId()).isPresent());

        // When
        orderRepository.deleteById(order1.getId());

        // Then
        assertFalse(orderRepository.findById(order1.getId()).isPresent());
    }

    @Test
    void testExistsById() {
        // Given
        orderRepository.save(order1);

        // When & Then
        assertTrue(orderRepository.existsById(order1.getId()));
        assertFalse(orderRepository.existsById(UUID.randomUUID()));
    }

    @Test
    void testCount() {
        // Given
        assertEquals(0, orderRepository.count());

        // When
        orderRepository.save(order1);
        orderRepository.save(order2);

        // Then
        assertEquals(2, orderRepository.count());
    }

    @Test
    void testSaveWithNullValues() {
        // Given
        Order orderWithNulls = new Order();
        orderWithNulls.setId(UUID.randomUUID());
        orderWithNulls.setTransactionId(UUID.randomUUID());
        orderWithNulls.setCustomerId(null); // Null customer ID
        orderWithNulls.setProductId(UUID.randomUUID()); // Required field
        orderWithNulls.setQuantity(1); // Required field
        orderWithNulls.setPrice(10.0); // Required field
        orderWithNulls.setStatus(OrderStatus.PENDING_PAYMENT);
        orderWithNulls.setCreatedAt(Instant.now());
        orderWithNulls.setShippingAddress(null); // Null address
        orderWithNulls.setEmail(null); // Null email

        // When/Then - Expect DataIntegrityViolationException due to null customerId
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            orderRepository.save(orderWithNulls);
        });
    }

    @Test
    void testSaveWithLongStrings() {
        // Given
        StringBuilder longAddress = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longAddress.append("Very long address part ");
        }
        
        StringBuilder longEmail = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longEmail.append("verylongemailpart");
        }
        longEmail.append("@gmail.com");

        order1.setShippingAddress(longAddress.toString());
        order1.setEmail(longEmail.toString());

        // When
        Order savedOrder = orderRepository.save(order1);

        // Then
        assertNotNull(savedOrder);
        assertEquals(longAddress.toString(), savedOrder.getShippingAddress());
        assertEquals(longEmail.toString(), savedOrder.getEmail());
    }

    @Test
    void testMultipleOrdersForSameCustomer() {
        // Given
        orderRepository.save(order1);
        orderRepository.save(order2); // Both have same customerId1

        // When
        List<Order> allOrders = orderRepository.findAll();

        // Then
        assertEquals(2, allOrders.size());
        assertTrue(allOrders.stream().allMatch(o -> o.getCustomerId().equals(customerId1)));
    }

    @Test
    void testOrderStateConsistency() {
        // Given - Test all order statuses
        OrderStatus[] allStatuses = OrderStatus.values();
        
        for (int i = 0; i < allStatuses.length; i++) {
            Order testOrder = new Order();
            testOrder.setId(UUID.randomUUID());
            testOrder.setTransactionId(UUID.randomUUID());
            testOrder.setCustomerId(UUID.randomUUID());
            testOrder.setProductId(UUID.randomUUID());
            testOrder.setQuantity(1);
            testOrder.setPrice(50.0);
            testOrder.setStatus(allStatuses[i]);
            testOrder.setCreatedAt(Instant.now());
            testOrder.setShippingAddress("Test Address " + i);
            testOrder.setEmail("test" + i + "@gmail.com");

            // When
            Order savedOrder = orderRepository.save(testOrder);

            // Then
            assertEquals(allStatuses[i], savedOrder.getStatus());
        }
    }

    @Test
    void testTransactionIsolation() {
        // Given
        orderRepository.save(order1);

        // When - Simulate concurrent access
        Optional<Order> order1Read1 = orderRepository.findById(order1.getId());
        Optional<Order> order1Read2 = orderRepository.findById(order1.getId());

        // Then
        assertTrue(order1Read1.isPresent());
        assertTrue(order1Read2.isPresent());
        assertEquals(order1Read1.get().getId(), order1Read2.get().getId());
        assertEquals(order1Read1.get().getStatus(), order1Read2.get().getStatus());
    }

    @Test
    void testFlushAndClear() {
        // Given
        Order savedOrder = orderRepository.save(order1);
        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Order> foundOrder = orderRepository.findById(order1.getId());

        // Then
        assertTrue(foundOrder.isPresent());
        assertEquals(savedOrder.getId(), foundOrder.get().getId());
    }
}