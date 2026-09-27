package com.company.warehouse.services;

import com.company.warehouse.models.Alarm;
import com.company.warehouse.models.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlarmService {

    private static final Logger log = LoggerFactory.getLogger(AlarmService.class);

    public void activate(Measurement measurement, double threshold) {
        var alarm = new Alarm(measurement.sensorId(), measurement.sensorType(), measurement.value(), threshold, measurement.receivedAt());
        log.warn("ALARM | sensorId={} | type={} | value={} | threshold={} | timestamp={}", alarm.sensorId(), alarm.sensorType(), alarm.measuredValue(), alarm.threshold(), alarm.occurredAt());
    }
}
