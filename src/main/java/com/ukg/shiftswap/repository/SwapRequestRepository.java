package com.ukg.shiftswap.repository;

import com.ukg.shiftswap.domain.SwapRequest;
import com.ukg.shiftswap.domain.SwapRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SwapRequestRepository extends JpaRepository<SwapRequest, UUID> {

    boolean existsByStatusAndRequesterShiftId(SwapRequestStatus status, Long requesterShiftId);

    boolean existsByStatusAndTargetShiftId(SwapRequestStatus status, Long targetShiftId);

    @Query("SELECT sr FROM SwapRequest sr WHERE (sr.requesterId = :employeeId OR sr.targetEmployeeId = :employeeId) "
            + "AND (:status IS NULL OR sr.status = :status) ORDER BY sr.createdAt DESC")
    List<SwapRequest> findMine(@Param("employeeId") Long employeeId, @Param("status") SwapRequestStatus status);

    @Query("SELECT sr FROM SwapRequest sr, Employee e WHERE sr.requesterId = e.id AND e.managerId = :managerId "
            + "AND (:status IS NULL OR sr.status = :status) "
            + "AND (:employeeId IS NULL OR sr.requesterId = :employeeId) "
            + "ORDER BY sr.createdAt DESC")
    List<SwapRequest> findManaged(@Param("managerId") Long managerId,
                                   @Param("status") SwapRequestStatus status,
                                   @Param("employeeId") Long employeeId);
}
