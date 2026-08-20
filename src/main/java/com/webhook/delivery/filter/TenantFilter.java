package com.webhook.delivery.filter;

import com.webhook.delivery.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String tenantIdHeader = request.getHeader("X-Tenant-Id");

        if (tenantIdHeader != null && !tenantIdHeader.isEmpty()) {
            try {
                Long tenantId = Long.parseLong(tenantIdHeader);
                TenantContext.setTenantId(tenantId);
            } catch (NumberFormatException ignored) {
                // invalid header – we'll handle missing tenant in service layer
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}