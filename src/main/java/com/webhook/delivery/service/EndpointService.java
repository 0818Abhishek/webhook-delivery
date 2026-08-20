package com.webhook.delivery.service;

import com.webhook.delivery.dto.EndpointRequest;
import com.webhook.delivery.dto.EndpointResponse;
import com.webhook.delivery.entity.Endpoint;

import java.util.List;

public interface EndpointService {

    EndpointResponse registerEndpoint(EndpointRequest request);

    List<EndpointResponse> listEndpoints();

    Endpoint getEndpointById(Long endpointId);

    void deleteEndpoint(Long endpointId);
}