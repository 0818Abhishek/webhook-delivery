package com.webhook.delivery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "endpoints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Endpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 255)
    private String secret; // HMAC signing secret

    @Column(name = "subscribed_event_types", columnDefinition = "TEXT[]")
    private List<String> subscribedEventTypes; // e.g., ["invoice.paid", "user.created"]

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, DISABLED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}