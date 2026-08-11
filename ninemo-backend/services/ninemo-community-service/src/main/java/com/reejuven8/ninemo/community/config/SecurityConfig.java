package com.reejuven8.ninemo.community.config;
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
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // The API gateway is the authentication boundary: it validates the JWT and
            // injects X-User-Id/X-User-Role, which the controllers read. This service has no
            // JWT resource-server filter, so `.authenticated()` could never pass (403 on every
            // REST call) — permit all and trust the gateway-injected identity. STOMP is
            // separately authenticated by StompAuthChannelInterceptor on CONNECT. See IS-034.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**", "/ws/**").permitAll()
                .anyRequest().permitAll());
        return http.build();
    }
}
