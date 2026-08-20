package com.webhook.delivery.serviceImpl;

import com.webhook.delivery.context.TenantContext;
import com.webhook.delivery.dto.EventRequest;
import com.webhook.delivery.dto.EventResponse;
import com.webhook.delivery.entity.Delivery;
import com.webhook.delivery.entity.Endpoint;
import com.webhook.delivery.entity.Event;
import com.webhook.delivery.entity.Tenant;
import com.webhook.delivery.repository.DeliveryRepository;
import com.webhook.delivery.repository.EndpointRepository;
import com.webhook.delivery.repository.EventRepository;
import com.webhook.delivery.service.EventService;
import com.webhook.delivery.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EndpointRepository endpointRepository;
    private final DeliveryRepository deliveryRepository;
    private final TenantService tenantService;
    private final tools.jackson.databind.ObjectMapper objectMapper;

    @Override
    @Transactional
    public EventResponse ingestEvent(EventRequest request) {
        // 1. Get Tenant from Context
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant ID not found in request header X-Tenant-Id");
        }

        // 2. Check for Duplicate (Idempotency)
        Optional<Event> existingEvent = eventRepository
                .findByTenantIdAndEventIdExternal(tenantId, request.getEventId());

        if (existingEvent.isPresent()) {
            log.info("Duplicate event detected: tenant={}, eventId={}", tenantId, request.getEventId());
            return EventResponse.builder()
                    .id(existingEvent.get().getId())
                    .status("DUPLICATE")
                    .message("Event already ingested")
                    .build();
        }

        // 3. Fetch Tenant
        Tenant tenant = tenantService.getTenantById(tenantId);

        // 4. Convert payload to JSON string
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(request.getPayload());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }

        // 5. Save the Event
        Event event = Event.builder()
                .tenant(tenant)
                .eventIdExternal(request.getEventId())
                .type(request.getType())
                .payload(payloadJson)
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Event ingested: id={}, type={}, tenant={}", savedEvent.getId(), request.getType(), tenantId);

        // 6. Fan-out to matching endpoints
        List<Endpoint> activeEndpoints = endpointRepository.findActiveEndpointsByTenantId(tenantId);

        int deliveryCount = 0;
        for (Endpoint endpoint : activeEndpoints) {
            // Check if this endpoint subscribes to this event type
            if (endpoint.getSubscribedEventTypes() != null &&
                    endpoint.getSubscribedEventTypes().contains(request.getType())) {

                // Create a PENDING delivery
                Delivery delivery = Delivery.builder()
                        .event(savedEvent)
                        .endpoint(endpoint)
                        .tenant(tenant)
                        .status("PENDING")
                        .attemptCount(0)
                        .nextAttemptAt(Instant.now())
                        .build();

                deliveryRepository.save(delivery);
                deliveryCount++;
                log.debug("Created delivery for endpoint: {}", endpoint.getId());
            }
        }

        log.info("Created {} deliveries for event {}", deliveryCount, savedEvent.getId());

        return EventResponse.builder()
                .id(savedEvent.getId())
                .status("ACCEPTED")
                .message("Event accepted and " + deliveryCount + " deliveries created")
                .build();
    }
}