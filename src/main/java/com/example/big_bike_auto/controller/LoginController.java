package com.example.big_bike_auto.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * LoginController:
 * - อ่าน username/password จากฟอร์ม
 * - ตรวจสอบแบบง่าย (ตัวอย่าง): admin / 1234
 * - ถ้าสำเร็จ -> เรียก onLoginSuccess.accept(username)
 *
 * Junior Tips:
 * - fx:id ต้องตรงกับ FXML
 * - เมธอด onAction="#onLogin" ต้องมี @FXML
 *
 * Senior Notes:
 * - โค้ดนี้ mock การตรวจสอบ (hard-coded)
 * - แนะนำในโปรดักชัน: อ่าน users.json + bcrypt/argon2 + policy lockout + rate limit
 */
public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btn_login;

    private Consumer<String> onLoginSuccess;

    /** App จะเซ็ต callback นี้จาก HelloApplication */
    public void setOnLoginSuccess(Consumer<String> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    @FXML
    public void initialize() {
        // ใส่ default focus
        txtUsername.requestFocus();
    }

    @FXML
    private void onLogin() {
        String u = safe(txtUsername.getText());
        String p = safe(txtPassword.getText());

        // ตรวจความครบถ้วน
        if (u.isBlank() || p.isBlank()) {
            showWarn("ข้อมูลไม่ครบ", "กรุณากรอก Username และ Password");
            return;
        }

        // ===== ตัวอย่างตรวจแบบง่ายมาก (ควรเปลี่ยนเป็นอ่านจากฐานข้อมูล/ไฟล์ + hash) =====
        boolean ok = Objects.equals(u, "admin") && Objects.equals(p, "1234");

        if (!ok) {
            showWarn("เข้าสู่ระบบไม่สำเร็จ", "Username หรือ Password ไม่ถูกต้อง");
            return;
        }

        if (onLoginSuccess != null) {
            onLoginSuccess.accept(u); // callback กลับไปที่ HelloApplication
        }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    public void showError(String title, Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(String.valueOf(ex.getMessage()));
        TextArea ta = new TextArea(stack(ex));
        ta.setEditable(false);
        ta.setWrapText(false);

        VBox.setVgrow(ta, Priority.ALWAYS);
        a.getDialogPane().setExpandableContent(ta);
        a.showAndWait();
    }

    private void showWarn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String stack(Throwable t) {
        StringBuilder sb = new StringBuilder();
        while (t != null) {
            sb.append(t).append("\n");
            for (StackTraceElement el : t.getStackTrace()) sb.append("  at ").append(el).append("\n");
            t = t.getCause();
            if (t != null) sb.append("Caused by: ");
        }
        return sb.toString();
    }
}
