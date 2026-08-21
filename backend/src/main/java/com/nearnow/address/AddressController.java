package com.nearnow.address;

import com.nearnow.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponseDTO>>> getAddresses(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(addressService.getAddresses(authentication.getName())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponseDTO>> addAddress(
            Authentication authentication, @Valid @RequestBody AddressRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                addressService.addAddress(authentication.getName(), request), "Address added"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponseDTO>> updateAddress(
            Authentication authentication, @PathVariable Long id, @Valid @RequestBody AddressRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.success(
                addressService.updateAddress(authentication.getName(), id, request), "Address updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(Authentication authentication, @PathVariable Long id) {
        addressService.deleteAddress(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Address deleted"));
    }

    @PutMapping("/{id}/set-default")
    public ResponseEntity<ApiResponse<AddressResponseDTO>> setDefault(
            Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                addressService.setDefaultAddress(authentication.getName(), id), "Default address updated"));
    }
}
