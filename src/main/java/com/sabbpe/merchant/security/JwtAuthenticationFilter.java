package com.sabbpe.merchant.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * JWT Authentication Filter
 * Intercepts requests to validate JWT token and set authentication context
 * Runs once per request to extract and validate JWT tokens
 * 
 * IMPORTANT: This filter now properly sets authorities/roles in the Authentication object
 * which fixes the 403 Forbidden issue by ensuring Spring Security recognizes authenticated users
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        log.info("JwtAuthenticationFilter created with JwtUtil instance hash: {} (singleton reference)",
                 System.identityHashCode(jwtUtil));
    }

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            // ✅ CRITICAL FIX: Extract role from token and create GrantedAuthority
            if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
                String merchantId = jwtUtil.getMerchantIdFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                log.debug("JWT token valid for merchantId: {} with role: {} (JwtUtil hash: {})", 
                         merchantId, role, System.identityHashCode(jwtUtil));

                // ✅ Create authorities collection with role from JWT
                Collection<GrantedAuthority> authorities = buildAuthorities(role);

                // ✅ Create authentication with authorities (THIS FIXES 403 ERROR)
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                merchantId,
                                null,
                                authorities  // ✅ Authorities now populated from JWT role claim
                        );

                // Store additional details for later access if needed
                authenticationToken.setDetails(new JwtAuthenticationDetails(merchantId, role));

                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                
                log.debug("JWT authentication set for merchantId: {}, authorities: {}", merchantId, authorities);
                log.trace("SecurityContext populated with principal: {}, authenticated: {}", 
                         authenticationToken.getPrincipal(), authenticationToken.isAuthenticated());
                
            } else if (StringUtils.hasText(token)) {
                log.warn("Invalid or expired JWT token provided");
                log.debug("Token validation failed. Setting empty SecurityContext.");
            }
        } catch (Exception e) {
            log.error("JWT authentication filter error: {}", e.getMessage(), e);
            // Continue filter chain even on error - other filters may handle
        }

        filterChain.doFilter(request, response);
    }

    /**
     * ✅ HELPER METHOD: Build GrantedAuthority collection from role claim
     * Converts role string (e.g., "ROLE_MERCHANT") into Spring Security GrantedAuthority objects
     * 
     * @param role role string from JWT claim
     * @return Collection of GrantedAuthority objects
     */
    private Collection<GrantedAuthority> buildAuthorities(String role) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        
        if (StringUtils.hasText(role)) {
            // ✅ Role is already in Spring Security format (ROLE_MERCHANT)
            // Simply wrap it in SimpleGrantedAuthority
            authorities.add(new SimpleGrantedAuthority(role));
            log.debug("Added authority to SecurityContext: {}", role);
        } else {
            log.warn("No role found in JWT token. User will have NO authorities.");
        }
        
        return authorities;
    }

    /**
     * Extract JWT token from Authorization header
     * Expected format: Authorization: Bearer <token>
     * @param request HTTP request
     * @return JWT token or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            log.trace("Extracted JWT token from Authorization header");
            return token;
        }

        return null;
    }

    /**
     * Helper class to store JWT details for later access
     */
    public static class JwtAuthenticationDetails {
        private final String merchantId;
        private final String role;

        public JwtAuthenticationDetails(String merchantId, String role) {
            this.merchantId = merchantId;
            this.role = role;
        }

        public String getMerchantId() {
            return merchantId;
        }

        public String getRole() {
            return role;
        }
    }
}
