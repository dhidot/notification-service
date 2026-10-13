package com.careertrack.notification.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public final class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private final byte[] configuredKeyHash;

    public InternalApiKeyFilter(@Value("${careertrack.internal.api-key:}") String configuredKey) {
        // Fail closed. Never start the service with an unconfigured internal endpoint.
        if (configuredKey == null || configuredKey.length() < 32) {
            throw new IllegalStateException("INTERNAL_API_KEY must contain at least 32 characters");
        }
        this.configuredKeyHash = sha256(configuredKey);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Protect every request by default. Only explicitly public health endpoints
        // are exempt; MockMvc may leave getServletPath() empty.
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()
                && path.startsWith(contextPath + "/")) {
            path = path.substring(contextPath.length());
        }

        return path.equals("/actuator/health")
                || path.equals("/actuator/health/liveness")
                || path.equals("/actuator/health/readiness")
                || path.equals("/actuator/info");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader(API_KEY_HEADER);
        if (provided == null || !MessageDigest.isEqual(configuredKeyHash, sha256(provided))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("Cache-Control", "no-store");
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }
}
