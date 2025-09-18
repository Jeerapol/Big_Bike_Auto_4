package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.auth.Role;
import com.example.big_bike_auto.auth.SessionContext;
import com.example.big_bike_auto.common.PoDraftImporter;
import com.example.big_bike_auto.parts.PartOrder;
import com.example.big_bike_auto.parts.PartOrderRepository;
import com.example.big_bike_auto.parts.PartRepository;
import com.example.big_bike_auto.parts.PartService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * OrdersPageController
 * - แสดงรายการใบสั่งซื้อจาก part_orders.json
 * - เรียก Importer อัตโนมัติ: นำเข้าไฟล์ใน data/po_drafts/* → part_orders.json (ครั้งแรก)
 * - เปลี่ยนสถานะ Place / Receive / Cancel
 */
public class OrdersPageController {

    @FXML private TableView<OrderRow> tvOrders;
    @FXML private TableColumn<OrderRow, String> colId;
    @FXML private TableColumn<OrderRow, String> colSupplier;
    @FXML private TableColumn<OrderRow, String> colDate;
    @FXML private TableColumn<OrderRow, String> colStatus;
    @FXML private TableColumn<OrderRow, Integer> colLines;
    @FXML private TableColumn<OrderRow, String> colTotal;

    @FXML private Button btnPlace;
    @FXML private Button btnReceive;
    @FXML private Button btnCancel;

    private final PartOrderRepository orderRepo = new PartOrderRepository();
    private final PartRepository partRepo = new PartRepository();
    private final PartService service = new PartService(partRepo, orderRepo);
    private final ObservableList<OrderRow> data = FXCollections.observableArrayList();
    private final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colLines.setCellValueFactory(new PropertyValueFactory<>("lines"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        tvOrders.setItems(data);

        // RBAC
        Role r = SessionContext.getCurrentRole();
        boolean canPlace = r == Role.ADMIN || r == Role.STAFF;
        btnPlace.setDisable(!canPlace);
        btnReceive.setDisable(!canPlace);
        btnCancel.setDisable(!(r == Role.ADMIN));

        // 🔁 นำเข้าข้อมูลเก่าจาก po_drafts/* → part_orders.json
        try {
            int imported = PoDraftImporter.importFromPoDrafts(orderRepo);
            if (imported > 0) {
                info("นำเข้าใบสั่งซื้อเก่า " + imported + " รายการจากโฟลเดอร์ po_drafts แล้ว");
            }
        } catch (Exception ignore) { /* อย่าให้ล้มหน้า */ }

        loadData();
    }

    @FXML private void onRefresh() { loadData(); }

    @FXML private void onPlace() {
        OrderRow sel = tvOrders.getSelectionModel().getSelectedItem();
        if (sel == null) { info("ยังไม่ได้เลือก PO"); return; }
        try {
            service.updateOrderStatus(UUID.fromString(sel.id), PartOrder.Status.PLACED);
            loadData();
            info("สำเร็จ: สั่งซื้อแล้ว");
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    @FXML private void onReceive() {
        OrderRow sel = tvOrders.getSelectionModel().getSelectedItem();
        if (sel == null) { info("ยังไม่ได้เลือก PO"); return; }
        try {
            service.updateOrderStatus(UUID.fromString(sel.id), PartOrder.Status.RECEIVED);
            loadData();
            info("สำเร็จ: รับเข้าและปิดใบสั่งแล้ว");
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    @FXML private void onCancel() {
        OrderRow sel = tvOrders.getSelectionModel().getSelectedItem();
        if (sel == null) { info("ยังไม่ได้เลือก PO"); return; }
        try {
            service.updateOrderStatus(UUID.fromString(sel.id), PartOrder.Status.CANCELED);
            loadData();
            info("สำเร็จ: ยกเลิกแล้ว");
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    private void loadData() {
        var all = orderRepo.findAll();
        data.setAll(all.stream().map(po -> new OrderRow(
                po.getOrderId().toString(),
                po.getSupplier(),
                po.getCreatedDate() == null ? "-" : DATE.format(po.getCreatedDate()),
                mapStatus(po.getStatus()),
                po.getLines() == null ? 0 : po.getLines().size(),
                po.getGrandTotal() == null ? "0.00" : po.getGrandTotal().toPlainString()
        )).toList());
    }

    private String mapStatus(PartOrder.Status st) {
        if (st == null) return "Draft";
        return switch (st) {
            case DRAFT -> "Draft";
            case PLACED -> "สั่งซื้อแล้ว";
            case RECEIVED -> "รับเข้าแล้ว";
            case CANCELED -> "ยกเลิก";
        };
    }

    // ===== Row model =====
    public static class OrderRow {
        public final String id, supplier, date, status, total;
        public final int lines;
        public OrderRow(String id, String supplier, String date, String status, int lines, String total) {
            this.id = id; this.supplier = supplier; this.date = date; this.status = status;
            this.lines = lines; this.total = total;
        }
        public String getId() { return id; }
        public String getSupplier() { return supplier; }
        public String getDate() { return date; }
        public String getStatus() { return status; }
        public int getLines() { return lines; }
        public String getTotal() { return total; }
    }

    private static void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
    private static void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("เกิดข้อผิดพลาด");
        a.setContentText(msg);
        a.showAndWait();
    }
}
