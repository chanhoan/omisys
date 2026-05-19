package com.omisys.common.domain.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Objects;

public class InternalSecretFilter implements Filter {

    private final String internalSecret;

    public InternalSecretFilter(String internalSecret) {
        this.internalSecret = internalSecret;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!isInternalPath(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String providedSecret = httpRequest.getHeader(InternalSecretConstant.X_INTERNAL_SECRET);
        if (internalSecret == null || internalSecret.isBlank()
                || !Objects.equals(internalSecret, providedSecret)) {
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isInternalPath(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/internal/");
    }
}
