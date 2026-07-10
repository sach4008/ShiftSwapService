package com.ukg.shiftswap.web;

import com.ukg.shiftswap.service.DirectoryService;
import com.ukg.shiftswap.web.dto.EmployeeSummaryResponse;
import com.ukg.shiftswap.web.dto.ShiftResponse;
import com.ukg.shiftswap.web.security.CurrentUserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DirectoryController {

    private final DirectoryService directoryService;

    public DirectoryController(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping("/employees")
    public List<EmployeeSummaryResponse> employees(@CurrentUserId Long callerId, @RequestParam String scope) {
        return directoryService.listEmployees(callerId, scope).stream()
                .map(EmployeeSummaryResponse::from)
                .toList();
    }

    @GetMapping("/employees/{id}/shifts")
    public List<ShiftResponse> shifts(@CurrentUserId Long callerId, @PathVariable Long id) {
        return directoryService.listShifts(callerId, id).stream()
                .map(ShiftResponse::from)
                .toList();
    }
}
