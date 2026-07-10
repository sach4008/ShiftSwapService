package com.ukg.shiftswap.web;

import com.ukg.shiftswap.domain.SwapRequest;
import com.ukg.shiftswap.domain.SwapRequestStatus;
import com.ukg.shiftswap.service.CreateSwapRequestCommand;
import com.ukg.shiftswap.service.SwapRequestService;
import com.ukg.shiftswap.service.SwapRequestView;
import com.ukg.shiftswap.web.dto.CreateSwapRequestRequest;
import com.ukg.shiftswap.web.dto.ResolveRequestBody;
import com.ukg.shiftswap.web.dto.SwapRequestResponse;
import com.ukg.shiftswap.web.security.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/swap-requests")
public class SwapRequestController {

    private final SwapRequestService swapRequestService;

    public SwapRequestController(SwapRequestService swapRequestService) {
        this.swapRequestService = swapRequestService;
    }

    @PostMapping
    public ResponseEntity<SwapRequestResponse> create(@CurrentUserId Long callerId,
                                                        @Valid @RequestBody CreateSwapRequestRequest body) {
        SwapRequest created = swapRequestService.create(callerId,
                new CreateSwapRequestCommand(body.requesterShiftId(), body.targetShiftId(), body.reason()));
        URI location = URI.create("/api/v1/swap-requests/" + created.getId());
        return ResponseEntity.created(location).body(SwapRequestResponse.of(created, created.getStatus()));
    }

    @GetMapping("/{id}")
    public SwapRequestResponse get(@CurrentUserId Long callerId, @PathVariable UUID id) {
        SwapRequestView view = swapRequestService.get(callerId, id);
        return SwapRequestResponse.of(view.request(), view.effectiveStatus());
    }

    @GetMapping
    public List<SwapRequestResponse> list(@CurrentUserId Long callerId,
                                           @RequestParam(required = false) String scope,
                                           @RequestParam(required = false) SwapRequestStatus status,
                                           @RequestParam(required = false) Long employeeId) {
        return swapRequestService.list(callerId, scope, status, employeeId).stream()
                .map(view -> SwapRequestResponse.of(view.request(), view.effectiveStatus()))
                .toList();
    }

    @PostMapping("/{id}/approve")
    public SwapRequestResponse approve(@CurrentUserId Long callerId, @PathVariable UUID id,
                                        @Valid @RequestBody(required = false) ResolveRequestBody body) {
        SwapRequest resolved = swapRequestService.approve(callerId, id, resolutionNote(body));
        return SwapRequestResponse.of(resolved, resolved.getStatus());
    }

    @PostMapping("/{id}/reject")
    public SwapRequestResponse reject(@CurrentUserId Long callerId, @PathVariable UUID id,
                                       @Valid @RequestBody(required = false) ResolveRequestBody body) {
        SwapRequest resolved = swapRequestService.reject(callerId, id, resolutionNote(body));
        return SwapRequestResponse.of(resolved, resolved.getStatus());
    }

    @PostMapping("/{id}/cancel")
    public SwapRequestResponse cancel(@CurrentUserId Long callerId, @PathVariable UUID id) {
        SwapRequest resolved = swapRequestService.cancel(callerId, id);
        return SwapRequestResponse.of(resolved, resolved.getStatus());
    }

    private String resolutionNote(ResolveRequestBody body) {
        return body == null ? null : body.resolutionNote();
    }
}
