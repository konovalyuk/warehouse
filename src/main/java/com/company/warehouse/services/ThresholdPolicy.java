package com.company.warehouse.services;

import com.company.warehouse.models.SensorType;
import com.company.warehouse.models.WarehouseProperties;
import org.springframework.stereotype.Service;

@Service
public class ThresholdPolicy {

    private final WarehouseProperties.ThresholdProperties thresholds;

    public ThresholdPolicy(WarehouseProperties properties) {
        this.thresholds = properties.threshold();
    }

    public double getThreshold(SensorType sensorType) {
        return switch (sensorType) {
            case TEMPERATURE -> thresholds.temperature();
            case HUMIDITY -> thresholds.humidity();
        };
    }

    public boolean isExceeded(SensorType sensorType, double value) {
        return value > getThreshold(sensorType);
    }

}