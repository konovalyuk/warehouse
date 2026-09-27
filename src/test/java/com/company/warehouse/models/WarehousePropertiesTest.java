package com.company.warehouse.models;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehousePropertiesTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidProperties() {
        WarehouseProperties properties = new WarehouseProperties(true,
                        new WarehouseProperties.UdpProperties(3344, 3355),
                        new WarehouseProperties.ThresholdProperties(35.0, 50.0));
        Set<ConstraintViolation<WarehouseProperties>> violations = validator.validate(properties);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectTemperaturePortBelowMinimum() {
        WarehouseProperties properties = new WarehouseProperties(true,
                        new WarehouseProperties.UdpProperties(0, 3355),
                        new WarehouseProperties.ThresholdProperties(35.0, 50.0));

        Set<ConstraintViolation<WarehouseProperties>> violations = validator.validate(properties);
        assertEquals(1, violations.size());
    }

    @Test
    void shouldRejectHumidityPortAboveMaximum() {
        WarehouseProperties properties = new WarehouseProperties(true,
                        new WarehouseProperties.UdpProperties(3344, 65536),
                        new WarehouseProperties.ThresholdProperties(35.0, 50.0));

        Set<ConstraintViolation<WarehouseProperties>> violations = validator.validate(properties);
        assertEquals(1, violations.size());
    }

    @Test
    void shouldRejectNegativeTemperatureThreshold() {
        WarehouseProperties properties = new WarehouseProperties(true,
                        new WarehouseProperties.UdpProperties(3344, 3355),
                        new WarehouseProperties.ThresholdProperties(-1.0, 50.0));

        Set<ConstraintViolation<WarehouseProperties>> violations = validator.validate(properties);
        assertEquals(1, violations.size());
    }

    @Test
    void shouldRejectNegativeHumidityThreshold() {
        WarehouseProperties properties = new WarehouseProperties(true,
                        new WarehouseProperties.UdpProperties(3344, 3355),
                        new WarehouseProperties.ThresholdProperties(35.0, -1.0));

        Set<ConstraintViolation<WarehouseProperties>> violations = validator.validate(properties);
        assertEquals(1, violations.size());
    }
}