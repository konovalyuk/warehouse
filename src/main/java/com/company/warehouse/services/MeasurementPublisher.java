package com.company.warehouse.services;

import com.company.warehouse.models.Measurement;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface MeasurementPublisher {

    Mono<Void> publish(Measurement measurement);
}
