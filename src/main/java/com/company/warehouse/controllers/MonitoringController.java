package com.company.warehouse.controllers;

import com.company.warehouse.models.Measurement;
import com.company.warehouse.services.MeasurementPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/measurements")
public class MonitoringController {

    private final MeasurementPublisher measurementPublisher;

    public MonitoringController(MeasurementPublisher measurementPublisher) {
        this.measurementPublisher = measurementPublisher;
    }

    @PostMapping
    public Mono<ResponseEntity<Void>> publish(@RequestBody Measurement measurement) {
        return measurementPublisher.publish(measurement)
                .thenReturn(ResponseEntity.accepted().build());
    }

}
