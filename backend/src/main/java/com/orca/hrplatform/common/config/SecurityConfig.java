package com.orca.hrplatform.common.config;

import com.orca.hrplatform.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/health", "/health").permitAll()
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/ad/login", "/api/v1/auth/refresh", "/auth/login", "/auth/ad/login", "/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/attendance/**", "/api/v1/attendance/**").permitAll()
                .requestMatchers("/employees/*/files/**", "/api/v1/employees/*/files/**").permitAll()
                .requestMatchers("/admin/integrations/ad/**", "/api/v1/admin/integrations/ad/**").permitAll()
                .requestMatchers("/api/v1/auth/logout", "/api/v1/auth/me", "/auth/logout", "/auth/me").authenticated()
                .requestMatchers("/admin/**", "/api/v1/admin/**").hasAnyRole("SUPER_ADMIN", "HR_ADMIN", "INTEGRATION_ADMIN")
                .requestMatchers(HttpMethod.POST, "/attendance/import", "/api/v1/attendance/import").hasAnyRole("SUPER_ADMIN", "HR_ADMIN", "INTEGRATION_ADMIN")
                .requestMatchers("/attendance/**", "/api/v1/attendance/**").hasAnyRole("SUPER_ADMIN", "HR_ADMIN", "INTEGRATION_ADMIN", "MANAGER", "EMPLOYEE")
                .requestMatchers("/employees/**", "/api/v1/employees/**").authenticated()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        DelegatingPasswordEncoder encoder = (DelegatingPasswordEncoder) PasswordEncoderFactories.createDelegatingPasswordEncoder();
        encoder.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
        return encoder;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
