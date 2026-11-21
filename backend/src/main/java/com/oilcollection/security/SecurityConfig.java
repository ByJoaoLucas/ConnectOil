package com.oilcollection.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true)
                        )
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicy(policy -> policy
                                .policy("geolocation=(), microphone=(), camera=()")
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/collection-points/**").hasAnyRole("ADMIN", "COLLECTOR", "CLIENT", "ESTABLISHMENT")
                        .requestMatchers(HttpMethod.POST, "/api/collection-points/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/collection-points/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/collection-points/**").hasRole("ADMIN")
                        .requestMatchers("/api/schedules/**").hasAnyRole("ADMIN", "COLLECTOR", "ESTABLISHMENT")
                        .requestMatchers("/api/records/**").hasAnyRole("ADMIN", "COLLECTOR", "ESTABLISHMENT")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/reports/**").hasRole("ADMIN")
                        .requestMatchers("/api/products/**").hasAnyRole("ADMIN", "COLLECTOR", "ESTABLISHMENT")
                        .requestMatchers(HttpMethod.GET, "/api/exchange-items/**").hasAnyRole("ADMIN", "COLLECTOR", "ESTABLISHMENT")
                        .requestMatchers("/api/exchange-items/**").hasRole("ADMIN")
                        .requestMatchers("/api/incidents/**").hasAnyRole("ADMIN", "COLLECTOR", "ESTABLISHMENT")
                        .requestMatchers("/api/notifications/**").hasAnyRole("ADMIN", "COLLECTOR", "ESTABLISHMENT")
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "COLLECTOR", "ESTABLISHMENT")
                        .requestMatchers(HttpMethod.GET, "/api/system-config/price/base-oil").hasAnyRole("ADMIN", "COLLECTOR")
                        .requestMatchers("/api/system-config/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/collector-coverage/**").hasAnyRole("ADMIN", "COLLECTOR")
                        .requestMatchers("/api/collector-coverage/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow specific frontend origins - no wildcard with credentials
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://connectoil.com.br"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Cache-Control",
                "X-File-Name"
        ));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}