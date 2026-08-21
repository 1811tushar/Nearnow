package com.nearnow.order;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Didn't exist until now — OrderItem rows were only ever accessed via
 * Order's own @OneToMany cascade (Order.getItems()). Review is the
 * first feature that needs to query OrderItems directly, independent
 * of loading a specific Order — "has this user ever ordered this
 * product, across ANY of their orders?"
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Spring Data JPA traverses the relationship chain from the method
    // name itself: OrderItem -> order -> user -> id, AND OrderItem ->
    // product -> id. No hand-written query needed.
    boolean existsByOrder_User_IdAndProduct_IdAndOrder_Status(Long userId, Long productId, OrderStatus status);
}
