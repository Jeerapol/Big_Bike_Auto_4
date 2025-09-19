package com.example.big_bike_auto.model.viewmodel;

import javafx.beans.property.*;

/**
 * ViewModel สำหรับตาราง Inventory
 * - แยกจาก Controller ให้ javafx.base เข้าถึงได้ (อย่าทำเป็น inner class)
 * - ใช้ JavaFX Property เพื่อ binding ปลอดภัย
 */
public class InventoryRow {
    private final StringProperty partCode = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty supplier = new SimpleStringProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final IntegerProperty inStock = new SimpleIntegerProperty();
    private final IntegerProperty minStock = new SimpleIntegerProperty();
    private final IntegerProperty reserved = new SimpleIntegerProperty();
    private final IntegerProperty onOrder = new SimpleIntegerProperty();
    private final IntegerProperty needed = new SimpleIntegerProperty();

    public InventoryRow(String code, String name, String supplier, String category,
                        int inStock, int minStock, int reserved, int onOrder, int needed) {
        this.partCode.set(code);
        this.name.set(name);
        this.supplier.set(supplier);
        this.category.set(category);
        this.inStock.set(inStock);
        this.minStock.set(minStock);
        this.reserved.set(reserved);
        this.onOrder.set(onOrder);
        this.needed.set(needed);
    }

    // แปลงเป็น draft line (โครงสร้างเบื้องต้นสำหรับ Orders page)
    public DraftLine toDraftLine() {
        return new DraftLine(getSupplier(), getPartCode(), getName(), Math.max(0, getNeeded()));
    }

    public String getPartCode() { return partCode.get(); }
    public StringProperty partCodeProperty() { return partCode; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getSupplier() { return supplier.get(); }
    public StringProperty supplierProperty() { return supplier; }

    public String getCategory() { return category.get(); }
    public StringProperty categoryProperty() { return category; }

    public int getInStock() { return inStock.get(); }
    public IntegerProperty inStockProperty() { return inStock; }

    public int getMinStock() { return minStock.get(); }
    public IntegerProperty minStockProperty() { return minStock; }

    public int getReserved() { return reserved.get(); }
    public IntegerProperty reservedProperty() { return reserved; }

    public int getOnOrder() { return onOrder.get(); }
    public IntegerProperty onOrderProperty() { return onOrder; }

    public int getNeeded() { return needed.get(); }
    public IntegerProperty neededProperty() { return needed; }

    /** โครงสร้าง draft line ที่ส่งต่อไปหน้า Orders */
    public static final class DraftLine {
        private final String supplier;
        private final String partCode;
        private final String name;
        private final int orderQty;

        public DraftLine(String supplier, String partCode, String name, int orderQty) {
            this.supplier = supplier;
            this.partCode = partCode;
            this.name = name;
            this.orderQty = orderQty;
        }
        public String getSupplier() { return supplier; }
        public String getPartCode() { return partCode; }
        public String getName() { return name; }
        public int getOrderQty() { return orderQty; }
    }
}
