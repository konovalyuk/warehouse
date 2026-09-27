# Warehouse Monitoring Service

A reactive Spring Boot service for collecting warehouse sensor measurements over UDP and monitoring temperature and humidity thresholds.

## Overview

The application receives measurements from warehouse sensors via UDP, parses and validates incoming messages, and forwards measurements to a central monitoring component.

When a measurement exceeds its configured threshold, the application generates an alarm and writes it to the application log.

Supported sensor types:

- Temperature
- Humidity

The application does not cover a GUI or user interaction.

## Architecture

The application is organized around two logical responsibilities:

- **Warehouse Service** — receives sensor measurements over UDP, parses them, validates them, and forwards them.
- **Central Monitoring Service** — evaluates measurements against configured thresholds and generates alarms.

The current implementation runs both responsibilities inside a single Spring Boot application.

```text
                    Warehouse Sensors
                           |
              +------------+------------+
              |                         |
        UDP port 3344             UDP port 3355
        Temperature                 Humidity
              |                         |
              +------------+------------+
                           |
                  UdpSensorRepository
                           |
                    WarehouseService
                           |
                  MeasurementParser
                           |
                    Measurement
                           |
              MeasurementPublisher
                           |
             CentralMonitoringService
                           |
                   ThresholdPolicy
                           |
                  value > threshold
                           |
                     AlarmService
                           |
                      Application
                         Log
```

## Technology Stack

- Java 25
- Spring Boot
- Spring WebFlux
- Project Reactor
- Gradle
- JUnit 5
- Mockito
- Reactor Test
- Awaitility
- UDP via Java NIO

## Sensor Protocol

### Temperature

Temperature sensors send UDP messages to port `3344`.

Message format:

```text
sensor_id=t1; value=30
```

Default threshold:

```text
35.0 °C
```

An alarm is generated when:

```text
value > 35.0
```

A value equal to the threshold does not generate an alarm.

### Humidity

Humidity sensors send UDP messages to port `3355`.

Message format:

```text
sensor_id=h1; value=40
```

Default threshold:

```text
50.0 %
```

An alarm is generated when:

```text
value > 50.0
```

A value equal to the threshold does not generate an alarm.

## Configuration

Configuration is located in:

```text
src/main/resources/application.properties
```

Default configuration:

```properties
spring.application.name=warehouse-monitor

warehouse.udp-enabled=true

warehouse.udp.temperature-port=3344
warehouse.udp.humidity-port=3355

warehouse.threshold.temperature=35.0
warehouse.threshold.humidity=50.0

management.endpoints.web.exposure.include=health,info
```

The configuration is mapped to a typed `WarehouseProperties` object and validated when the application starts.

UDP processing can be disabled with:

```properties
warehouse.udp-enabled=false
```

## Running the Application

### Requirements

- Java 25
- Gradle
- `netcat` (`nc`) for manual UDP testing

Run the application with:

```bash
./gradlew bootRun
```

The application starts the temperature receiver on UDP port `3344` and the humidity receiver on UDP port `3355`.

## Simulating Sensors

### Temperature below threshold

```bash
echo "sensor_id=t1; value=30" | nc -u -w1 localhost 3344
```

No alarm is generated.

### Temperature above threshold

```bash
echo "sensor_id=t1; value=36" | nc -u -w1 localhost 3344
```

The application logs an alarm similar to:

```text
ALARM | sensorId=t1 | type=TEMPERATURE | value=36.0 | threshold=35.0
```

### Humidity below threshold

```bash
echo "sensor_id=h1; value=40" | nc -u -w1 localhost 3355
```

No alarm is generated.

### Humidity above threshold

```bash
echo "sensor_id=h1; value=51" | nc -u -w1 localhost 3355
```

The application logs an alarm similar to:

```text
ALARM | sensorId=h1 | type=HUMIDITY | value=51.0 | threshold=50.0
```

## Alarm Behaviour

The application uses a strict threshold comparison:

```text
measurement value > configured threshold
```

Therefore:

| Sensor | Value | Threshold | Alarm |
|---|---:|---:|---|
| Temperature | 30.0 | 35.0 | No |
| Temperature | 35.0 | 35.0 | No |
| Temperature | 36.0 | 35.0 | Yes |
| Humidity | 40.0 | 50.0 | No |
| Humidity | 50.0 | 50.0 | No |
| Humidity | 51.0 | 50.0 | Yes |

Every measurement that exceeds the threshold generates an alarm.

There is currently no alarm suppression or hysteresis.

## Input Validation

Incoming sensor messages are validated before being converted into measurements.

Invalid messages are rejected without stopping the UDP receiver.

Examples of invalid input include:

```text
invalid message
```

```text
sensor_id=t1
```

```text
value=36
```

```text
sensor_id=t1; value=abc
```

```text
sensor_id=; value=36
```

Duplicate fields and non-finite numeric values are also rejected.

## REST API

The application also exposes an optional REST endpoint for submitting measurements directly to the monitoring component.

### Endpoint

```http
POST /api/measurements
```

Example:

```bash
curl -X POST http://localhost:8080/api/measurements \
  -H "Content-Type: application/json" \
  -d '{
    "sensorId": "t1",
    "sensorType": "TEMPERATURE",
    "value": 36.0,
    "receivedAt": "2026-09-26T13:00:00Z"
  }'
```

The endpoint returns:

```text
202 Accepted
```

The REST endpoint is an additional integration point; sensor simulation is performed through UDP.

## Testing

The project contains unit and integration tests covering:

- Measurement parsing
- Input validation
- Temperature threshold handling
- Humidity threshold handling
- Alarm generation
- Warehouse service processing
- UDP message reception
- Multiple UDP messages
- Invalid UDP messages
- UDP integration
- Configuration validation
- Spring application context

Run all tests with:

```bash
./gradlew clean test
```

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/company/warehouse/
│   │       ├── WarehouseMonitorApplication.java
│   │       │
│   │       ├── controllers/
│   │       │   └── MonitoringController.java
│   │       │
│   │       ├── models/
│   │       │   ├── Alarm.java
│   │       │   ├── Measurement.java
│   │       │   ├── SensorType.java
│   │       │   └── WarehouseProperties.java
│   │       │
│   │       ├── repositories/
│   │       │   └── UdpSensorRepository.java
│   │       │
│   │       └── services/
│   │           ├── AlarmService.java
│   │           ├── CentralMonitoringService.java
│   │           ├── MeasurementParser.java
│   │           ├── MeasurementPublisher.java
│   │           ├── ThresholdPolicy.java
│   │           └── WarehouseService.java
│   │
│   └── resources/
│       └── application.properties
│
└── test/
    └── java/
        └── com/company/warehouse/
            ├── WarehouseMonitorApplicationTests.java
            ├── models/
            │   └── WarehousePropertiesTest.java
            ├── repositories/
            │   └── UdpSensorRepositoryTest.java
            └── services/
                ├── CentralMonitoringServiceTest.java
                ├── MeasurementParserTest.java
                ├── ThresholdPolicyTest.java
                ├── WarehouseServiceTest.java
                └── WarehouseUdpIntegrationTest.java
```

## Design Decisions

### Reactive processing

Spring WebFlux and Project Reactor are used for the processing pipeline.

The measurement flow is:

```text
UDP
  -> Flux<String>
  -> parsing
  -> Measurement
  -> MeasurementPublisher
  -> monitoring
  -> alarm
```

### Separation of responsibilities

The application separates:

- UDP communication
- Message parsing
- Measurement processing
- Threshold evaluation
- Alarm generation
- Configuration

This keeps the monitoring logic independent from the UDP transport.

### Configuration-driven thresholds

Thresholds and UDP ports are not hard-coded in the business logic. They are provided through application configuration.

This allows different environments to use different sensor ports and threshold values without changing the application code.

### No message broker

A message broker is not required for the current scope. The application communicates directly between the warehouse and monitoring components.

A broker could be introduced later if the system needs independent deployment, buffering, multiple consumers, or higher-scale event distribution.

## Possible Future Extensions

The current implementation intentionally focuses on the required functionality.

Possible extensions include:

- Message broker integration
- Persistent measurement storage
- Persistent alarm history
- Multiple warehouse instances
- Sensor registration
- Alarm acknowledgement
- Metrics and monitoring
- Authentication and authorization
- Separate deployment of Warehouse and Central Monitoring services

These are outside the current implementation scope.
