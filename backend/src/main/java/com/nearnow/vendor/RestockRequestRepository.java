package com.nearnow.vendor;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface RestockRequestRepository extends JpaRepository<RestockRequest,Long>{ List<RestockRequest> findByVendorIdOrderByCreatedAtDesc(Long vendorId); List<RestockRequest> findByStatusOrderByCreatedAtAsc(RestockRequestStatus status); }
