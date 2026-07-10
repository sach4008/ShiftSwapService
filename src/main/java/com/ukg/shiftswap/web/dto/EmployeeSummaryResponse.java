package com.ukg.shiftswap.web.dto;

import com.ukg.shiftswap.domain.Employee;

/** No email here — email is PII, returned only to the employee themself. */
public record EmployeeSummaryResponse(Long id, String name, String title) {

    public static EmployeeSummaryResponse from(Employee employee) {
        return new EmployeeSummaryResponse(employee.getId(), employee.getName(), employee.getTitle());
    }
}
