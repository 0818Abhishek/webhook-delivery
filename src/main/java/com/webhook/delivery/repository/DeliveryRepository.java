package com.webhook.delivery.repository;

import com.webhook.delivery.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // CRITICAL: This is the SKIP LOCKED query – no @Lock annotation needed
    @Query(value = """
            SELECT * FROM deliveries 
            WHERE tenant_id = :tenantId 
              AND status = 'PENDING' 
              AND next_attempt_at <= NOW() 
            ORDER BY next_attempt_at ASC 
            LIMIT 1 
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<Delivery> findAndLockOnePendingDelivery(@Param("tenantId") Long tenantId);

    List<Delivery> findByEventId(Long eventId);

    List<Delivery> findByEndpointId(Long endpointId);

    @Modifying
    @Query("UPDATE Delivery d SET d.status = 'PENDING', d.attemptCount = 0, d.nextAttemptAt = :nextAttempt, d.lastResponseCode = NULL, d.lastResponseSnippet = NULL WHERE d.id = :id")
    void resetDelivery(@Param("id") Long id, @Param("nextAttempt") Instant nextAttempt);
}