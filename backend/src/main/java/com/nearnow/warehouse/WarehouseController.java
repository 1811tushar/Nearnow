package com.nearnow.warehouse;

import com.nearnow.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.nearnow.vendor.RestockRequestResponseDTO;
import com.nearnow.vendor.RestockRequestStatus;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/pick-lists")
    public ResponseEntity<ApiResponse<List<PickListResponseDTO>>> getPickLists(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                warehouseService.getPickLists(authentication.getName())
        ));
    }

    @PutMapping("/pick-lists/{id}/items/{itemId}/pick")
    public ResponseEntity<ApiResponse<PickListResponseDTO>> pickItem(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody PickItemRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                warehouseService.pickItem(
                        authentication.getName(), id, itemId, request
                ),
                "Item picked"
        ));
    }

    @PutMapping("/pick-lists/{id}/complete")
    public ResponseEntity<ApiResponse<PickListResponseDTO>> completePickList(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                warehouseService.completePickList(authentication.getName(), id),
                "Pick list completed"
        ));
    }

    @GetMapping("/restock-requests")
    public ResponseEntity<ApiResponse<List<RestockRequestResponseDTO>>> getRestockRequests(Authentication authentication){
        return ResponseEntity.ok(ApiResponse.success(warehouseService.getRestockRequests(authentication.getName())));
    }

    @PutMapping("/restock-requests/{id}/status")
    public ResponseEntity<ApiResponse<RestockRequestResponseDTO>> updateRestockRequest(Authentication authentication,@PathVariable Long id,@RequestParam RestockRequestStatus status){
        return ResponseEntity.ok(ApiResponse.success(warehouseService.updateRestockRequest(authentication.getName(),id,status),"Restock request updated"));
    }

    @GetMapping("/stock")
    public ResponseEntity<ApiResponse<List<StockLevelResponseDTO>>> getStock(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                warehouseService.getStock(authentication.getName())
        ));
    }

    @PutMapping("/stock")
    public ResponseEntity<ApiResponse<StockLevelResponseDTO>> adjustStock(
            Authentication authentication,
            @Valid @RequestBody StockAdjustmentRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                warehouseService.adjustStock(authentication.getName(), request),
                "Stock level updated"
        ));
    }
}
