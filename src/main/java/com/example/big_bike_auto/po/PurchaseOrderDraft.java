package com.example.big_bike_auto.po;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * โครงสร้าง PO Draft สำหรับบันทึกไฟล์
 * - แยกตาม Supplier หนึ่งไฟล์ต่อหนึ่งผู้ขาย
 * - รองรับ Gson serialization/deserialization (ต้องมี getter/setter ครบ)
 */
public class PurchaseOrderDraft {

    // ===== Inner Item =====
    public static class Item {
        private String sku;
        private String name;
        private String unit;
        private int quantity;
        private BigDecimal unitCost;
        private BigDecimal subTotal;

        public Item() {}

        public Item(String sku, String name, String unit, int quantity,
                    BigDecimal unitCost, BigDecimal subTotal) {
            this.sku = sku;
            this.name = name;
            this.unit = unit;
            this.quantity = quantity;
            this.unitCost = unitCost;
            this.subTotal = subTotal;
        }

        // --- Getters ---
        public String getSku() { return sku; }
        public String getName() { return name; }
        public String getUnit() { return unit; }
        public int getQuantity() { return quantity; }
        public BigDecimal getUnitCost() { return unitCost; }
        public BigDecimal getSubTotal() { return subTotal; }

        // --- Setters (สำคัญสำหรับ Gson) ---
        public void setSku(String sku) { this.sku = sku; }
        public void setName(String name) { this.name = name; }
        public void setUnit(String unit) { this.unit = unit; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
        public void setSubTotal(BigDecimal subTotal) { this.subTotal = subTotal; }
    }

    // ===== Fields =====
    private String supplier;
    private LocalDate createdDate;
    private List<Item> items = new ArrayList<>();
    private BigDecimal grandTotal = BigDecimal.ZERO;

    // ===== Constructors =====
    public PurchaseOrderDraft() {}

    public PurchaseOrderDraft(String supplier) {
        this.supplier = supplier;
        this.createdDate = LocalDate.now();
    }

    // ===== Getters =====
    public String getSupplier() { return supplier; }
    public LocalDate getCreatedDate() { return createdDate; }
    public List<Item> getItems() { return items; }
    public BigDecimal getGrandTotal() { return grandTotal; }

    // ===== Setters (สำคัญสำหรับ Gson) =====
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
    public void setItems(List<Item> items) { this.items = items; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    // ===== Logic =====
    public void recomputeTotal() {
        grandTotal = items.stream()
                .map(Item::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
