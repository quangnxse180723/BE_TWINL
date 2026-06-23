package com.twinl.config;

import com.twinl.entity.Role;
import com.twinl.entity.RoleName;
import com.twinl.entity.User;
import com.twinl.entity.Category;
import com.twinl.repository.CategoryRepository;
import com.twinl.repository.RoleRepository;
import com.twinl.repository.UserRepository;
import com.twinl.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {
	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final CategoryRepository categoryRepository;

	private final ProductRepository productRepository;

	private final javax.sql.DataSource dataSource;

	public DatabaseSeeder(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder, CategoryRepository categoryRepository, ProductRepository productRepository, javax.sql.DataSource dataSource) {
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
		this.dataSource = dataSource;
	}

	@Override
	@Transactional
	public void run(String... args) {
		// Drop unique constraint on categories table if exists
		try (java.sql.Connection conn = dataSource.getConnection();
			 java.sql.Statement stmt = conn.createStatement()) {
			stmt.execute("ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_name_key;");
			stmt.execute("ALTER TABLE categories DROP CONSTRAINT IF EXISTS uk_t8o6pivur7nn124jehx7cygw5;"); // common hibernate generated
			// Better way: find constraint dynamically
			java.sql.ResultSet rs = stmt.executeQuery("SELECT conname FROM pg_constraint WHERE conrelid = 'categories'::regclass AND contype = 'u';");
			while (rs.next()) {
				String constraintName = rs.getString(1);
				stmt.execute("ALTER TABLE categories DROP CONSTRAINT IF EXISTS " + constraintName + ";");
			}
		} catch (Exception e) {
			System.out.println("Could not drop unique constraint on categories: " + e.getMessage());
		}

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

		// 3. Seed Categories
		seedCategories();
	}

	private void seedCategories() {
		if (categoryRepository.count() > 0) {
			return; // Categories already seeded
		}
		// Nữ
		Category nu = categoryRepository.save(Category.builder().name("Nữ").build());
		categoryRepository.save(Category.builder().name("Áo (Nữ)").parent(nu).build());
		categoryRepository.save(Category.builder().name("Quần (Nữ)").parent(nu).build());
		categoryRepository.save(Category.builder().name("Váy (Nữ)").parent(nu).build());
		categoryRepository.save(Category.builder().name("Đầm (Nữ)").parent(nu).build());
		categoryRepository.save(Category.builder().name("Giày dép (Nữ)").parent(nu).build());
		categoryRepository.save(Category.builder().name("Phụ kiện (Nữ)").parent(nu).build());

		// Nam
		Category nam = categoryRepository.save(Category.builder().name("Nam").build());
		categoryRepository.save(Category.builder().name("Áo (Nam)").parent(nam).build());
		categoryRepository.save(Category.builder().name("Quần (Nam)").parent(nam).build());
		categoryRepository.save(Category.builder().name("Giày dép (Nam)").parent(nam).build());
		categoryRepository.save(Category.builder().name("Phụ kiện (Nam)").parent(nam).build());

		// Thể thao
		Category theThao = categoryRepository.save(Category.builder().name("Thể thao").build());
		categoryRepository.save(Category.builder().name("Đồ tập").parent(theThao).build());
		categoryRepository.save(Category.builder().name("Giày thể thao").parent(theThao).build());
		categoryRepository.save(Category.builder().name("Phụ kiện thể thao").parent(theThao).build());
		}
	private void seedUser(String displayName, String email, String password, RoleName roleName) {
		if (!userRepository.existsByEmail(email)) {
			Role role = roleRepository.findByName(roleName).orElseThrow();
			User user = User.builder()
					.displayName(displayName)
					.email(email)
					.password(passwordEncoder.encode(password))
					.active(true)
					.build();
			user.getRoles().add(role);
			userRepository.save(user);
		}
	}
}
