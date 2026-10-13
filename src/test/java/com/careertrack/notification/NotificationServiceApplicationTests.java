package com.careertrack.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "careertrack.internal.api-key=test-only-internal-api-key-32-chars-minimum")
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
