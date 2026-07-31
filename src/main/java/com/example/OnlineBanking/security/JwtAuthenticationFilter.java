package com.example.OnlineBanking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        // 🌟 THE FIX: Add the open-account path match check to your critical guard clause right here!
        if ((path.startsWith("/api/bank/auth/") && !path.contains("/verify-mpin")) && !path.contains("/refresh") || path.contains("/open-account")) {
            filterChain.doFilter(request, response);
            return; // Stops running token validations for this route entirely!
        }


        // 1. Extract the Authorization Header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Check if the header is missing or doesn't start with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the clean token string (skip the first 7 characters: "Bearer ")
        jwt = authHeader.substring(7);

        try {
            userEmail = jwtUtils.extractUsername(jwt);
            String role = jwtUtils.extractRole(jwt); // Extracted clean once right here!

            // 4. If a username is found and the security context isn't already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Validate if the token matches our expectations and hasn't expired
                if (jwtUtils.isTokenValid(jwt, userEmail)) {
                    String requestPath = request.getRequestURI();

                    // CASE A: Admins bypass MPIN completely.
                    if (role.equals("ROLE_ADMIN")) {
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userEmail, null, List.of(authority));
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }

                    // CASE B: Customers checking
                    else {
                        boolean isMpinVerified = jwtUtils.extractMpinVerificationStatus(jwt);

                        // Allow entry IF:
                        // 1. Their token is fully verified (isMpinVerified == true)
                        // 2. They are on the step-2 verification gate screen (/verify-mpin)
                        // 3. They are a brand new user initializing their account profile (/open-account)
                        if (isMpinVerified ||
                                requestPath.contains("/verify-mpin") ||
                                requestPath.contains("/refresh") ||
                                requestPath.contains("/open-account") ||
                                requestPath.contains("/reset-mpin")) {

                            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userEmail, null, List.of(authority));
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }

                        // Otherwise, block any access to dashboards or transaction layers!
                        else {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Security Gate: Complete your 2-Step MPIN authentication first.\"}");
                            return; // Stop processing the filter chain immediately!
                        }
                    }
                }
            }
        } catch (Exception e) {
            // If token is tampered with, let it pass through to be handled as unauthenticated
        }

        // 5. Continue processing the request down the chain
        filterChain.doFilter(request, response);
    }
}