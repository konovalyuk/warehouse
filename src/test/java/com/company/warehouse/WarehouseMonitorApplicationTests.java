package com.company.warehouse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "warehouse.udp.enabled=false")
class WarehouseMonitorApplicationTests {

	@Test
	void contextLoads() {
	}

}
