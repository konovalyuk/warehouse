package com.company.warehouse.services;

import com.company.warehouse.models.Measurement;
import com.company.warehouse.models.SensorType;
import com.company.warehouse.models.WarehouseProperties;
import com.company.warehouse.repositories.UdpSensorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WarehouseUdpIntegrationTest {

    private WarehouseService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.stop();
        }
    }

    @Test
    void shouldReceiveTemperatureMeasurementThroughUdp() throws Exception {

        int temperaturePort = findFreeUdpPort();
        int humidityPort = findFreeUdpPort();

        var repository = new UdpSensorRepository();
        var parser = new MeasurementParser();
        CopyOnWriteArrayList<Measurement> received = new CopyOnWriteArrayList<>();

        MeasurementPublisher publisher = measurement -> {
            received.add(measurement);
            return Mono.empty();
        };

        service = new WarehouseService(repository, parser, publisher,
                createProperties(temperaturePort, humidityPort, true));
        service.start();

        sendUdpMessage(temperaturePort, "sensor_id=t1; value=36");

        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertEquals(1, received.size()));

        Measurement measurement = received.getFirst();
        assertEquals("t1", measurement.sensorId());
        assertEquals(SensorType.TEMPERATURE, measurement.sensorType());
        assertEquals(36.0, measurement.value());
    }

    @Test
    void shouldReceiveHumidityMeasurementThroughUdp() throws Exception {
        int temperaturePort = findFreeUdpPort();
        int humidityPort = findFreeUdpPort();

        var repository = new UdpSensorRepository();
        var parser = new MeasurementParser();
        CopyOnWriteArrayList<Measurement> received = new CopyOnWriteArrayList<>();

        MeasurementPublisher publisher = measurement -> {
            received.add(measurement);
            return Mono.empty();
        };

        service = new WarehouseService(repository, parser, publisher, createProperties(temperaturePort, humidityPort, true));
        service.start();

        sendUdpMessage(humidityPort, "sensor_id=h1; value=51");

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> assertEquals(1, received.size()));

        Measurement measurement = received.getFirst();
        assertEquals("h1", measurement.sensorId());
        assertEquals(SensorType.HUMIDITY, measurement.sensorType());

        assertEquals(51.0, measurement.value());
    }

    @Test
    void shouldIgnoreMalformedUdpMeasurement() throws Exception {

        int temperaturePort = findFreeUdpPort();
        int humidityPort = findFreeUdpPort();

        var repository = new UdpSensorRepository();
        var parser = new MeasurementParser();

        CopyOnWriteArrayList<Measurement> received = new CopyOnWriteArrayList<>();

        MeasurementPublisher publisher = measurement -> {
            received.add(measurement);
            return Mono.empty();
        };

        service = new WarehouseService(repository, parser, publisher,
                createProperties(temperaturePort, humidityPort, true));
        service.start();

        sendUdpMessage(temperaturePort, "invalid message");
        Thread.sleep(300);
        assertEquals(0, received.size());
    }

    private static WarehouseProperties createProperties(int temperaturePort, int humidityPort, boolean udpEnabled) {
        return new WarehouseProperties(udpEnabled,
                new WarehouseProperties.UdpProperties(temperaturePort, humidityPort),
                new WarehouseProperties.ThresholdProperties(35.0, 50.0));
    }

    private static void sendUdpMessage(int port, String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        var packet = new DatagramPacket(data, data.length, InetAddress.getLoopbackAddress(), port);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(packet);
        }
    }

    private static int findFreeUdpPort() throws IOException {
        try (DatagramSocket socket = new DatagramSocket(0)) {
            return socket.getLocalPort();
        }
    }

}