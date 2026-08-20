package com.webhook.delivery.service;

import com.webhook.delivery.entity.Tenant;
import com.webhook.delivery.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    public Tenant getTenantById(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));
    }
}