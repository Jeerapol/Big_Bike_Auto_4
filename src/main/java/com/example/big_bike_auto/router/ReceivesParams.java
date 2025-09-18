package com.example.big_bike_auto.router;

import java.util.Map;

/**
 * ใช้กับ Controller ใดๆ ที่ต้อง "รับพารามิเตอร์" ตอนถูกนำทางมาหน้า
 * Router จะตรวจว่า controller implements ReceivesParams หรือไม่
 * ถ้าใช่จะเรียก onParams(params) ให้อัตโนมัติ
 */
public interface ReceivesParams {
    /**
     * เรียกทันทีหลังโหลด FXML และก่อนแสดงผล
     * @param params key-value ที่ Router ส่งมา เช่น {"customerId": "..."}
     */
    void onParams(Map<String, Object> params);
}
