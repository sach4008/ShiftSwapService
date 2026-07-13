package com.ukg.shiftswap.web;

import com.ukg.shiftswap.domain.Employee;
import com.ukg.shiftswap.domain.Shift;
import com.ukg.shiftswap.domain.SwapRequest;
import com.ukg.shiftswap.domain.SwapRequestStatus;
import com.ukg.shiftswap.domain.exception.InvalidStateTransitionException;
import com.ukg.shiftswap.domain.exception.RequestExpiredException;
import com.ukg.shiftswap.repository.EmployeeRepository;
import com.ukg.shiftswap.repository.ShiftRepository;
import com.ukg.shiftswap.repository.SwapRequestRepository;
import com.ukg.shiftswap.service.CreateSwapRequestCommand;
import com.ukg.shiftswap.service.SwapRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SwapRequestConcurrencyTest {

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private SwapRequestRepository swapRequestRepository;
    @Autowired
    private SwapRequestService swapRequestService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void expiryOnTransition_persistsDespiteTheCallReporting409() throws Exception {
        Employee manager = employeeRepository.save(new Employee("Manager", "expiry-mgr@example.com", "Manager", null));
        Employee requester = employeeRepository.save(new Employee("Requester", "expiry-req@example.com", "Associate", manager.getId()));
        Employee target = employeeRepository.save(new Employee("Target", "expiry-tgt@example.com", "Associate", manager.getId()));

        Instant now = Instant.now();
        Shift requesterShift = shiftRepository.save(future(requester.getId(), now, 2));
        Shift targetShift = shiftRepository.save(future(target.getId(), now, 3));

        SwapRequest request = swapRequestService.create(requester.getId(),
                new CreateSwapRequestCommand(requesterShift.getId(), targetShift.getId(), "note"));

        jdbcTemplate.update("UPDATE shift SET starts_at = ? WHERE id = ?",
                now.minus(1, ChronoUnit.HOURS), requesterShift.getId());

        try {
            swapRequestService.approve(manager.getId(), request.getId(), "ok");
            org.junit.jupiter.api.Assertions.fail("expected RequestExpiredException");
        } catch (RequestExpiredException expected) {
            // expected: the call reports the conflict...
        }

        SwapRequest persisted = swapRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(SwapRequestStatus.EXPIRED);
        assertThat(persisted.getResolvedBy()).isNull();
        assertThat(persisted.getResolvedAt()).isNotNull();
    }

    @Test
    void raceA_parallelApproveApprove_exactlyOneSucceeds() throws Exception {
        Employee manager = employeeRepository.save(new Employee("Manager", "racea-mgr@example.com", "Manager", null));
        Employee requester = employeeRepository.save(new Employee("Requester", "racea-req@example.com", "Associate", manager.getId()));
        Employee target = employeeRepository.save(new Employee("Target", "racea-tgt@example.com", "Associate", manager.getId()));

        Instant now = Instant.now();
        Shift requesterShift = shiftRepository.save(future(requester.getId(), now, 2));
        Shift targetShift = shiftRepository.save(future(target.getId(), now, 3));

        SwapRequest request = swapRequestService.create(requester.getId(),
                new CreateSwapRequestCommand(requesterShift.getId(), targetShift.getId(), "note"));

        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<String> attempt = () -> attemptApprove(request.getId(), manager.getId(), barrier);
        List<String> results = runConcurrently(List.of(attempt, attempt)).stream()
                .map(SwapRequestConcurrencyTest::resultOf)
                .toList();

        long successCount = results.stream().filter(r -> r.equals("OK")).count();
        long rejectedCount = results.stream()
                .filter(r -> r.equals("CONFLICT") || r.equals("ALREADY_DECIDED"))
                .count();
        assertThat(successCount).as("outcomes: %s", results).isEqualTo(1);
        assertThat(rejectedCount).as("outcomes: %s", results).isEqualTo(1);

        SwapRequest resolved = swapRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(SwapRequestStatus.APPROVED);

        Shift refreshedRequesterShift = shiftRepository.findById(requesterShift.getId()).orElseThrow();
        Shift refreshedTargetShift = shiftRepository.findById(targetShift.getId()).orElseThrow();
        assertThat(refreshedRequesterShift.getEmployeeId()).isEqualTo(target.getId());
        assertThat(refreshedTargetShift.getEmployeeId()).isEqualTo(requester.getId());
    }

    @Test
    void raceB_twoPendingRequestsSharingAShift_neverProduceADoubleSwap() throws Exception {

        Employee manager = employeeRepository.save(new Employee("Manager", "raceb-mgr@example.com", "Manager", null));
        Employee employeeA = employeeRepository.save(new Employee("A", "raceb-a@example.com", "Associate", manager.getId()));
        Employee employeeB = employeeRepository.save(new Employee("B", "raceb-b@example.com", "Associate", manager.getId()));
        Employee employeeC = employeeRepository.save(new Employee("C", "raceb-c@example.com", "Associate", manager.getId()));

        Instant now = Instant.now();
        Shift shiftA = shiftRepository.save(future(employeeA.getId(), now, 2));
        Shift shiftB = shiftRepository.save(future(employeeB.getId(), now, 3));
        Shift shiftC = shiftRepository.save(future(employeeC.getId(), now, 4));

        SwapRequest requestAB = swapRequestRepository.save(
                new SwapRequest(employeeA.getId(), shiftA.getId(), shiftB.getId(), employeeB.getId(), "A<->B", now));
        SwapRequest requestAC = swapRequestRepository.save(
                new SwapRequest(employeeA.getId(), shiftA.getId(), shiftC.getId(), employeeC.getId(), "A<->C", now));

        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Future<String>> outcomes = runConcurrently(List.of(
                () -> attemptApprove(requestAB.getId(), manager.getId(), barrier),
                () -> attemptApprove(requestAC.getId(), manager.getId(), barrier)
        ));
        outcomes.forEach(SwapRequestConcurrencyTest::resultOf); // wait for both to finish

        long approvedCount =
                (swapRequestRepository.findById(requestAB.getId()).orElseThrow().getStatus() == SwapRequestStatus.APPROVED ? 1 : 0)
                        + (swapRequestRepository.findById(requestAC.getId()).orElseThrow().getStatus() == SwapRequestStatus.APPROVED ? 1 : 0);
        assertThat(approvedCount).isEqualTo(1);

        // Shift A must end up with exactly one consistent owner — never lost, never duplicated.
        Shift refreshedShiftA = shiftRepository.findById(shiftA.getId()).orElseThrow();
        assertThat(refreshedShiftA.getEmployeeId()).isIn(employeeB.getId(), employeeC.getId());
    }

    private Shift future(Long employeeId, Instant now, int daysAhead) {
        Instant start = now.plus(daysAhead, ChronoUnit.DAYS);
        return new Shift(employeeId, start, start.plus(8, ChronoUnit.HOURS));
    }

    private String attemptApprove(UUID requestId, Long managerId, CyclicBarrier barrier) {
        try {
            barrier.await();
            swapRequestService.approve(managerId, requestId, "Coverage confirmed");
            return "OK";
        } catch (ConcurrencyFailureException e) {
            return "CONFLICT";
        } catch (InvalidStateTransitionException e) {
            return "ALREADY_DECIDED";
        } catch (RequestExpiredException e) {
            return "EXPIRED";
        } catch (Exception e) {
            return "OTHER:" + e.getClass().getSimpleName();
        }
    }

    private List<Future<String>> runConcurrently(List<Callable<String>> tasks) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        try {
            return pool.invokeAll(tasks);
        } finally {
            pool.shutdown();
        }
    }

    private static String resultOf(Future<String> future) {
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
