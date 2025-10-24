package com.innowise.orderservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.Collections;
import java.util.List;

import static com.innowise.orderservice.config.AuthConstant.AUTH_HEADER;
import static com.innowise.orderservice.config.AuthConstant.ROLE_PREFIX;
import static com.innowise.orderservice.config.AuthConstant.TOKEN_PREFIX;

/**
 * @ClassName JwtAuthenticationFilter
 * @Description Security filter responsible for extracting and validating JWT tokens from incoming requests.
 * @Author dshparko
 * @Date 16.10.2025 10:08
 * @Version 1.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null) {
                Claims claims = parseClaims(token);
                String username = claims.getSubject();
                List<GrantedAuthority> authorities = extractRoles(claims);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, token, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            filterChain.doFilter(request, response);
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            request.setAttribute("SPRING_SECURITY_LAST_EXCEPTION", ex);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        return (authHeader != null && authHeader.startsWith(TOKEN_PREFIX))
                ? authHeader.substring(TOKEN_PREFIX.length())
                : null;
    }

    private Claims parseClaims(String token) {
        try {
            return jwtParser().parseClaimsJws(token).getBody();
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthenticationException("Invalid JWT: " + e.getMessage()) {
            };
        }
    }

    private List<GrantedAuthority> extractRoles(Claims claims) {
        String role = claims.get("role", String.class);
        return (role == null || role.isBlank())
                ? Collections.emptyList()
                : List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role));
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private io.jsonwebtoken.JwtParser jwtParser() {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build();
    }
}

