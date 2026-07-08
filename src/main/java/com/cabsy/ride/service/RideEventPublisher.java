package com.cabsy.ride.service;

import com.cabsy.ride.event.RideRequestedEvent;
import com.cabsy.ride.event.RideStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RideEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String rideRequestedTopic;
    private final String rideStatusChangedTopic;

    public RideEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${cabsy.kafka.topics.ride-requested}") String rideRequestedTopic,
            @Value("${cabsy.kafka.topics.ride-status-changed}") String rideStatusChangedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.rideRequestedTopic = rideRequestedTopic;
        this.rideStatusChangedTopic = rideStatusChangedTopic;
    }

    public void publishRideRequested(RideRequestedEvent event) {
        send(rideRequestedTopic, event.rideId(), event);
    }

    public void publishRideStatusChanged(RideStatusChangedEvent event) {
        send(rideStatusChangedTopic, event.rideId(), event);
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.warn("Failed to publish event to Kafka topic '{}': {}", topic, ex.getMessage());
                }
            });
        } catch (Exception ex) {
            log.warn("Failed to publish event to Kafka topic '{}': {}", topic, ex.getMessage());
        }
    }
}