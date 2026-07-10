package com.ukg.shiftswap.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ukg.shiftswap.domain.Employee;
import com.ukg.shiftswap.domain.Shift;
import com.ukg.shiftswap.repository.EmployeeRepository;
import com.ukg.shiftswap.repository.ShiftRepository;
import com.ukg.shiftswap.repository.SwapRequestRepository;
import com.ukg.shiftswap.web.dto.CreateSwapRequestRequest;
import com.ukg.shiftswap.web.dto.ResolveRequestBody;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class AbstractApiTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected EmployeeRepository employeeRepository;
    @Autowired
    protected ShiftRepository shiftRepository;
    @Autowired
    protected SwapRequestRepository swapRequestRepository;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @PersistenceContext
    protected EntityManager entityManager;

    protected Employee manager;
    protected Employee otherManager;
    protected Employee requester;
    protected Employee target;
    protected Employee thirdReport;
    protected Employee outsider;

    protected Shift requesterShift;
    protected Shift targetShift;
    protected Shift thirdShift;
    protected Shift outsiderShift;

    @BeforeEach
    void seedOrg() {
        manager = employeeRepository.save(new Employee("Manager One", "m1@example.com", "Manager", null));
        otherManager = employeeRepository.save(new Employee("Manager Two", "m2@example.com", "Manager", null));

        requester = employeeRepository.save(new Employee("Requester", "requester@example.com", "Associate", manager.getId()));
        target = employeeRepository.save(new Employee("Target", "target@example.com", "Associate", manager.getId()));
        thirdReport = employeeRepository.save(new Employee("Third", "third@example.com", "Associate", manager.getId()));
        outsider = employeeRepository.save(new Employee("Outsider", "outsider@example.com", "Associate", otherManager.getId()));

        Instant now = Instant.now();
        requesterShift = shiftRepository.save(future(requester.getId(), now, 2));
        targetShift = shiftRepository.save(future(target.getId(), now, 3));
        thirdShift = shiftRepository.save(future(thirdReport.getId(), now, 4));
        outsiderShift = shiftRepository.save(future(outsider.getId(), now, 2));
    }

    private Shift future(Long employeeId, Instant now, int daysAhead) {
        Instant start = now.plus(daysAhead, ChronoUnit.DAYS);
        return new Shift(employeeId, start, start.plus(8, ChronoUnit.HOURS));
    }

    protected void backdateShiftStart(Long shiftId, Instant startsAt) {
        entityManager.flush();
        jdbcTemplate.update("UPDATE shift SET starts_at = ? WHERE id = ?", startsAt, shiftId);
        entityManager.clear();
    }

    protected ResultActions createRequest(Long callerId, Long requesterShiftId, Long targetShiftId, String reason) throws Exception {
        CreateSwapRequestRequest body = new CreateSwapRequestRequest(requesterShiftId, targetShiftId, reason);
        return mockMvc.perform(post("/api/v1/swap-requests")
                .header("X-User-Id", callerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions decide(Long callerId, String action, String requestId, String resolutionNote) throws Exception {
        ResolveRequestBody body = new ResolveRequestBody(resolutionNote);
        return mockMvc.perform(post("/api/v1/swap-requests/{id}/{action}", requestId, action)
                .header("X-User-Id", callerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions getRequest(Long callerId, String requestId) throws Exception {
        return mockMvc.perform(get("/api/v1/swap-requests/{id}", requestId)
                .header("X-User-Id", callerId));
    }
}
