package com.fixlink.domain.model;

import java.time.LocalDateTime;

/**
 * Domain model thuần (POJO) của một mốc chuyển trạng thái yêu cầu sửa chữa.
 * Không phụ thuộc Spring/JPA/Lombok.
 */
public class WorkProgress {
    private Long id;
    private Long requestId;
    private RequestStatus fromStatus;
    private RequestStatus toStatus;
    private String note;
    private LocalDateTime createdAt;
    private Long createdBy;

    public WorkProgress() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public RequestStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(RequestStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public RequestStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(RequestStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    /** Builder viết tay thay cho Lombok @Builder. */
    public static final class Builder {
        private final WorkProgress instance = new WorkProgress();

        public Builder id(Long id) {
            instance.id = id;
            return this;
        }

        public Builder requestId(Long requestId) {
            instance.requestId = requestId;
            return this;
        }

        public Builder fromStatus(RequestStatus fromStatus) {
            instance.fromStatus = fromStatus;
            return this;
        }

        public Builder toStatus(RequestStatus toStatus) {
            instance.toStatus = toStatus;
            return this;
        }

        public Builder note(String note) {
            instance.note = note;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            instance.createdAt = createdAt;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            instance.createdBy = createdBy;
            return this;
        }

        public WorkProgress build() {
            return instance;
        }
    }
}
