package com.company.warehouse.services;

import com.company.warehouse.models.Measurement;
import com.company.warehouse.models.SensorType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class MeasurementParserTest {

    private final MeasurementParser parser = new MeasurementParser();

    @Test
    void shouldParseTemperatureMeasurement() {
        Measurement measurement = parser.parse("sensor_id=t1; value=30", SensorType.TEMPERATURE);

        assertEquals("t1", measurement.sensorId());
        assertEquals(SensorType.TEMPERATURE, measurement.sensorType());
        assertEquals(30.0, measurement.value());
        assertNotNull(measurement.receivedAt());
    }

    @Test
    void shouldParseHumidityMeasurement() {
        Measurement measurement = parser.parse("sensor_id=h1; value=40", SensorType.HUMIDITY);

        assertEquals("h1", measurement.sensorId());
        assertEquals(SensorType.HUMIDITY, measurement.sensorType());
        assertEquals(40.0, measurement.value());
        assertNotNull(measurement.receivedAt());
    }

    @Test
    void shouldIgnoreWhitespace() {
        Measurement measurement = parser.parse("  sensor_id = t1 ;   value = 30  ", SensorType.TEMPERATURE);

        assertEquals("t1", measurement.sensorId());
        assertEquals(30.0, measurement.value());
    }

    @Test
    void shouldAcceptFieldsInAnyOrder() {
        Measurement measurement = parser.parse("value=30; sensor_id=t1", SensorType.TEMPERATURE);

        assertEquals("t1", measurement.sensorId());
        assertEquals(30.0, measurement.value());
    }

    @Test
    void shouldRejectNullMessage() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null, SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectBlankMessage() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("   ", SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectMissingSensorId() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("value=30", SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectEmptySensorId() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=; value=30", SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectMissingValue() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1", SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectEmptyValue() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1; value=", SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectInvalidNumericValue() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1; value=abc", SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectNaN() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1; value=NaN", SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectPositiveInfinity() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1; value=Infinity", SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectNegativeInfinity() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1; value=-Infinity", SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectMalformedField() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1; value", SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectDuplicateField() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1; sensor_id=t2; value=30", SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectEmptyFieldName() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("=t1; value=30", SensorType.TEMPERATURE));
    }

    @Test
    void shouldRejectEmptyFieldValue() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("sensor_id=t1; value= ", SensorType.TEMPERATURE));
    }

    @Test
    void shouldCreateReceivedTimestampCloseToCurrentTime() {
        Instant before = Instant.now();
        Measurement measurement = parser.parse("sensor_id=t1; value=30", SensorType.TEMPERATURE);
        Instant after = Instant.now();

        assertFalse(measurement.receivedAt().isBefore(before));
        assertFalse(measurement.receivedAt().isAfter(after));
    }

}
