package com.company.warehouse.services;

import com.company.warehouse.models.Measurement;
import com.company.warehouse.models.SensorType;
import com.company.warehouse.models.WarehouseProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CentralMonitoringServiceTest {

    private ThresholdPolicy thresholdPolicy;
    private AlarmService alarmService;
    private CentralMonitoringService service;

    @BeforeEach
    void setUp() {
        var properties = new WarehouseProperties(true,
                new WarehouseProperties.UdpProperties(3344, 3355),
                new WarehouseProperties.ThresholdProperties(35.0, 50.0));

        thresholdPolicy = new ThresholdPolicy(properties);
        alarmService = mock(AlarmService.class);

        service = new CentralMonitoringService(thresholdPolicy, alarmService);
    }

    @Test
    void shouldActivateTemperatureAlarmAboveThreshold() {
        var measurement = new Measurement("t1", SensorType.TEMPERATURE, 36.0, Instant.now());
        service.publish(measurement).block();
        verify(alarmService).activate(measurement, 35.0);
    }

    @Test
    void shouldNotActivateTemperatureAlarmAtThreshold() {
        var measurement = new Measurement("t1", SensorType.TEMPERATURE, 35.0, Instant.now());
        service.publish(measurement).block();
        verifyNoInteractions(alarmService);
    }

    @Test
    void shouldNotActivateTemperatureAlarmBelowThreshold() {
        var measurement = new Measurement("t1", SensorType.TEMPERATURE, 34.0, Instant.now());
        service.publish(measurement).block();
        verifyNoInteractions(alarmService);
    }

    @Test
    void shouldActivateHumidityAlarmAboveThreshold() {
        var measurement = new Measurement("h1", SensorType.HUMIDITY, 51.0, Instant.now());
        service.publish(measurement).block();
        verify(alarmService).activate(measurement, 50.0);
    }

    @Test
    void shouldNotActivateHumidityAlarmAtThreshold() {
        var measurement = new Measurement("h1", SensorType.HUMIDITY, 50.0, Instant.now());
        service.publish(measurement).block();
        verifyNoInteractions(alarmService);
    }

    @Test
    void shouldNotActivateHumidityAlarmBelowThreshold() {
        var measurement = new Measurement("h1", SensorType.HUMIDITY, 49.0, Instant.now());
        service.publish(measurement).block();
        verifyNoInteractions(alarmService);
    }

    @Test
    void shouldActivateAlarmForEveryExceededMeasurement() {
        var first = new Measurement("t1", SensorType.TEMPERATURE, 36.0, Instant.now());
        var second = new Measurement("t1", SensorType.TEMPERATURE, 40.0, Instant.now());

        service.publish(first).block();
        service.publish(second).block();

        verify(alarmService, times(2)).activate(any(Measurement.class), eq(35.0));
    }

}