package com.nearnow.admin;

public record AdminDashboardDTO(long ordersToday, long lowStockCount, long activeProducts, long totalOrders) {}
