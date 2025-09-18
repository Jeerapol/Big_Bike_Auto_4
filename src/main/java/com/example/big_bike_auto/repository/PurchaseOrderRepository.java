package com.example.big_bike_auto.repository;

import com.example.big_bike_auto.common.JsonUtil;
import com.example.big_bike_auto.model.PurchaseOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * PurchaseOrderRepository:
 * - จัดการอ่าน/เขียน purchase_orders.json
 * - ใช้ JsonUtil ที่รองรับ LocalDate แล้ว
 */
public class PurchaseOrderRepository {

    private static final String FILE = "data/purchase_orders.json";

    public List<PurchaseOrder> findAll() {
        // กันไฟล์หาย -> สร้าง []
        JsonUtil.ensureJsonArrayFile(FILE);
        return new ArrayList<>(JsonUtil.readList(FILE, PurchaseOrder.class));
    }

    public void saveAll(List<PurchaseOrder> orders) {
        JsonUtil.ensureJsonArrayFile(FILE);
        JsonUtil.writeList(FILE, orders != null ? orders : List.of());
    }
}
