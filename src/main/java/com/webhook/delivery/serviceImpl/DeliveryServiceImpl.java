package com.webhook.delivery.serviceImpl;

import com.webhook.delivery.context.TenantContext;
import com.webhook.delivery.dto.DeliveryResponse;
import com.webhook.delivery.entity.Delivery;
import com.webhook.delivery.repository.DeliveryRepository;
import com.webhook.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;

    @Override
    public List<DeliveryResponse> getDeliveriesByEvent(Long eventId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant ID not found");
        }

        // Fetch deliveries for this event, filter by tenant in Java or DB
        List<Delivery> deliveries = deliveryRepository.findByEventId(eventId);

        // Filter by tenant to ensure isolation
        return deliveries.stream()
                .filter(d -> d.getTenant().getId().equals(tenantId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeliveryResponse> getDeliveriesByEndpoint(Long endpointId) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant ID not found");
        }

        List<Delivery> deliveries = deliveryRepository.findByEndpointId(endpointId);

        return deliveries.stream()
                .filter(d -> d.getTenant().getId().equals(tenantId))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private DeliveryResponse toResponse(Delivery delivery) {
        return DeliveryResponse.builder()
                .id(delivery.getId())
                .eventId(delivery.getEvent().getId())
                .endpointId(delivery.getEndpoint().getId())
                .status(delivery.getStatus())
                .attemptCount(delivery.getAttemptCount())
                .lastResponseCode(delivery.getLastResponseCode())
                .lastResponseSnippet(delivery.getLastResponseSnippet())
                .nextAttemptAt(delivery.getNextAttemptAt())
                .createdAt(delivery.getCreatedAt())
                .build();
    }
}