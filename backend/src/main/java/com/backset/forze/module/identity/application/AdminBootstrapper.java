package com.backset.forze.module.identity.application;

import java.util.UUID;

import com.backset.forze.configuration.BootstrapProperties;
import com.backset.forze.module.identity.domain.UserAccount;
import com.backset.forze.module.identity.infrastructure.UserAccountRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "forze.identity", name = "enabled", havingValue = "true", matchIfMissing = true)
class AdminBootstrapper implements ApplicationRunner {

	private final BootstrapProperties properties;
	private final UserAccountRepository users;
	private final PasswordEncoder passwordEncoder;

	AdminBootstrapper(BootstrapProperties properties, UserAccountRepository users, PasswordEncoder passwordEncoder) {
		this.properties = properties;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!properties.enabled()) {
			return;
		}
		if (properties.initialPassword() == null || properties.initialPassword().isBlank()) {
			throw new IllegalStateException("ADMIN_INITIAL_PASSWORD is required when admin bootstrap is enabled.");
		}
		if (users.existsByUsername(properties.username())) {
			return;
		}

		UserAccount admin = new UserAccount(
				UUID.randomUUID(),
				properties.username(),
				properties.email(),
				passwordEncoder.encode(properties.initialPassword()),
				true);
		users.save(admin);
	}
}
