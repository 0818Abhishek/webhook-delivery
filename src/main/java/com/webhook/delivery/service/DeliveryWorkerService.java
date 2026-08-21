package com.webhook.delivery.service;

import com.webhook.delivery.dto.WebhookResult;
import com.webhook.delivery.entity.Delivery;
import com.webhook.delivery.entity.DeliveryAttempt;
import com.webhook.delivery.entity.Event;
import com.webhook.delivery.repository.DeliveryAttemptRepository;
import com.webhook.delivery.repository.DeliveryRepository;
import com.webhook.delivery.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryWorkerService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final TenantRepository tenantRepository;
    private final WebhookSenderService webhookSenderService;

    private static final int MAX_ATTEMPTS = 8;
    private static final Random random = new Random();

    /**
     * Runs every 1 second. Claims one pending delivery per tenant using SKIP LOCKED.
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processPendingDeliveries() {
        var tenants = tenantRepository.findAll();

        for (var tenant : tenants) {
            try {
                Optional<Delivery> optionalDelivery = deliveryRepository
                        .findAndLockOnePendingDelivery(tenant.getId());

                if (optionalDelivery.isEmpty()) {
                    continue;
                }

                Delivery delivery = optionalDelivery.get();
                Event event = delivery.getEvent();

                log.debug("Worker claimed delivery: id={}, tenant={}, endpoint={}",
                        delivery.getId(), tenant.getId(), delivery.getEndpoint().getId());

                // Send the webhook and get result
                WebhookResult result = webhookSenderService.sendWebhook(delivery, event.getPayload());

                // Record the attempt
                DeliveryAttempt attempt = DeliveryAttempt.builder()
                        .delivery(delivery)
                        .attemptNumber(delivery.getAttemptCount() + 1)
                        .responseCode(result.statusCode())
                        .error(result.responseSnippet())
                        .build();

                if (result.success()) {
                    delivery.setStatus("SUCCESS");
                    delivery.setLastResponseCode(result.statusCode());
                    log.info("Delivery SUCCESS: id={}, endpoint={}", delivery.getId(), delivery.getEndpoint().getUrl());
                } else {
                    int newAttemptCount = delivery.getAttemptCount() + 1;
                    delivery.setAttemptCount(newAttemptCount);
                    delivery.setLastResponseCode(result.statusCode());
                    if (result.responseSnippet() != null) {
                        delivery.setLastResponseSnippet(result.responseSnippet());
                    }

                    if (newAttemptCount >= MAX_ATTEMPTS) {
                        delivery.setStatus("DEAD_LETTERED");
                        delivery.setNextAttemptAt(null);
                        log.warn("Delivery DEAD_LETTERED: id={}, max attempts reached", delivery.getId());
                    } else {
                        long delaySeconds = calculateBackoff(newAttemptCount);
                        Instant nextAttempt = Instant.now().plusSeconds(delaySeconds);
                        delivery.setNextAttemptAt(nextAttempt);
                        delivery.setStatus("PENDING");
                        log.info("Delivery RETRY scheduled: id={}, attempt={}, next={}",
                                delivery.getId(), newAttemptCount, nextAttempt);
                    }
                }

                deliveryRepository.save(delivery);
                deliveryAttemptRepository.save(attempt);

            } catch (Exception e) {
                log.error("Error processing delivery for tenant {}: {}", tenant.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Exponential backoff with jitter: 2^attempt * 1 second + random jitter (0-20%)
     * Example: attempt 1 = 2s, 2 = 4s, 3 = 8s, ... 8 = 256s (~4 min)
     */
    private long calculateBackoff(int attemptCount) {
        long baseDelay = (long) Math.pow(2, attemptCount);
        double jitterFactor = 0.8 + (0.4 * random.nextDouble()); // 0.8 to 1.2
        return (long) (baseDelay * jitterFactor);
    }
}