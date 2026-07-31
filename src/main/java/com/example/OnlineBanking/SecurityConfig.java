package com.example.OnlineBanking;

import com.example.OnlineBanking.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. Strictly public authorization endpoints
                        .requestMatchers(
                                "/api/bank/auth/login",
                                "/api/bank/auth/signup",
                                "/api/bank/auth/admin/signup",
                                "/api/bank/auth/refresh",
                                "/api/bank/auth/forgot-password",
                                "/api/bank/auth/reset-password").permitAll()
                        .requestMatchers("/api/bank/user/open-account").permitAll()

                        // 2. REQUIRED CHANGE: Allow users holding a login token to hit the reset route!
                        .requestMatchers("/api/bank/user/reset-mpin").authenticated()
                        .requestMatchers("/api/bank/auth/verify-mpin").authenticated()

                        // 3. Application domains
                        .requestMatchers("/api/bank/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        .requestMatchers("/api/bank/transactions/**").hasAnyAuthority("ROLE_USER", "USER", "ROLE_ADMIN", "ADMIN")
                        .requestMatchers("/api/bank/user/**").hasAnyAuthority("ROLE_USER", "USER")
                        .anyRequest().authenticated())

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // This MUST sit cleanly before the UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 🌟 2. This single bean now perfectly covers BOTH standard endpoints AND security endpoints!
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }


}
