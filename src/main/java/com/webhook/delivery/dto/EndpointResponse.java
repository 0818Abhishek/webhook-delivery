package com.webhook.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointResponse {

    private Long id;
    private String url;
    private String secret; // Only for the first creation, then never exposed again
    private List<String> subscribedEventTypes;
    private String status;
    private Instant createdAt;
}