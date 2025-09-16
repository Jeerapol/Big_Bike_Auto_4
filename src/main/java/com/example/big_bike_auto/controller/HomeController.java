package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.Router;
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

    private Router router;

    @FXML
    private void initialize() {
        // สร้าง Router ผูกกับ Home นี้ แล้วเผยแพร่ผ่าน RouterHub
        router = new Router(this);
        RouterHub.setRouter(router);

        // เปิดหน้าแรก (dashboard)
        router.navigate("dashboard");
    }

    /** เปิดหน้าลงทะเบียน */
    @FXML
    private void goRegister() {
        RouterHub.getRouter().navigate("register");
    }

    /** เปิดหน้ารายละเอียดงานซ่อม */
    @FXML
    private void goRepairDetails() {
        RouterHub.getRouter().navigate("repairDetails");
    }

    /** เปิดหน้าสินค้าคงคลัง */
    @FXML
    private void goInventory() {
        // ตอนนี้ยังไม่มี inventory.fxml จริง → พาไป dashboard ชั่วคราว
        RouterHub.getRouter().navigate("dashboard");
    }

    /** เปิดหน้ารายการซ่อม */
    @FXML
    private void goRepairList() {
        RouterHub.getRouter().navigate("repairList");
    }

    /** ให้ Router เข้าถึงพื้นที่ contentRoot ได้ */
    public AnchorPane getContentRoot() {
        return contentRoot;
    }
}
