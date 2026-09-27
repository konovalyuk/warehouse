package com.company.warehouse.services;

import com.company.warehouse.models.Measurement;
import com.company.warehouse.models.SensorType;
import com.company.warehouse.models.WarehouseProperties;
import com.company.warehouse.repositories.UdpSensorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WarehouseServiceTest {

    private final UdpSensorRepository repository = mock(UdpSensorRepository.class);
    private final MeasurementPublisher publisher = mock(MeasurementPublisher.class);
    private final MeasurementParser parser = new MeasurementParser();

    private WarehouseService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.stop();
        }
    }

    @Test
    void shouldProcessTemperatureUdpMeasurement() {
        when(repository.receive(3344)).thenReturn(Flux.just("sensor_id=t1; value=30"));
        when(repository.receive(3355)).thenReturn(Flux.empty());
        when(publisher.publish(any(Measurement.class))).thenReturn(Mono.empty());

        service = new WarehouseService(repository, parser, publisher, createProperties(true));
        service.start();

        var captor = org.mockito.ArgumentCaptor.forClass(Measurement.class);
        verify(publisher).publish(captor.capture());

        Measurement measurement = captor.getValue();
        assertEquals("t1", measurement.sensorId());
        assertEquals(SensorType.TEMPERATURE, measurement.sensorType());
        assertEquals(30.0, measurement.value());
        assertNotNull(measurement.receivedAt());
    }

    @Test
    void shouldProcessHumidityUdpMeasurement() {
        when(repository.receive(3344)).thenReturn(Flux.empty());
        when(repository.receive(3355)).thenReturn(Flux.just("sensor_id=h1; value=60"));
        when(publisher.publish(any(Measurement.class))).thenReturn(Mono.empty());

        service = new WarehouseService(repository, parser, publisher, createProperties(true));
        service.start();

        var captor = org.mockito.ArgumentCaptor.forClass(Measurement.class);
        verify(publisher).publish(captor.capture());

        Measurement measurement = captor.getValue();
        assertEquals("h1", measurement.sensorId());
        assertEquals(SensorType.HUMIDITY, measurement.sensorType());
        assertEquals(60.0, measurement.value());
        assertNotNull(measurement.receivedAt());
    }

    @Test
    void shouldIgnoreInvalidUdpMeasurement() {
        when(repository.receive(3344)).thenReturn(Flux.just("invalid message"));
        when(repository.receive(3355)).thenReturn(Flux.empty());

        service = new WarehouseService(repository, parser, publisher, createProperties(true));
        service.start();

        verifyNoInteractions(publisher);
    }

    @Test
    void shouldContinueProcessingAfterInvalidMeasurement() {
        when(repository.receive(3344)).thenReturn(Flux.just("invalid message", "sensor_id=t1; value=40"));
        when(repository.receive(3355)).thenReturn(Flux.empty());
        when(publisher.publish(any(Measurement.class))).thenReturn(Mono.empty());

        service = new WarehouseService(repository, parser, publisher, createProperties(true));
        service.start();

        var captor = org.mockito.ArgumentCaptor.forClass(Measurement.class);
        verify(publisher).publish(captor.capture());

        assertEquals("t1", captor.getValue().sensorId());
        assertEquals(40.0, captor.getValue().value());
    }

    @Test
    void shouldNotStartUdpReceiversWhenDisabled() {
        service = new WarehouseService(repository, parser, publisher, createProperties(false));
        service.start();

        verifyNoInteractions(repository);
        verifyNoInteractions(publisher);
    }

    private static WarehouseProperties createProperties(boolean udpEnabled) {
        return new WarehouseProperties(udpEnabled,
                new WarehouseProperties.UdpProperties(3344, 3355),
                new WarehouseProperties.ThresholdProperties(35.0, 50.0));
    }
}