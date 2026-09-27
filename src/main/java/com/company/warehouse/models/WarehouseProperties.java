package com.company.warehouse.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "warehouse")
@Validated
public record WarehouseProperties(
        boolean udpEnabled,
        @Valid
        @NotNull
        UdpProperties udp,
        @Valid
        @NotNull
        ThresholdProperties threshold) {

    public record UdpProperties(
            @Min(1)
            @Max(65535)
            int temperaturePort,
            @Min(1)
            @Max(65535)
            int humidityPort) {
    }

    public record ThresholdProperties(
            @DecimalMin("0.0")
            double temperature,
            @DecimalMin("0.0")
            double humidity) {
    }

}