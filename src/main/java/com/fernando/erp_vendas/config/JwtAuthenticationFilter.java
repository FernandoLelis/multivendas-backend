package com.fernando.erp_vendas.config;

import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.repository.UserRepository;
import com.fernando.erp_vendas.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        System.out.println("🔄 JWT FILTER - Construtor chamado! Filtro instanciado.");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("🚀 JWT FILTER - Iniciando para: " + request.getMethod() + " " + request.getRequestURI());

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        System.out.println("🔐 JWT FILTER - Authorization Header: " + authHeader);

        // Se não tem header Authorization, continua sem autenticação
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("❌ JWT FILTER - No Bearer token found, continuando chain...");
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Remove "Bearer "
        userEmail = jwtService.extractUsername(jwt);

        System.out.println("📧 JWT FILTER - Email extraído: " + userEmail);

        // Se tem email no token e não tem autenticação no contexto
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("👤 JWT FILTER - Buscando usuário no banco...");

            // Buscar usuário no banco
            User user = userRepository.findByEmail(userEmail).orElse(null);
            System.out.println("👤 JWT FILTER - Usuário encontrado: " + (user != null ? user.getEmail() : "null"));

            if (user != null && jwtService.validateToken(jwt, user)) {
                System.out.println("✅ JWT FILTER - Token válido! Configurando autenticação...");

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                System.out.println("✅ JWT FILTER - Autenticação configurada para: " + user.getEmail());
            } else {
                System.out.println("❌ JWT FILTER - Token inválido ou usuário não encontrado");
            }
        } else {
            System.out.println("ℹ️ JWT FILTER - Já autenticado ou sem email no token");
        }

        System.out.println("➡️ JWT FILTER - Continuando filter chain...");
        filterChain.doFilter(request, response);
    }
}