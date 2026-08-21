package com.nearnow.admin;

import com.nearnow.common.dto.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A "seed the database with demo data" button has no place in a real
 * production admin panel — production data comes only from real
 * business events (real signups, real orders). This is purely a
 * local-development convenience for populating a fresh database
 * without manually creating dozens of products/categories by hand.
 *
 * @Profile("dev") means this entire controller — including the route
 * itself — is NEVER registered unless the "dev" profile is active
 * (see application.properties: spring.profiles.active). In a "prod"
 * (or any non-dev) deployment, POST /api/admin/seed simply does not
 * exist — a 404, not a permission error — because the bean was never
 * created. Nothing to accidentally leave enabled, nothing to forget
 * to remove before deploying.
 */
@Profile("dev")
@RestController
@RequestMapping("/api/admin")
public class AdminSeedController {

    private final AdminService adminService;

    public AdminSeedController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<String>> seed() {
        return ResponseEntity.ok(ApiResponse.success(adminService.seedAll()));
    }
}
