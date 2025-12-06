package org.netflixpp.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Context
    private UriInfo uriInfo;

    private static final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        startTime.set(System.currentTimeMillis());

        String method = requestContext.getMethod();
        String path = uriInfo.getRequestUri().getPath();
        String query = uriInfo.getRequestUri().getQuery();
        String clientIP = getClientIP(requestContext);

        String fullPath = query != null ? path + "?" + query : path;

        logger.info("▶️ {} {} from {}", method, fullPath, clientIP);
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        Long start = startTime.get();
        if (start != null) {
            long duration = System.currentTimeMillis() - start;
            String method = requestContext.getMethod();
            String path = uriInfo.getRequestUri().getPath();
            int status = responseContext.getStatus();

            String emoji = getStatusEmoji(status);

            logger.info("{} {} {} -> {} ({}ms)",
                    emoji, method, path, status, duration);

            startTime.remove();
        }
    }

    private String getClientIP(ContainerRequestContext requestContext) {
        String xForwardedFor = requestContext.getHeaderString("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = requestContext.getHeaderString("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return requestContext.getHeaderString("Remote-Addr");
    }

    private String getStatusEmoji(int status) {
        if (status >= 200 && status < 300) return "✅";
        if (status >= 300 && status < 400) return "🔀";
        if (status >= 400 && status < 500) return "⚠️";
        if (status >= 500) return "❌";
        return "🔷";
    }
}