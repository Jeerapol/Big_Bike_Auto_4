package com.example.big_bike_auto;

import com.example.big_bike_auto.controller.HomeController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * แอปหลัก: โหลด Home.fxml -> ติดตั้ง Router -> เปิดหน้าแรก (dashboard)
 */
public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1) โหลด Home.fxml จาก resources (เส้นทางต้องตรงกับที่วางไฟล์)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/big_bike_auto/Home.fxml"));
        Parent root = loader.load();

        // 2) ดึง HomeController แล้วสร้าง Router
        HomeController home = loader.getController();
        Router router = new Router(home);

        // 3) ติดตั้ง Router เข้า Hub ให้ controller อื่น ๆ เรียกผ่าน RouterHub ได้
        RouterHub.setRouter(router);

        // 4) เปิดหน้าต่าง
        stage.setTitle("Big_Bike Auto");
        stage.setScene(new Scene(root));
        stage.show();

        // 5) นำทางไปหน้าแรก (ต้องทำหลัง setRouter แล้วเท่านั้น)
        RouterHub.getRouter().navigate("dashboard");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
