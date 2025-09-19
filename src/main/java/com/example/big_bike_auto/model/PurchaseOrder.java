package com.example.big_bike_auto.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseOrder {
    private String id;
    private String supplier;
    private LocalDate orderDate;
    private boolean received;
    private List<OrderItem> items = new ArrayList<>();

    public PurchaseOrder(String id, String supplier, LocalDate orderDate) {
        this.id = id;
        this.supplier = supplier;
        this.orderDate = orderDate;
        this.received = false;
    }

    // --- getters ---
    public String getId() { return id; }
    public String getSupplier() { return supplier; }
    public LocalDate getOrderDate() { return orderDate; }
    public boolean isReceived() { return received; }
    public List<OrderItem> getItems() { return items; }

    // --- methods ---
    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void markAsReceived() {
        this.received = true;
    }

    // ✅ ใช้ OrderItem.getTotal()
    public double getTotalAmount() {
        return items.stream()
                .mapToDouble(OrderItem::getTotal)
                .sum();
    }
}
