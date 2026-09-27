package com.company.warehouse.services;

import com.company.warehouse.models.Measurement;
import com.company.warehouse.models.SensorType;
import com.company.warehouse.models.WarehouseProperties;
import com.company.warehouse.repositories.UdpSensorRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class WarehouseService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseService.class);

    private final UdpSensorRepository udpSensorRepository;
    private final MeasurementParser measurementParser;
    private final MeasurementPublisher measurementPublisher;
    private final WarehouseProperties properties;

    private final List<Disposable> subscriptions = new ArrayList<>();

    public WarehouseService(UdpSensorRepository udpSensorRepository, MeasurementParser measurementParser, MeasurementPublisher measurementPublisher, WarehouseProperties properties) {
        this.udpSensorRepository = udpSensorRepository;
        this.measurementParser = measurementParser;
        this.measurementPublisher = measurementPublisher;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (!properties.udpEnabled()) {
            log.info("Warehouse UDP service is disabled");
            return;
        }
        if (!subscriptions.isEmpty()) {
            return;
        }

        int temperaturePort = properties.udp().temperaturePort();
        int humidityPort = properties.udp().humidityPort();

        log.info("Starting temperature sensor receiver on UDP port {}", temperaturePort);
        Disposable temperatureSubscription = udpSensorRepository
                .receive(temperaturePort)
                .flatMap(message -> processMessage(message, SensorType.TEMPERATURE))
                .doOnError(error -> log.error("Temperature UDP receiver stopped", error))
                .subscribe();

        log.info("Starting humidity sensor receiver on UDP port {}", humidityPort);
        Disposable humiditySubscription = udpSensorRepository
                .receive(humidityPort)
                .flatMap(message -> processMessage(message, SensorType.HUMIDITY))
                .doOnError(error -> log.error("Humidity UDP receiver stopped", error))
                .subscribe();

        subscriptions.add(temperatureSubscription);
        subscriptions.add(humiditySubscription);

        log.info("Warehouse UDP service started");
    }

    private Mono<Void> processMessage(String message, SensorType sensorType) {
        return Mono.defer(() -> {
            final Measurement measurement;
            try {
                measurement = measurementParser.parse(message, sensorType);
            } catch (IllegalArgumentException exception) {
                log.warn("Invalid {} sensor measurement: {} | reason={}", sensorType, message, exception.getMessage());
                return Mono.empty();
            }
            return measurementPublisher.publish(measurement)
                    .onErrorResume(exception -> {
                        log.error("Failed to publish {} measurement: {}", sensorType, measurement, exception);
                        return Mono.empty();
                    });
        });
    }

    @PreDestroy
    public synchronized void stop() {
        subscriptions.forEach(Disposable::dispose);
        subscriptions.clear();
        log.info("Warehouse UDP service stopped");
    }

}