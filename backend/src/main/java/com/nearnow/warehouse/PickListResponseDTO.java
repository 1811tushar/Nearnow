package com.nearnow.warehouse;

import java.util.List;

public class PickListResponseDTO {

    private final Long id;
    private final Long orderId;
    private final Long storeId;
    private final PickListStatus status;
    private final List<PickListItemResponseDTO> items;

    public PickListResponseDTO(Long id, Long orderId, Long storeId,
                               PickListStatus status, List<PickListItemResponseDTO> items) {
        this.id = id;
        this.orderId = orderId;
        this.storeId = storeId;
        this.status = status;
        this.items = items;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getStoreId() { return storeId; }
    public PickListStatus getStatus() { return status; }
    public List<PickListItemResponseDTO> getItems() { return items; }
}
