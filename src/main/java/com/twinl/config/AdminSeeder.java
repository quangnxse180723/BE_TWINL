package com.twinl.config;

import com.twinl.entity.Role;
import com.twinl.entity.RoleName;
import com.twinl.entity.User;
import com.twinl.repository.RoleRepository;
import com.twinl.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {
	private static final String ADMIN_EMAIL = "twinladmin@gmail.com";
	private static final String ADMIN_PASSWORD = "Twinl14124869@";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminSeeder(
			UserRepository userRepository,
			RoleRepository roleRepository,
			PasswordEncoder passwordEncoder
	) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.existsByEmail(ADMIN_EMAIL)) {
			return;
		}

		Role adminRole = roleRepository.findByName(RoleName.ADMIN)
				.orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).build()));

		User adminUser = User.builder()
				.displayName("Twinl Admin")
				.email(ADMIN_EMAIL)
				.password(passwordEncoder.encode(ADMIN_PASSWORD))
				.active(true)
				.build();
		adminUser.getRoles().add(adminRole);

		userRepository.save(adminUser);
	}
}
