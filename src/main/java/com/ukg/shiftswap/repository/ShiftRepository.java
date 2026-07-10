package com.ukg.shiftswap.repository;

import com.ukg.shiftswap.domain.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    List<Shift> findByEmployeeId(Long employeeId);
}
