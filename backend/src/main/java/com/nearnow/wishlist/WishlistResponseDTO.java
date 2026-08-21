package com.nearnow.wishlist;

import java.util.List;

/**
 * Deliberately just a list of ids — mirrors WishlistModel.dart's exact
 * shape ({productIds: [...]}). Full Product details are NOT included
 * here: verified that wishlist_page.dart already combines these ids
 * with ProductProvider's own batch-fetch (getProductsByIds -> our
 * existing GET /api/products/batch?ids= from Phase 4) rather than
 * expecting the wishlist endpoint itself to embed full product objects.
 */
public class WishlistResponseDTO {

    private List<Long> productIds;

    public WishlistResponseDTO(List<Long> productIds) {
        this.productIds = productIds;
    }

    public List<Long> getProductIds() {
        return productIds;
    }
}
