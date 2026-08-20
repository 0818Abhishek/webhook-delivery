package com.webhook.delivery.service;

import com.webhook.delivery.dto.EventRequest;
import com.webhook.delivery.dto.EventResponse;

public interface EventService {
    EventResponse ingestEvent(EventRequest request);
}