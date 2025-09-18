package com.example.big_bike_auto.service;

import com.example.big_bike_auto.parts.Part;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * ReorderService
 * - คำนวณคำแนะนำสั่งซื้อจากรายการอะไหล่
 * - สูตรพื้นฐาน: ถ้า Available < Min -> แนะนำสั่ง
 * - เป้าหมายเติมให้ถึง targetLevel = max(2*Min, Min + safetyStock)
 * - เคารพ MOQ และ PackSize
 *
 * Junior-friendly:
 * - ฟังก์ชันแยกย่อยชัดเจน: คำนวณ available, target, ปัดแพ็ค
 *
 * Senior perspective:
 * - ออกแบบ pure function เพื่อทดสอบง่าย
 * - ใช้ BigDecimal ในการคำนวณเงินเพื่อเลี่ยง floating error
 */
public class ReorderService {

    public static class Suggestion {
        public final String sku;
        public final String name;
        public final String unit;
        public final String supplier;
        public final int available;
        public final int minStock;
        public final int moq;       // ขั้นต่ำที่ต้องสั่ง
        public final int packSize;  // แพ็คละกี่ชิ้น
        public final int suggestQty;
        public final BigDecimal lastCost;
        public final BigDecimal subTotal;
        public final String reason;

        public Suggestion(String sku, String name, String unit, String supplier,
                          int available, int minStock, int moq, int packSize,
                          int suggestQty, BigDecimal lastCost, BigDecimal subTotal, String reason) {
            this.sku = sku;
            this.name = name;
            this.unit = unit;
            this.supplier = supplier;
            this.available = available;
            this.minStock = minStock;
            this.moq = moq;
            this.packSize = packSize;
            this.suggestQty = suggestQty;
            this.lastCost = lastCost;
            this.subTotal = subTotal;
            this.reason = reason;
        }
    }

    /**
     * คำนวณคำแนะนำสำหรับทุก Part
     * @param parts รายการอะไหล่
     * @param safetyStock จำนวนสำรองความเสี่ยง (หน่วยชิ้น)
     * @return รายการ Suggestion ที่ควรซื้อ
     */
    public List<Suggestion> buildSuggestions(List<Part> parts, int safetyStock) {
        List<Suggestion> out = new ArrayList<>();
        for (Part p : parts) {
            int available = Math.max(0, p.getOnHand() - p.getReserved());
            int min = Math.max(0, p.getMinStock());
            if (available >= min) {
                // ไม่ต้องสั่ง เพราะของพอแล้ว
                continue;
            }

            int targetLevel = Math.max(min * 2, min + safetyStock);
            int deficit = Math.max(0, targetLevel - available);

            int moq = Math.max(1, optInt(p.getMoq(), 1));
            int pack = Math.max(1, optInt(p.getPackSize(), 1));

            int qty = Math.max(deficit, moq);
            qty = ceilToPack(qty, pack); // ปัดขึ้นตามขนาดแพ็ค

            BigDecimal cost = p.getLastCost() != null ? p.getLastCost() : BigDecimal.ZERO;
            BigDecimal sub = cost.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

            String reason = "Available(" + available + ") < Min(" + min + "), target=" + targetLevel;

            out.add(new Suggestion(
                    p.getSku(),
                    p.getName(),
                    p.getUnit(),
                    nvl(p.getSupplier(), "UNKNOWN"),
                    available,
                    min,
                    moq,
                    pack,
                    qty,
                    cost,
                    sub,
                    reason
            ));
        }
        // จัดเรียงให้อ่านง่าย: supplier -> SKU
        out.sort(Comparator.comparing((Suggestion s) -> s.supplier).thenComparing(s -> s.sku));
        return out;
    }

    private static int ceilToPack(int qty, int pack) {
        // ปัดขึ้นเป็นจำนวนเท่าของ pack (เช่น pack=5, qty=7 -> 10)
        if (pack <= 1) return qty;
        int k = qty / pack;
        if (qty % pack != 0) k++;
        return k * pack;
    }

    private static int optInt(Integer v, int def) {
        return v == null ? def : v;
    }

    private static String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
