package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.router.Router;     // ✅ ต้องมีบรรทัดนี้
import com.example.big_bike_auto.router.RouterHub;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;

/**
 * HomeController:
 * - เป็น Shell หลัก (Header + Menu + content area)
 * - สร้าง Router ด้วย HomeController แล้ว set เข้า RouterHub
 * - นำทางไปหน้าเริ่มต้น (dashboard)
 */
public class HomeController {

    @FXML private BorderPane root;

    @FXML
    private void initialize() {
        // ✅ สร้าง Router และผูกกับ Hub
        Router router = new Router(this);
        RouterHub.setRouter(router);

        // ✅ หน้าเริ่มต้น
        router.navigate("dashboard");
    }

    /** ให้ Router เรียกเพื่อวาง view ในตรงกลางของ shell */
    public void setContent(javafx.scene.Parent content) {
        root.setCenter(content);
    }

    // ===== Menu Actions =====

    @FXML
    private void goDashboard() { navigateSafely("dashboard"); }

    @FXML
    private void goRegister() { navigateSafely("register"); }

    @FXML
    private void goRepairDetails() { navigateSafely("repairDetails"); }

    @FXML
    private void goStock() { navigateSafely("inventory"); }

    @FXML
    private void goOrders() { navigateSafely("ordersPage"); }

    @FXML
    private void goLookup() { navigateSafely("lookup"); }

    private void navigateSafely(String page) {
        if (!RouterHub.isReady()) {
            throw new IllegalStateException("Router ยังไม่ได้ set! (ควรตั้งค่าใน HelloApplication ก่อน)");
        }
        RouterHub.getRouter().navigate(page);
    }
}
