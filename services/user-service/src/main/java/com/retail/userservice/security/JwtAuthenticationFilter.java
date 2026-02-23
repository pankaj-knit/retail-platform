package com.retail.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter.
 *
 * This filter intercepts EVERY incoming HTTP request and checks for a JWT token.
 * It runs BEFORE the request reaches any controller.
 *
 * Spring Security uses a "filter chain" -- a series of filters that process
 * each request in order. Our JWT filter is inserted into this chain.
 *
 * Request flow:
 *   Client -> [JwtAuthFilter] -> [Other Security Filters] -> Controller
 *
 * What this filter does:
 *   1. Looks for "Authorization: Bearer <token>" header
 *   2. If found, validates the token signature and expiration
 *   3. If valid, extracts user info and sets it in the SecurityContext
 *   4. SecurityContext is how Spring Security knows "who is the current user"
 *   5. Downstream code can then use @AuthenticationPrincipal or SecurityContextHolder
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No token? Let the request through -- Spring Security will block it
        // if the endpoint requires authentication.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (jwtService.isTokenValid(token)) {
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);
            log.debug("Authenticated user: {}, role: {}", email, role);

            var authentication = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            log.warn("Rejected invalid or expired JWT token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
