package com.example.big_bike_auto.model;

public class OrderItem {
    private String sku;       // รหัสอะไหล่
    private String name;      // ชื่ออะไหล่
    private String unit;      // หน่วย
    private int quantity;     // จำนวนที่สั่ง
    private double unitPrice; // ราคาต่อหน่วย

    public OrderItem(String sku, String name, String unit, int quantity, double unitPrice) {
        this.sku = sku;
        this.name = name;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // --- getters ---
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }

    // --- setters ---
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    // --- helper ---
    public double getTotal() {
        return quantity * unitPrice;
    }
}
