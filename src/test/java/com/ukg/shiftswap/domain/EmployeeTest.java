package com.ukg.shiftswap.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeTest {

    private final Employee manager = new Employee(1L, "Alice", "alice@example.com", "Manager", null);
    private final Employee reportA = new Employee(2L, "Bob", "bob@example.com", "Associate", 1L);
    private final Employee reportB = new Employee(3L, "Carol", "carol@example.com", "Associate", 1L);
    private final Employee otherManagerReport = new Employee(4L, "Dave", "dave@example.com", "Associate", 99L);
    private final Employee topOfTree = new Employee(5L, "Eve", "eve@example.com", "VP", null);

    @Test
    void isManagerOf_trueForDirectReport() {
        assertThat(manager.isManagerOf(reportA)).isTrue();
    }

    @Test
    void isManagerOf_falseForNonReport() {
        assertThat(manager.isManagerOf(otherManagerReport)).isFalse();
    }

    @Test
    void isManagerOf_falseWhenOtherHasNoManager() {
        assertThat(manager.isManagerOf(topOfTree)).isFalse();
    }

    @Test
    void sharesManagerWith_trueForSiblings() {
        assertThat(reportA.sharesManagerWith(reportB)).isTrue();
    }

    @Test
    void sharesManagerWith_falseForDifferentManagers() {
        assertThat(reportA.sharesManagerWith(otherManagerReport)).isFalse();
    }

    @Test
    void sharesManagerWith_falseForTopOfTreeEmployee() {
        assertThat(topOfTree.sharesManagerWith(reportA)).isFalse();
    }

    @Test
    void sharesManagerWith_falseWhenBothAreTopOfTree() {
        Employee anotherTopOfTree = new Employee(6L, "Frank", "frank@example.com", "VP", null);
        assertThat(topOfTree.sharesManagerWith(anotherTopOfTree)).isFalse();
    }
}
