package com.example.big_bike_auto.model;

import java.math.BigDecimal;

public class Part {
    private String sku;        // รหัสอะไหล่
    private String name;       // ชื่ออะไหล่
    private String unit;       // หน่วย
    private int onHand;        // คงเหลือ
    private int reserved;      // จอง
    private int minStock;      // Min stock
    private BigDecimal lastCost; // ต้นทุนล่าสุด
    private String supplier;   // ผู้ขาย

    // ✅ no-arg constructor (จำเป็นมาก)
    public Part() {
    }

    // ✅ full constructor (ใช้ตอนสร้างใหม่)
    public Part(String sku, String name, String unit,
                int onHand, int reserved, int minStock,
                BigDecimal lastCost, String supplier) {
        this.sku = sku;
        this.name = name;
        this.unit = unit;
        this.onHand = onHand;
        this.reserved = reserved;
        this.minStock = minStock;
        this.lastCost = lastCost;
        this.supplier = supplier;
    }

    // --- getters ---
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public int getOnHand() { return onHand; }
    public int getReserved() { return reserved; }
    public int getMinStock() { return minStock; }
    public BigDecimal getLastCost() { return lastCost; }
    public String getSupplier() { return supplier; }

    // --- setters (ครบทุก field) ---
    public void setSku(String sku) { this.sku = sku; }
    public void setName(String name) { this.name = name; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setOnHand(int onHand) { this.onHand = onHand; }
    public void setReserved(int reserved) { this.reserved = reserved; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
    public void setLastCost(BigDecimal lastCost) { this.lastCost = lastCost; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    // --- convenience ---
    public int getAvailable() {
        return Math.max(0, onHand - reserved);
    }
}
