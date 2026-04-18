package com.example.vertexSpace.config;

import com.example.vertexSpace.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ============================================================
                        // PUBLIC ENDPOINTS
                        // ============================================================

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                        .requestMatchers("/actuator/health", "/health").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/auth/logout").authenticated()

                        // ============================================================
                        // BUILDINGS
                        // ============================================================

                        .requestMatchers(HttpMethod.POST, "/api/v1/buildings/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/buildings/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/buildings/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/buildings/**").authenticated()

                        // ============================================================
                        // FLOORS
                        // ============================================================

                        .requestMatchers(HttpMethod.POST, "/api/v1/floors/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/floors/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/floors/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/floors/**").authenticated()

                        // ============================================================
                        // RESOURCES
                        // ============================================================

                        .requestMatchers(HttpMethod.POST, "/api/v1/resources/**")
                        .hasAnyRole("SYSTEM_ADMIN", "DEPT_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/resources/**")
                        .hasAnyRole("SYSTEM_ADMIN", "DEPT_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/resources/**")
                        .hasAnyRole("SYSTEM_ADMIN", "DEPT_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/resources/**").authenticated()

                        // ============================================================
                        // BOOKINGS (order matters)
                        // ============================================================

                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/all")
                        .hasAnyRole("SYSTEM_ADMIN", "DEPT_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/user/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/resource/**")
                        .hasAnyRole("SYSTEM_ADMIN", "DEPT_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/department/**")
                        .hasAnyRole("SYSTEM_ADMIN", "DEPT_ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/my-bookings/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/bookings/**").authenticated()

                        // ============================================================
                        // AVAILABILITY
                        // ============================================================

                        .requestMatchers(HttpMethod.GET, "/api/v1/availability/**").authenticated()

                        // ============================================================
                        // WAITLIST (legacy paths)
                        // ============================================================

                        .requestMatchers(HttpMethod.GET, "/api/v1/waitlist/all")
                        .hasAnyRole("SYSTEM_ADMIN", "DEPT_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/waitlist/resource/**")
                        .hasAnyRole("SYSTEM_ADMIN", "DEPT_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/waitlist").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/waitlist/my-entries").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/waitlist/*").authenticated()

                        // ============================================================
                        // WAITLIST-ENTRIES (new paths) ✅ includes /me
                        // ============================================================

                        .requestMatchers(HttpMethod.GET, "/api/v1/waitlist-entries/all")
                        .hasAnyRole("SYSTEM_ADMIN", "DEPT_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/waitlist-entries/resource/**")
                        .hasAnyRole("SYSTEM_ADMIN", "DEPT_ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/waitlist-entries").authenticated()

                        // Most specific user endpoints first:
                        .requestMatchers(HttpMethod.GET, "/api/v1/waitlist-entries/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/waitlist-entries/my-entries").authenticated()

                        // Collection listing (if you use it):
                        .requestMatchers(HttpMethod.GET, "/api/v1/waitlist-entries").authenticated()

                        .requestMatchers(HttpMethod.DELETE, "/api/v1/waitlist-entries/*").authenticated()

                        // ============================================================
                        // OFFERS
                        // ============================================================

                        .requestMatchers(HttpMethod.GET, "/api/v1/offers/all").hasAnyRole("SYSTEM_ADMIN", "DEPT_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/offers/my-offers").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/offers/*/accept").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/offers/*/decline").authenticated()

                        // ============================================================
                        // CATCH-ALL
                        // ============================================================

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
