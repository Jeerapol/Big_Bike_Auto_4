package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.Router;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;

/**
 * ควบคุมหน้า Home (เมนู + พื้นที่ contentRoot)
 * - ผูกเมนูไปยัง Router.navigate(...)
 * - ให้ getContentRoot() สำหรับ Router วางหน้าใหม่ทับ
 * - หลังเริ่มต้น (initialize) จะเปิด Dashboard เป็นค่าเริ่มต้น
 */
public class HomeController {

    @FXML private AnchorPane contentRoot;

    @FXML private MenuItem menuRegister;
    @FXML private MenuItem menuRepairDetails;
    @FXML private MenuItem menuInventory;
    @FXML private MenuItem menuRepairList;

    private Router router;

    /** ให้ Router เข้าถึง contentRoot ได้ */
    public AnchorPane getContentRoot() {
        return contentRoot;
    }

    @FXML
    private void initialize() {
        // สร้าง Router โดยอ้างอิง HomeController นี้
        router = new Router(this);

        // ผูกเมนูไปยังหน้าเป้าหมาย (กดเมนูเมื่อไหร่ค่อยไปหน้าอื่น)
        if (menuRegister != null)       menuRegister.setOnAction(e -> router.navigate("register"));
        if (menuRepairDetails != null)  menuRepairDetails.setOnAction(e -> router.navigate("repairDetails"));
        if (menuInventory != null)      menuInventory.setOnAction(e -> router.navigate("inventory"));
        if (menuRepairList != null)     menuRepairList.setOnAction(e -> router.navigate("repairList"));

        // ✅ เปิด Dashboard เป็นค่าเริ่มต้น (อย่าไปหน้า repairList ถ้ายังไม่มีไฟล์)
        router.navigate("dashboard");
    }
}
