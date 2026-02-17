package com.sabbpe.merchant.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration
 * Configures stateless JWT-based authentication for merchant APIs
 * Protects all payment APIs with JWT token validation
 * 
 * SECURITY FLOW:
 * 1. Request arrives with Authorization: Bearer <jwt_token>
 * 2. JwtAuthenticationFilter extracts token and validates it
 * 3. Filter extracts merchantId and role from JWT claims
 * 4. Filter creates Authentication with GrantedAuthority (ROLE_MERCHANT)
 * 5. SecurityContext receives populated Authentication object
 * 6. SecurityFilterChain checks .authenticated() - PASSES because authorities are populated
 * 7. Request proceeds to controller
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        log.info("SecurityConfig created with JwtAuthenticationFilter instance hash: {} (singleton reference)",
                 System.identityHashCode(jwtAuthenticationFilter));
    }

    /**
     * Configure HTTP security and filter chain
     * 
     * ENDPOINT SECURITY RULES:
     * - Public: /api/auth/** (login required, no auth token)
     * - Public: /h2-console/** (development only)
     * - Public: /actuator/** (health checks)
     * - Protected: All other endpoints (require valid JWT token with ROLE_MERCHANT)
     * 
     * @param http HttpSecurity configuration object
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ✅ Disable CSRF for stateless API (JWT is used instead of session cookies)
                .csrf(csrf -> {
                    csrf.disable();
                    log.debug("CSRF protection disabled for stateless API");
                })

                // ✅ Set session management to stateless (no session cookies, no SessionId tracking)
                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                    log.debug("Session management configured as STATELESS");
                })

                // ✅ Configure endpoint security with proper authorization rules
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - authentication NOT required
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // ✅ CRITICAL FIX: All other requests require authentication
                        // With our fix, JWT filter populates authorities, so .authenticated() now works
                        .anyRequest().authenticated()
                )

                // ✅ Exception handling for security failures
                .exceptionHandling(exceptionHandling -> {
                    exceptionHandling
                            .authenticationEntryPoint((request, response, ex) -> {
                                log.warn("Authentication entry point triggered. URI: {}, Error: {}", 
                                        request.getRequestURI(), ex.getMessage());
                                response.setStatus(401);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" 
                                        + ex.getMessage() + "\"}");
                            })
                            .accessDeniedHandler((request, response, ex) -> {
                                log.warn("Access denied. URI: {}, Principal: {}, Error: {}", 
                                        request.getRequestURI(), 
                                        request.getUserPrincipal(), 
                                        ex.getMessage());
                                response.setStatus(403);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"" 
                                        + ex.getMessage() + "\"}");
                            });
                    log.debug("Exception handling configured");
                })

                // ✅ Allow H2 console headers (development only - not for production)
                .headers(headers -> {
                    headers.frameOptions(frame -> {
                        frame.disable();
                        log.debug("Frame options disabled for H2 console");
                    });
                })

                // ✅ Add JWT filter BEFORE UsernamePasswordAuthenticationFilter
                // This ensures JWT validation happens before standard authentication attempts
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        ;

        log.info("SecurityFilterChain configured successfully");
        return http.build();
    }

    /**
     * Password encoder bean - BCrypt
     * Used for merchant password hashing (if implemented)
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.debug("BCryptPasswordEncoder bean created");
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication manager bean
     * @param http HttpSecurity
     * @return AuthenticationManager instance
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        log.debug("AuthenticationManager bean created");
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .build();
    }
}
