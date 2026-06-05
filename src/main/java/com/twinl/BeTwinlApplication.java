package com.twinl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BeTwinlApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeTwinlApplication.class, args);
	}

}
