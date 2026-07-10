package com.ukg.shiftswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;

import java.util.Objects;

@Entity
@Table(name = "employee")
@Check(constraints = "manager_id IS NULL OR manager_id <> id")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String title;

    @Column(name = "manager_id")
    private Long managerId;

    protected Employee() {
        // JPA
    }

    public Employee(String name, String email, String title, Long managerId) {
        this.name = name;
        this.email = email;
        this.title = title;
        this.managerId = managerId;
    }

    public Employee(Long id, String name, String email, String title, Long managerId) {
        this(name, email, title, managerId);
        this.id = id;
    }

    /** True iff {@code other} currently reports directly to this employee. */
    public boolean isManagerOf(Employee other) {
        Objects.requireNonNull(other, "other");
        return this.id != null && this.id.equals(other.managerId);
    }

    /** True iff both employees currently report to the same non-null manager. */
    public boolean sharesManagerWith(Employee other) {
        Objects.requireNonNull(other, "other");
        return this.managerId != null && this.managerId.equals(other.managerId);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getTitle() {
        return title;
    }

    public Long getManagerId() {
        return managerId;
    }
}
