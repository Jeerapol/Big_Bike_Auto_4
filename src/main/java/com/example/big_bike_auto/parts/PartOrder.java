package com.example.big_bike_auto.parts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ใบสั่งซื้ออะไหล่ (PO)
 * - orderId: ไอดีใบสั่ง
 * - supplier: ผู้ขาย
 * - lines: รายการสินค้าที่สั่ง
 * - status: DRAFT/PLACED/RECEIVED/CANCELED
 */
public class PartOrder {

    public enum Status { DRAFT, PLACED, RECEIVED, CANCELED }

    public static class Line {
        private String sku;
        private String name;
        private int qty;
        private BigDecimal unitCost;

        public Line() { }

        public Line(String sku, String name, int qty, BigDecimal unitCost) {
            this.sku = sku;
            this.name = name;
            this.qty = qty;
            this.unitCost = unitCost == null ? BigDecimal.ZERO : unitCost;
        }

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getQty() { return qty; }
        public void setQty(int qty) { this.qty = qty; }

        public BigDecimal getUnitCost() { return unitCost; }
        public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

        public BigDecimal getLineTotal() {
            return unitCost.multiply(BigDecimal.valueOf(qty));
        }
    }

    private UUID orderId;
    private String supplier;
    private LocalDate createdDate;
    private Status status;
    private List<Line> lines = new ArrayList<>();

    public PartOrder() {}

    public PartOrder(UUID orderId, String supplier, LocalDate createdDate, Status status) {
        this.orderId = orderId;
        this.supplier = supplier;
        this.createdDate = createdDate;
        this.status = status;
    }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines; }

    public BigDecimal getGrandTotal() {
        return lines.stream().map(Line::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
