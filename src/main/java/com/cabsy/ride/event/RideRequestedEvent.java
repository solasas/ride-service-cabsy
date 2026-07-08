package com.cabsy.ride.event;

import java.time.Instant;

public record RideRequestedEvent(
        String rideId,
        String riderId,
        double pickupLat,
        double pickupLng,
        double dropoffLat,
        double dropoffLng,
        Instant requestedAt
) {
}