package com.company.warehouse.services;

import com.company.warehouse.models.Measurement;
import com.company.warehouse.models.SensorType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MeasurementParser {

    public Measurement parse(String message, SensorType sensorType) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Measurement message must not be empty");
        }

        Map<String, String> fields = Arrays.stream(message.split(";"))
                .map(String::trim)
                .filter(field -> !field.isEmpty())
                .map(this::parseField)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (first, second) -> {throw new IllegalArgumentException("Duplicate field: " + first);}
                ));

        String sensorId = fields.get("sensor_id");
        String valueText = fields.get("value");

        if (sensorId == null || sensorId.isBlank()) {
            throw new IllegalArgumentException("Missing or empty sensor_id");
        }

        if (valueText == null || valueText.isBlank()) {
            throw new IllegalArgumentException("Missing or empty value");
        }

        double value;

        try {
            value = Double.parseDouble(valueText);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid measurement value: " + valueText, exception);
        }

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Measurement value must be finite: " + valueText);
        }

        return new Measurement(sensorId, sensorType, value, Instant.now());
    }


    private Map.Entry<String, String> parseField(String field) {
        String[] parts = field.split("=", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid field format: " + field);
        }

        String key = parts[0].trim();
        String value = parts[1].trim();

        if (key.isEmpty()) {
            throw new IllegalArgumentException("Field name must not be empty");
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Field value must not be empty for field: " + key);
        }

        return Map.entry(key, value);
    }

}
