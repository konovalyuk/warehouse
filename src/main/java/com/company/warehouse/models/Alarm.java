package com.company.warehouse.models;

import java.time.Instant;

public record Alarm(
        String sensorId,
        SensorType sensorType,
        double measuredValue,
        double threshold,
        Instant occurredAt) {
}
