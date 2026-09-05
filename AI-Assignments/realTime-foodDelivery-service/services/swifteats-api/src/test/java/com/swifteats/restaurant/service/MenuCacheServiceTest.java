package com.swifteats.restaurant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swifteats.restaurant.config.RestaurantProperties;
import com.swifteats.restaurant.dto.RestaurantMenuResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuCacheServiceTest {

    private static final UUID RESTAURANT_ID = UUID.randomUUID();

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private MenuCacheService menuCacheService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RestaurantProperties properties = new RestaurantProperties();
        properties.setCacheEnabled(true);
        properties.setMenuCacheTtl(Duration.ofMinutes(10));

        menuCacheService = new MenuCacheService(redisTemplate, objectMapper, properties);
    }

    @Test
    void putAndGetMenu_roundTripsJson() throws Exception {
        RestaurantMenuResponse response = new RestaurantMenuResponse(
                RESTAURANT_ID, "Misal House", true, 25, List.of(), Instant.parse("2026-08-21T18:00:00Z"));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("restaurant:" + RESTAURANT_ID + ":menu"))
                .thenReturn(new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(response));

        RestaurantMenuResponse cached = menuCacheService.getMenu(RESTAURANT_ID);

        assertThat(cached.restaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(cached.name()).isEqualTo("Misal House");
    }

    @Test
    void putMenu_writesToRedis() throws Exception {
        RestaurantMenuResponse response = new RestaurantMenuResponse(
                RESTAURANT_ID, "Misal House", true, 25, List.of(), Instant.now());

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        menuCacheService.putMenu(RESTAURANT_ID, response);

        verify(valueOperations).set(
                eq("restaurant:" + RESTAURANT_ID + ":menu"),
                anyString(),
                eq(Duration.ofMinutes(10)));
    }
}
