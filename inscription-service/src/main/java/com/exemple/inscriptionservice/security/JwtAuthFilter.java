package com.exemple.inscriptionservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Map<String,Object> claims = jwtService.parseClaims(token);
                String subject = (String) claims.get("sub");
                Collection<SimpleGrantedAuthority> authorities = extractAuthorities(claims);
                var principal = new User(subject, "", authorities);
                var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                if (log.isDebugEnabled()) {
                    log.debug("JWT OK for subject={}, path={}, roles={}", subject, request.getRequestURI(), authorities);
                }
            } catch (Exception ignored) {}
        }
        filterChain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private Collection<SimpleGrantedAuthority> extractAuthorities(Map<String,Object> claims) {
        Object rolesObj = claims.get("roles");
        if (rolesObj == null) return List.of();

        if (rolesObj instanceof Collection<?> col) {
            return col.stream()
                    .map(elem -> {
                        if (elem instanceof Map<?,?> m) {
                            Object name = m.get("name");
                            return name != null ? String.valueOf(name) : String.valueOf(m);
                        }
                        return String.valueOf(elem);
                    })
                    .map(this::normalizeRole)
                    .flatMap(r -> List.of(
                            new SimpleGrantedAuthority("ROLE_" + r),
                            new SimpleGrantedAuthority(r)
                    ).stream())
                    .collect(Collectors.toSet());
        }

        String single = normalizeRole(String.valueOf(rolesObj));
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + single),
                new SimpleGrantedAuthority(single)
        );
    }

    private String normalizeRole(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        int dot = s.lastIndexOf('.');
        if (dot >= 0 && dot < s.length()-1) s = s.substring(dot+1);
        s = s.replaceAll("[^A-Za-z_]+", "");
        return s.toUpperCase();
    }
}
