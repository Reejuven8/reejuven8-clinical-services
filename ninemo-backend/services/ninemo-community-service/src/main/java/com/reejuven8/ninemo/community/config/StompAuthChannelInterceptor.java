package com.reejuven8.ninemo.community.config;

import com.reejuven8.ninemo.community.security.JwtValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtValidator jwtValidator;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command)) {
            if (accessor.getUser() == null) {
                throw new IllegalArgumentException("Unauthenticated STOMP " + command);
            }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Missing Bearer token on STOMP CONNECT");
        }
        try {
            Claims claims = jwtValidator.validate(header.substring(BEARER_PREFIX.length()));
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
            log.debug("STOMP CONNECT authenticated userId={}", userId);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("STOMP CONNECT rejected: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT on STOMP CONNECT");
        }
    }
}
