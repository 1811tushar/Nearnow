package com.nearnow.rider;

import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;
import com.nearnow.common.exception.DuplicateResourceException;
import com.nearnow.common.exception.InvalidOperationException;
import com.nearnow.common.exception.ResourceNotFoundException;
import com.nearnow.order.Order;
import com.nearnow.order.OrderRepository;
import com.nearnow.order.OrderStatus;
import com.nearnow.warehouse.PickList;
import com.nearnow.warehouse.PickListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Delivery-partner domain logic.
 *
 * Rider selection is intentionally deterministic and data-driven without ML:
 * find active/available riders, sort by Haversine distance to the warehouse,
 * then pessimistically lock the chosen row before marking it unavailable.
 * This prevents two concurrent dispatch requests from assigning the same
 * rider at the same time.
 */
@Service
public class RiderService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final BigDecimal BASE_PAYOUT = new BigDecimal("30.00");
    private static final BigDecimal PAYOUT_PER_KM = new BigDecimal("8.00");

    private final RiderRepository riderRepository;
    private final DeliveryAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PickListRepository pickListRepository;

    public RiderService(RiderRepository riderRepository,
                        DeliveryAssignmentRepository assignmentRepository,
                        UserRepository userRepository,
                        OrderRepository orderRepository,
                        PickListRepository pickListRepository) {
        this.riderRepository = riderRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.pickListRepository = pickListRepository;
    }

    @Transactional(readOnly = true)
    public RiderResponseDTO getProfile(String email) {
        return toRiderDTO(getRider(email));
    }

    @Transactional
    public RiderResponseDTO updateProfile(String email, RiderProfileRequestDTO request) {
        User user = getUser(email);
        Rider rider = riderRepository.findByUserId(user.getId())
                .orElseGet(() -> new Rider(user, request.getVehicleType(), request.getVehicleNumber()));
        rider.setVehicleType(request.getVehicleType());
        rider.setVehicleNumber(request.getVehicleNumber());
        rider.setActive(true);
        return toRiderDTO(riderRepository.save(rider));
    }

    @Transactional
    public RiderResponseDTO updateLocation(String email, RiderLocationRequestDTO request) {
        Rider current = getRider(email);
        Rider rider = riderRepository.findByIdForUpdate(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        rider.setCurrentLatitude(request.getLatitude());
        rider.setCurrentLongitude(request.getLongitude());

        // A rider with an active assignment should not be able to mark
        // themselves available for a second delivery. The availability flag
        // is therefore constrained by the assignment lifecycle below.
        if (request.getAvailable()) {
            boolean hasActiveAssignment = assignmentRepository.findByRiderIdOrderByAssignedAtDesc(rider.getId())
                    .stream()
                    .anyMatch(a -> a.getStatus() != DeliveryAssignmentStatus.DELIVERED);
            if (hasActiveAssignment) {
                throw new InvalidOperationException("Rider cannot become available while an assignment is active");
            }
        }
        rider.setAvailable(request.getAvailable());
        return toRiderDTO(riderRepository.save(rider));
    }

    @Transactional(readOnly = true)
    public List<DeliveryAssignmentResponseDTO> getAssignments(String email) {
        Rider rider = getRider(email);
        return assignmentRepository.findByRiderIdOrderByAssignedAtDesc(rider.getId())
                .stream()
                .map(this::toAssignmentDTO)
                .toList();
    }

    @Transactional
    public DeliveryAssignmentResponseDTO updateAssignmentStatus(
            String email, Long assignmentId, RiderAssignmentStatusRequestDTO request) {
        User user = getUser(email);
        DeliveryAssignment assignment = assignmentRepository.findByIdAndRiderUserId(assignmentId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found"));

        DeliveryAssignmentStatus current = assignment.getStatus();
        DeliveryAssignmentStatus next = request.getStatus();

        if (current == next) {
            return toAssignmentDTO(assignment);
        }

        if (current == DeliveryAssignmentStatus.ASSIGNED
                && next != DeliveryAssignmentStatus.PICKED_UP) {
            throw new InvalidOperationException("ASSIGNED can only transition to PICKED_UP");
        }
        if (current == DeliveryAssignmentStatus.PICKED_UP
                && next != DeliveryAssignmentStatus.DELIVERED) {
            throw new InvalidOperationException("PICKED_UP can only transition to DELIVERED");
        }
        if (current == DeliveryAssignmentStatus.DELIVERED) {
            throw new InvalidOperationException("A delivered assignment cannot transition again");
        }

        assignment.setStatus(next);
        Order order = assignment.getOrder();

        if (next == DeliveryAssignmentStatus.PICKED_UP) {
            if (order.getStatus() != OrderStatus.PACKED) {
                throw new InvalidOperationException("Order must be PACKED before rider pickup");
            }
            order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        } else if (next == DeliveryAssignmentStatus.DELIVERED) {
            if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
                throw new InvalidOperationException("Order must be OUT_FOR_DELIVERY before delivery completion");
            }
            order.setStatus(OrderStatus.DELIVERED);
            assignment.getRider().setAvailable(true);
            riderRepository.save(assignment.getRider());
        }

        orderRepository.save(order);
        return toAssignmentDTO(assignmentRepository.save(assignment));
    }

    /**
     * Admin/dispatch-side operation. The client supplies only the order id;
     * rider selection, distance and payout are entirely server-computed.
     */
    @Transactional
    public DeliveryAssignmentResponseDTO autoAssignOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PACKED) {
            throw new InvalidOperationException("Only PACKED orders can be assigned to a rider");
        }

        if (assignmentRepository.findByOrderId(orderId).isPresent()) {
            throw new DuplicateResourceException("Order already has a delivery assignment");
        }

        PickList pickList = pickListRepository.findByOrderId(orderId)
                .orElseThrow(() -> new InvalidOperationException(
                        "Order has no warehouse pick list and cannot be dispatched"
                ));

        List<Rider> candidates = findAvailableRidersSorted(pickList);
        if (candidates.isEmpty()) {
            throw new InvalidOperationException("No active rider is currently available");
        }

        Rider lockedRider = null;
        for (Rider candidate : candidates) {
            Rider current = riderRepository.findByIdForUpdate(candidate.getId())
                    .orElse(null);
            if (current != null && current.isActive() && current.isAvailable()) {
                lockedRider = current;
                break;
            }
        }
        if (lockedRider == null) {
            throw new InvalidOperationException("No active rider is currently available; retry dispatch");
        }

        double riderToStoreKm = haversineKm(
                lockedRider.getCurrentLatitude(),
                lockedRider.getCurrentLongitude(),
                pickList.getStore().getLatitude(),
                pickList.getStore().getLongitude()
        );
        double storeToCustomerKm = haversineKm(
                pickList.getStore().getLatitude(),
                pickList.getStore().getLongitude(),
                order.getDeliveryAddress().getLatitude(),
                order.getDeliveryAddress().getLongitude()
        );
        double distanceKm = riderToStoreKm + storeToCustomerKm;
        BigDecimal payout = calculatePayout(distanceKm);

        DeliveryAssignment assignment = new DeliveryAssignment(order, lockedRider, payout, distanceKm);
        lockedRider.setAvailable(false);
        riderRepository.save(lockedRider);
        return toAssignmentDTO(assignmentRepository.save(assignment));
    }

    private List<Rider> findAvailableRidersSorted(PickList pickList) {
        double storeLat = pickList.getStore().getLatitude();
        double storeLng = pickList.getStore().getLongitude();

        return riderRepository.findByActiveTrueAndAvailableTrueOrderByIdAsc()
                .stream()
                .sorted(Comparator
                        .comparingDouble((Rider r) -> haversineKm(
                                storeLat, storeLng,
                                r.getCurrentLatitude(), r.getCurrentLongitude()
                        ))
                        .thenComparing(Rider::getId))
                .toList();
    }

    private BigDecimal calculatePayout(double distanceKm) {
        return BASE_PAYOUT
                .add(PAYOUT_PER_KM.multiply(BigDecimal.valueOf(distanceKm)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Rider getRider(String email) {
        Rider rider = riderRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Rider profile not found"));
        if (!rider.isActive()) {
            throw new InvalidOperationException("Rider account is inactive");
        }
        return rider;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private RiderResponseDTO toRiderDTO(Rider rider) {
        return new RiderResponseDTO(
                rider.getId(),
                rider.getUser().getId(),
                rider.getUser().getEmail(),
                rider.getVehicleType(),
                rider.getVehicleNumber(),
                rider.getCurrentLatitude(),
                rider.getCurrentLongitude(),
                rider.isActive(),
                rider.isAvailable()
        );
    }

    private DeliveryAssignmentResponseDTO toAssignmentDTO(DeliveryAssignment assignment) {
        return new DeliveryAssignmentResponseDTO(
                assignment.getId(),
                assignment.getOrder().getId(),
                assignment.getRider().getId(),
                assignment.getStatus(),
                assignment.getAssignedAt(),
                assignment.getPayoutAmount(),
                assignment.getDistanceKm()
        );
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
