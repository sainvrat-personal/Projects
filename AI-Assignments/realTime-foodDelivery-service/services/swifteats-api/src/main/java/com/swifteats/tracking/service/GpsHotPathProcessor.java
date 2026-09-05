package com.swifteats.tracking.service;

import com.swifteats.tracking.dto.GpsLocationEvent;
import org.springframework.stereotype.Service;

@Service
public class GpsHotPathProcessor {

    private final DriverLocationCacheService locationCacheService;

    public GpsHotPathProcessor(DriverLocationCacheService locationCacheService) {
        this.locationCacheService = locationCacheService;
    }

    public void process(GpsLocationEvent event) {
        locationCacheService.storeHotLocation(event);
    }
}
