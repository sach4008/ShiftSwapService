package com.ukg.shiftswap.service;

import com.ukg.shiftswap.domain.Employee;
import com.ukg.shiftswap.domain.Shift;
import com.ukg.shiftswap.domain.SwapRequest;
import com.ukg.shiftswap.domain.SwapRequestStatus;
import com.ukg.shiftswap.domain.exception.ForbiddenException;
import com.ukg.shiftswap.domain.exception.NotFoundException;
import com.ukg.shiftswap.domain.exception.RequestExpiredException;
import com.ukg.shiftswap.domain.exception.ShiftAlreadyInRequestException;
import com.ukg.shiftswap.domain.exception.SwapRuleViolation;
import com.ukg.shiftswap.domain.exception.SwapRuleViolationException;
import com.ukg.shiftswap.domain.exception.ValidationException;
import com.ukg.shiftswap.repository.EmployeeRepository;
import com.ukg.shiftswap.repository.ShiftRepository;
import com.ukg.shiftswap.repository.SwapRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SwapRequestService {

    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    private final SwapRequestRepository swapRequestRepository;
    private final Clock clock;
    private final SwapMetrics metrics;

    public SwapRequestService(EmployeeRepository employeeRepository,
                               ShiftRepository shiftRepository,
                               SwapRequestRepository swapRequestRepository,
                               Clock clock,
                               SwapMetrics metrics) {
        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
        this.swapRequestRepository = swapRequestRepository;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public SwapRequest create(Long requesterId, CreateSwapRequestCommand command) {
        if (command.requesterShiftId().equals(command.targetShiftId())) {
            throw new ValidationException("requesterShiftId and targetShiftId must be different");
        }
        if (command.reason() != null && command.reason().length() > 500) {
            throw new ValidationException("reason must be 500 characters or fewer");
        }

        Employee requester = getEmployee(requesterId);

        Shift requesterShift = shiftRepository.findById(command.requesterShiftId())
                .orElseThrow(() -> new NotFoundException("Shift " + command.requesterShiftId() + " not found"));
        if (!requesterShift.getEmployeeId().equals(requesterId)) {
            throw new SwapRuleViolationException(SwapRuleViolation.SHIFT_NOT_OWNED,
                    "Requester does not own shift " + requesterShift.getId());
        }

        // A non-existent target shift is indistinguishable from one outside the caller's scope,
        // so both collapse into the same generic 422 — create can't be used to probe shift IDs.
        Shift targetShift = shiftRepository.findById(command.targetShiftId())
                .orElseThrow(() -> new SwapRuleViolationException(SwapRuleViolation.INVALID_REFERENCE,
                        "Target shift is not a valid swap counterpart"));
        Employee target = employeeRepository.findById(targetShift.getEmployeeId())
                .orElseThrow(() -> new SwapRuleViolationException(SwapRuleViolation.INVALID_REFERENCE,
                        "Target shift is not a valid swap counterpart"));

        if (target.getId().equals(requester.getId())) {
            throw new SwapRuleViolationException(SwapRuleViolation.SELF_SWAP_NOT_ALLOWED,
                    "Cannot request a swap with your own shift");
        }

        Instant now = clock.instant();
        if (!requesterShift.isInFuture(now) || !targetShift.isInFuture(now)) {
            throw new SwapRuleViolationException(SwapRuleViolation.SHIFT_IN_PAST,
                    "Both shifts must start in the future");
        }

        if (!requester.sharesManagerWith(target)) {
            throw new SwapRuleViolationException(SwapRuleViolation.DIFFERENT_MANAGERS,
                    "Requester and target must report to the same manager");
        }

        assertNoOverlap(requester, requesterShift, target, targetShift);

        assertShiftNotActive(requesterShift.getId());
        assertShiftNotActive(targetShift.getId());

        SwapRequest request = new SwapRequest(requesterId, requesterShift.getId(), targetShift.getId(),
                target.getId(), command.reason(), now);
        SwapRequest saved = swapRequestRepository.save(request);
        metrics.recordTransition(saved, null, "CREATE", requesterId);
        return saved;
    }

    // RequestExpiredException is thrown only after the EXPIRED transition has already been saved
    @Transactional(noRollbackFor = RequestExpiredException.class)
    public SwapRequest approve(Long callerId, UUID requestId, String resolutionNote) {
        SwapRequest request = getRequest(requestId);
        Shift requesterShift = getShift(request.getRequesterShiftId());
        Shift targetShift = getShift(request.getTargetShiftId());
        Instant now = clock.instant();

        expireIfShiftStarted(request, requesterShift, targetShift, now);

        Employee caller = getEmployee(callerId);
        Employee requester = getEmployee(request.getRequesterId());
        Employee target = getEmployee(request.getTargetEmployeeId());
        assertResolverAuthority(callerId, request, caller, requester, target);

        if (!requesterShift.getEmployeeId().equals(request.getRequesterId())
                || !targetShift.getEmployeeId().equals(request.getTargetEmployeeId())) {
            request.expire(now);
            swapRequestRepository.save(request);
            metrics.recordTransition(request, SwapRequestStatus.PENDING, "EXPIRE", null);
            throw new RequestExpiredException(request.getId());
        }

        assertNoOverlap(requester, requesterShift, target, targetShift);

        requesterShift.reassignOwner(target.getId());
        targetShift.reassignOwner(requester.getId());
        request.approve(callerId, resolutionNote, now);

        shiftRepository.save(requesterShift);
        shiftRepository.save(targetShift);
        SwapRequest saved = swapRequestRepository.save(request);
        metrics.recordTransition(saved, SwapRequestStatus.PENDING, "APPROVE", callerId);
        return saved;
    }

    @Transactional(noRollbackFor = RequestExpiredException.class)
    public SwapRequest reject(Long callerId, UUID requestId, String resolutionNote) {
        SwapRequest request = getRequest(requestId);
        Shift requesterShift = getShift(request.getRequesterShiftId());
        Shift targetShift = getShift(request.getTargetShiftId());
        Instant now = clock.instant();

        expireIfShiftStarted(request, requesterShift, targetShift, now);

        Employee caller = getEmployee(callerId);
        Employee requester = getEmployee(request.getRequesterId());
        Employee target = getEmployee(request.getTargetEmployeeId());
        assertResolverAuthority(callerId, request, caller, requester, target);

        request.reject(callerId, resolutionNote, now);
        SwapRequest saved = swapRequestRepository.save(request);
        metrics.recordTransition(saved, SwapRequestStatus.PENDING, "REJECT", callerId);
        return saved;
    }

    @Transactional(noRollbackFor = RequestExpiredException.class)
    public SwapRequest cancel(Long callerId, UUID requestId) {
        SwapRequest request = getRequest(requestId);
        Shift requesterShift = getShift(request.getRequesterShiftId());
        Shift targetShift = getShift(request.getTargetShiftId());
        Instant now = clock.instant();

        expireIfShiftStarted(request, requesterShift, targetShift, now);

        if (!request.getRequesterId().equals(callerId)) {
            throw new ForbiddenException("Only the requester may cancel this request");
        }

        request.cancel(callerId, now);
        SwapRequest saved = swapRequestRepository.save(request);
        metrics.recordTransition(saved, SwapRequestStatus.PENDING, "CANCEL", callerId);
        return saved;
    }

    @Transactional(readOnly = true)
    public SwapRequestView get(Long callerId, UUID requestId) {
        SwapRequest request = getRequest(requestId);
        Employee caller = getEmployee(callerId);

        boolean isParty = callerId.equals(request.getRequesterId()) || callerId.equals(request.getTargetEmployeeId());
        if (!isParty && !isSharedManager(caller, request)) {
            throw new NotFoundException("Swap request " + requestId + " not found");
        }

        return toView(request, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<SwapRequestView> list(Long callerId, String scope, SwapRequestStatus status, Long employeeId) {
        String effectiveScope = scope == null ? "mine" : scope;
        List<SwapRequest> requests = switch (effectiveScope) {
            case "mine" -> swapRequestRepository.findMine(callerId, status);
            case "to-approve" -> swapRequestRepository.findManaged(callerId, SwapRequestStatus.PENDING, employeeId);
            case "managed" -> swapRequestRepository.findManaged(callerId, status, employeeId);
            default -> throw new ValidationException("Unknown scope: " + scope);
        };

        Instant now = clock.instant();
        return requests.stream()
                .map(r -> toView(r, now))
                .filter(view -> !"to-approve".equals(effectiveScope) || view.effectiveStatus() == SwapRequestStatus.PENDING)
                .toList();
    }

    private void assertResolverAuthority(Long callerId, SwapRequest request, Employee caller, Employee requester, Employee target) {
        if (callerId.equals(request.getRequesterId()) || callerId.equals(request.getTargetEmployeeId())) {
            throw new ForbiddenException("The resolver may not be a party to the swap");
        }
        if (!caller.isManagerOf(requester) || !caller.isManagerOf(target)) {
            throw new ForbiddenException("Only the shared manager may decide this request");
        }
    }

    private void expireIfShiftStarted(SwapRequest request, Shift requesterShift, Shift targetShift, Instant now) {
        if (request.getStatus() == SwapRequestStatus.PENDING
                && (requesterShift.hasStarted(now) || targetShift.hasStarted(now))) {
            request.expire(now);
            swapRequestRepository.save(request);
            metrics.recordTransition(request, SwapRequestStatus.PENDING, "EXPIRE", null);
            throw new RequestExpiredException(request.getId());
        }
    }

    private void assertNoOverlap(Employee requester, Shift requesterShift, Employee target, Shift targetShift) {
        boolean requesterWouldOverlap = shiftRepository.findByEmployeeId(requester.getId()).stream()
                .filter(s -> !s.getId().equals(requesterShift.getId()))
                .anyMatch(s -> s.overlapsWith(targetShift));
        boolean targetWouldOverlap = shiftRepository.findByEmployeeId(target.getId()).stream()
                .filter(s -> !s.getId().equals(targetShift.getId()))
                .anyMatch(s -> s.overlapsWith(requesterShift));
        if (requesterWouldOverlap || targetWouldOverlap) {
            throw new SwapRuleViolationException(SwapRuleViolation.OVERLAP_CONFLICT,
                    "The prospective swap would double-book an employee");
        }
    }

    private void assertShiftNotActive(Long shiftId) {
        if (swapRequestRepository.existsByStatusAndRequesterShiftId(SwapRequestStatus.PENDING, shiftId)
                || swapRequestRepository.existsByStatusAndTargetShiftId(SwapRequestStatus.PENDING, shiftId)) {
            throw new ShiftAlreadyInRequestException(shiftId);
        }
    }

    private boolean isSharedManager(Employee caller, SwapRequest request) {
        Employee requester = getEmployee(request.getRequesterId());
        Employee target = getEmployee(request.getTargetEmployeeId());
        return caller.isManagerOf(requester) && caller.isManagerOf(target);
    }

    private SwapRequestView toView(SwapRequest request, Instant now) {
        Shift requesterShift = getShift(request.getRequesterShiftId());
        Shift targetShift = getShift(request.getTargetShiftId());
        SwapRequestStatus effective = EffectiveStatusCalculator.compute(request, requesterShift, targetShift, now);
        return new SwapRequestView(request, effective);
    }

    private SwapRequest getRequest(UUID requestId) {
        return swapRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Swap request " + requestId + " not found"));
    }

    private Shift getShift(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shift " + id + " not found"));
    }

    private Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee " + id + " not found"));
    }
}
