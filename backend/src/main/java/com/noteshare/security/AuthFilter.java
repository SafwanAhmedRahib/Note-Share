package com.noteshare.security;

import com.noteshare.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Resolves the Authorization header into a userId once, and stores it as a
 * request attribute. Individual controllers/services no longer parse the
 * header themselves - they just call AuthContext.requireUserId(request).
 * This filter never blocks a request by itself; each endpoint still decides
 * whether it requires auth by calling (or not calling) requireUserId.
 */
@Component
public class AuthFilter implements jakarta.servlet.Filter {

    private static final List<String> PUBLIC_PATHS = List.of("/api/register", "/api/login", "/");

    private final AuthService authService;

    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String path = request.getRequestURI();
        if (!PUBLIC_PATHS.contains(path)) {
            String header = request.getHeader("Authorization");
            String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
            Long userId = authService.userIdForToken(token);
            if (userId != null) {
                request.setAttribute("userId", userId);
            }
        }
        chain.doFilter(req, res);
    }
}
