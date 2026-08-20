package com.webhook.delivery.controller;

import com.webhook.delivery.dto.DeliveryResponse;
import com.webhook.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/events/{eventId}/deliveries")
    public List<DeliveryResponse> getDeliveriesByEvent(@PathVariable Long eventId) {
        return deliveryService.getDeliveriesByEvent(eventId);
    }

    @GetMapping("/endpoints/{endpointId}/deliveries")
    public List<DeliveryResponse> getDeliveriesByEndpoint(@PathVariable Long endpointId) {
        return deliveryService.getDeliveriesByEndpoint(endpointId);
    }
}