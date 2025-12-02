package com.fernando.erp_vendas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ✅ CONFIGURAÇÃO DE SEGURANÇA TEMPORARIAMENTE DESABILITADA
    // TODO: Restaurar configuração completa após resolver problema de autenticação

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ Configuração CORS básica para permitir frontend
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://erpmultivendas.vercel.app",
                "https://multivendas-frontend-pqp2py6jb-fernandolelis-projects.vercel.app",
                "http://localhost:4200",
                "https://multivendas-frontend.vercel.app",
                "https://*.vercel.app"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", "X-Auth-Token"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ⚠️ FILTRO JWT TEMPORARIAMENTE DESABILITADO
    // @Bean
    // public JwtAuthenticationFilter jwtAuthenticationFilter() {
    //     return new JwtAuthenticationFilter(jwtService, userRepository);
    // }

    // ⚠️ SECURITY FILTER CHAIN TEMPORARIAMENTE DESABILITADO
    // @Bean
    // public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    //     http
    //             .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    //             .csrf(csrf -> csrf.disable())
    //             .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    //             .authorizeHttpRequests(auth -> auth
    //                     .requestMatchers("/api/auth/**").permitAll()
    //                     .requestMatchers("/api/migracao/**").permitAll()
    //                     .anyRequest().authenticated()
    //             )
    //             .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    //
    //     return http.build();
    // }
}