package com.example.big_bike_auto.customer;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer = แหล่งความจริงเดียว (Single Source of Truth)
 * - เพิ่มฟิลด์ 'repair' เพื่อเก็บรายละเอียดงานซ่อม (notes/parts/ยอดรวม/เวลาอัปเดต)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Customer {

    // ระวัง: ต้องมี no-args constructor + getter/setter ให้ Jackson
    public Customer() {}

    // --- Core identity/profile ---
    private String id;
    private String name;
    private String phone;
    private String plate;
    private String province;
    private String bikeModel;

    // สถานะงาน (ใช้ร่วมกับ Dashboard)
    public enum RepairStatus {
        RECEIVED, DIAGNOSING, WAIT_PARTS, REPAIRING, QA, DONE
    }
    private RepairStatus status;

    // ข้อมูลฟอร์มเบื้องต้น
    private String symptom;
    private LocalDate receivedDate;      // วันที่รับรถ
    private LocalDateTime registeredAt;  // ลงทะเบียนเข้า system

    // ✨ รายละเอียดงานซ่อม (เก็บใน customers.json)
    private RepairInfo repair;

    // ---------- factory สร้างใหม่ ----------
    public static Customer create(
            String name, String phone, String plate, String province,
            String bikeModel, RepairStatus st, String symptom, LocalDate receivedDate,
            LocalDateTime registeredAt, String id
    ) {
        Customer c = new Customer();
        c.setId(id);
        c.setName(name);
        c.setPhone(phone);
        c.setPlate(plate);
        c.setProvince(province);
        c.setBikeModel(bikeModel);
        c.setStatus(st);
        c.setSymptom(symptom);
        c.setReceivedDate(receivedDate);
        c.setRegisteredAt(registeredAt);
        return c;
    }

    // ---------- nested: ข้อมูลงานซ่อม ----------
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RepairInfo {
        public RepairInfo() {}
        private String notes;
        private List<Part> parts = new ArrayList<>();
        private Double grandTotal;           // คำนวณก่อนเขียน เพื่อแสดงเร็ว
        private LocalDateTime lastUpdated;   // ไว้ sorting / audit

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public List<Part> getParts() { return parts; }
        public void setParts(List<Part> parts) { this.parts = parts; }
        public Double getGrandTotal() { return grandTotal; }
        public void setGrandTotal(Double grandTotal) { this.grandTotal = grandTotal; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Part {
        public Part() {}
        private String name;
        private Integer quantity;
        private String unit;
        private Double unitPrice;

        public Part(String name, Integer quantity, String unit, Double unitPrice) {
            this.name = name; this.quantity = quantity; this.unit = unit; this.unitPrice = unitPrice;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public Double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

        public double getTotal() {
            double q = quantity == null ? 0 : quantity;
            double p = unitPrice == null ? 0 : unitPrice;
            return q * p;
        }
    }

    // ---------- getters/setters ----------
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getBikeModel() { return bikeModel; }
    public void setBikeModel(String bikeModel) { this.bikeModel = bikeModel; }
    public RepairStatus getStatus() { return status; }
    public void setStatus(RepairStatus status) { this.status = status; }
    public String getSymptom() { return symptom; }
    public void setSymptom(String symptom) { this.symptom = symptom; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public RepairInfo getRepair() { return repair; }
    public void setRepair(RepairInfo repair) { this.repair = repair; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
