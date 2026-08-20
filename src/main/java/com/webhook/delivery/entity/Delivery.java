package com.webhook.delivery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private Endpoint endpoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PROCESSING, SUCCESS, FAILED, DEAD_LETTERED

    @Column(name = "attempt_count")
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "next_attempt_at")
    @Builder.Default
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_response_code")
    private Integer lastResponseCode;

    @Column(name = "last_response_snippet")
    private String lastResponseSnippet;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DeliveryAttempt> attempts = new ArrayList<>();
}