package com.example.big_bike_auto.controller;

import javafx.beans.property.*;

public class PartModel {
    private final StringProperty partName = new SimpleStringProperty();
    private final IntegerProperty qty = new SimpleIntegerProperty();
    private final StringProperty unit = new SimpleStringProperty();
    private final DoubleProperty unitPrice = new SimpleDoubleProperty();
    private final DoubleProperty total = new SimpleDoubleProperty();

    public PartModel(String partName, int qty, String unit, double unitPrice) {
        this.partName.set(partName);
        this.qty.set(qty);
        this.unit.set(unit);
        this.unitPrice.set(unitPrice);
        this.total.bind(this.qty.multiply(this.unitPrice));
    }

    // getters
    public String getPartName() { return partName.get(); }
    public int getQty() { return qty.get(); }
    public String getUnit() { return unit.get(); }
    public double getUnitPrice() { return unitPrice.get(); }
    public double getTotal() { return total.get(); }

    // properties
    public StringProperty partNameProperty() { return partName; }
    public IntegerProperty qtyProperty() { return qty; }
    public StringProperty unitProperty() { return unit; }
    public DoubleProperty unitPriceProperty() { return unitPrice; }
    public DoubleProperty totalProperty() { return total; }
}
