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

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    private static final String HELLO_FXML = "/com/example/big_bike_auto/hello-view.fxml";


    @FXML
    public void onLogin(ActionEvent event){
        String u = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        String p = txtPassword.getText() == null ? "" : txtPassword.getText();


        if (u.isEmpty() || p.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Invalid input", "Please enter username and password.");
            return;
        }

        if (u.equals("admin") && p.equals("1234")) {
            try {
                switchToHello(event);
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Navigation error", "Cannot open hello view: " + e.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Access denied", "ชื่อผู้ใช้หรือรหัสไม่ถูกต้อง");
            if (txtPassword != null) txtPassword.clear(); // ล้างรหัสผ่านเพื่อความปลอดภัย
        }
    }


    private void switchToHello(ActionEvent event) throws IOException {
        URL fxml = getClass().getResource(HELLO_FXML);
        if (fxml == null) {
            throw new IOException("FXML not found at: " + HELLO_FXML);
        }
        Parent root = FXMLLoader.load(fxml);
        Scene scene = new Scene(root, 600, 400);

        // ดึง Stage ปัจจุบันจากปุ่มที่กด
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.centerOnScreen();
    }


    private void showAlert(Alert.AlertType type, String title, String msg){
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
}
