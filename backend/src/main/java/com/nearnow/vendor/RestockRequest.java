package com.nearnow.vendor;

import com.nearnow.product.Product;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="restock_requests")
public class RestockRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="vendor_id",nullable=false) private Vendor vendor;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id",nullable=false) private Product product;
 @Column(nullable=false) private int quantity;
 @Column(length=1000) private String note;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private RestockRequestStatus status=RestockRequestStatus.PENDING;
 @Column(nullable=false,updatable=false) private Instant createdAt;
 protected RestockRequest(){}
 public RestockRequest(Vendor vendor,Product product,int quantity,String note){this.vendor=vendor;this.product=product;this.quantity=quantity;this.note=note;}
 @PrePersist protected void onCreate(){createdAt=Instant.now();}
 public Long getId(){return id;} public Vendor getVendor(){return vendor;} public Product getProduct(){return product;} public int getQuantity(){return quantity;} public String getNote(){return note;} public RestockRequestStatus getStatus(){return status;} public void setStatus(RestockRequestStatus s){status=s;} public Instant getCreatedAt(){return createdAt;}
}
