package com.twinl.service.impl;

import com.twinl.entity.User;
import com.twinl.repository.UserRepository;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		boolean isActive = user.getActive() == null || Boolean.TRUE.equals(user.getActive());

		return org.springframework.security.core.userdetails.User.builder()
				.username(user.getEmail())
				.password(user.getPassword())
				.disabled(!isActive)
				.authorities(user.getRoles().stream()
						.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
						.collect(Collectors.toSet()))
				.build();
	}
}
