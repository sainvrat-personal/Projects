package com.swifteats.restaurant.service;

import com.swifteats.common.domain.RestaurantStatus;
import com.swifteats.common.exception.ResourceNotFoundException;
import com.swifteats.restaurant.dto.CreateRestaurantRequest;
import com.swifteats.restaurant.dto.RestaurantMenuResponse;
import com.swifteats.restaurant.dto.RestaurantPageResponse;
import com.swifteats.restaurant.entity.MenuItem;
import com.swifteats.restaurant.entity.Restaurant;
import com.swifteats.restaurant.mapper.RestaurantMapper;
import com.swifteats.restaurant.repository.CuisineRepository;
import com.swifteats.restaurant.repository.MenuItemRepository;
import com.swifteats.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    private static final UUID RESTAURANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private CuisineRepository cuisineRepository;
    @Mock
    private RestaurantMapper restaurantMapper;
    @Mock
    private MenuCacheService menuCacheService;

    @InjectMocks
    private RestaurantService restaurantService;

    @Test
    void getMenu_returnsCachedValueWhenPresent() {
        RestaurantMenuResponse cached = new RestaurantMenuResponse(
                RESTAURANT_ID, "Misal House", true, 25, List.of(), Instant.now());
        when(menuCacheService.getMenu(RESTAURANT_ID)).thenReturn(cached);

        RestaurantMenuResponse result = restaurantService.getMenu(RESTAURANT_ID);

        assertThat(result).isSameAs(cached);
        verify(restaurantRepository, never()).findActiveWithMenu(any(), any());
    }

    @Test
    void getMenu_loadsFromDatabaseOnCacheMiss() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        RestaurantMenuResponse loaded = new RestaurantMenuResponse(
                RESTAURANT_ID, "Misal House", true, 25, List.of(), Instant.now());

        when(menuCacheService.getMenu(RESTAURANT_ID)).thenReturn(null);
        when(restaurantRepository.findActiveWithMenu(RESTAURANT_ID, RestaurantStatus.ACTIVE))
                .thenReturn(Optional.of(restaurant));
        when(restaurantMapper.toMenuResponse(eq(restaurant), any(Instant.class))).thenReturn(loaded);

        RestaurantMenuResponse result = restaurantService.getMenu(RESTAURANT_ID);

        assertThat(result).isSameAs(loaded);
        verify(menuCacheService).putMenu(RESTAURANT_ID, loaded);
    }

    @Test
    void getMenu_throwsWhenRestaurantNotActive() {
        when(menuCacheService.getMenu(RESTAURANT_ID)).thenReturn(null);
        when(restaurantRepository.findActiveWithMenu(RESTAURANT_ID, RestaurantStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getMenu(RESTAURANT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void approveRestaurant_setsActiveAndOpen() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        restaurant.setStatus(RestaurantStatus.PENDING);

        when(restaurantRepository.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);
        when(restaurantMapper.toDetail(restaurant)).thenReturn(null);

        restaurantService.approveRestaurant(RESTAURANT_ID);

        assertThat(restaurant.getStatus()).isEqualTo(RestaurantStatus.ACTIVE);
        assertThat(restaurant.isOpen()).isTrue();
        verify(menuCacheService).invalidateMenu(RESTAURANT_ID);
        verify(menuCacheService).invalidateListCaches();
    }

    @Test
    void searchRestaurants_usesCacheWhenAvailable() {
        RestaurantPageResponse cached = new RestaurantPageResponse(List.of(), 0, 20, 0);
        when(menuCacheService.hashListQuery(null, null, null, null, null, 0, 20)).thenReturn("abc");
        when(menuCacheService.getList("abc")).thenReturn(cached);

        RestaurantPageResponse result = restaurantService.searchRestaurants(
                null, null, null, null, null, 0, 20);

        assertThat(result).isSameAs(cached);
        verify(restaurantRepository, never()).searchActive(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createRestaurant_startsAsPending() {
        CreateRestaurantRequest request = new CreateRestaurantRequest(
                "New Place", "FC Road", "Pune", List.of("Biryani"), "a@b.com", null, null);
        Restaurant saved = new Restaurant();
        saved.setId(UUID.randomUUID());
        saved.setStatus(RestaurantStatus.PENDING);

        when(cuisineRepository.findByName("Biryani")).thenReturn(Optional.empty());
        when(cuisineRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(saved);
        when(restaurantMapper.toDetail(saved)).thenReturn(null);

        restaurantService.createRestaurant(request);

        verify(restaurantRepository).save(any(Restaurant.class));
        verify(menuCacheService).invalidateListCaches();
    }
}
