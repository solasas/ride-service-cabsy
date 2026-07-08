package com.cabsy.ride.dto;

import com.cabsy.ride.entity.RideStatus;
import jakarta.validation.constraints.NotNull;

public record RideStatusUpdateDto(
        @NotNull RideStatus status,
        String driverId
) {
}