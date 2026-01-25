package com.ahd.trading_platform.shared.infrastructure.web.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP request/response logging filter.
 * Logs incoming HTTP requests and outgoing responses with execution time.
 */
@Slf4j
@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_PAYLOAD_LENGTH = 1000; // Maximum length of request/response body to log

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only log API controller endpoints (not Camunda, actuator, etc.)
        String path = request.getRequestURI();
        if (!shouldFilter(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Wrap request and response to enable reading body multiple times
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // Log request
            logRequest(wrappedRequest);

            // Proceed with the request
            filterChain.doFilter(wrappedRequest, wrappedResponse);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log response
            logResponse(wrappedRequest, wrappedResponse, duration);

            // Copy response body back to original response (important!)
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullPath = queryString != null ? uri + "?" + queryString : uri;

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n>>> HTTP REQUEST: ").append(method).append(" ").append(fullPath);

        // Log headers only in DEBUG mode
        if (log.isDebugEnabled()) {
            Map<String, String> headers = getHeaders(request);
            if (!headers.isEmpty()) {
                logMessage.append("\n    Headers: ").append(headers);
            }
        }

        // Log body for POST/PUT/PATCH
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            String body = getRequestBody(request);
            if (!body.isEmpty()) {
                logMessage.append("\n    Body: ").append(body);
            }
        }

        log.info(logMessage.toString());
    }

    private void logResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n<<< HTTP RESPONSE: ").append(method).append(" ").append(uri);
        logMessage.append("\n    Status: ").append(status);
        logMessage.append("\n    Duration: ").append(duration).append("ms");

        // Log response body for JSON/text responses
        String contentType = response.getContentType();
        if (contentType != null && (contentType.contains("application/json") || contentType.contains("text"))) {
            String body = getResponseBody(response);
            if (!body.isEmpty()) {
                logMessage.append("\n    Body: ").append(body);
            }
        }

        // Use different log level based on status code
        if (status >= 500) {
            log.error(logMessage.toString());
        } else if (status >= 400) {
            log.warn(logMessage.toString());
        } else {
            log.info(logMessage.toString());
        }
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // Skip sensitive headers
            if (!isSensitiveHeader(headerName)) {
                headers.put(headerName, request.getHeader(headerName));
            }
        }

        return headers;
    }

    private boolean isSensitiveHeader(String headerName) {
        String lowerCaseName = headerName.toLowerCase();
        return lowerCaseName.contains("authorization") ||
               lowerCaseName.contains("password") ||
               lowerCaseName.contains("token") ||
               lowerCaseName.contains("cookie") ||
               lowerCaseName.contains("api-key") ||
               lowerCaseName.contains("apikey");
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            return truncate(body);
        }
        return "";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            String body = new String(content, StandardCharsets.UTF_8);
            return truncate(body);
        }
        return "";
    }

    private String truncate(String content) {
        if (content.length() > MAX_PAYLOAD_LENGTH) {
            return content.substring(0, MAX_PAYLOAD_LENGTH) + "... (truncated)";
        }
        return content;
    }

    /**
     * Only filter API controller endpoints.
     * Excludes Camunda endpoints, actuator, Swagger, and static resources.
     */
    private boolean shouldFilter(String path) {
        // Only log paths starting with /api/
        return path.startsWith("/api/");
    }
}
