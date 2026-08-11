package com.reejuven8.identity.integration;

import com.reejuven8.common.exception.UnauthorizedException;
import com.reejuven8.identity.service.OtpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PostgreSQL (Flyway V1–V5) + Redis OTP flow against real containers.
 * Skipped automatically when Docker is not available.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class IdentityIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reejuven8_identity");

    @Container
    static final GenericContainer<?> redis =
        new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired OtpService otpService;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayMigrationsApplied() {
        Integer applied = jdbc.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(5);
        Integer users = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_name = 'users'", Integer.class);
        assertThat(users).isEqualTo(1);
    }

    @Test
    void otpStoredWithTtlThenVerifiedAndConsumed() {
        String phone = "+919876543210";
        String otp = otpService.generateAndStore(phone);

        assertThat(otp).hasSize(6);
        Long ttl = redisTemplate.getExpire("otp:" + phone, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(1L, 300L);

        otpService.verifyAndConsume(phone, otp);

        // consumed — second use must fail
        assertThatThrownBy(() -> otpService.verifyAndConsume(phone, otp))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void wrongOtpRejectedAndNotConsumed() {
        String phone = "+919812345678";
        String otp = otpService.generateAndStore(phone);

        assertThatThrownBy(() -> otpService.verifyAndConsume(phone, "000000"))
            .isInstanceOf(UnauthorizedException.class);

        // correct OTP still valid after failed attempt
        otpService.verifyAndConsume(phone, otp);
    }
}
