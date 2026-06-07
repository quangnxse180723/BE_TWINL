package com.twinl.config;

import com.twinl.service.impl.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtService jwtService;
	private final CustomUserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String token = null;
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7);
		} else {
			token = request.getParameter("token");
		}

		if (token == null || token.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}
		try {
			String username = jwtService.extractUsername(token);
			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				// Tránh gọi Database (userDetailsService) mỗi lần gửi request
				if (jwtService.isTokenValid(token, username)) {
					java.util.List<String> roles = jwtService.extractRoles(token);
					// Nếu token cũ không có roles, fallback lại DB lần này thôi
					java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities;
					if (roles != null && !roles.isEmpty()) {
						authorities = roles.stream()
								.map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
										role.startsWith("ROLE_") ? role : "ROLE_" + role))
								.collect(java.util.stream.Collectors.toList());
					} else {
						UserDetails fallback = userDetailsService.loadUserByUsername(username);
						authorities = fallback.getAuthorities().stream()
								.map(a -> new org.springframework.security.core.authority.SimpleGrantedAuthority(a.getAuthority()))
								.collect(java.util.stream.Collectors.toList());
					}

					UserDetails userDetails = new org.springframework.security.core.userdetails.User(
							username, "", authorities
					);

					UsernamePasswordAuthenticationToken authentication =
							new UsernamePasswordAuthenticationToken(
									userDetails,
									null,
									authorities
						);
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}
		} catch (Exception ex) {
			SecurityContextHolder.clearContext();
		}

		filterChain.doFilter(request, response);
	}
}
