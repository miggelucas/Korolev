package br.ufpe.cin.taes2.korolev_engine.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Filter to intercept all API requests and log the processing time.
 * This provides performance monitoring metrics in the application logs.
 */
@Slf4j
@Component
public class PerformanceLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only log performance for our API endpoints
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTimeNs = System.nanoTime();
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            long endTimeNs = System.nanoTime();
            long durationMs = Duration.ofNanos(endTimeNs - startTimeNs).toMillis();
            
            // Log performance format: [Performance] GET /api/flags/graph executed in 45ms
            log.info("[Performance] {} {} executed in {}ms with status {}", 
                    request.getMethod(), 
                    request.getRequestURI(), 
                    durationMs,
                    response.getStatus());
        }
    }
}
