package com.webhook.delivery.repository;

import com.webhook.delivery.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByTenantIdAndEventIdExternal(Long tenantId, String eventIdExternal);
}