package com.webhook.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EndpointRequest {

    @NotBlank(message = "URL is required")
    private String url;

    @NotNull(message = "Event types list cannot be null")
    private List<String> eventTypes; // e.g., ["invoice.paid", "user.created"]
}