package com.webhook.delivery.service;

import com.webhook.delivery.context.TenantContext;
import com.webhook.delivery.dto.EndpointRequest;
import com.webhook.delivery.dto.EndpointResponse;
import com.webhook.delivery.entity.Endpoint;
import com.webhook.delivery.entity.Tenant;
import com.webhook.delivery.repository.EndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service  // This stays on the Implementation
@RequiredArgsConstructor
public class EndpointServiceImpl implements EndpointService { // <-- Add implements

    private final EndpointRepository endpointRepository;
    private final TenantService tenantService;

    @Override // Add @Override to all methods
    @Transactional
    public EndpointResponse registerEndpoint(EndpointRequest request) {
        // ... (keep the exact same code you already have)
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant ID not found in request header X-Tenant-Id");
        }
        validateUrl(request.getUrl());
        Tenant tenant = tenantService.getTenantById(tenantId);
        String secret = UUID.randomUUID().toString();
        Endpoint endpoint = Endpoint.builder()
                .tenant(tenant)
                .url(request.getUrl())
                .secret(secret)
                .subscribedEventTypes(request.getEventTypes())
                .status("ACTIVE")
                .build();
        Endpoint saved = endpointRepository.save(endpoint);
        return EndpointResponse.builder()
                .id(saved.getId())
                .url(saved.getUrl())
                .secret(saved.getSecret())
                .subscribedEventTypes(saved.getSubscribedEventTypes())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public List<EndpointResponse> listEndpoints() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant ID not found");
        }
        List<Endpoint> endpoints = endpointRepository.findActiveEndpointsByTenantId(tenantId);
        return endpoints.stream()
                .map(e -> EndpointResponse.builder()
                        .id(e.getId())
                        .url(e.getUrl())
                        .secret(null)
                        .subscribedEventTypes(e.getSubscribedEventTypes())
                        .status(e.getStatus())
                        .createdAt(e.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Endpoint getEndpointById(Long endpointId) {
        Long tenantId = TenantContext.getTenantId();
        return endpointRepository.findById(endpointId)
                .filter(e -> e.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Endpoint not found or access denied"));
    }

    @Override
    @Transactional
    public void deleteEndpoint(Long endpointId) {
        Endpoint endpoint = getEndpointById(endpointId);
        endpoint.setStatus("DISABLED");
        endpointRepository.save(endpoint);
    }

    private void validateUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                throw new RuntimeException("Only HTTP/HTTPS URLs are allowed");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL format: " + urlString);
        }
    }
}