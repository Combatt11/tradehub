package com.nexusmarket.adapters.security;

import com.nexusmarket.domain.models.User;
import com.nexusmarket.domain.ports.out.UserRepositoryPort;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Filtro que valida el token Bearer y autentica la peticion en el contexto
 * de Spring Security.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepositoryPort userRepositoryPort;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepositoryPort userRepositoryPort
    ) {
        this.jwtService = Objects.requireNonNull(jwtService, "jwtService must not be null");
        this.userRepositoryPort = Objects.requireNonNull(
                userRepositoryPort, "userRepositoryPort must not be null");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null
                && authorization.regionMatches(
                        true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            authenticate(request, authorization.substring(BEARER_PREFIX.length()));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            String email = jwtService.extractUsername(token);

            if (email == null || SecurityContextHolder.getContext().getAuthentication() != null) {
                return;
            }

            userRepositoryPort.findByEmail(email)
                    .filter(User::isActive)
                    .ifPresent(user -> setAuthentication(request, user));
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
        }
    }

    private void setAuthentication(HttpServletRequest request, User user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        AuthenticatedUser.from(user),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
