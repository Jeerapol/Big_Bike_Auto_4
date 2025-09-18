package com.example.big_bike_auto.parts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * PartService = กฏธุรกิจของอะไหล่
 * รวมฟังก์ชันสำคัญ: reserve/consume/release/receive/adjust + reorder suggestions
 *
 * คำอธิบายแต่ละเมธอด (Junior-friendly):
 * - reserve: จองของสำหรับงานซ่อม (เพิ่ม reserved)
 * - consume: เบิกของจากของที่จอง (ลด reserved และ onHand)
 * - release: คืนของที่จอง (ลด reserved)
 * - receive: รับของเข้าคลัง (เพิ่ม onHand และอัปเดต lastCost/supplier)
 * - adjust: ปรับยอดคงเหลือ (กรณีพิเศษ เช่น นับสต็อกต่าง, เสียหาย)
 */
public class PartService {

    private final PartRepository partRepo;
    private final PartOrderRepository orderRepo;

    public PartService(PartRepository partRepo, PartOrderRepository orderRepo) {
        this.partRepo = partRepo;
        this.orderRepo = orderRepo;
    }

    // -------- Stock Movements --------

    public synchronized PartReservation reserve(UUID jobId, String sku, int qty) {
        if (jobId == null) throw new IllegalArgumentException("jobId is required");
        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("sku is required");
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");

        Part p = partRepo.findBySku(sku).orElseThrow(() -> new IllegalArgumentException("Part not found: " + sku));

        int available = p.getAvailable();
        if (available < qty) {
            throw new IllegalStateException("Not enough available stock. Available=" + available);
        }

        p.setReserved(p.getReserved() + qty);
        partRepo.upsert(p);

        PartReservation r = new PartReservation(UUID.randomUUID(), jobId, sku, qty, PartReservation.Status.RESERVED, LocalDateTime.now());
        // ในเวอร์ชันไฟล์ง่ายๆ เราไม่เก็บประวัติ reservation ลงไฟล์แยกเพื่อย่อโค้ด
        // (ถ้าต้องการเก็บ log เพิ่ม สามารถเพิ่ม Repository ใหม่ได้)
        return r;
    }

    public synchronized void consume(UUID jobId, String sku, int qty) {
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");
        Part p = partRepo.findBySku(sku).orElseThrow(() -> new IllegalArgumentException("Part not found: " + sku));
        if (p.getReserved() < qty) throw new IllegalStateException("Reserved not enough to consume");
        if (p.getOnHand() < qty) throw new IllegalStateException("OnHand not enough to consume");

        p.setReserved(p.getReserved() - qty);
        p.setOnHand(p.getOnHand() - qty);
        partRepo.upsert(p);
    }

    public synchronized void release(UUID jobId, String sku, int qty) {
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");
        Part p = partRepo.findBySku(sku).orElseThrow(() -> new IllegalArgumentException("Part not found: " + sku));
        if (p.getReserved() < qty) throw new IllegalStateException("Reserved not enough to release");

        p.setReserved(p.getReserved() - qty);
        partRepo.upsert(p);
    }

    public synchronized void receive(String sku, int qty, BigDecimal unitCost, String supplier) {
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");
        Part p = partRepo.findBySku(sku).orElseThrow(() -> new IllegalArgumentException("Part not found: " + sku));
        p.setOnHand(p.getOnHand() + qty);
        if (unitCost != null) p.setLastCost(unitCost);
        if (supplier != null && !supplier.isBlank()) p.setSupplier(supplier);
        partRepo.upsert(p);
    }

    public synchronized void adjust(String sku, int delta, String reason) {
        Part p = partRepo.findBySku(sku).orElseThrow(() -> new IllegalArgumentException("Part not found: " + sku));
        int newOnHand = p.getOnHand() + delta;
        if (newOnHand < 0) throw new IllegalStateException("Adjust would make negative onHand");
        p.setOnHand(newOnHand);
        partRepo.upsert(p);
    }

    // -------- Purchase Orders --------

    /** สร้างใบสั่งซื้อแบบง่ายจากรายการที่ต่ำกว่า minStock */
    public synchronized PartOrder buildReorderSuggestion(String supplierName, int targetQtyIfLow) {
        List<Part> parts = partRepo.findAll();
        PartOrder po = new PartOrder(UUID.randomUUID(), supplierName, LocalDate.now(), PartOrder.Status.DRAFT);
        for (Part p : parts) {
            int available = p.getAvailable();
            if (available < p.getMinStock()) {
                int orderQty = Math.max(targetQtyIfLow, p.getMinStock() - available);
                po.getLines().add(new PartOrder.Line(p.getSku(), p.getName(), orderQty, p.getLastCost()));
            }
        }
        orderRepo.upsert(po);
        return po;
    }

    /** เปลี่ยนสถานะ PO และถ้ารับเข้า (RECEIVED) จะไปเพิ่มสต็อก */
    public synchronized void updateOrderStatus(UUID orderId, PartOrder.Status newStatus) {
        PartOrder po = orderRepo.findById(orderId).orElseThrow(() -> new IllegalArgumentException("PO not found"));
        if (po.getStatus() == PartOrder.Status.CANCELED || po.getStatus() == PartOrder.Status.RECEIVED) {
            throw new IllegalStateException("Cannot modify a finalized PO");
        }
        po.setStatus(newStatus);
        orderRepo.upsert(po);

        if (newStatus == PartOrder.Status.RECEIVED) {
            // รับเข้าจริง: บันทึกเข้า stock
            for (PartOrder.Line ln : po.getLines()) {
                receive(ln.getSku(), ln.getQty(), ln.getUnitCost(), po.getSupplier());
            }
        }
    }
}
