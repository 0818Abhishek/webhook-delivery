package com.webhook.delivery.controller;

import com.webhook.delivery.dto.EndpointRequest;
import com.webhook.delivery.dto.EndpointResponse;
import com.webhook.delivery.service.EndpointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/endpoints")
@RequiredArgsConstructor
public class EndpointController {

    private final EndpointService endpointService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointResponse registerEndpoint(@Valid @RequestBody EndpointRequest request) {
        return endpointService.registerEndpoint(request);
    }

    @GetMapping
    public List<EndpointResponse> listEndpoints() {
        return endpointService.listEndpoints();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEndpoint(@PathVariable Long id) {
        endpointService.deleteEndpoint(id);
    }
}