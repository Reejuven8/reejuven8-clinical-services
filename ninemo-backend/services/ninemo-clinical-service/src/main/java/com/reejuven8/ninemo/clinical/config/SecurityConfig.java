package com.reejuven8.ninemo.clinical.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // The API gateway is the authentication boundary: it validates the JWT and injects
            // X-User-Id/X-User-Role, which the controllers read. This service has no JWT
            // resource-server filter, so `.authenticated()` could never pass — the SecurityContext
            // is always empty and every /api/v1/** call 403s. Permit all and trust the
            // gateway-injected identity; per-resource ownership is enforced in the service layer
            // (ChildAccessGuard) and by X-User-Id scoping in each controller. See IS-034.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
