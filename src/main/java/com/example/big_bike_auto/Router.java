package com.example.big_bike_auto;

import com.example.big_bike_auto.controller.HomeController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Router สำหรับสลับหน้าในพื้นที่ contentRoot ของ Home
 * - ใช้ชื่อเส้นทาง (route) เข้าใจง่าย เช่น "dashboard", "register"
 * - map ไปยังไฟล์ FXML ใน resources: /com/example/big_bike_auto/ui/*.fxml
 * - ป้องกัน error ด้วย try/catch และแสดง Alert ที่อ่านง่าย
 */
public class Router {
    private final HomeController home;
    private final Map<String, String> routes = new HashMap<>();

    public Router(HomeController home) {
        this.home = home;

        // กำหนด mapping ชื่อ route -> path ของ FXML
        routes.put("dashboard", "/com/example/big_bike_auto/ui/Dashboard.fxml");
        routes.put("register",  "/com/example/big_bike_auto/ui/register.fxml");
        routes.put("repairDetails", "/com/example/big_bike_auto/ui/RepairDetails.fxml");
        routes.put("inventory", "/com/example/big_bike_auto/ui/inventory.fxml");
        routes.put("repairList", "/com/example/big_bike_auto/ui/Dashboard.fxml");
    }

    /**
     * นำทางไปยังหน้าเป้าหมายในพื้นที่ contentRoot
     * @param route ชื่อหน้า (เช่น "dashboard")
     */
    public void navigate(String route) {
        String fxmlPath = routes.get(route);
        if (fxmlPath == null) {
            showError("ไม่พบเส้นทาง: " + route);
            return;
        }
        try {
            URL url = Objects.requireNonNull(getClass().getResource(fxmlPath), "ไม่พบ resource: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(url);
            Node content = loader.load();

            AnchorPane contentRoot = home.getContentRoot();
            contentRoot.getChildren().setAll(content);
            AnchorPane.setTopAnchor(content, 0.0);
            AnchorPane.setRightAnchor(content, 0.0);
            AnchorPane.setBottomAnchor(content, 0.0);
            AnchorPane.setLeftAnchor(content, 0.0);

        } catch (IOException ex) {
            ex.printStackTrace();
            showError("โหลดหน้าไม่สำเร็จ: " + fxmlPath + "\nสาเหตุ: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("เกิดข้อผิดพลาด");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
