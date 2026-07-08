package com.cabsy.ride.controller;

import com.cabsy.ride.dto.RideRequestDto;
import com.cabsy.ride.dto.RideResponseDto;
import com.cabsy.ride.dto.RideStatusUpdateDto;
import com.cabsy.ride.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RideResponseDto requestRide(@Valid @RequestBody RideRequestDto dto) {
        return rideService.requestRide(dto);
    }

    @GetMapping("/{id}")
    public RideResponseDto getRide(@PathVariable String id) {
        return rideService.getRide(id);
    }

    @GetMapping
    public List<RideResponseDto> getRides(@RequestParam String riderId) {
        return rideService.getRidesForRider(riderId);
    }

    @PatchMapping("/{id}/status")
    public RideResponseDto updateStatus(@PathVariable String id, @Valid @RequestBody RideStatusUpdateDto dto) {
        return rideService.updateStatus(id, dto);
    }
}