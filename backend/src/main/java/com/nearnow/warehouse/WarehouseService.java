package com.nearnow.warehouse;

import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;
import com.nearnow.common.exception.InvalidOperationException;
import com.nearnow.common.exception.ResourceNotFoundException;
import com.nearnow.order.Order;
import com.nearnow.product.Product;
import com.nearnow.product.ProductRepository;
import com.nearnow.vendor.RestockRequest;
import com.nearnow.vendor.RestockRequestRepository;
import com.nearnow.vendor.RestockRequestResponseDTO;
import com.nearnow.vendor.RestockRequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Warehouse business rules live here rather than in the Controller.
 *
 * Important integration rule:
 * StockLevel is the authoritative per-store inventory for products that
 * have been migrated into warehouse inventory. Product.stock is retained
 * as a compatibility aggregate and is synchronized from StockLevel after
 * warehouse stock changes.
 */
@Service
public class WarehouseService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final StoreRepository storeRepository;
    private final StockLevelRepository stockLevelRepository;
    private final PickListRepository pickListRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RestockRequestRepository restockRequestRepository;

    public WarehouseService(StoreRepository storeRepository,
                            StockLevelRepository stockLevelRepository,
                            PickListRepository pickListRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository, RestockRequestRepository restockRequestRepository) {
        this.storeRepository = storeRepository;
        this.stockLevelRepository = stockLevelRepository;
        this.pickListRepository = pickListRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.restockRequestRepository = restockRequestRepository;
    }

    /**
     * Existing OrderService calls this after the Order has been persisted
     * but before its transaction commits. Returns false for legacy products
     * that have not yet been migrated to StockLevel inventory.
     *
     * The transaction is intentionally shared with checkout: if no store
     * can satisfy the order, stock reservation and the order itself roll
     * back together.
     */
    @Transactional
    public boolean reserveForOrder(Order order) {
        if (order == null || order.getId() == null) {
            throw new InvalidOperationException("Order must be persisted before warehouse allocation");
        }

        if (order.getItems().isEmpty()) {
            throw new InvalidOperationException("Cannot allocate an order with no items");
        }

        Map<Long, Integer> requiredQuantities = aggregateRequiredQuantities(order);
        List<Long> productIds = requiredQuantities.keySet().stream().toList();

        long warehouseManagedCount = productIds.stream()
                .filter(stockLevelRepository::existsByProductId)
                .count();

        // No StockLevel rows means this is still a legacy Product.stock order.
        // Returning false lets OrderService preserve the pre-warehouse path.
        if (warehouseManagedCount == 0) {
            return false;
        }

        // A mixed order would otherwise have two different inventory sources
        // inside one checkout. Force migration of the remaining products before
        // enabling warehouse allocation for the basket.
        if (warehouseManagedCount != productIds.size()) {
            throw new InvalidOperationException(
                    "Order contains a mix of warehouse-managed and legacy products; migrate all products before checkout"
            );
        }

        List<Store> candidates = storeRepository.findByActiveTrueOrderByIdAsc()
                .stream()
                .sorted(Comparator.comparingDouble(store ->
                        haversineKm(
                                order.getDeliveryAddress().getLatitude(),
                                order.getDeliveryAddress().getLongitude(),
                                store.getLatitude(),
                                store.getLongitude()
                        )))
                .toList();

        for (Store store : candidates) {
            /*
             * Lock this candidate's relevant inventory rows before making the
             * final decision. If another checkout is currently changing the
             * same rows, this query waits for that transaction and then sees
             * the committed quantity instead of making a stale decision.
             */
            List<StockLevel> lockedLevels = stockLevelRepository
                    .findByStoreIdAndProductIdInForUpdate(store.getId(), productIds);

            Map<Long, StockLevel> levelsByProduct = new HashMap<>();
            for (StockLevel level : lockedLevels) {
                levelsByProduct.put(level.getProduct().getId(), level);
            }

            if (!hasRequiredStock(lockedLevels, requiredQuantities)) {
                continue;
            }

            for (Map.Entry<Long, Integer> required : requiredQuantities.entrySet()) {
                StockLevel level = levelsByProduct.get(required.getKey());
                if (level == null) {
                    throw new InvalidOperationException(
                            "Warehouse inventory changed while allocating the order"
                    );
                }

                int updated = stockLevelRepository.decrementQuantity(
                        store.getId(),
                        required.getKey(),
                        required.getValue()
                );

                if (updated != 1) {
                    throw new InvalidOperationException(
                            "Unable to reserve stock for product: " + required.getKey()
                    );
                }
            }

            PickList pickList = new PickList(order, store);
            for (Map.Entry<Long, Integer> required : requiredQuantities.entrySet()) {
                StockLevel level = levelsByProduct.get(required.getKey());
                pickList.addItem(new PickListItem(level.getProduct(), required.getValue()));
            }

            pickListRepository.save(pickList);

            /*
             * Product.stock remains a compatibility aggregate for existing
             * customer-facing DTOs and legacy code. StockLevel is the source
             * of truth; this value is recomputed from it.
             */
            for (Long productId : requiredQuantities.keySet()) {
                syncProductAggregate(productId);
            }

            order.setInventoryReserved(true);
            return true;
        }

        throw new InvalidOperationException(
                "No active warehouse has sufficient stock for this order"
        );
    }

    /**
     * Re-reserves a previously warehouse-managed cancelled order when an admin
     * changes its status back to an active state. The existing PickList row is
     * reused rather than creating a second row for the same order.
     *
     * Returns false when the order predates warehouse inventory.
     */
    @Transactional
    public boolean reserveExistingOrder(Order order) {
        if (order == null || order.getId() == null) {
            throw new InvalidOperationException("Order must be persisted before warehouse allocation");
        }

        var pickListOptional = pickListRepository.findByOrderId(order.getId());
        if (pickListOptional.isEmpty()) {
            return false;
        }

        PickList pickList = pickListOptional.get();
        Map<Long, Integer> quantities = new TreeMap<>();
        for (PickListItem item : pickList.getItems()) {
            quantities.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }
        if (quantities.isEmpty()) {
            throw new InvalidOperationException("Warehouse pick list has no items");
        }

        List<Long> productIds = quantities.keySet().stream().toList();
        List<StockLevel> lockedLevels = stockLevelRepository
                .findByStoreIdAndProductIdInForUpdate(pickList.getStore().getId(), productIds);

        Map<Long, StockLevel> levelsByProduct = new HashMap<>();
        for (StockLevel level : lockedLevels) {
            levelsByProduct.put(level.getProduct().getId(), level);
        }

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            StockLevel level = levelsByProduct.get(entry.getKey());
            if (level == null || level.getQuantity() < entry.getValue()) {
                throw new InvalidOperationException(
                        "Insufficient warehouse stock to reactivate order #" + order.getId()
                );
            }
        }

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            int updated = stockLevelRepository.decrementQuantity(
                    pickList.getStore().getId(), entry.getKey(), entry.getValue()
            );
            if (updated != 1) {
                throw new InvalidOperationException(
                        "Unable to reserve warehouse stock for product: " + entry.getKey()
                );
            }
        }

        for (PickListItem item : pickList.getItems()) {
            item.setPicked(false);
        }
        pickList.setStatus(PickListStatus.PENDING);
        pickListRepository.save(pickList);

        for (Long productId : quantities.keySet()) {
            syncProductAggregate(productId);
        }
        order.setInventoryReserved(true);
        order.setStockRestored(false);
        return true;
    }

    /**
     * Restores warehouse-reserved stock for an order that is cancelled.
     *
     * Returns false for a legacy order that predates warehouse allocation;
     * the parent OrderService can then keep its existing Product.stock
     * restoration path for that legacy data.
     */
    @Transactional
    public boolean restoreReservedStock(Order order) {
        if (!order.isInventoryReserved() || order.isStockRestored()) {
            return true;
        }

        var pickListOptional = pickListRepository.findByOrderId(order.getId());
        if (pickListOptional.isEmpty()) {
            return false;
        }

        PickList pickList = pickListOptional.get();

        Map<Long, Integer> quantities = new TreeMap<>();
        for (PickListItem item : pickList.getItems()) {
            quantities.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }

        List<Long> productIds = quantities.keySet().stream().toList();
        List<StockLevel> lockedLevels = stockLevelRepository
                .findByStoreIdAndProductIdInForUpdate(pickList.getStore().getId(), productIds);

        Map<Long, StockLevel> levelsByProduct = new HashMap<>();
        for (StockLevel level : lockedLevels) {
            levelsByProduct.put(level.getProduct().getId(), level);
        }

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            StockLevel level = levelsByProduct.get(entry.getKey());
            if (level == null) {
                throw new InvalidOperationException(
                        "Warehouse stock row missing for product: " + entry.getKey()
                );
            }
            level.setQuantity(level.getQuantity() + entry.getValue());
            stockLevelRepository.save(level);
        }

        for (Long productId : quantities.keySet()) {
            syncProductAggregate(productId);
        }

        order.setStockRestored(true);
        return true;
    }

    @Transactional(readOnly = true)
    public List<PickListResponseDTO> getPickLists(String managerEmail) {
        Store store = getAssignedStore(managerEmail);
        return pickListRepository.findByStoreIdOrderByIdDesc(store.getId())
                .stream()
                .map(this::toPickListDTO)
                .toList();
    }

    @Transactional
    public PickListResponseDTO pickItem(String managerEmail, Long pickListId,
                                        Long itemId, PickItemRequestDTO request) {
        User manager = getUser(managerEmail);
        PickList pickList = pickListRepository.findOwnedByManager(pickListId, manager.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pick list not found"));

        if (pickList.getStatus() == PickListStatus.COMPLETED) {
            throw new InvalidOperationException("Pick list is already completed");
        }

        PickListItem item = pickList.getItems().stream()
                .filter(candidate -> candidate.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Pick-list item not found"));

        if (item.isPicked()) {
            return toPickListDTO(pickList);
        }

        String expectedBarcode = item.getProduct().getBarcode();
        if (expectedBarcode == null || !expectedBarcode.equals(request.getScannedBarcode())) {
            throw new InvalidOperationException("Scanned barcode does not match the product");
        }

        item.setPicked(true);
        if (pickList.getStatus() == PickListStatus.PENDING) {
            pickList.setStatus(PickListStatus.IN_PROGRESS);
        }

        return toPickListDTO(pickListRepository.save(pickList));
    }

    @Transactional
    public PickListResponseDTO completePickList(String managerEmail, Long pickListId) {
        User manager = getUser(managerEmail);
        PickList pickList = pickListRepository.findOwnedByManager(pickListId, manager.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pick list not found"));

        if (pickList.getStatus() == PickListStatus.COMPLETED) {
            return toPickListDTO(pickList);
        }

        boolean allPicked = pickList.getItems().stream().allMatch(PickListItem::isPicked);
        if (!allPicked) {
            throw new InvalidOperationException("Every pick-list item must be scanned before completion");
        }

        pickList.setStatus(PickListStatus.COMPLETED);

        // A completed warehouse pick corresponds to the existing Order
        // lifecycle's PACKED state. Do not overwrite a later state that
        // another legitimate workflow may already have applied.
        if (pickList.getOrder().getStatus() == com.nearnow.order.OrderStatus.PLACED) {
            pickList.getOrder().setStatus(com.nearnow.order.OrderStatus.PACKED);
        }

        return toPickListDTO(pickListRepository.save(pickList));
    }

    @Transactional(readOnly = true)
    public List<RestockRequestResponseDTO> getRestockRequests(String managerEmail) {
        Store store=getAssignedStore(managerEmail);
        return restockRequestRepository.findByStatusOrderByCreatedAtAsc(RestockRequestStatus.PENDING).stream()
                .filter(r -> stockLevelRepository.existsByStoreIdAndProductId(store.getId(), r.getProduct().getId()))
                .map(this::toRestockDTO).toList();
    }

    @Transactional
    @CacheEvict(value="products", allEntries=true)
    public RestockRequestResponseDTO updateRestockRequest(String managerEmail, Long requestId, RestockRequestStatus status) {
        Store store=getAssignedStore(managerEmail);
        RestockRequest request=restockRequestRepository.findById(requestId).orElseThrow(()->new ResourceNotFoundException("Restock request not found"));
        if(request.getStatus()!=RestockRequestStatus.PENDING) throw new InvalidOperationException("Restock request already processed");
        if(status==RestockRequestStatus.APPROVED){
            StockLevel level=stockLevelRepository.findByStoreIdAndProductIdForUpdate(store.getId(),request.getProduct().getId())
                    .orElseThrow(()->new InvalidOperationException("This product is not managed by the assigned warehouse"));
            level.setQuantity(level.getQuantity()+request.getQuantity()); stockLevelRepository.save(level); syncProductAggregate(request.getProduct().getId());
        }
        request.setStatus(status); return toRestockDTO(restockRequestRepository.save(request));
    }

    private RestockRequestResponseDTO toRestockDTO(RestockRequest r){return new RestockRequestResponseDTO(r.getId(),r.getProduct().getId(),r.getProduct().getName(),r.getQuantity(),r.getNote(),r.getStatus(),r.getCreatedAt());}

    @Transactional(readOnly = true)
    public List<StockLevelResponseDTO> getStock(String managerEmail) {
        Store store = getAssignedStore(managerEmail);
        return stockLevelRepository.findByStoreIdOrderByProduct_NameAsc(store.getId())
                .stream()
                .map(this::toStockDTO)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public StockLevelResponseDTO adjustStock(String managerEmail,
                                              StockAdjustmentRequestDTO request) {
        Store store = getAssignedStore(managerEmail);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + request.getProductId()
                ));

        StockLevel level = stockLevelRepository
                .findByStoreIdAndProductIdForUpdate(store.getId(), product.getId())
                .orElseGet(() -> new StockLevel(store, product, 0));

        level.setQuantity(request.getQuantity());
        StockLevel saved = stockLevelRepository.save(level);

        syncProductAggregate(product.getId());
        return toStockDTO(saved);
    }


    private boolean hasRequiredStock(List<StockLevel> levels,
                                     Map<Long, Integer> requiredQuantities) {
        Map<Long, Integer> available = new HashMap<>();
        for (StockLevel level : levels) {
            available.merge(level.getProduct().getId(), level.getQuantity(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> required : requiredQuantities.entrySet()) {
            if (available.getOrDefault(required.getKey(), 0) < required.getValue()) {
                return false;
            }
        }
        return true;
    }

    private Map<Long, Integer> aggregateRequiredQuantities(Order order) {
        Map<Long, Integer> required = new TreeMap<>();

        order.getItems().forEach(item -> {
            if (item.getProduct() == null) {
                throw new InvalidOperationException(
                        "Order item has no product reference; warehouse allocation cannot continue"
                );
            }

            required.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        });

        return required;
    }

    private void syncProductAggregate(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        long aggregate = stockLevelRepository.sumQuantityByProductId(productId);
        if (aggregate > Integer.MAX_VALUE) {
            throw new InvalidOperationException("Aggregate stock exceeds supported product stock range");
        }

        product.setStock((int) aggregate);
        productRepository.save(product);
    }

    private Store getAssignedStore(String managerEmail) {
        User manager = getUser(managerEmail);
        return storeRepository.findByWarehouseManagerIdAndActiveTrue(manager.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active warehouse is assigned to this user"
                ));
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private PickListResponseDTO toPickListDTO(PickList pickList) {
        List<PickListItemResponseDTO> items = pickList.getItems().stream()
                .map(item -> new PickListItemResponseDTO(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getBarcode(),
                        item.getQuantity(),
                        item.isPicked()
                ))
                .toList();

        return new PickListResponseDTO(
                pickList.getId(),
                pickList.getOrder().getId(),
                pickList.getStore().getId(),
                pickList.getStatus(),
                items
        );
    }

    private StockLevelResponseDTO toStockDTO(StockLevel level) {
        return new StockLevelResponseDTO(
                level.getId(),
                level.getStore().getId(),
                level.getProduct().getId(),
                level.getProduct().getName(),
                level.getProduct().getBarcode(),
                level.getQuantity()
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
