package com.cabsy.ride.exception;

public class InvalidRideStateTransitionException extends RuntimeException {

    public InvalidRideStateTransitionException(String message) {
        super(message);
    }
}