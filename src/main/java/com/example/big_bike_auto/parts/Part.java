package com.example.big_bike_auto.parts;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Part
 * - โดเมนโมเดลอะไหล่
 * - เพิ่มฟิลด์เสริมด้านการสั่งซื้อ: moq (ขั้นต่ำที่ต้องสั่ง) และ packSize (จำนวนต่อแพ็ค)
 *
 * Junior-friendly:
 * - ใช้ Integer สำหรับฟิลด์เสริม เพื่อให้รองรับ null ได้ (กรณีข้อมูลเก่าไม่มีฟิลด์นี้ใน JSON)
 * - ใส่ validation เบื้องต้นใน setter ไม่ให้ติดลบ
 *
 * Senior perspective:
 * - equals/hashCode อิง SKU เพื่อใช้ในคอลเลกชันได้ปลอดภัย
 * - ใช้ BigDecimal สำหรับตัวเงิน (lastCost)
 */
public class Part {

    // --- ข้อมูลหลัก ---
    private String sku;            // รหัสสินค้า (unique)
    private String name;           // ชื่อสินค้า
    private String unit;           // หน่วยนับ (ชิ้น/ชุด/ลิตร ฯลฯ)
    private int onHand;            // คงเหลือในสต็อก
    private int reserved;          // จำนวนที่จองไว้
    private int minStock;          // ระดับสต็อกขั้นต่ำ
    private BigDecimal lastCost;   // ต้นทุนล่าสุด/ต่อหน่วย
    private String supplier;       // ผู้ขายหลัก

    // --- ข้อมูลเสริมด้านการสั่งซื้อ ---
    private Integer moq;           // Minimum Order Quantity (nullable -> ปล่อยให้ default 1)
    private Integer packSize;      // จำนวนต่อแพ็ค (nullable -> default 1)

    // ====== Constructors ======

    public Part() {
        // no-arg constructor สำหรับ JSON libs
    }

    public Part(String sku, String name, String unit,
                int onHand, int reserved, int minStock,
                BigDecimal lastCost, String supplier) {
        this.sku = sku;
        this.name = name;
        this.unit = unit;
        this.onHand = Math.max(0, onHand);
        this.reserved = Math.max(0, reserved);
        this.minStock = Math.max(0, minStock);
        this.lastCost = lastCost == null ? BigDecimal.ZERO : lastCost;
        this.supplier = supplier;
        // ค่าเสริมปล่อยว่าง -> ไป default ที่ service (1)
        this.moq = null;
        this.packSize = null;
    }

    public Part(String sku, String name, String unit,
                int onHand, int reserved, int minStock,
                BigDecimal lastCost, String supplier,
                Integer moq, Integer packSize) {
        this(sku, name, unit, onHand, reserved, minStock, lastCost, supplier);
        setMoq(moq);
        setPackSize(packSize);
    }

    // ====== Getters / Setters ======

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getOnHand() { return onHand; }
    public void setOnHand(int onHand) { this.onHand = Math.max(0, onHand); }

    public int getReserved() { return reserved; }
    public void setReserved(int reserved) { this.reserved = Math.max(0, reserved); }

    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = Math.max(0, minStock); }

    public BigDecimal getLastCost() { return lastCost; }
    public void setLastCost(BigDecimal lastCost) { this.lastCost = lastCost == null ? BigDecimal.ZERO : lastCost; }

    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }

    /** MOQ (nullable): ถ้า null จะไป default เป็น 1 ใน ReorderService */
    public Integer getMoq() { return moq; }
    public void setMoq(Integer moq) {
        if (moq != null && moq < 1) moq = 1;
        this.moq = moq;
    }

    /** PackSize (nullable): ถ้า null จะไป default เป็น 1 ใน ReorderService */
    public Integer getPackSize() { return packSize; }
    public void setPackSize(Integer packSize) {
        if (packSize != null && packSize < 1) packSize = 1;
        this.packSize = packSize;
    }

    // ====== Helper Methods ======

    /** จำนวนที่เบิกได้จริง = onHand - reserved (ไม่ติดลบ) */
    public int getAvailable() {
        int v = onHand - reserved;
        return Math.max(0, v);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Part)) return false;
        Part part = (Part) o;
        return Objects.equals(sku, part.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }

    @Override
    public String toString() {
        return "Part{" +
                "sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", onHand=" + onHand +
                ", reserved=" + reserved +
                ", minStock=" + minStock +
                ", lastCost=" + lastCost +
                ", supplier='" + supplier + '\'' +
                ", moq=" + moq +
                ", packSize=" + packSize +
                '}';
    }
}
