package com.backset.forze;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		classes = TestBudgetingExcludedConfiguration.class,
		properties = {
				"debug=false",
				"forze.identity.enabled=false",
				"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
		})
class ForzeApplicationTests {

	@Test
	void contextLoads() {
	}
}
