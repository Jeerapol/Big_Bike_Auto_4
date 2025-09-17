package com.example.big_bike_auto.repair;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * RepairJob:
 * โมเดลงานซ่อม (Serializable เพื่อเก็บเป็นไฟล์ .ser ต่อรายการ)
 * - ออกแบบให้ "พอเพียง" สำหรับหน้า Register และเปิดต่อใน Repair Details
 */
public class RepairJob implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;                 // UUID ของงานซ่อม
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String vehiclePlate;

    private RepairStatus status;       // ใช้ Enum เดียวกันทั้งระบบ
    private String initialNote;        // โน้ตจากหน้า Register (ถ้ามี)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RepairJob() {}

    // ---------- Getters/Setters ----------
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }

    public RepairStatus getStatus() { return status; }
    public void setStatus(RepairStatus status) { this.status = status; }

    public String getInitialNote() { return initialNote; }
    public void setInitialNote(String initialNote) { this.initialNote = initialNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ---------- Utility ----------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepairJob)) return false;
        RepairJob that = (RepairJob) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "RepairJob{" +
                "id='" + id + '\'' +
                ", customerName='" + customerName + '\'' +
                ", vehiclePlate='" + vehiclePlate + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
