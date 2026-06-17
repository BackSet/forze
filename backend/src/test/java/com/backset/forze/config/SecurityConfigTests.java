package com.backset.forze.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backset.forze.TestBudgetingExcludedConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
		classes = TestBudgetingExcludedConfiguration.class,
		properties = {
				"debug=false",
				"forze.identity.enabled=false",
				"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
		})
@AutoConfigureMockMvc
class SecurityConfigTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void permitsHealth() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	@Test
	void permitsOpenApiDocument() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk());
	}

	@Test
	void deniesUnknownEndpointsByDefault() throws Exception {
		mockMvc.perform(get("/internal/not-defined"))
				.andExpect(status().isForbidden());
	}
}
