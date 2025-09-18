package com.example.big_bike_auto;

import com.example.big_bike_auto.controller.HomeController;
import com.example.big_bike_auto.controller.LoginController;
import com.example.big_bike_auto.router.Router;
import com.example.big_bike_auto.router.RouterHub;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * HelloApplication:
 * 1) เปิด Login.fxml เป็นหน้าแรก
 * 2) เมื่อ Login สำเร็จ -> โหลด Home.fxml, set RouterHub, แล้ว navigate ไปหน้า dashboard
 */
public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1) โหลดหน้า Login ก่อน
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/com/example/big_bike_auto/login-view.fxml"));
        Parent loginRoot = loginLoader.load();
        LoginController loginController = loginLoader.getController();

        Scene scene = new Scene(loginRoot);
        stage.setScene(scene);
        stage.setTitle("Big Bike Auto - Sign in");
        stage.show();

        // 2) ติด callback เมื่อ login สำเร็จ
        loginController.setOnLoginSuccess(user -> {
            try {
                // โหลด Home.fxml
                FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/com/example/big_bike_auto/Home.fxml"));
                Parent homeRoot = homeLoader.load();
                HomeController homeController = homeLoader.getController();

                // สร้าง Router + set ให้ RouterHub
                Router router = new Router(homeController);
                RouterHub.setRouter(router);

                // เปลี่ยน Scene เป็น Home
                stage.setTitle("Big Bike Auto");
                stage.setScene(new Scene(homeRoot));
                stage.show();

                // นำทางหน้าแรกหลัง stage พร้อม
                Platform.runLater(() -> router.navigate("dashboard"));

            } catch (Exception ex) {
                ex.printStackTrace();
                // ถ้าโหลด Home ล้มเหลว กลับไปหน้า Login พร้อมแจ้ง
                loginController.showError("ไม่สามารถโหลดหน้า Home", ex);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
