package com.company.warehouse.services;

import com.company.warehouse.models.Measurement;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CentralMonitoringService implements MeasurementPublisher {

    private final ThresholdPolicy thresholdPolicy;
    private final AlarmService alarmService;

    public CentralMonitoringService(ThresholdPolicy thresholdPolicy, AlarmService alarmService) {
        this.thresholdPolicy = thresholdPolicy;
        this.alarmService = alarmService;
    }

    @Override
    public Mono<Void> publish(Measurement measurement) {
        return Mono.fromRunnable(() -> process(measurement));
    }

    public void process(Measurement measurement) {
        double threshold = thresholdPolicy.getThreshold(measurement.sensorType());
        if (thresholdPolicy.isExceeded(measurement.sensorType(), measurement.value())) {
            alarmService.activate(measurement, threshold);
        }
    }

}
