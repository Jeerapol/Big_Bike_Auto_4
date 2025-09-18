package com.example.big_bike_auto.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseOrder {
    private String id;
    private String supplier;
    private LocalDate orderDate;
    private List<Part> items;
    private boolean received;

    public PurchaseOrder(String id, String supplier, LocalDate orderDate) {
        this.id = id;
        this.supplier = supplier;
        this.orderDate = orderDate;
        this.items = new ArrayList<>();
        this.received = false;
    }

    public String getId() { return id; }
    public String getSupplier() { return supplier; }
    public LocalDate getOrderDate() { return orderDate; }
    public List<Part> getItems() { return items; }
    public boolean isReceived() { return received; }

    public void addItem(Part part) {
        this.items.add(part);
    }

    public void markAsReceived() {
        this.received = true;
    }
}
