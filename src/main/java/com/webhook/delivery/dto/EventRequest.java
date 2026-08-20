package com.webhook.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventRequest {

    @NotBlank(message = "eventId is required")
    private String eventId;

    @NotBlank(message = "type is required")
    private String type;

    @NotNull(message = "payload cannot be null")
    private Object payload; // Can be any JSON object
}