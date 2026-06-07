package com.twinl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
public class BeTwinlApplication {

	private final JdbcTemplate jdbcTemplate;

	public BeTwinlApplication(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public static void main(String[] args) {
		SpringApplication.run(BeTwinlApplication.class, args);
	}

	@PostConstruct
	public void dropPaymentMethodConstraint() {
		try {
			jdbcTemplate.execute("ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_method_check;");
		} catch (Exception e) {
			// Ignore if it doesn't exist
		}
	}
}
