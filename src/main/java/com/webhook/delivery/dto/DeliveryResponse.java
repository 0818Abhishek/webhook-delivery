package com.webhook.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponse {

    private Long id;
    private Long eventId;
    private Long endpointId;
    private String status; // PENDING, SUCCESS, FAILED, DEAD_LETTERED
    private Integer attemptCount;
    private Integer lastResponseCode;
    private String lastResponseSnippet;
    private Instant nextAttemptAt;
    private Instant createdAt;
}