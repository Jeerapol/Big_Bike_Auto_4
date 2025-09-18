package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.RouterHub;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

/**
 * HomeController (หน้าแม่) มี contentRoot สำหรับสลับหน้า child ผ่าน Router
 * - ใช้ RouterHub.getRouter() เพื่อเปลี่ยนหน้า
 * - รองรับเมนู onAction จาก Home.fxml
 */
public class HomeController {

    @FXML
    private AnchorPane contentRoot; // พื้นที่วางเนื้อหา child

    /** เรียกโดย FXMLLoader หลังฉากโหลด */
    @FXML
    private void initialize() {
        // เปิดหน้าแรกตามต้องการ (เช่น dashboard) ถ้าอยาก
        // RouterHub.getRouter().navigate("dashboard");
    }

    /** เปิดหน้าลงทะเบียน */
    @FXML
    private void goRegister() {
        RouterHub.getRouter().navigate("register");
    }

    /** เปิดหน้ารายละเอียดงานซ่อม (ตัวอย่างเดิม) */
    @FXML
    private void goRepairDetails() {
        RouterHub.getRouter().navigate("repairDetails");
    }

    /** เปิดหน้ารายการซ่อม */
    @FXML
    private void goRepairList() {
        RouterHub.getRouter().navigate("repairList");
    }

    /** เปิดหน้าสต็อกอะไหล่ (เมนูใหม่) */
    @FXML
    private void goStock() {
        RouterHub.getRouter().navigate("stock");
    }

    /** เปิดหน้าใบสั่งซื้อ (เมนูใหม่) */
    @FXML
    private void goOrders() {
        RouterHub.getRouter().navigate("orders");
    }

    /** ให้ Router เข้าถึงพื้นที่ contentRoot ได้ */
    public AnchorPane getContentRoot() {
        return contentRoot;
    }
}
