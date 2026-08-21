package com.nearnow.warehouse;

import com.nearnow.product.Product;
import jakarta.persistence.*;

@Entity
@Table(name = "pick_list_items")
public class PickListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pick_list_id", nullable = false)
    private PickList pickList;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean picked = false;

    protected PickListItem() {
    }

    public PickListItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public void setPickList(PickList pickList) {
        this.pickList = pickList;
    }

    public Long getId() { return id; }
    public PickList getPickList() { return pickList; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public boolean isPicked() { return picked; }

    public void setPicked(boolean picked) { this.picked = picked; }
}
