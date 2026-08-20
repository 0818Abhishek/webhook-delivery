package com.webhook.delivery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "delivery_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(columnDefinition = "TEXT")
    private String error;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}