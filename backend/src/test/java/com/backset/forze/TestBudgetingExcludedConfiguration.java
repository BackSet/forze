package com.backset.forze;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import com.backset.forze.module.budgeting.infrastructure.MembershipRepository;
import org.mockito.Mockito;

@SpringBootConfiguration
@EnableAutoConfiguration(
		excludeName = "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration"
)
@ComponentScan(
		basePackages = {
				"com.backset.forze.configuration",
				"com.backset.forze.shared.tenant",
				"com.backset.forze.module.document"
		}
)
@ConfigurationPropertiesScan
public class TestBudgetingExcludedConfiguration {

	@Bean
	public MembershipRepository membershipRepository() {
		return Mockito.mock(MembershipRepository.class);
	}
}
