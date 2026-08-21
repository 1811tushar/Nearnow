package com.nearnow.common.pricing;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {
    private final PricingService pricing = new PricingService();

    @Test void chargesDeliveryBelowThreshold() {
        assertEquals(new BigDecimal("20.00"), pricing.deliveryFee(new BigDecimal("100.00")));
        assertEquals(new BigDecimal("120.00"), pricing.total(new BigDecimal("100.00")));
    }

    @Test void freeDeliveryAtThreshold() {
        assertEquals(BigDecimal.ZERO, pricing.deliveryFee(new BigDecimal("199.00")));
        assertEquals(new BigDecimal("199.00"), pricing.total(new BigDecimal("199.00")));
    }
}
