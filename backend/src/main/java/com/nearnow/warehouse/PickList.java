package com.nearnow.warehouse;

import com.nearnow.order.Order;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pick_lists", uniqueConstraints = {
        @UniqueConstraint(name = "uq_pick_list_order", columnNames = "order_id")
})
public class PickList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PickListStatus status = PickListStatus.PENDING;

    @OneToMany(mappedBy = "pickList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PickListItem> items = new ArrayList<>();

    protected PickList() {
    }

    public PickList(Order order, Store store) {
        this.order = order;
        this.store = store;
        this.status = PickListStatus.PENDING;
    }

    public void addItem(PickListItem item) {
        items.add(item);
        item.setPickList(this);
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Store getStore() { return store; }
    public PickListStatus getStatus() { return status; }
    public List<PickListItem> getItems() { return items; }

    public void setStatus(PickListStatus status) { this.status = status; }
}
