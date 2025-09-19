package com.example.big_bike_auto;

import com.example.big_bike_auto.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * HelloApplication:
 * 1) เปิดหน้า Login ก่อน
 * 2) เมื่อ Login สำเร็จ -> โหลด Home.fxml แล้วแสดง
 * 3) ปล่อยให้ HomeController.initialize() จัดการสร้าง Router + navigate("dashboard")
 *
 * หมายเหตุ:
 * - LoginController#setOnLoginSuccess ต้องรับ Consumer<String>
 *   ดังนั้น lambda ที่ส่งเข้ามาต้องมีพารามิเตอร์ String (username)
 */
public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // โหลดหน้า Login
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/com/example/big_bike_auto/login-view.fxml"));
        Parent loginRoot = loginLoader.load();
        LoginController loginController = loginLoader.getController();

        Scene loginScene = new Scene(loginRoot);
        stage.setTitle("Big Bike Auto - Login");
        stage.setScene(loginScene);
        stage.show();

        // ✅ ใช้ lambda ที่รับ String (username) ให้ตรงกับ Consumer<String>
        loginController.setOnLoginSuccess((String username) -> {
            try {
                // โหลดหน้า Home
                FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/com/example/big_bike_auto/Home.fxml"));
                Parent homeRoot = homeLoader.load();

                // แสดงหน้า Home
                Scene homeScene = new Scene(homeRoot);
                stage.setTitle("Big Bike Auto");
                stage.setScene(homeScene);
                stage.show();

                // ไม่ต้อง navigate ที่นี่ ปล่อยให้ HomeController.initialize() ทำ (ลดการซ้ำซ้อน)
            } catch (Exception ex) {
                ex.printStackTrace();
                loginController.showError("ไม่สามารถโหลดหน้า Home", ex);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
