package com.backset.forze.configuration;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TimeConfiguration {

	@Bean
	Clock appClock(AppProperties properties) {
		return Clock.system(properties.timeZone());
	}
}
