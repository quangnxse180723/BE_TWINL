package com.twinl.config;

import com.twinl.entity.Role;
import com.twinl.entity.RoleName;
import com.twinl.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {
	private final RoleRepository roleRepository;

	public DatabaseSeeder(RoleRepository roleRepository) {
		this.roleRepository = roleRepository;
	}

	@Override
	public void run(String... args) {
		for (RoleName roleName : RoleName.values()) {
			roleRepository.findByName(roleName)
					.orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
		}
	}
}
