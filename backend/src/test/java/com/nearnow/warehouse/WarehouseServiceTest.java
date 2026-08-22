package com.nearnow.warehouse;

import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;
import com.nearnow.common.exception.InvalidOperationException;
import com.nearnow.common.exception.ResourceNotFoundException;
import com.nearnow.order.DeliveryAddressSnapshot;
import com.nearnow.order.Order;
import com.nearnow.order.OrderItem;
import com.nearnow.product.Product;
import com.nearnow.product.ProductRepository;
import com.nearnow.vendor.RestockRequest;
import com.nearnow.vendor.RestockRequestRepository;
import com.nearnow.vendor.RestockRequestResponseDTO;
import com.nearnow.vendor.RestockRequestStatus;
import com.nearnow.vendor.Vendor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WarehouseService — the piece that owns the actual
 * physical-stock rules: which store fulfils an order, what happens to
 * reserved stock on cancel/reactivate, and the pick-list + restock-request
 * workflows a warehouse manager drives from the Partner Ops Portal.
 *
 * All repositories are mocked — no real Postgres, no real locking. That
 * means the pessimistic-lock ("...ForUpdate") queries themselves aren't
 * exercised here; only the business logic that runs after those rows
 * come back is. Concurrency-under-load is exactly what the Tier 1.2
 * Testcontainers integration test is for.
 */
@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private StockLevelRepository stockLevelRepository;
    @Mock private PickListRepository pickListRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private RestockRequestRepository restockRequestRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    private User manager;
    private Store store;
    private Product product;

    @BeforeEach
    void setUp() {
        manager = new User("manager@nearnow.com", "hash", "Store Manager", "");
        setId(manager, 100L);

        store = new Store("Delhi Hub", "MG Road", "Delhi", "110001",
                28.6139, 77.2090, 500,
                LocalTime.of(9, 0), LocalTime.of(21, 0));
        setId(store, 1L);

        product = new Product("Milk 1L", "Fresh milk", null, new BigDecimal("50.00"), "carton", 0);
        setId(product, 10L);
    }

    // Entities in this codebase use DB-generated ids with no public setter
    // (correct design) — tests reach into the field directly, same pattern
    // as the AuthServiceTest already written for this project.
    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Order orderWithItems(Product p, int qty) {
        DeliveryAddressSnapshot address = new DeliveryAddressSnapshot(
                "Home", "Test User", "9999999999", "Test Address",
                "Delhi", "110001", 28.6, 77.2);
        Order order = new Order(manager, new BigDecimal("100.00"), "COD", address);
        setId(order, 500L);
        OrderItem item = new OrderItem(p, p.getName(), null, new BigDecimal("50.00"), "carton", qty);
        order.getItems().add(item);
        return order;
    }

    // ==================== reserveForOrder() ====================

    @Test
    void reserveForOrder_withUnpersistedOrder_throwsInvalidOperationException() {
        Order order = orderWithItems(product, 2);
        setId(order, null);

        assertThatThrownBy(() -> warehouseService.reserveForOrder(order))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("must be persisted");
    }

    @Test
    void reserveForOrder_withNoItems_throwsInvalidOperationException() {
        Order order = orderWithItems(product, 2);
        order.getItems().clear();

        assertThatThrownBy(() -> warehouseService.reserveForOrder(order))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("no items");
    }

    @Test
    void reserveForOrder_whenProductNotWarehouseManaged_returnsFalseForLegacyPath() {
        Order order = orderWithItems(product, 2);
        when(stockLevelRepository.existsByProductId(10L)).thenReturn(false);

        boolean result = warehouseService.reserveForOrder(order);

        assertThat(result).isFalse();
        // Legacy path: no store lookup, no locking, nothing touched.
        verifyNoInteractions(storeRepository);
    }

    @Test
    void reserveForOrder_whenNoActiveStoreHasStock_throwsInvalidOperationException() {
        Order order = orderWithItems(product, 5);
        when(stockLevelRepository.existsByProductId(10L)).thenReturn(true);
        when(storeRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(store));

        StockLevel insufficientLevel = new StockLevel(store, product, 2); // less than required 5
        when(stockLevelRepository.findByStoreIdAndProductIdInForUpdate(eq(1L), anyList()))
                .thenReturn(List.of(insufficientLevel));

        assertThatThrownBy(() -> warehouseService.reserveForOrder(order))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("No active warehouse has sufficient stock");
    }

    @Test
    void reserveForOrder_withSufficientStock_reservesAndCreatesPickList() {
        Order order = orderWithItems(product, 3);
        when(stockLevelRepository.existsByProductId(10L)).thenReturn(true);
        when(storeRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(store));

        StockLevel sufficientLevel = new StockLevel(store, product, 10);
        when(stockLevelRepository.findByStoreIdAndProductIdInForUpdate(eq(1L), anyList()))
                .thenReturn(List.of(sufficientLevel));
        when(stockLevelRepository.decrementQuantity(1L, 10L, 3)).thenReturn(1);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(stockLevelRepository.sumQuantityByProductId(10L)).thenReturn(7L);

        boolean result = warehouseService.reserveForOrder(order);

        assertThat(result).isTrue();
        assertThat(order.isInventoryReserved()).isTrue();
        verify(pickListRepository).save(any(PickList.class));
        verify(productRepository).save(product); // aggregate sync happened
    }

    @Test
    void reserveForOrder_whenAtomicDecrementLosesRace_throwsInvalidOperationException() {
        // decrementQuantity returning != 1 means the SQL guard
        // ("quantity >= :quantity") failed at the DB level — another
        // transaction won the race between the lock read and this write.
        Order order = orderWithItems(product, 3);
        when(stockLevelRepository.existsByProductId(10L)).thenReturn(true);
        when(storeRepository.findByActiveTrueOrderByIdAsc()).thenReturn(List.of(store));

        StockLevel level = new StockLevel(store, product, 10);
        when(stockLevelRepository.findByStoreIdAndProductIdInForUpdate(eq(1L), anyList()))
                .thenReturn(List.of(level));
        when(stockLevelRepository.decrementQuantity(1L, 10L, 3)).thenReturn(0);

        assertThatThrownBy(() -> warehouseService.reserveForOrder(order))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Unable to reserve stock");
    }

    @Test
    void reserveForOrder_withMixedWarehouseAndLegacyProducts_throwsInvalidOperationException() {
        Product legacyProduct = new Product("Bread", "desc", null, new BigDecimal("30.00"), "pack", 0);
        setId(legacyProduct, 20L);

        Order order = orderWithItems(product, 2);
        order.getItems().add(new OrderItem(legacyProduct, "Bread", null, new BigDecimal("30.00"), "pack", 1));

        when(stockLevelRepository.existsByProductId(10L)).thenReturn(true);
        when(stockLevelRepository.existsByProductId(20L)).thenReturn(false);

        assertThatThrownBy(() -> warehouseService.reserveForOrder(order))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("mix of warehouse-managed and legacy products");
    }

    // ==================== reserveExistingOrder() ====================

    @Test
    void reserveExistingOrder_withNoExistingPickList_returnsFalse() {
        Order order = orderWithItems(product, 2);
        when(pickListRepository.findByOrderId(500L)).thenReturn(Optional.empty());

        boolean result = warehouseService.reserveExistingOrder(order);

        assertThat(result).isFalse();
    }

    @Test
    void reserveExistingOrder_withInsufficientStock_throwsInvalidOperationException() {
        Order order = orderWithItems(product, 2);
        PickList pickList = new PickList(order, store);
        pickList.addItem(new PickListItem(product, 5));

        when(pickListRepository.findByOrderId(500L)).thenReturn(Optional.of(pickList));
        StockLevel insufficient = new StockLevel(store, product, 1); // less than required 5
        when(stockLevelRepository.findByStoreIdAndProductIdInForUpdate(eq(1L), anyList()))
                .thenReturn(List.of(insufficient));

        assertThatThrownBy(() -> warehouseService.reserveExistingOrder(order))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Insufficient warehouse stock");
    }

    @Test
    void reserveExistingOrder_withSufficientStock_resetsPickListToPendingAndUnpicksItems() {
        Order order = orderWithItems(product, 2);
        PickList pickList = new PickList(order, store);
        PickListItem item = new PickListItem(product, 3);
        item.setPicked(true); // was picked before cancellation
        pickList.addItem(item);
        pickList.setStatus(PickListStatus.IN_PROGRESS);

        when(pickListRepository.findByOrderId(500L)).thenReturn(Optional.of(pickList));
        StockLevel level = new StockLevel(store, product, 10);
        when(stockLevelRepository.findByStoreIdAndProductIdInForUpdate(eq(1L), anyList()))
                .thenReturn(List.of(level));
        when(stockLevelRepository.decrementQuantity(1L, 10L, 3)).thenReturn(1);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(stockLevelRepository.sumQuantityByProductId(10L)).thenReturn(7L);

        boolean result = warehouseService.reserveExistingOrder(order);

        assertThat(result).isTrue();
        assertThat(pickList.getStatus()).isEqualTo(PickListStatus.PENDING);
        assertThat(item.isPicked()).isFalse(); // must be reset, not left over from before
        assertThat(order.isInventoryReserved()).isTrue();
        assertThat(order.isStockRestored()).isFalse();
    }

    // ==================== restoreReservedStock() ====================

    @Test
    void restoreReservedStock_whenNotInventoryReserved_isNoOpAndReturnsTrue() {
        Order order = orderWithItems(product, 2);
        // inventoryReserved defaults to false — nothing to restore.

        boolean result = warehouseService.restoreReservedStock(order);

        assertThat(result).isTrue();
        verifyNoInteractions(pickListRepository);
    }

    @Test
    void restoreReservedStock_whenAlreadyRestored_isIdempotentNoOp() {
        Order order = orderWithItems(product, 2);
        order.setInventoryReserved(true);
        order.setStockRestored(true); // already done once — must not double-restore

        boolean result = warehouseService.restoreReservedStock(order);

        assertThat(result).isTrue();
        verifyNoInteractions(pickListRepository);
    }

    @Test
    void restoreReservedStock_withNoPickList_returnsFalseForLegacyPath() {
        Order order = orderWithItems(product, 2);
        order.setInventoryReserved(true);
        order.setStockRestored(false);
        when(pickListRepository.findByOrderId(500L)).thenReturn(Optional.empty());

        boolean result = warehouseService.restoreReservedStock(order);

        assertThat(result).isFalse();
    }

    @Test
    void restoreReservedStock_withPickList_incrementsStockAndMarksRestored() {
        Order order = orderWithItems(product, 2);
        order.setInventoryReserved(true);
        order.setStockRestored(false);

        PickList pickList = new PickList(order, store);
        pickList.addItem(new PickListItem(product, 3));
        when(pickListRepository.findByOrderId(500L)).thenReturn(Optional.of(pickList));

        StockLevel level = new StockLevel(store, product, 5);
        when(stockLevelRepository.findByStoreIdAndProductIdInForUpdate(eq(1L), anyList()))
                .thenReturn(List.of(level));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(stockLevelRepository.sumQuantityByProductId(10L)).thenReturn(8L);

        boolean result = warehouseService.restoreReservedStock(order);

        assertThat(result).isTrue();
        assertThat(level.getQuantity()).isEqualTo(8); // 5 + 3 restored
        assertThat(order.isStockRestored()).isTrue();
        verify(stockLevelRepository).save(level);
    }

    // ==================== getPickLists() ====================

    @Test
    void getPickLists_returnsAllPickListsForManagersStore() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.of(store));

        Order order = orderWithItems(product, 1);
        PickList pickList = new PickList(order, store);
        pickList.addItem(new PickListItem(product, 1));
        when(pickListRepository.findByStoreIdOrderByIdDesc(1L)).thenReturn(List.of(pickList));

        List<PickListResponseDTO> result = warehouseService.getPickLists("manager@nearnow.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo(500L);
    }

    @Test
    void getPickLists_withNoAssignedStore_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.getPickLists("manager@nearnow.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No active warehouse is assigned");
    }

    // ==================== pickItem() ====================

    @Test
    void pickItem_withCorrectBarcode_marksItemPickedAndSetsInProgress() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));

        // Product created with no barcode in the shared setUp() — this
        // test needs its own product with a real barcode so the scan
        // match is meaningful rather than trivially true.
        Product barcoded = new Product("Milk 1L", "Fresh milk", null, new BigDecimal("50.00"), "carton", 0);
        setId(barcoded, 10L);
        barcoded.setBarcode("8901234567890");

        Order order = orderWithItems(barcoded, 1);
        PickListItem item = new PickListItem(barcoded, 1);
        setId(item, 7L);
        PickList pickList = new PickList(order, store);
        pickList.addItem(item);
        pickList.setStatus(PickListStatus.PENDING);
        setId(pickList, 55L);

        when(pickListRepository.findOwnedByManager(55L, 100L)).thenReturn(Optional.of(pickList));
        when(pickListRepository.save(pickList)).thenReturn(pickList);

        PickItemRequestDTO request = new PickItemRequestDTO();
        request.setScannedBarcode("8901234567890");

        PickListResponseDTO response = warehouseService.pickItem("manager@nearnow.com", 55L, 7L, request);

        assertThat(response).isNotNull();
        assertThat(item.isPicked()).isTrue();
        assertThat(pickList.getStatus()).isEqualTo(PickListStatus.IN_PROGRESS);
    }

    @Test
    void pickItem_withWrongBarcode_throwsInvalidOperationException() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        Order order = orderWithItems(product, 1);
        Product barcoded = new Product("Milk 1L", "desc", null, new BigDecimal("50.00"), "carton", 0);
        setId(barcoded, 10L);
        barcoded.setBarcode("8901234567890");

        PickListItem item = new PickListItem(barcoded, 1);
        setId(item, 7L);
        PickList pickList = new PickList(order, store);
        pickList.addItem(item);
        setId(pickList, 55L);

        when(pickListRepository.findOwnedByManager(55L, 100L)).thenReturn(Optional.of(pickList));

        PickItemRequestDTO request = new PickItemRequestDTO();
        request.setScannedBarcode("0000000000000"); // wrong barcode

        assertThatThrownBy(() -> warehouseService.pickItem("manager@nearnow.com", 55L, 7L, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void pickItem_onCompletedPickList_throwsInvalidOperationException() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        Order order = orderWithItems(product, 1);
        PickList pickList = new PickList(order, store);
        pickList.addItem(new PickListItem(product, 1));
        pickList.setStatus(PickListStatus.COMPLETED);
        setId(pickList, 55L);

        when(pickListRepository.findOwnedByManager(55L, 100L)).thenReturn(Optional.of(pickList));

        PickItemRequestDTO request = new PickItemRequestDTO();
        request.setScannedBarcode("anything");

        assertThatThrownBy(() -> warehouseService.pickItem("manager@nearnow.com", 55L, 999L, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void pickItem_alreadyPicked_isIdempotentAndDoesNotRecheck() {
        // Second scan of the same item should just return current state,
        // not fail or re-validate the barcode — this is what makes a
        // double-scan in the warehouse harmless instead of an error.
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        Order order = orderWithItems(product, 1);
        PickListItem item = new PickListItem(product, 1);
        item.setPicked(true);
        setId(item, 7L);
        PickList pickList = new PickList(order, store);
        pickList.addItem(item);
        setId(pickList, 55L);

        when(pickListRepository.findOwnedByManager(55L, 100L)).thenReturn(Optional.of(pickList));

        PickItemRequestDTO request = new PickItemRequestDTO();
        request.setScannedBarcode("does-not-matter");

        PickListResponseDTO response = warehouseService.pickItem("manager@nearnow.com", 55L, 7L, request);

        assertThat(response).isNotNull();
        verify(pickListRepository, never()).save(any()); // no write happened, purely idempotent read
    }

    @Test
    void pickItem_withUnknownItemId_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        Order order = orderWithItems(product, 1);
        PickListItem item = new PickListItem(product, 1);
        setId(item, 7L);
        PickList pickList = new PickList(order, store);
        pickList.addItem(item);
        setId(pickList, 55L);

        when(pickListRepository.findOwnedByManager(55L, 100L)).thenReturn(Optional.of(pickList));

        PickItemRequestDTO request = new PickItemRequestDTO();
        request.setScannedBarcode("whatever");

        assertThatThrownBy(() -> warehouseService.pickItem("manager@nearnow.com", 55L, 999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pick-list item not found");
    }

    // ==================== completePickList() ====================

    @Test
    void completePickList_whenAllItemsPicked_marksCompletedAndAdvancesOrderToPacked() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        Order order = orderWithItems(product, 1);
        order.setStatus(com.nearnow.order.OrderStatus.PLACED);
        PickListItem item = new PickListItem(product, 1);
        item.setPicked(true);
        PickList pickList = new PickList(order, store);
        pickList.addItem(item);
        pickList.setStatus(PickListStatus.IN_PROGRESS);
        setId(pickList, 55L);

        when(pickListRepository.findOwnedByManager(55L, 100L)).thenReturn(Optional.of(pickList));
        when(pickListRepository.save(pickList)).thenReturn(pickList);

        PickListResponseDTO response = warehouseService.completePickList("manager@nearnow.com", 55L);

        assertThat(response).isNotNull();
        assertThat(pickList.getStatus()).isEqualTo(PickListStatus.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(com.nearnow.order.OrderStatus.PACKED);
    }

    @Test
    void completePickList_whenNotAllItemsPicked_throwsInvalidOperationException() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        Order order = orderWithItems(product, 1);
        PickListItem pickedItem = new PickListItem(product, 1);
        pickedItem.setPicked(true);
        PickListItem unpickedItem = new PickListItem(product, 1);
        // unpickedItem.picked defaults to false
        PickList pickList = new PickList(order, store);
        pickList.addItem(pickedItem);
        pickList.addItem(unpickedItem);
        setId(pickList, 55L);

        when(pickListRepository.findOwnedByManager(55L, 100L)).thenReturn(Optional.of(pickList));

        assertThatThrownBy(() -> warehouseService.completePickList("manager@nearnow.com", 55L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("must be scanned before completion");
    }

    @Test
    void completePickList_whenAlreadyCompleted_isIdempotentNoOp() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        Order order = orderWithItems(product, 1);
        PickList pickList = new PickList(order, store);
        pickList.addItem(new PickListItem(product, 1));
        pickList.setStatus(PickListStatus.COMPLETED);
        setId(pickList, 55L);

        when(pickListRepository.findOwnedByManager(55L, 100L)).thenReturn(Optional.of(pickList));

        PickListResponseDTO response = warehouseService.completePickList("manager@nearnow.com", 55L);

        assertThat(response).isNotNull();
        verify(pickListRepository, never()).save(any()); // already-completed short-circuit
    }

    @Test
    void completePickList_doesNotOverwriteOrderStatusPastPlaced() {
        // If the order somehow already advanced further than PLACED (e.g.
        // a separate admin action), completing the pick shouldn't drag it
        // backwards to PACKED — the "only if still PLACED" guard exists
        // exactly for this.
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        Order order = orderWithItems(product, 1);
        order.setStatus(com.nearnow.order.OrderStatus.OUT_FOR_DELIVERY);
        PickListItem item = new PickListItem(product, 1);
        item.setPicked(true);
        PickList pickList = new PickList(order, store);
        pickList.addItem(item);
        setId(pickList, 55L);

        when(pickListRepository.findOwnedByManager(55L, 100L)).thenReturn(Optional.of(pickList));
        when(pickListRepository.save(pickList)).thenReturn(pickList);

        warehouseService.completePickList("manager@nearnow.com", 55L);

        assertThat(order.getStatus()).isEqualTo(com.nearnow.order.OrderStatus.OUT_FOR_DELIVERY);
    }

    // ==================== getRestockRequests() ====================

    @Test
    void getRestockRequests_onlyReturnsRequestsForProductsManagedByThisStore() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.of(store));

        Vendor vendor = new Vendor(manager, "Some Vendor", "Address", "GST123");
        RestockRequest managedRequest = new RestockRequest(vendor, product, 20, "please restock");
        setId(managedRequest, 1L);

        Product otherStoreProduct = new Product("Eggs", "desc", null, new BigDecimal("60.00"), "dozen", 0);
        setId(otherStoreProduct, 99L);
        RestockRequest unrelatedRequest = new RestockRequest(vendor, otherStoreProduct, 5, "note");
        setId(unrelatedRequest, 2L);

        when(restockRequestRepository.findByStatusOrderByCreatedAtAsc(RestockRequestStatus.PENDING))
                .thenReturn(List.of(managedRequest, unrelatedRequest));
        when(stockLevelRepository.existsByStoreIdAndProductId(1L, 10L)).thenReturn(true);
        when(stockLevelRepository.existsByStoreIdAndProductId(1L, 99L)).thenReturn(false);

        List<RestockRequestResponseDTO> result = warehouseService.getRestockRequests("manager@nearnow.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(10L);
    }

    // ==================== updateRestockRequest() ====================

    @Test
    void updateRestockRequest_approved_increasesStockAndSyncsAggregate() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.of(store));

        Vendor vendor = new Vendor(manager, "Some Vendor", "Address", "GST123");
        RestockRequest request = new RestockRequest(vendor, product, 20, "please restock");
        setId(request, 1L);
        when(restockRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        StockLevel level = new StockLevel(store, product, 5);
        when(stockLevelRepository.findByStoreIdAndProductIdForUpdate(1L, 10L)).thenReturn(Optional.of(level));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(stockLevelRepository.sumQuantityByProductId(10L)).thenReturn(25L);
        when(restockRequestRepository.save(request)).thenReturn(request);

        RestockRequestResponseDTO response = warehouseService.updateRestockRequest(
                "manager@nearnow.com", 1L, RestockRequestStatus.APPROVED);

        assertThat(response).isNotNull();
        assertThat(level.getQuantity()).isEqualTo(25); // 5 existing + 20 restocked
        assertThat(request.getStatus()).isEqualTo(RestockRequestStatus.APPROVED);
    }

    @Test
    void updateRestockRequest_rejected_onlyChangesStatusWithNoStockChange() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.of(store));

        Vendor vendor = new Vendor(manager, "Some Vendor", "Address", "GST123");
        RestockRequest request = new RestockRequest(vendor, product, 20, "please restock");
        setId(request, 1L);
        when(restockRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(restockRequestRepository.save(request)).thenReturn(request);

        warehouseService.updateRestockRequest("manager@nearnow.com", 1L, RestockRequestStatus.REJECTED);

        assertThat(request.getStatus()).isEqualTo(RestockRequestStatus.REJECTED);
        // Rejection must never touch stock levels — verifying the lock
        // query for stock was never even called guards against a copy-paste
        // bug that applies the approve-branch's stock math unconditionally.
        verify(stockLevelRepository, never()).findByStoreIdAndProductIdForUpdate(anyLong(), anyLong());
    }

    @Test
    void updateRestockRequest_alreadyProcessed_throwsInvalidOperationException() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.of(store));

        Vendor vendor = new Vendor(manager, "Some Vendor", "Address", "GST123");
        RestockRequest request = new RestockRequest(vendor, product, 20, "note");
        setId(request, 1L);
        request.setStatus(RestockRequestStatus.APPROVED); // already processed once

        when(restockRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> warehouseService.updateRestockRequest(
                "manager@nearnow.com", 1L, RestockRequestStatus.REJECTED))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already processed");
    }

    @Test
    void updateRestockRequest_approvedForUnmanagedProduct_throwsInvalidOperationException() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.of(store));

        Vendor vendor = new Vendor(manager, "Some Vendor", "Address", "GST123");
        RestockRequest request = new RestockRequest(vendor, product, 20, "note");
        setId(request, 1L);
        when(restockRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        // No StockLevel row exists for this store+product combination —
        // the request was for a product this warehouse doesn't stock at all.
        when(stockLevelRepository.findByStoreIdAndProductIdForUpdate(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.updateRestockRequest(
                "manager@nearnow.com", 1L, RestockRequestStatus.APPROVED))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("not managed by the assigned warehouse");
    }

    @Test
    void updateRestockRequest_withUnknownRequestId_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.of(store));
        when(restockRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.updateRestockRequest(
                "manager@nearnow.com", 999L, RestockRequestStatus.APPROVED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Restock request not found");
    }

    // ==================== getStock() ====================

    @Test
    void getStock_returnsAllStockLevelsForManagersStore() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.of(store));

        StockLevel level = new StockLevel(store, product, 15);
        when(stockLevelRepository.findByStoreIdOrderByProduct_NameAsc(1L)).thenReturn(List.of(level));

        List<StockLevelResponseDTO> result = warehouseService.getStock("manager@nearnow.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuantity()).isEqualTo(15);
    }

    // ==================== adjustStock() ====================

    @Test
    void adjustStock_forExistingStockLevel_overwritesQuantity() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.of(store));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        StockLevel existing = new StockLevel(store, product, 5);
        when(stockLevelRepository.findByStoreIdAndProductIdForUpdate(1L, 10L)).thenReturn(Optional.of(existing));
        when(stockLevelRepository.save(existing)).thenReturn(existing);
        when(stockLevelRepository.sumQuantityByProductId(10L)).thenReturn(50L);

        StockAdjustmentRequestDTO request = new StockAdjustmentRequestDTO();
        request.setProductId(10L);
        request.setQuantity(50);

        StockLevelResponseDTO response = warehouseService.adjustStock("manager@nearnow.com", request);

        assertThat(response.getQuantity()).isEqualTo(50);
        assertThat(existing.getQuantity()).isEqualTo(50); // overwritten, not added to
    }

    @Test
    void adjustStock_forProductWithNoExistingStockRow_createsNewStockLevel() {
        // First time this store ever stocks this product — no row to find,
        // orElseGet builds a brand-new StockLevel(store, product, 0) first.
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.of(store));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(stockLevelRepository.findByStoreIdAndProductIdForUpdate(1L, 10L)).thenReturn(Optional.empty());
        when(stockLevelRepository.save(any(StockLevel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stockLevelRepository.sumQuantityByProductId(10L)).thenReturn(30L);

        StockAdjustmentRequestDTO request = new StockAdjustmentRequestDTO();
        request.setProductId(10L);
        request.setQuantity(30);

        StockLevelResponseDTO response = warehouseService.adjustStock("manager@nearnow.com", request);

        assertThat(response.getQuantity()).isEqualTo(30);
        verify(stockLevelRepository).save(any(StockLevel.class));
    }

    @Test
    void adjustStock_withUnknownProduct_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.of(store));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        StockAdjustmentRequestDTO request = new StockAdjustmentRequestDTO();
        request.setProductId(999L);
        request.setQuantity(10);

        assertThatThrownBy(() -> warehouseService.adjustStock("manager@nearnow.com", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== shared guard: getAssignedStore() ====================

    @Test
    void anyManagerMethod_withNoAssignedActiveStore_throwsResourceNotFoundException() {
        // getStock() stands in for every method that routes through
        // getAssignedStore() — they all share the exact same guard.
        when(userRepository.findByEmail("manager@nearnow.com")).thenReturn(Optional.of(manager));
        when(storeRepository.findByWarehouseManagerIdAndActiveTrue(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.getStock("manager@nearnow.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No active warehouse is assigned");
    }

    @Test
    void anyManagerMethod_withUnknownEmail_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("ghost@nearnow.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.getStock("ghost@nearnow.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
