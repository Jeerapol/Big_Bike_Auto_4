package com.example.big_bike_auto.controller;

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

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * LoginController
 * - รับค่า username/password จากฟอร์ม
 * - ตรวจสอบแบบง่าย (demo): ช่องว่างห้ามว่าง
 * - เมื่อล็อกอินสำเร็จ: เปลี่ยนไปหน้าหลัก (Home.fxml) ซึ่งจะเปิด Dashboard ให้อัตโนมัติ
 *
 * หมายเหตุ (Production):
 * - ควรเชื่อมต่อระบบ auth จริง (เช่น database / API) และ hash password
 * - เพิ่ม rate limiting / lockout ป้องกัน brute-force
 */
public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    private void onLogin(ActionEvent event) {
        String user = txtUsername != null ? txtUsername.getText().trim() : "";
        String pass = txtPassword != null ? txtPassword.getText().trim() : "";

        // Validation ง่าย ๆ: ห้ามว่าง
        if (user.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "กรอกข้อมูลไม่ครบ", "กรุณากรอก Username และ Password");
            return;
        }

        // TODO: แทนที่ด้วยการตรวจสอบจริง (เช่นเช็คกับฐานข้อมูล)
        boolean ok = true;

        if (ok) {
            // ไปหน้า Home (เมนู + content area)
            try {
                URL url = Objects.requireNonNull(getClass().getResource("/com/example/big_bike_auto/Home.fxml"), "ไม่พบ Home.fxml");
                Parent root = FXMLLoader.load(url);

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setTitle("Big Bike Auto");
                stage.setScene(new Scene(root));
                stage.centerOnScreen();
                stage.show();
                // หมายเหตุ: HomeController.initialize() จะเรียก router.navigate("dashboard") ให้เอง
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "เปิดหน้าหลักไม่สำเร็จ", e.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "เข้าสู่ระบบล้มเหลว", "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง");
        }
    }

    @FXML
    private void onRegister(ActionEvent event) throws IOException {
        // ไปหน้า Register (ถ้าจำเป็น)
        try {
            URL url = Objects.requireNonNull(getClass().getResource("/com/example/big_bike_auto/ui/register.fxml"), "ไม่พบ register.fxml");
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("Register");
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("เกิดข้อผิดพลาดในการโหลดหน้า Register: " + e.getMessage(), e);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg){
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
}
