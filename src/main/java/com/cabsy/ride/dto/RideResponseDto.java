package com.cabsy.ride.dto;

import com.cabsy.ride.entity.RideStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record RideResponseDto(
        String id,
        String riderId,
        String driverId,
        double pickupLat,
        double pickupLng,
        double dropoffLat,
        double dropoffLng,
        RideStatus status,
        Instant requestedAt,
        Instant matchedAt,
        Instant completedAt,
        BigDecimal fareEstimate
) {
}