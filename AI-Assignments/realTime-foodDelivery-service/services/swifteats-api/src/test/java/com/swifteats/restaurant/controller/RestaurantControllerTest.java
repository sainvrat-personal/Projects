package com.swifteats.restaurant.controller;

import com.swifteats.common.config.SecurityConfig;
import com.swifteats.common.security.AdminApiKeyFilter;
import com.swifteats.common.security.GpsRateLimiter;
import com.swifteats.order.repository.CustomerRepository;
import com.swifteats.restaurant.dto.MenuItemResponse;
import com.swifteats.restaurant.dto.RestaurantMenuResponse;
import com.swifteats.restaurant.dto.RestaurantPageResponse;
import com.swifteats.restaurant.dto.RestaurantSummaryResponse;
import com.swifteats.restaurant.service.RestaurantService;
import com.swifteats.tracking.repository.DriverRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RestaurantController.class, AdminRestaurantController.class})
@Import({AdminApiKeyFilter.class, SecurityConfig.class})
@TestPropertySource(properties = {
        "swifteats.admin.api-key=dev-admin-key",
        "swifteats.security.allow-insecure-defaults=true"
})
class RestaurantControllerTest {

    private static final UUID RESTAURANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantService restaurantService;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private DriverRepository driverRepository;

    @MockBean
    private GpsRateLimiter gpsRateLimiter;

    @Test
    void listRestaurants_returnsPage() throws Exception {
        RestaurantSummaryResponse summary = new RestaurantSummaryResponse(
                RESTAURANT_ID, "Misal House", "Pune", BigDecimal.valueOf(4.5), true,
                List.of("Maharashtrian"), 25);
        when(restaurantService.searchRestaurants(eq("Pune"), eq(null), eq(null), eq(null), eq(null), eq(0), eq(20)))
                .thenReturn(new RestaurantPageResponse(List.of(summary), 0, 20, 1));

        mockMvc.perform(get("/api/v1/restaurants").param("city", "Pune"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Misal House"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getMenu_returnsMenuBundle() throws Exception {
        RestaurantMenuResponse menu = new RestaurantMenuResponse(
                RESTAURANT_ID,
                "Misal House",
                true,
                25,
                List.of(new MenuItemResponse(UUID.randomUUID(), "Kolhapuri Misal", "Main", BigDecimal.valueOf(120), true)),
                Instant.parse("2026-08-21T18:00:00Z"));
        when(restaurantService.getMenu(RESTAURANT_ID)).thenReturn(menu);

        mockMvc.perform(get("/api/v1/restaurants/{id}/menu", RESTAURANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Misal House"))
                .andExpect(jsonPath("$.menuItems[0].name").value("Kolhapuri Misal"));
    }

    @Test
    void adminCreate_requiresApiKey() throws Exception {
        mockMvc.perform(post("/api/v1/admin/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Test",
                                  "address": "Addr",
                                  "city": "Pune",
                                  "cuisines": ["Biryani"]
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
