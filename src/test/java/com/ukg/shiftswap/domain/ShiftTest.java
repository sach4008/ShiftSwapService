package com.ukg.shiftswap.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShiftTest {

    private final Instant now = Instant.parse("2026-07-09T12:00:00Z");

    @Test
    void constructor_rejectsEndEqualToStart() {
        assertThatThrownBy(() -> new Shift(1L, now, now))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsEndBeforeStart() {
        assertThatThrownBy(() -> new Shift(1L, now, now.minus(1, ChronoUnit.HOURS)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isInFuture_trueWhenStartsStrictlyAfterNow() {
        Shift shift = new Shift(1L, now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS));
        assertThat(shift.isInFuture(now)).isTrue();
    }

    @Test
    void isInFuture_falseAtExactStartInstant() {
        Shift shift = new Shift(1L, now, now.plus(1, ChronoUnit.HOURS));
        assertThat(shift.isInFuture(now)).isFalse();
    }

    @Test
    void isInFuture_falseWhenStartedInThePast() {
        Shift shift = new Shift(1L, now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        assertThat(shift.isInFuture(now)).isFalse();
    }

    @Test
    void hasStarted_isTheComplementOfIsInFuture_atBoundary() {
        Shift shift = new Shift(1L, now, now.plus(1, ChronoUnit.HOURS));
        assertThat(shift.hasStarted(now)).isTrue();
        assertThat(shift.isInFuture(now)).isFalse();
    }

    @Test
    void hasStarted_falseForFutureShift() {
        Shift shift = new Shift(1L, now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS));
        assertThat(shift.hasStarted(now)).isFalse();
    }

    @Test
    void overlapsWith_trueForPartialOverlap() {
        Shift a = new Shift(1L, now, now.plus(2, ChronoUnit.HOURS));
        Shift b = new Shift(2L, now.plus(1, ChronoUnit.HOURS), now.plus(3, ChronoUnit.HOURS));
        assertThat(a.overlapsWith(b)).isTrue();
        assertThat(b.overlapsWith(a)).isTrue();
    }

    @Test
    void overlapsWith_trueWhenOneShiftContainsTheOther() {
        Shift outer = new Shift(1L, now, now.plus(4, ChronoUnit.HOURS));
        Shift inner = new Shift(2L, now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS));
        assertThat(outer.overlapsWith(inner)).isTrue();
    }

    @Test
    void overlapsWith_falseForBackToBackShifts() {
        Shift a = new Shift(1L, now, now.plus(2, ChronoUnit.HOURS));
        Shift b = new Shift(2L, now.plus(2, ChronoUnit.HOURS), now.plus(4, ChronoUnit.HOURS));
        assertThat(a.overlapsWith(b)).isFalse();
        assertThat(b.overlapsWith(a)).isFalse();
    }

    @Test
    void overlapsWith_falseForDisjointShifts() {
        Shift a = new Shift(1L, now, now.plus(1, ChronoUnit.HOURS));
        Shift b = new Shift(2L, now.plus(5, ChronoUnit.HOURS), now.plus(6, ChronoUnit.HOURS));
        assertThat(a.overlapsWith(b)).isFalse();
    }

    @Test
    void reassignOwner_changesEmployeeId() {
        Shift shift = new Shift(1L, now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS));
        shift.reassignOwner(42L);
        assertThat(shift.getEmployeeId()).isEqualTo(42L);
    }
}
