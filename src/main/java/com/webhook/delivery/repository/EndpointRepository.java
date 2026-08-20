package com.webhook.delivery.repository;

import com.webhook.delivery.entity.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, Long> {

    @Query("SELECT e FROM Endpoint e WHERE e.tenant.id = :tenantId AND e.status = 'ACTIVE'")
    List<Endpoint> findActiveEndpointsByTenantId(@Param("tenantId") Long tenantId);
}