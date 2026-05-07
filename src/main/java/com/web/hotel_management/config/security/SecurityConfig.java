package com.web.hotel_management.config.security;

import com.web.hotel_management.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Note: with server.servlet.context-path=/hotel-management, matchers should NOT include it.
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/auth/client/**").permitAll()
                // Self profile endpoints for any authenticated user
                .requestMatchers("/me/**").authenticated()
                // Only ADMIN can manage users via REST
                .requestMatchers("/user/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Public endpoints (guest web helpers)
                .requestMatchers("/api/public/**").permitAll()
                // Public booking create requires CLIENT login
                .requestMatchers(HttpMethod.POST, "/api/bookings").hasRole("CLIENT")
                // Client self profile endpoints
                .requestMatchers("/api/client/**").hasRole("CLIENT")
                // Receptionist endpoints
                .requestMatchers("/api/reception/**").hasRole("RECEPTIONIST")
                // Branch manager endpoints (also allowed for admin)
                .requestMatchers("/api/branch/**").hasAnyRole("BRANCH_MANAGER", "ADMIN")
                .requestMatchers(
                        "/",
                        "/index.html",
                        "/room-types.html",
                        "/room-type-detail.html",
                        "/room-list.html",
                        "/room.html",
                        "/client/**",
                        "/admin",
                        "/admin/**",
                        "/staff/**",
                        "/reception/**",
                        "/uploads/**",
                        "/assets/**"
                ).permitAll()
                // Public APIs for guest web
                .requestMatchers("/api/hotels/**").permitAll()
                .requestMatchers("/api/rooms/**").permitAll()
                .requestMatchers("/api/room-types/**").permitAll()
                .requestMatchers("/api/bookings/**").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
