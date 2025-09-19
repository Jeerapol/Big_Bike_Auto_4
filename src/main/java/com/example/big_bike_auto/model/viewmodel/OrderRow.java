package com.example.big_bike_auto.model.viewmodel;

import javafx.beans.property.*;

/**
 * ViewModel สำหรับ Draft ในหน้า Orders
 * - supplier/partCode/name/needed/orderQty
 * - ใช้ Property เพื่อรองรับการแก้ไข orderQty ใน TableView
 */
public class OrderRow {
    private final StringProperty supplier = new SimpleStringProperty();
    private final StringProperty partCode = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final IntegerProperty needed = new SimpleIntegerProperty();
    private final IntegerProperty orderQty = new SimpleIntegerProperty();

    public OrderRow(String supplier, String partCode, String name, int needed, int orderQty) {
        this.supplier.set(supplier);
        this.partCode.set(partCode);
        this.name.set(name);
        this.needed.set(Math.max(0, needed));
        this.orderQty.set(Math.max(0, orderQty));
    }

    public String getSupplier() { return supplier.get(); }
    public StringProperty supplierProperty() { return supplier; }

    public String getPartCode() { return partCode.get(); }
    public StringProperty partCodeProperty() { return partCode; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public int getNeeded() { return needed.get(); }
    public IntegerProperty neededProperty() { return needed; }

    public int getOrderQty() { return orderQty.get(); }
    public void setOrderQty(int v) { orderQty.set(Math.max(0, v)); }
    public IntegerProperty orderQtyProperty() { return orderQty; }
}
