package com.hrplatform.backend;

import com.orca.hrplatform.HrPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		classes = HrPlatformApplication.class,
		properties = {
				"spring.datasource.url=jdbc:h2:mem:hr_peopleops_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
				"spring.datasource.driver-class-name=org.h2.Driver",
				"spring.datasource.username=sa",
				"spring.datasource.password=",
				"spring.jpa.hibernate.ddl-auto=create-drop",
				"spring.flyway.enabled=false",
				"app.zkteco.enabled=false",
				"app.ad.enabled=false",
				"app.synology.enabled=false"
		}
)
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
