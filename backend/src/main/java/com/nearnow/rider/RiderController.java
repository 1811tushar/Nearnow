package com.nearnow.rider;

import com.nearnow.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rider")
public class RiderController {

    private final RiderService riderService;

    public RiderController(RiderService riderService) {
        this.riderService = riderService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<RiderResponseDTO>> getProfile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                riderService.getProfile(authentication.getName())
        ));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<RiderResponseDTO>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody RiderProfileRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                riderService.updateProfile(authentication.getName(), request),
                "Rider profile updated"
        ));
    }

    @PutMapping("/location")
    public ResponseEntity<ApiResponse<RiderResponseDTO>> updateLocation(
            Authentication authentication,
            @Valid @RequestBody RiderLocationRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                riderService.updateLocation(authentication.getName(), request),
                "Rider location updated"
        ));
    }

    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<DeliveryAssignmentResponseDTO>>> getAssignments(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                riderService.getAssignments(authentication.getName())
        ));
    }

    @PutMapping("/assignments/{id}/status")
    public ResponseEntity<ApiResponse<DeliveryAssignmentResponseDTO>> updateAssignmentStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody RiderAssignmentStatusRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                riderService.updateAssignmentStatus(authentication.getName(), id, request),
                "Assignment status updated"
        ));
    }
}
