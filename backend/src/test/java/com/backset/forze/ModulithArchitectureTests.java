package com.backset.forze;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTests {

	@Test
	void verifiesModularBoundaries() {
		ApplicationModules.of(ForzeApplication.class).verify();
	}
}
