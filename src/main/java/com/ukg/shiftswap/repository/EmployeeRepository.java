package com.ukg.shiftswap.repository;

import com.ukg.shiftswap.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByManagerId(Long managerId);
}
