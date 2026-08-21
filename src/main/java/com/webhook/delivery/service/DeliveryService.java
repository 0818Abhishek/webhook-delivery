package com.webhook.delivery.service;

import com.webhook.delivery.dto.DeliveryResponse;

import java.util.List;

public interface DeliveryService {

    List<DeliveryResponse> getDeliveriesByEvent(Long eventId);

    List<DeliveryResponse> getDeliveriesByEndpoint(Long endpointId);

    void redriveDelivery(Long deliveryId);
}