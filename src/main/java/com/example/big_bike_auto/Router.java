package com.example.big_bike_auto;

import com.example.big_bike_auto.controller.HomeController;
import com.example.big_bike_auto.controller.RepairDetailsController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Router สำหรับสลับหน้าในพื้นที่ contentRoot ของ Home (Content-Root Navigation)
 * - ใช้กับ Scene/Stage เดิม ไม่เปิดหน้าต่างใหม่
 * - รองรับ navigate(route, controllerInit) เพื่อ inject ข้อมูลเข้า controller ก่อนแสดงผล
 * - มี toRepairDetails(UUID) เพื่อเปิดหน้า "รายละเอียดงานซ่อม" และส่ง jobId เข้า controller
 */
public class Router {
    private final HomeController home;
    private final Map<String, String> routes = new HashMap<>();

    public Router(HomeController home) {
        this.home = home;

        // mapping: route -> FXML path (ปรับเพิ่มตามหน้าในระบบ)
        routes.put("dashboard",     "/com/example/big_bike_auto/ui/Dashboard.fxml");
        routes.put("register",      "/com/example/big_bike_auto/ui/register.fxml");
        routes.put("repairDetails", "/com/example/big_bike_auto/ui/RepairDetails.fxml");
        routes.put("inventory",     "/com/example/big_bike_auto/ui/inventory.fxml");
        // TODO: เปลี่ยนปลายทางเมื่อมี RepairList จริง
        routes.put("repairList",    "/com/example/big_bike_auto/ui/RepairList.fxml");
    }

    /** นำทางแบบทั่วไป (ไม่ส่งค่าเข้า controller) */
    public void navigate(String route) {
        navigate(route, null);
    }

    /**
     * นำทาง + ส่งค่าเข้า controller
     * @param route ชื่อ route ตามที่ map ไว้
     * @param controllerInit callback รับ controller จริง (Object) ให้ผู้เรียก cast แล้วตั้งค่าได้
     */
    public void navigate(String route, Consumer<Object> controllerInit) {
        String fxmlPath = routes.get(route);
        if (fxmlPath == null) {
            showError("ไม่พบเส้นทาง: " + route);
            return;
        }
        try {
            URL url = Objects.requireNonNull(getClass().getResource(fxmlPath), "ไม่พบ resource: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(url);
            Node content = loader.load();

            // ส่งค่าให้ controller ถ้ามี
            if (controllerInit != null) {
                Object controller = loader.getController();
                controllerInit.accept(controller);
            }

            // สลับเนื้อหาใน contentRoot ของ Home
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

    /** เปิดหน้า "รายละเอียดงานซ่อม" พร้อม inject jobId เข้า RepairDetailsController */
    public void toRepairDetails(UUID jobId) {
        navigate("repairDetails", controller -> {
            if (controller instanceof RepairDetailsController ctrl) {
                ctrl.loadJob(jobId); // ต้องเป็น public ใน RepairDetailsController
            } else {
                throw new IllegalStateException("Unexpected controller for repairDetails: " + controller);
            }
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("เกิดข้อผิดพลาด");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
