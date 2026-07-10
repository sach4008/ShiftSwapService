package com.ukg.shiftswap.service;

import com.ukg.shiftswap.domain.Employee;
import com.ukg.shiftswap.domain.Shift;
import com.ukg.shiftswap.domain.exception.NotFoundException;
import com.ukg.shiftswap.domain.exception.ValidationException;
import com.ukg.shiftswap.repository.EmployeeRepository;
import com.ukg.shiftswap.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read-only discovery endpoints */
@Service
public class DirectoryService {

    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;

    public DirectoryService(EmployeeRepository employeeRepository, ShiftRepository shiftRepository) {
        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
    }

    @Transactional(readOnly = true)
    public List<Employee> listEmployees(Long callerId, String scope) {
        Employee caller = getEmployee(callerId);
        return switch (scope) {
            case "reports" -> employeeRepository.findByManagerId(caller.getId());
            case "colleagues" -> {
                if (caller.getManagerId() == null) {
                    yield List.of();
                }
                yield employeeRepository.findByManagerId(caller.getManagerId()).stream()
                        .filter(e -> !e.getId().equals(caller.getId()))
                        .toList();
            }
            default -> throw new ValidationException("Unknown scope: " + scope);
        };
    }

    @Transactional(readOnly = true)
    public List<Shift> listShifts(Long callerId, Long employeeId) {
        Employee caller = getEmployee(callerId);
        Employee target = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee " + employeeId + " not found"));

        boolean isSelf = caller.getId().equals(target.getId());
        boolean isSameManagerPeer = caller.sharesManagerWith(target);
        boolean isTargetsManager = caller.isManagerOf(target);
        if (!isSelf && !isSameManagerPeer && !isTargetsManager) {
            throw new NotFoundException("Employee " + employeeId + " not found");
        }

        return shiftRepository.findByEmployeeId(employeeId);
    }

    private Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee " + id + " not found"));
    }
}
