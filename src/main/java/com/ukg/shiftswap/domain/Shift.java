package com.ukg.shiftswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "shift")
@Check(constraints = "ends_at > starts_at")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Version
    private Long version;

    protected Shift() {
        // JPA
    }

    public Shift(Long employeeId, Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
        this.employeeId = employeeId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public Shift(Long id, Long employeeId, Instant startsAt, Instant endsAt) {
        this(employeeId, startsAt, endsAt);
        this.id = id;
    }

    public boolean isInFuture(Instant now) {
        return startsAt.isAfter(now);
    }

    public boolean hasStarted(Instant now) {
        return !startsAt.isAfter(now);
    }

    public boolean overlapsWith(Shift other) {
        Objects.requireNonNull(other, "other");
        return this.startsAt.isBefore(other.endsAt) && other.startsAt.isBefore(this.endsAt);
    }

    public void reassignOwner(Long newEmployeeId) {
        this.employeeId = newEmployeeId;
    }

    public Long getId() {
        return id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public Long getVersion() {
        return version;
    }
}
