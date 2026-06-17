package com.backset.forze.configuration;

import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
class SecurityConfiguration {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ApplicationContext applicationContext) throws Exception {
		HttpSecurity configured = http
				.csrf((csrf) -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.ignoringRequestMatchers("/api/auth/**", "/v3/api-docs", "/v3/api-docs/**"))
				.cors(Customizer.withDefaults())
				.sessionManagement((sessions) -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.headers((headers) -> headers
						.contentSecurityPolicy((policy) -> policy.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
						.frameOptions((frame) -> frame.deny())
						.referrerPolicy(Customizer.withDefaults()))
				.authorizeHttpRequests((requests) -> requests
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info", "/v3/api-docs", "/v3/api-docs/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
						.anyRequest().authenticated());
		if (applicationContext.containsBean("jwtAuthenticationFilter")) {
			configured.addFilterBefore(applicationContext.getBean("jwtAuthenticationFilter", OncePerRequestFilter.class), UsernamePasswordAuthenticationFilter.class);
		}
		if (applicationContext.containsBean("tenantFilter")) {
			configured.addFilterAfter(applicationContext.getBean("tenantFilter", OncePerRequestFilter.class), UsernamePasswordAuthenticationFilter.class);
		}
		return configured.build();
	}

	@Bean
	@ConditionalOnProperty(prefix = "forze.identity", name = "enabled", havingValue = "true", matchIfMissing = true)
	AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
		return new ProviderManager(authenticationProvider);
	}

	@Bean
	@ConditionalOnProperty(prefix = "forze.identity", name = "enabled", havingValue = "true", matchIfMissing = true)
	AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.ORIGIN));
		configuration.setExposedHeaders(List.of());
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
