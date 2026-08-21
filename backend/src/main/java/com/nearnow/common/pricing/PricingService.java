package com.nearnow.common.pricing;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class PricingService {
    public static final BigDecimal DELIVERY_FEE = new BigDecimal("20.00");
    public static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("199.00");
    public BigDecimal deliveryFee(BigDecimal subtotal) {
        return subtotal.compareTo(FREE_DELIVERY_THRESHOLD) >= 0 ? BigDecimal.ZERO : DELIVERY_FEE;
    }
    public BigDecimal total(BigDecimal subtotal) { return subtotal.add(deliveryFee(subtotal)); }
}
