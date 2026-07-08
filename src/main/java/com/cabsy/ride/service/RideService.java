package com.cabsy.ride.service;

import com.cabsy.ride.dto.RideRequestDto;
import com.cabsy.ride.dto.RideResponseDto;
import com.cabsy.ride.dto.RideStatusUpdateDto;
import com.cabsy.ride.entity.Ride;
import com.cabsy.ride.entity.RideStatus;
import com.cabsy.ride.event.RideRequestedEvent;
import com.cabsy.ride.event.RideStatusChangedEvent;
import com.cabsy.ride.exception.InvalidRideStateTransitionException;
import com.cabsy.ride.exception.RideNotFoundException;
import com.cabsy.ride.repository.RideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RideService {

    private static final Map<RideStatus, Set<RideStatus>> ALLOWED_TRANSITIONS = Map.of(
            RideStatus.REQUESTED, EnumSet.of(RideStatus.MATCHED, RideStatus.CANCELLED),
            RideStatus.MATCHED, EnumSet.of(RideStatus.ONGOING, RideStatus.CANCELLED),
            RideStatus.ONGOING, EnumSet.of(RideStatus.COMPLETED),
            RideStatus.COMPLETED, EnumSet.noneOf(RideStatus.class),
            RideStatus.CANCELLED, EnumSet.noneOf(RideStatus.class)
    );

    private final RideRepository rideRepository;
    private final RideEventPublisher rideEventPublisher;

    public RideService(RideRepository rideRepository, RideEventPublisher rideEventPublisher) {
        this.rideRepository = rideRepository;
        this.rideEventPublisher = rideEventPublisher;
    }

    @Transactional
    public RideResponseDto requestRide(RideRequestDto dto) {
        Ride ride = new Ride();
        ride.setRiderId(dto.riderId());
        ride.setPickupLat(dto.pickupLat());
        ride.setPickupLng(dto.pickupLng());
        ride.setDropoffLat(dto.dropoffLat());
        ride.setDropoffLng(dto.dropoffLng());
        ride.setStatus(RideStatus.REQUESTED);
        ride.setRequestedAt(Instant.now());

        Ride saved = rideRepository.save(ride);

        rideEventPublisher.publishRideRequested(new RideRequestedEvent(
                saved.getId(),
                saved.getRiderId(),
                saved.getPickupLat(),
                saved.getPickupLng(),
                saved.getDropoffLat(),
                saved.getDropoffLng(),
                saved.getRequestedAt()
        ));

        return toDto(saved);
    }

    public RideResponseDto getRide(String rideId) {
        return toDto(findRideOrThrow(rideId));
    }

    public List<RideResponseDto> getRidesForRider(String riderId) {
        return rideRepository.findByRiderId(riderId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public RideResponseDto updateStatus(String rideId, RideStatusUpdateDto dto) {
        Ride ride = findRideOrThrow(rideId);
        RideStatus oldStatus = ride.getStatus();
        RideStatus newStatus = dto.status();

        if (!ALLOWED_TRANSITIONS.getOrDefault(oldStatus, EnumSet.noneOf(RideStatus.class)).contains(newStatus)) {
            throw new InvalidRideStateTransitionException(
                    "Cannot transition ride from %s to %s".formatted(oldStatus, newStatus));
        }

        if (newStatus == RideStatus.MATCHED) {
            if (dto.driverId() == null || dto.driverId().isBlank()) {
                throw new InvalidRideStateTransitionException("driverId is required when transitioning to MATCHED");
            }
            ride.setDriverId(dto.driverId());
            ride.setMatchedAt(Instant.now());
        } else if (newStatus == RideStatus.COMPLETED) {
            ride.setCompletedAt(Instant.now());
        }

        ride.setStatus(newStatus);
        Ride saved = rideRepository.save(ride);

        rideEventPublisher.publishRideStatusChanged(new RideStatusChangedEvent(
                saved.getId(),
                saved.getRiderId(),
                saved.getDriverId(),
                oldStatus,
                newStatus,
                Instant.now()
        ));

        return toDto(saved);
    }

    private Ride findRideOrThrow(String rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Ride not found: " + rideId));
    }

    private RideResponseDto toDto(Ride ride) {
        return new RideResponseDto(
                ride.getId(),
                ride.getRiderId(),
                ride.getDriverId(),
                ride.getPickupLat(),
                ride.getPickupLng(),
                ride.getDropoffLat(),
                ride.getDropoffLng(),
                ride.getStatus(),
                ride.getRequestedAt(),
                ride.getMatchedAt(),
                ride.getCompletedAt(),
                ride.getFareEstimate()
        );
    }
}