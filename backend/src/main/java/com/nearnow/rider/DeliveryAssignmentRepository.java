package com.nearnow.rider;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {

    Optional<DeliveryAssignment> findByOrderId(Long orderId);

    List<DeliveryAssignment> findByRiderIdOrderByAssignedAtDesc(Long riderId);

    Optional<DeliveryAssignment> findByIdAndRiderUserId(Long assignmentId, Long userId);
}
