package com.webhook.delivery.service;

import com.webhook.delivery.entity.Tenant;

public interface TenantService {

    Tenant getTenantById(Long tenantId);
}