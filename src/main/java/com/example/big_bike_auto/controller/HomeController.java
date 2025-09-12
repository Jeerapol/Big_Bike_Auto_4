package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.Router;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;

/**
 * Controller สำหรับหน้า Home (มีเมนูด้านบน + พื้นที่เนื้อหาอยู่ด้านล่าง)
 * - ทำหน้าที่เชื่อมเมนูไปยัง Router.navigate(...)
 */
public class HomeController {

    @FXML
    private AnchorPane contentRoot; // พื้นที่สำหรับใส่หน้าอื่น

    @FXML private MenuItem menuRegister;
    @FXML private MenuItem menuRepairDetails;
    @FXML private MenuItem menuInventory;
    @FXML private MenuItem menuRepairList;

    private Router router;

    /**
     * ให้ Router เข้าถึง contentRoot ของหน้า Home ได้
     */
    public AnchorPane getContentRoot() {
        return contentRoot;
    }

    /**
     * initialize() จะถูกเรียกหลังจากโหลด FXML เสร็จ
     * - ผูก event เมนูไปยัง Router
     * - เปิดหน้าเริ่มต้น (เช่น RepairList)
     */
    @FXML
    private void initialize() {
        // สร้าง Router พร้อมอ้างอิง HomeController ปัจจุบัน
        router = new Router(this);

        // ผูกเมนูไปยังปลายทาง
        menuRegister.setOnAction(e -> router.navigate("register"));
        menuRepairDetails.setOnAction(e -> router.navigate("repairDetails"));
        menuInventory.setOnAction(e -> router.navigate("inventory"));
        menuRepairList.setOnAction(e -> router.navigate("repairList"));

        // เปิดหน้าเริ่มต้น
        router.navigate("repairList");
    }
}
