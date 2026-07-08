package com.cabsy.ride.repository;

import com.cabsy.ride.entity.Ride;
import com.cabsy.ride.entity.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RideRepository extends JpaRepository<Ride, String> {

    List<Ride> findByRiderId(String riderId);

    List<Ride> findByStatus(RideStatus status);
}
