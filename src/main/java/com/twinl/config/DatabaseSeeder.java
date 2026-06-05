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
public class DatabaseSeeder implements CommandLineRunner {
	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public DatabaseSeeder(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		// 1. Seed Roles (nếu chưa có)
		for (RoleName roleName : RoleName.values()) {
			roleRepository.findByName(roleName)
					.orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
		}

		// 2. Seed Accounts mẫu
		seedUser("Twinl Admin", "twinladmin@gmail.com", "Twinl14124869@", RoleName.ADMIN);
		seedUser("Nhân viên Staff", "staff@gmail.com", "123456", RoleName.STAFF);
		seedUser("Người dùng User", "user@gmail.com", "123456", RoleName.USER);
		seedUser("Nhân viên Shipper", "shipper@gmail.com", "123456", RoleName.SHIPPER);
	}

	private void seedUser(String displayName, String email, String password, RoleName roleName) {
		if (!userRepository.existsByEmail(email)) {
			Role role = roleRepository.findByName(roleName).orElseThrow();
			User user = User.builder()
					.displayName(displayName)
					.email(email)
					.password(passwordEncoder.encode(password))
					.active(true)
					// Điền tạm các thông tin bắt buộc khi đặt hàng (để test dễ hơn)
					.phone("0987654321")
					.address("123 Đường Test, Phường Test, Quận Test, TP.HCM")
					.wardCode("21207")
					.districtId(1454)
					.provinceId(202)
					.build();
			user.getRoles().add(role);
			userRepository.save(user);
		}
	}
}
