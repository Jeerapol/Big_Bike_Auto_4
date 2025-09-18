package com.example.big_bike_auto.parts;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * เอกสาร "การจองอะไหล่" ผูกกับงานซ่อม (jobId)
 * - reservationId: ไอดีของรายการจอง
 * - jobId: อ้างอิงงานซ่อม (UUID ของ Customer.id)
 * - sku: อะไหล่ที่จอง
 * - qty: จำนวนที่จอง
 * - status: RESERVED หรือ CONSUMED หรือ RELEASED
 */
public class PartReservation {

    public enum Status { RESERVED, CONSUMED, RELEASED }

    private UUID reservationId;
    private UUID jobId;
    private String sku;
    private int qty;
    private Status status;
    private LocalDateTime createdAt;

    public PartReservation() {}

    public PartReservation(UUID reservationId, UUID jobId, String sku, int qty, Status status, LocalDateTime createdAt) {
        this.reservationId = reservationId;
        this.jobId = jobId;
        this.sku = sku;
        this.qty = qty;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getReservationId() { return reservationId; }
    public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
