package com.sabbpe.merchant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson Configuration for REST API Serialization
 * 
 * Configures proper handling of Java 8+ date/time types (LocalDateTime, LocalDate, etc.)
 * and ensures dates are serialized in ISO-8601 format instead of timestamps.
 * 
 * This configuration class defines the PRIMARY ObjectMapper bean for the entire application.
 * It is used by:
 * - Spring's JSON serialization/deserialization
 * - RestTemplate for HTTP client calls
 * - All REST API endpoints
 * 
 * Spring Boot 3 Note: There should be only ONE ObjectMapper bean defined in the application.
 * This is the single source of truth for JSON serialization configuration.
 * 
 * Key configurations:
 * 1. Registers JavaTimeModule for LocalDateTime/LocalDate/LocalTime support
 * 2. Disables WRITE_DATES_AS_TIMESTAMPS to use ISO format
 * 3. Ensures consistent JSON serialization across all REST endpoints
 */
@Configuration
public class JacksonConfig {

    /**
     * Configure ObjectMapper for proper date serialization
     * 
     * PRIMARY BEAN: This is the application-wide ObjectMapper bean used for ALL JSON serialization.
     * 
     * Configuration:
     * - Registers JavaTimeModule to handle LocalDateTime, LocalDate, LocalTime types
     * - Disables timestamp writing to use ISO-8601 format
     * - Allows custom @JsonFormat annotations on fields
     * 
     * Usage: Automatically injected by Spring into:
     * - RestTemplate
     * - Http message converters
     * - JSON serialization/deserialization throughout the application
     * 
     * @return Configured ObjectMapper bean (application-wide singleton)
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register JavaTimeModule for Java 8+ date/time support
        mapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps - use ISO-8601 format instead
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}
