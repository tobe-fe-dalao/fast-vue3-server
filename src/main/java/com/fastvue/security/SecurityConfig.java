package com.fastvue.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 核心配置。
 *
 * <p>无状态 JWT 认证，禁用 Session；401 / 403 统一输出 {@link ApiResponse} 结构。</p>
 */
@Configuration
@EnableMethodSecurity
@org.springframework.boot.context.properties.EnableConfigurationProperties({
    com.fastvue.config.JwtProperties.class,
    com.fastvue.config.CorsProperties.class
})
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final org.springframework.core.env.Environment environment;

    private final com.fastvue.config.CorsProperties corsProperties;

    private static final String[] WHITELIST = {
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/v3/api-docs/**",
        "/api-docs-ui/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/swagger-ui-default.html",
        "/actuator/health",
        "/actuator/health/liveness",
        "/actuator/health/readiness",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, RateLimitFilter rateLimitFilter) throws Exception {
        http
                // Only explicit Authorization bearer tokens authenticate; no cookie/session
                // credentials.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> {
                            if (!environment.getProperty(
                                    "springdoc.api-docs.enabled", Boolean.class, true)) {
                                auth.requestMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/swagger-ui-default.html",
                                                "/api-docs-ui/**")
                                        .denyAll();
                            }
                            auth.requestMatchers(WHITELIST)
                                    .permitAll()
                                    .requestMatchers(HttpMethod.GET, "/api/v1/public/**")
                                    .permitAll()
                                    .requestMatchers(HttpMethod.POST, "/api/v1/public/**")
                                    .permitAll()
                                    .requestMatchers("/api/v1/auth/me")
                                    .authenticated()
                                    .requestMatchers("/actuator/**")
                                    .hasAuthority("audit:view")
                                    .anyRequest()
                                    .authenticated();
                        })
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(
                                                (request, response, authException) ->
                                                        writeError(
                                                                response,
                                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                                ErrorCode.UNAUTHORIZED))
                                        .accessDeniedHandler(
                                                (request, response, accessDeniedException) ->
                                                        writeError(
                                                                response,
                                                                HttpServletResponse.SC_FORBIDDEN,
                                                                ErrorCode.FORBIDDEN)))
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter>
            jwtRegistration() {
        var registration =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(
                        jwtAuthenticationFilter);
        registration.setEnabled(false); // Owned exclusively by Spring Security's chain.
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(corsProperties.allowedOriginPatterns());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setExposedHeaders(List.of("X-Request-Id", "Retry-After"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    private void writeError(HttpServletResponse response, int status, ErrorCode errorCode)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}
