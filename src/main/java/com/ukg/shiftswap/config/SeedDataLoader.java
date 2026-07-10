package com.ukg.shiftswap.config;

import com.ukg.shiftswap.domain.Employee;
import com.ukg.shiftswap.domain.Shift;
import com.ukg.shiftswap.repository.EmployeeRepository;
import com.ukg.shiftswap.repository.ShiftRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Seeds a small org so the API is immediately demoable; not loaded for the "test" profile. */
@Component
@Profile({"local", "docker"})
public class SeedDataLoader implements ApplicationRunner {

    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    private final Clock clock;

    public SeedDataLoader(EmployeeRepository employeeRepository, ShiftRepository shiftRepository, Clock clock) {
        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (employeeRepository.count() > 0) {
            return;
        }

        Employee vp = employeeRepository.save(new Employee("Alice VP", "alice@example.com", "VP Operations", null));
        Employee manager = employeeRepository.save(
                new Employee("Priya Manager", "priya@example.com", "Shift Manager", vp.getId()));
        Employee bob = employeeRepository.save(
                new Employee("Bob Associate", "bob@example.com", "Associate", manager.getId()));
        Employee carol = employeeRepository.save(
                new Employee("Carol Associate", "carol@example.com", "Associate", manager.getId()));
        Employee dave = employeeRepository.save(
                new Employee("Dave Associate", "dave@example.com", "Associate", manager.getId()));

        Instant tomorrow9am = clock.instant().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

        shiftRepository.save(new Shift(bob.getId(), tomorrow9am, tomorrow9am.plus(8, ChronoUnit.HOURS)));
        shiftRepository.save(new Shift(carol.getId(),
                tomorrow9am.plus(1, ChronoUnit.DAYS), tomorrow9am.plus(1, ChronoUnit.DAYS).plus(8, ChronoUnit.HOURS)));
        shiftRepository.save(new Shift(dave.getId(),
                tomorrow9am.plus(2, ChronoUnit.DAYS), tomorrow9am.plus(2, ChronoUnit.DAYS).plus(8, ChronoUnit.HOURS)));
    }
}
