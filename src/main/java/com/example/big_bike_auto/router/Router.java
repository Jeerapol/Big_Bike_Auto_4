package com.example.big_bike_auto.router;

import com.example.big_bike_auto.controller.HomeController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.util.HashMap;
import java.util.Map;

/**
 * Router:
 * - แปลง key เป็นพาธ FXML แล้วโหลด
 * - เซ็ต content ผ่าน HomeController.setContent(...)
 * - รองรับการส่งพารามิเตอร์ให้ controller ด้วย ReceivesParams
 */
public class Router {

    private final HomeController home;

    private static final Map<String, String> ROUTES = new HashMap<>();
    static {
        ROUTES.put("dashboard", "/com/example/big_bike_auto/Dashboard.fxml");
        ROUTES.put("register", "/com/example/big_bike_auto/register.fxml");
        ROUTES.put("repairDetails", "/com/example/big_bike_auto/RepairDetails.fxml");
        ROUTES.put("inventory", "/com/example/big_bike_auto/Inventory.fxml");
        ROUTES.put("ordersPage", "/com/example/big_bike_auto/OrdersPage.fxml");
        ROUTES.put("lookup", "/com/example/big_bike_auto/CustomerLookup.fxml");

    }

    public Router(HomeController home) {
        this.home = home;
    }

    public void navigate(String page) {
        navigate(page, null);
    }

    public void navigate(String page, Map<String, Object> params) {
        String fxml = ROUTES.get(page);
        if (fxml == null) {
            throw new IllegalArgumentException("ไม่รู้จักหน้า: " + page);
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent view = loader.load();

            Object controller = loader.getController();
            if (params != null && controller instanceof ReceivesParams rp) {
                rp.onParams(params);
            }

            home.setContent(view);
        } catch (Exception ex) {
            throw new RuntimeException("ไม่สามารถโหลดหน้า: " + page + " (" + fxml + ")", ex);
        }
    }
}
