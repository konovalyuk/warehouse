package com.company.warehouse.repositories;

import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

@Repository
public class UdpSensorRepository {

    private static final int BUFFER_SIZE = 4096;

    public Flux<String> receive(int port) {
        return Flux.create(sink -> startReceiver(port, sink), FluxSink.OverflowStrategy.BUFFER);
    }

    private void startReceiver(int port, FluxSink<String> sink) {
        final Selector selector;
        final DatagramChannel channel;

        try {
            selector = Selector.open();
            channel = DatagramChannel.open();

            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress("0.0.0.0", port));
            channel.register(selector, SelectionKey.OP_READ);
        } catch (IOException exception) {
            sink.error(new IllegalStateException("Failed to start UDP receiver on port " + port, exception));
            return;
        }

        var receiverThread = new Thread(() -> receiveLoop(port, sink, selector, channel), "udp-sensor-receiver-" + port);
        receiverThread.setDaemon(true);

        sink.onCancel(() -> {
            receiverThread.interrupt();
            selector.wakeup();

            closeQuietly(channel);
            closeQuietly(selector);
        });

        sink.onDispose(() -> {
            receiverThread.interrupt();
            selector.wakeup();

            closeQuietly(channel);
            closeQuietly(selector);
        });

        receiverThread.start();
    }

    private void receiveLoop(int port, FluxSink<String> sink, Selector selector, DatagramChannel channel) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

            while (!Thread.currentThread().isInterrupted() && !sink.isCancelled()) {
                int readyChannels = selector.select(500);
                if (readyChannels == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (!key.isValid() || !key.isReadable()) {
                        continue;
                    }
                    buffer.clear();
                    channel.receive(buffer);

                    buffer.flip();
                    if (!buffer.hasRemaining()) {
                        continue;
                    }
                    String message = StandardCharsets.UTF_8
                                    .decode(buffer)
                                    .toString();
                    if (!sink.isCancelled()) {
                        sink.next(message);
                    }
                }
            }
        } catch (IOException exception) {
            if (!Thread.currentThread().isInterrupted() && !sink.isCancelled()) {
                sink.error(new IllegalStateException("UDP receiver failed on port " + port, exception));
            }
        } finally {
            closeQuietly(channel);
            closeQuietly(selector);
            if (!sink.isCancelled()) {
                sink.complete();
            }
        }
    }

    private void closeQuietly(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception ignored) {
        }
    }

}