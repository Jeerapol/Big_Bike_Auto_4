package com.example.big_bike_auto.router;

import java.util.Map;


public interface ReceivesParams {
    /**
     * เรียกทันทีหลังโหลด FXML และก่อนแสดงผล
     * @param params key-value ที่ Router ส่งมา เช่น {"customerId": "..."}
     */
    void onParams(Map<String, Object> params);
}
