package com.company.warehouse.services;

import com.company.warehouse.models.SensorType;
import com.company.warehouse.models.WarehouseProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThresholdPolicyTest {

    private final WarehouseProperties properties = new WarehouseProperties(true,
            new WarehouseProperties.UdpProperties(3344, 3355),
            new WarehouseProperties.ThresholdProperties(35.0, 50.0));

    private final ThresholdPolicy policy = new ThresholdPolicy(properties);

    @Test
    void shouldReturnTemperatureThreshold() {
        assertEquals(35.0, policy.getThreshold(SensorType.TEMPERATURE));
    }

    @Test
    void shouldReturnHumidityThreshold() {
        assertEquals(50.0, policy.getThreshold(SensorType.HUMIDITY));
    }

    @Test
    void shouldNotExceedTemperatureThresholdBelowLimit() {
        assertFalse(policy.isExceeded(SensorType.TEMPERATURE, 34.9));
    }

    @Test
    void shouldNotExceedTemperatureThresholdAtLimit() {
        assertFalse(policy.isExceeded(SensorType.TEMPERATURE, 35.0));
    }

    @Test
    void shouldExceedTemperatureThresholdAboveLimit() {
        assertTrue(policy.isExceeded(SensorType.TEMPERATURE, 35.1));
    }

    @Test
    void shouldNotExceedHumidityThresholdBelowLimit() {
        assertFalse(policy.isExceeded(SensorType.HUMIDITY, 49.9));
    }

    @Test
    void shouldNotExceedHumidityThresholdAtLimit() {
        assertFalse(policy.isExceeded(SensorType.HUMIDITY, 50.0));
    }

    @Test
    void shouldExceedHumidityThresholdAboveLimit() {
        assertTrue(policy.isExceeded(SensorType.HUMIDITY, 50.1));
    }

}