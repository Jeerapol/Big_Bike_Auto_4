package com.example.big_bike_auto.customer;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Customer: โครงสร้างข้อมูลสำหรับ customers.json
 * - ใช้ร่วมกับ CustomerRepository ที่มี updateRepair(...)
 * - Persist งานซ่อมไว้ในฟิลด์ "repair" (RepairInfo)
 */
public class Customer implements Serializable {

    // ===== สถานะงานซ่อมรวมของลูกค้าคนนี้ =====
    public enum RepairStatus {
        RECEIVED,      // รับงานแล้ว
        DIAGNOSING,    // กำลังวิเคราะห์อาการ
        WAIT_PARTS,    // รออะไหล่
        REPAIRING,     // กำลังซ่อม
        QA,            // ตรวจสอบคุณภาพ
        DONE           // เสร็จสิ้น
    }

    // ===== ฟิลด์ลูกค้าทั่วไป =====
    private String id;                 // UUID string
    private String name;
    private String phone;
    private String plate;
    private String province;
    private String bikeModel;          // optional
    private LocalDate receivedDate;    // วันที่รับรถ (optional)
    private LocalDateTime registeredAt;// เวลาสร้างเรคอร์ดในระบบ (optional)

    // สถานะรวม (ใช้โชว์บน Dashboard/รายการ)
    private RepairStatus status;

    // อาการจากลูกค้าตอนลงทะเบียน (ข้อมูลดิบ)
    private String symptom;

    // ===== งานซ่อมที่ persist จริง (ชิ้นส่วน/ยอดรวม/โน้ต/เวลาอัปเดต) =====
    private RepairInfo repair;

    // ===== getters / setters =====
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getBikeModel() { return bikeModel; }
    public void setBikeModel(String bikeModel) { this.bikeModel = bikeModel; }

    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public RepairStatus getStatus() { return status; }
    public void setStatus(RepairStatus status) { this.status = status; }

    public String getSymptom() { return symptom; }
    public void setSymptom(String symptom) { this.symptom = symptom; }

    public RepairInfo getRepair() { return repair; }
    public void setRepair(RepairInfo repair) { this.repair = repair; }

    // ----------------------------------------------------------------------
    // RepairInfo: ข้อมูลงานซ่อมที่ต้อง Persist
    // ----------------------------------------------------------------------
    public static class RepairInfo implements Serializable {
        /** รายการอะไหล่ที่ใช้ */
        private List<PartItem> parts;
        /** ยอดรวมทั้งหมดของอะไหล่ (คำนวณเก็บไว้ เผื่อเอาไปโชว์เร็ว ๆ) */
        private Double grandTotal;
        /** บันทึกงาน/สิ่งที่ทำ (ใช้แทน notes ของหน้า Repair Details) */
        private String notes;
        /** เวลาที่อัปเดตงานล่าสุด (Repository จะ set ให้ตอน updateRepair) */
        private LocalDateTime lastUpdated;

        public List<PartItem> getParts() { return parts; }
        public void setParts(List<PartItem> parts) { this.parts = parts; }
        public Double getGrandTotal() { return grandTotal; }
        public void setGrandTotal(Double grandTotal) { this.grandTotal = grandTotal; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    // ----------------------------------------------------------------------
    // PartItem: แถวอะไหล่แต่ละชิ้น
    // ----------------------------------------------------------------------
    public static class PartItem implements Serializable {
        private String partName;
        private Integer quantity;
        private String unit;
        private Double unitPrice;

        public PartItem() {}
        public PartItem(String partName, Integer quantity, String unit, Double unitPrice) {
            this.partName = partName;
            this.quantity = quantity;
            this.unit = unit;
            this.unitPrice = unitPrice;
        }

        public String getPartName() { return partName; }
        public void setPartName(String partName) { this.partName = partName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public Double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }
    }
}
