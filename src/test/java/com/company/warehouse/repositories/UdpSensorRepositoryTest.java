package com.company.warehouse.repositories;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

class UdpSensorRepositoryTest {

    @Test
    void shouldReceiveUdpMessage() {
        int port = findFreeUdpPort();
        var repository = new UdpSensorRepository();
        Flux<String> messages = repository.receive(port);

        StepVerifier.create(messages)
                .then(() -> sendUdpMessage(port, "sensor_id=t1; value=36"))
                .expectNext("sensor_id=t1; value=36")
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    void shouldReceiveMultipleUdpMessages() {
        int port = findFreeUdpPort();
        var repository = new UdpSensorRepository();
        Flux<String> messages = repository.receive(port);

        StepVerifier.create(messages)
                .then(() -> {
                    sendUdpMessage(port, "sensor_id=t1; value=30");
                    sendUdpMessage(port, "sensor_id=t2; value=40");
                })
                .expectNext("sensor_id=t1; value=30")
                .expectNext("sensor_id=t2; value=40")
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    private static void sendUdpMessage(int port, String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        var packet = new DatagramPacket(data, data.length, InetAddress.getLoopbackAddress(), port);

        try (var socket = new DatagramSocket()) {
            socket.send(packet);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to send UDP test message", exception);
        }
    }

    private static int findFreeUdpPort() {
        try (var socket = new DatagramSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to find free UDP port", exception);
        }
    }

}