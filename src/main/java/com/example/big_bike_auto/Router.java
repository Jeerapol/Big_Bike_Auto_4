package com.example.big_bike_auto;

import com.example.big_bike_auto.controller.HomeController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.util.Objects;

/**
 * Router สำหรับเปลี่ยนหน้า (View) ภายใน contentRoot ของ Home
 * - ป้องกันโค้ดซ้ำเวลา navigate
 * - มี error handling แสดง Alert เมื่อโหลด FXML ล้มเหลว
 */
public class Router {

    private final HomeController homeController;

    public Router(HomeController homeController) {
        this.homeController = homeController;
    }

    /**
     * เปลี่ยนหน้าโดยการโหลด FXML ตาม key ที่กำหนด
     * @param viewKey "register", "repairDetails", "inventory", "repairList"
     */
    public void navigate(String viewKey) {
        String fxmlPath = switch (viewKey) {
            case "register" -> "/com/example/app/views/Register.fxml";
            case "repairDetails" -> "/com/example/app/views/RepairDetails.fxml";
            case "inventory" -> "/com/example/app/views/Inventory.fxml";
            case "repairList" -> "/com/example/app/views/RepairList.fxml";
            default -> null;
        };

        if (fxmlPath == null) {
            showError("ไม่รู้จักหน้าที่ต้องการไป: " + viewKey);
            return;
        }

        try {
            var loader = new FXMLLoader(Objects.requireNonNull(App.class.getResource(fxmlPath)));
            Node content = loader.load();

            // วาง Node ใหม่ลงใน contentRoot แล้ว "anchor" รอบด้านให้เต็มพื้นที่
            AnchorPane contentRoot = homeController.getContentRoot();
            contentRoot.getChildren().setAll(content);
            AnchorPane.setTopAnchor(content, 0.0);
            AnchorPane.setRightAnchor(content, 0.0);
            AnchorPane.setBottomAnchor(content, 0.0);
            AnchorPane.setLeftAnchor(content, 0.0);

        } catch (IOException ex) {
            ex.printStackTrace();
            showError("โหลดหน้าไม่สำเร็จ: " + fxmlPath + "\n" + ex.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("เกิดข้อผิดพลาด");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
