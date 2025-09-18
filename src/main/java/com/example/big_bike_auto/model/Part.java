package com.example.big_bike_auto.model;

public class Part {
    private String partName;
    private int quantity;
    private String unit;
    private double unitPrice;

    public Part(String partName, int quantity, String unit, double unitPrice) {
        this.partName = partName;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPrice = unitPrice;
    }

    public String getPartName() { return partName; }
    public int getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public double getUnitPrice() { return unitPrice; }

    public double getTotalPrice() {
        return unitPrice * quantity;
    }
}
