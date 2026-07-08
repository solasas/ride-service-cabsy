package com.cabsy.ride.event;

import com.cabsy.ride.entity.RideStatus;

import java.time.Instant;

public record RideStatusChangedEvent(
        String rideId,
        String riderId,
        String driverId,
        RideStatus oldStatus,
        RideStatus newStatus,
        Instant changedAt
) {
}