package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.router.RouterHub;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

/**
 * HomeController:
 * - เป็น Shell หลัก (Header + Menu + contentRoot)
 * - ไม่สั่ง navigate ใน initialize แล้ว (ให้ HelloApplication เป็นคนสั่ง)
 */
public class HomeController {

    @FXML private BorderPane contentRoot;

    @FXML
    public void initialize() {
        // ไม่ navigate ที่นี่ เพื่อไม่ให้ชน RouterHub ยังไม่ได้ set
    }

    /** ให้ Router เรียกเพื่อวาง view ในตรงกลาง */
    public void setContent(Parent view) {
        contentRoot.setCenter(view);
    }

    // ===== Menu Actions =====

    @FXML
    private void goDashboard() {
        navigateSafely("dashboard");
    }

    @FXML
    private void goRegister() {
        navigateSafely("register");
    }

    @FXML
    private void goRepairDetails() {
        navigateSafely("repairDetails");
    }

    @FXML
    private void goStock() {
        navigateSafely("inventory");
    }

    @FXML
    private void goOrders() {
        navigateSafely("ordersPage");
    }

    @FXML
    private void goLookup() {
        navigateSafely("lookup");
    }

    private void navigateSafely(String page) {
        if (!RouterHub.isReady()) {
            throw new IllegalStateException("Router ยังไม่ได้ set! (ควรตั้งค่าใน HelloApplication ก่อน)");
        }
        RouterHub.getRouter().navigate(page);
    }
}
