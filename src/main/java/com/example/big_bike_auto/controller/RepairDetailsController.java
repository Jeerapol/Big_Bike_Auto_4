package com.example.big_bike_auto.controller;


import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * หน้ารายละเอียดงานซ่อม (ตัวอย่าง)
 */
public class RepairDetailsController {

    @FXML private Label lbInfo;

    @FXML
    private void initialize() {
        lbInfo.setText("รายละเอียดงานซ่อม (ตัวอย่าง) - ยังไม่เชื่อมฐานข้อมูล");
    }
}
