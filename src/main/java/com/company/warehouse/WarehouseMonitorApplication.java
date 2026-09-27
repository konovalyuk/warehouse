package com.company.warehouse;

import com.company.warehouse.models.WarehouseProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WarehouseProperties.class)
public class WarehouseMonitorApplication {

	public static void main(String[] args) {
		SpringApplication.run(WarehouseMonitorApplication.class, args);
	}

}
