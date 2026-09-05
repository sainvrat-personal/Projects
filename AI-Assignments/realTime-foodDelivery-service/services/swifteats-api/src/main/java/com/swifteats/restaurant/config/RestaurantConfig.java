package com.swifteats.restaurant.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RestaurantProperties.class)
public class RestaurantConfig {
}
