package com.twinl.config;

import com.twinl.service.impl.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomUserDetailsService userDetailsService;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CustomUserDetailsService userDetailsService) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.userDetailsService = userDetailsService;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			CorsConfigurationSource corsConfigurationSource
	) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						// Public endpoints
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers("/uploads/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/contact").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/analytics/track").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/ai/scan").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/ai/legit-check").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/ai/image-quality", "/api/v1/ai/autofill").permitAll()
						.requestMatchers("/api/payments/vnpay/return", "/api/payments/vnpay/ipn").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/categories", "/api/colors").permitAll()

						// In-house Shipper endpoints
						.requestMatchers(HttpMethod.POST, "/api/v1/orders/*/assign").hasAnyRole("ADMIN", "STAFF")
						.requestMatchers("/api/v1/shipper/**").hasRole("SHIPPER")

						// Admin/Staff endpoints
						.requestMatchers("/api/staff/**").hasAnyRole("ADMIN", "STAFF")
						.requestMatchers("/api/admin/**").hasRole("ADMIN")

						// Products
						.requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/products", "/api/products/images").authenticated()
						.requestMatchers("/api/products/**").hasAnyRole("ADMIN", "STAFF")

						// Users
						.requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
						.requestMatchers("/api/users/**").hasAnyRole("ADMIN", "STAFF")

						// Anything else requires authentication
						.anyRequest().authenticated()
				)
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origin}") String allowedOrigin
	) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(java.util.Arrays.asList(allowedOrigin.split(",")));
		configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
