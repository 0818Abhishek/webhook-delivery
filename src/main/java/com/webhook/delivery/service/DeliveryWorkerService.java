package com.webhook.delivery.service;

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
        // 1. Get all active tenants (to process deliveries per tenant)
        var tenants = tenantRepository.findAll();

        for (var tenant : tenants) {
            try {
                // 2. Claim ONE pending delivery for this tenant (SKIP LOCKED)
                Optional<Delivery> optionalDelivery = deliveryRepository
                        .findAndLockOnePendingDelivery(tenant.getId());

                if (optionalDelivery.isEmpty()) {
                    continue; // No pending deliveries for this tenant
                }

                Delivery delivery = optionalDelivery.get();
                Event event = delivery.getEvent();

                log.debug("Worker claimed delivery: id={}, tenant={}, endpoint={}",
                        delivery.getId(), tenant.getId(), delivery.getEndpoint().getId());

                // 3. Send the webhook
                boolean success = webhookSenderService.sendWebhook(delivery, event.getPayload());

                // 4. Record the attempt
                DeliveryAttempt attempt = DeliveryAttempt.builder()
                        .delivery(delivery)
                        .attemptNumber(delivery.getAttemptCount() + 1)
                        .build();

                if (success) {
                    // SUCCESS: mark delivery as done
                    delivery.setStatus("SUCCESS");
                    attempt.setResponseCode(200);
                    delivery.setLastResponseCode(200);
                    log.info("Delivery SUCCESS: id={}, endpoint={}", delivery.getId(), delivery.getEndpoint().getUrl());
                } else {
                    // FAILURE: retry or dead-letter
                    int newAttemptCount = delivery.getAttemptCount() + 1;
                    delivery.setAttemptCount(newAttemptCount);
                    attempt.setResponseCode(500); // Placeholder, real code would come from response

                    if (newAttemptCount >= MAX_ATTEMPTS) {
                        // Dead-letter
                        delivery.setStatus("DEAD_LETTERED");
                        log.warn("Delivery DEAD_LETTERED: id={}, max attempts reached", delivery.getId());
                    } else {
                        // Retry: exponential backoff + jitter
                        long delaySeconds = calculateBackoff(newAttemptCount);
                        Instant nextAttempt = Instant.now().plusSeconds(delaySeconds);
                        delivery.setNextAttemptAt(nextAttempt);
                        delivery.setStatus("PENDING"); // Keep pending for retry
                        log.info("Delivery RETRY scheduled: id={}, attempt={}, next={}",
                                delivery.getId(), newAttemptCount, nextAttempt);
                    }
                }

                // 5. Save the delivery and attempt
                deliveryRepository.save(delivery);
                deliveryAttemptRepository.save(attempt);

            } catch (Exception e) {
                log.error("Error processing delivery for tenant {}: {}", tenant.getId(), e.getMessage(), e);
                // Rollback happens automatically due to @Transactional
                // The locked row will be released, and the delivery stays PENDING
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