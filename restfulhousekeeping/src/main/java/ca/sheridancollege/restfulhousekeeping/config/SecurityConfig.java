package ca.sheridancollege.restfulhousekeeping.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.AllArgsConstructor;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
@SecurityScheme(
		name = "Bearer Authentication",
		type = SecuritySchemeType.HTTP,
		bearerFormat = "JWT",
		scheme = "bearer",
		in = SecuritySchemeIn.HEADER
)
public class SecurityConfig {

	private JwtAuthenticationFilter jwtAuthenticationFilter;
	private AuthenticationProvider authenticationProvider;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
						    "/api/v1/auth/register",
						    "/api/v1/auth/authenticate",
						    "/api/v1/auth/username-availability",
						    "/ws",
					        "/ws/**"
						).permitAll()
						.requestMatchers(HttpMethod.GET, "/properties", "/properties/**").hasAuthority("MANAGER")
						.requestMatchers(HttpMethod.PATCH, "/cleanings/*/cleaner").hasAuthority("MANAGER")
						.requestMatchers(HttpMethod.PATCH, "/cleanings/*/schedule").hasAuthority("MANAGER")
						.requestMatchers(HttpMethod.PUT, "/cleanings/*").hasAuthority("MANAGER")
						.requestMatchers(HttpMethod.DELETE, "/cleanings/*").hasAuthority("MANAGER")
						.requestMatchers(HttpMethod.DELETE, "/cleaners/*").hasAuthority("MANAGER")
						.requestMatchers(HttpMethod.POST, "/invitations").hasAuthority("MANAGER")
						.requestMatchers(HttpMethod.PATCH, "/invitations/*/accept").hasAuthority("CLEANER")
						.requestMatchers(HttpMethod.PATCH, "/invitations/*/decline").hasAuthority("CLEANER")
						.requestMatchers(HttpMethod.POST, "/cleanings/*/checklist-items").hasAuthority("MANAGER")
						.requestMatchers(HttpMethod.DELETE, "/cleanings/*/checklist-items/*").hasAuthority("MANAGER")
						.requestMatchers(HttpMethod.PATCH, "/cleanings/*/checklist-items/*").hasAuthority("CLEANER")
						.requestMatchers(HttpMethod.GET, "/organizations").permitAll()
						// TODO comment or remove for production
						.requestMatchers("/availability-slots", "/availability-slots/**").hasAuthority("CLEANER")
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/h2-console/**","/error").permitAll()
						.anyRequest().authenticated()
				)
				.csrf(csrf -> csrf.disable())
				.headers(headers -> headers
						.frameOptions(frame -> frame.sameOrigin()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authenticationProvider(authenticationProvider)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

}
