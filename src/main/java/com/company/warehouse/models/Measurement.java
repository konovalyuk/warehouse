package com.company.warehouse.models;

import java.time.Instant;

public record Measurement(
        String sensorId,
        SensorType sensorType,
        double value,
        Instant receivedAt) {
}
