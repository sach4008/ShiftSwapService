package com.ukg.shiftswap.domain;

import com.ukg.shiftswap.domain.exception.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "swap_request")
public class SwapRequest {

    @Id
    private UUID id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "requester_shift_id", nullable = false)
    private Long requesterShiftId;

    @Column(name = "target_shift_id", nullable = false)
    private Long targetShiftId;

    @Column(name = "target_employee_id", nullable = false)
    private Long targetEmployeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SwapRequestStatus status;

    @Column(length = 500)
    private String reason;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Version
    private Long version;

    protected SwapRequest() {
        // JPA
    }

    public SwapRequest(Long requesterId, Long requesterShiftId, Long targetShiftId,
                        Long targetEmployeeId, String reason, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.requesterId = requesterId;
        this.requesterShiftId = requesterShiftId;
        this.targetShiftId = targetShiftId;
        this.targetEmployeeId = targetEmployeeId;
        this.reason = reason;
        this.status = SwapRequestStatus.PENDING;
        this.createdAt = createdAt;
    }

    public void approve(Long resolverId, String resolutionNote, Instant now) {
        guardPending();
        this.status = SwapRequestStatus.APPROVED;
        this.resolutionNote = resolutionNote;
        resolve(resolverId, now);
    }

    public void reject(Long resolverId, String resolutionNote, Instant now) {
        guardPending();
        this.status = SwapRequestStatus.REJECTED;
        this.resolutionNote = resolutionNote;
        resolve(resolverId, now);
    }

    public void cancel(Long requesterId, Instant now) {
        guardPending();
        this.status = SwapRequestStatus.CANCELLED;
        resolve(requesterId, now);
    }

    /** System-triggered; no actor (DESIGN.md §4.2: "null actor on temporal expiry"). */
    public void expire(Instant now) {
        guardPending();
        this.status = SwapRequestStatus.EXPIRED;
        resolve(null, now);
    }

    private void resolve(Long resolvedBy, Instant now) {
        this.resolvedAt = now;
        this.resolvedBy = resolvedBy;
    }

    private void guardPending() {
        if (status != SwapRequestStatus.PENDING) {
            throw new InvalidStateTransitionException(id, status, resolvedBy);
        }
    }

    public UUID getId() {
        return id;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public Long getRequesterShiftId() {
        return requesterShiftId;
    }

    public Long getTargetShiftId() {
        return targetShiftId;
    }

    public Long getTargetEmployeeId() {
        return targetEmployeeId;
    }

    public SwapRequestStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public Long getVersion() {
        return version;
    }
}
