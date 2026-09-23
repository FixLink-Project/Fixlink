package com.fixlink.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_logs")
public class AuditLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "entity_name", length = 50, nullable = false)
    private String entityName;

    @Column(name = "entity_id", length = 50, nullable = false)
    private String entityId;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuditLogJpaEntity(Long userId, String action, String entityName, String entityId, String oldValues, String newValues) {
        this.userId = userId;
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
