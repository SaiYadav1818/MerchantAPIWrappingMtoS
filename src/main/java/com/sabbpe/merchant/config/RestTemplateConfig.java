package com.sabbpe.merchant.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate Configuration
 * 
 * Configures the Spring RestTemplate for making HTTP requests to external services
 * (e.g., Easebuzz payment gateway).
 * 
 * NOTE: ObjectMapper is NOT defined here. Use the one from JacksonConfig instead.
 * Spring Boot 3 does not allow duplicate bean definitions.
 * The application-wide ObjectMapper is defined in JacksonConfig.java
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Configure RestTemplate with connection and read timeouts
     * 
     * The RestTemplate will automatically use the ObjectMapper bean from JacksonConfig
     * for serialization/deserialization of JSON responses.
     * 
     * @param builder RestTemplateBuilder provided by Spring Boot
     * @return Configured RestTemplate bean
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(5))
            .build();
    }
}
