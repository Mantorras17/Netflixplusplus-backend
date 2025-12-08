package org.netflixpp.filter;

import jakarta.ws.rs.container.*;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String method = request.getMethod();
        String path = request.getUriInfo().getPath();
        String ip = request.getHeaderString("X-Forwarded-For");
        if (ip == null) ip = request.getHeaderString("Remote-Addr");

        System.out.printf("%s %s from %s%n", method, path, ip);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        String method = request.getMethod();
        String path = request.getUriInfo().getPath();
        int status = response.getStatus();
        String username = (String) request.getProperty("username");

        System.out.printf("%s %s -> %d (user: %s)%n",
                method, path, status, username != null ? username : "anonymous");
    }
}