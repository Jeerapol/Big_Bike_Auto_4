package com.example.big_bike_auto.model;

import java.time.LocalDate;
import java.util.List;

public class Repair {
    private String symptom;
    private String status; // RECEIVED, IN_PROGRESS, DONE
    private List<Part> parts;
    private double grandTotal;
    private LocalDate lastUpdated;

    public Repair(String symptom, String status, List<Part> parts, double grandTotal, LocalDate lastUpdated) {
        this.symptom = symptom;
        this.status = status;
        this.parts = parts;
        this.grandTotal = grandTotal;
        this.lastUpdated = lastUpdated;
    }

    public String getSymptom() { return symptom; }
    public String getStatus() { return status; }
    public List<Part> getParts() { return parts; }
    public double getGrandTotal() { return grandTotal; }
    public LocalDate getLastUpdated() { return lastUpdated; }
}
