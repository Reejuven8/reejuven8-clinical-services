package com.reejuven8.identity.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Getter
public class AbdmConfig {

    @Value("${abdm.base-url}")
    private String baseUrl;

    @Value("${abdm.client-id}")
    private String clientId;

    @Value("${abdm.client-secret}")
    private String clientSecret;

    @Bean
    public RestClient abdmRestClient() {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("X-CM-ID", "sbx")
            .build();
    }
}
