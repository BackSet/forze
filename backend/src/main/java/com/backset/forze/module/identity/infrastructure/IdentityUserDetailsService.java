package com.backset.forze.module.identity.infrastructure;

import com.backset.forze.module.identity.domain.UserAccount;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "forze.identity", name = "enabled", havingValue = "true", matchIfMissing = true)
class IdentityUserDetailsService implements UserDetailsService {

	private final UserAccountRepository users;

	IdentityUserDetailsService(UserAccountRepository users) {
		this.users = users;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		UserAccount user = users.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid credentials."));
		return new UserPrincipal(user.id(), user.username(), user.passwordHash(), user.enabled());
	}
}
