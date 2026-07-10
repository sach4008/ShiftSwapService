package com.ukg.shiftswap.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukg.shiftswap.repository.EmployeeRepository;
import com.ukg.shiftswap.service.SwapMetrics;
import com.ukg.shiftswap.web.error.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class IdentityFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String CURRENT_USER_ID_ATTRIBUTE = "com.ukg.shiftswap.currentUserId";

    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;
    private final SwapMetrics metrics;

    public IdentityFilter(EmployeeRepository employeeRepository, ObjectMapper objectMapper, SwapMetrics metrics) {
        this.employeeRepository = employeeRepository;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isExempt(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = parse(request.getHeader(USER_ID_HEADER));
        if (userId == null || employeeRepository.findById(userId).isEmpty()) {
            metrics.recordIdentityFailure();
            writeUnauthenticated(request, response);
            return;
        }

        request.setAttribute(CURRENT_USER_ID_ATTRIBUTE, userId);
        filterChain.doFilter(request, response);
    }

    private boolean isExempt(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")
                || path.startsWith("/actuator") || path.startsWith("/h2-console");
    }

    private Long parse(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(header.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void writeUnauthenticated(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                "X-User-Id header is missing or does not identify a known employee", request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
