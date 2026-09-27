package com.company.warehouse.controllers;

import com.company.warehouse.models.Measurement;
import com.company.warehouse.services.MeasurementPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MonitoringControllerTest {

    private MeasurementPublisher measurementPublisher;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        measurementPublisher = mock(MeasurementPublisher.class);
        when(measurementPublisher.publish(any(Measurement.class))).thenReturn(Mono.empty());
        MonitoringController controller = new MonitoringController(measurementPublisher);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void shouldAcceptMeasurement() {
        webTestClient.post()
                .uri("/api/measurements")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "sensorId": "t1",
                          "sensorType": "TEMPERATURE",
                          "value": 36.0,
                          "receivedAt": "2026-09-26T12:00:00Z"
                        }
                        """)
                .exchange()
                .expectStatus()
                .isAccepted();

        verify(measurementPublisher).publish(any(Measurement.class));
    }
}