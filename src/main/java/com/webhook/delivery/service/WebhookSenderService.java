package com.webhook.delivery.service;

import com.webhook.delivery.entity.Delivery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Service
@Slf4j
public class WebhookSenderService {

    private final RestClient restClient;

    // Read from application.yml
    @Value("${webhook.timeout.connect:5000}")
    private int connectTimeoutMs;

    @Value("${webhook.timeout.read:10000}")
    private int readTimeoutMs;

    public WebhookSenderService() {
        this.restClient = RestClient.builder()
                .build();
    }

    /**
     * Sends a webhook POST request to the endpoint URL with HMAC-SHA256 signing.
     * Returns true if response is 2xx, false otherwise.
     */
    public boolean sendWebhook(Delivery delivery, String payload) {
        String url = delivery.getEndpoint().getUrl();
        String secret = delivery.getEndpoint().getSecret();
        long timestamp = Instant.now().getEpochSecond();

        // 1. Build the signature
        String signature = generateHmacSha256(payload, secret, timestamp);

        // 2. Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Signature", signature);
        headers.set("X-Webhook-Timestamp", String.valueOf(timestamp));

        try {
            log.info("Sending webhook to: {}, attempt: {}", url, delivery.getAttemptCount() + 1);

            long startTime = System.currentTimeMillis();

            // 3. Send the request using RestClient
            var responseEntity = restClient.post()
                    .uri(url)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            long latencyMs = System.currentTimeMillis() - startTime;
            int statusCode = responseEntity.getStatusCode().value();

            log.info("Webhook response: {}, latency: {}ms", statusCode, latencyMs);

            // 4. Success = 2xx status
            return statusCode >= 200 && statusCode < 300;

        } catch (Exception e) {
            log.error("Webhook delivery failed: {}", e.getMessage());
            return false;
        }
    }
    /**
     * HMAC-SHA256 signing: signature = HMAC-SHA256(secret, payload + timestamp)
     */
    private String generateHmacSha256(String payload, String secret, long timestamp) {
        try {
            String data = payload + timestamp;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}