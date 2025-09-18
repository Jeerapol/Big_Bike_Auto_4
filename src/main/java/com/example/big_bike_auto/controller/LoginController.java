package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.Router;
import com.example.big_bike_auto.RouterHub;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

/**
 * LoginController
 * - รับค่า username/password จากฟอร์ม
 * - ตรวจสอบแบบง่าย (demo): ช่องว่างห้ามว่าง
 * - เมื่อล็อกอินสำเร็จ: โหลด Home.fxml -> ตั้งค่า Router -> นำทางไป Dashboard
 *
 * หมายเหตุ (Production):
 * - ควรตรวจสอบกับระบบ auth จริง (DB/API) + hash password
 * - เพิ่ม rate limiting / lockout ป้องกัน brute force
 */
public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    /** กดปุ่ม Sign in */
    @FXML
    private void onLogin(ActionEvent event) {
        final String user = txtUsername != null ? txtUsername.getText().trim() : "";
        final String pass = txtPassword != null ? txtPassword.getText().trim() : "";

        // Validation ง่าย ๆ: ห้ามว่าง
        if (user.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "กรอกข้อมูลไม่ครบ", "กรุณากรอก Username และ Password");
            return;
        }

        // TODO: แทนที่ด้วยการตรวจสอบจริง (เช่น DB/API)
        boolean ok = true;

        if (!ok) {
            showAlert(Alert.AlertType.ERROR, "เข้าสู่ระบบล้มเหลว", "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง");
            return;
        }

        try {
            // 1) โหลด Home.fxml แบบ non-static เพื่อให้ได้ controller
            URL url = Objects.requireNonNull(
                    getClass().getResource("/com/example/big_bike_auto/Home.fxml"),
                    "ไม่พบ Home.fxml"
            );
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            // 2) ดึง HomeController เพื่อตั้งค่า Router
            com.example.big_bike_auto.controller.HomeController home =
                    loader.getController();

            // 3) ตั้งค่า Router เข้า Hub (ต้องทำก่อน navigate)
            RouterHub.setRouter(new Router(home));

            // 4) สลับ Scene ไปยังหน้า Home
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Big Bike Auto");
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

            // 5) นำทางไป Dashboard (ตอนนี้มี Router แล้ว)
            RouterHub.getRouter().navigate("dashboard");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "เปิดหน้าหลักไม่สำเร็จ", e.getMessage());
        }
    }

    /** (ถ้ามีปุ่ม Register ก็ทำคล้ายกัน) */
    @FXML
    private void onRegister(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "ยังไม่เปิดใช้งาน", "หน้านี้ยังไม่พร้อมใช้งานในเดโม");
    }

    /** แสดง Alert ทั่วไป */
    private void showAlert(Alert.AlertType type, String title, String msg){
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
}
