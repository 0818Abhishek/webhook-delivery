package com.webhook.delivery.controller;

import com.webhook.delivery.dto.EventRequest;
import com.webhook.delivery.dto.EventResponse;
import com.webhook.delivery.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventResponse ingestEvent(@Valid @RequestBody EventRequest request) {
        return eventService.ingestEvent(request);
    }
}