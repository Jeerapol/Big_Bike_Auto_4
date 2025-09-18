package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.model.Part;
import com.example.big_bike_auto.model.PurchaseOrder;
import com.example.big_bike_auto.repository.PurchaseOrderRepository;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * OrdersPageController:
 * - แสดงรายการ PO
 * - ปุ่มรีเฟรช / สร้างใบสั่งซื้อ / รับเข้า / ยกเลิก
 *
 * หมายเหตุสำคัญ:
 * - ใช้ Callback ไม่ใช้ PropertyValueFactory เพื่อลดปัญหา reflection + module
 * - ป้องกันไฟล์ JSON หายโดย ensurePoFileExists()
 */
public class OrdersPageController {

    // ------- FXML components (ต้องตรงกับ OrdersPage.fxml) -------
    @FXML private TableView<PurchaseOrder> tvOrders;
    @FXML private TableColumn<PurchaseOrder, String> colId;
    @FXML private TableColumn<PurchaseOrder, String> colSupplier;
    @FXML private TableColumn<PurchaseOrder, String> colOrderDate;
    @FXML private TableColumn<PurchaseOrder, Integer> colItemCount; // แก้เป็น Integer ให้ตรงกับ wrapper
    @FXML private TableColumn<PurchaseOrder, String> colStatus;

    @FXML private Button btnPlace;
    @FXML private Button btnReceive;
    @FXML private Button btnCancel;

    // ------- Repository / State -------
    private final PurchaseOrderRepository repo = new PurchaseOrderRepository();
    private final ObservableList<PurchaseOrder> orders = FXCollections.observableArrayList();

    // ไฟล์ JSON ที่ repo ใช้
    private static final Path PO_JSON = Path.of("data", "purchase_orders.json");

    @FXML
    public void initialize() {
        // Map คอลัมน์แบบ callback (หลบ reflection)
        colId.setCellValueFactory(row -> new ReadOnlyStringWrapper(ns(row.getValue().getId())));
        colSupplier.setCellValueFactory(row -> new ReadOnlyStringWrapper(ns(row.getValue().getSupplier())));
        colOrderDate.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                row.getValue().getOrderDate() != null ? row.getValue().getOrderDate().toString() : ""
        ));
        colItemCount.setCellValueFactory(row ->
                new ReadOnlyIntegerWrapper(row.getValue().getItems() != null ? row.getValue().getItems().size() : 0)
                        .asObject()
        );
        colStatus.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                row.getValue().isReceived() ? "ปิดแล้ว" : "เปิด"
        ));

        tvOrders.setItems(orders);

        // อัปเดตสถานะปุ่มตาม selection
        tvOrders.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> updateButtons());

        // โหลดข้อมูลครั้งแรก
        safeRefresh();
    }

    // -------------------- Event Handlers --------------------

    /** กด "รีเฟรช" → โหลดไฟล์ JSON ใหม่ */
    @FXML
    private void onRefresh() {
        safeRefresh();
    }

    /**
     * กด "สั่งซื้อ" → สร้าง PO ตัวอย่าง (ปรับเชื่อมกับหน้าจริงได้ภายหลัง)
     */
    @FXML
    private void onPlace() {
        Dialog<PurchaseOrder> dialog = new Dialog<>();
        dialog.setTitle("สร้างใบสั่งซื้อ (ตัวอย่าง)");
        dialog.setHeaderText("สร้าง PO ตัวอย่างเพื่อทดสอบ flow");

        Label lbl = new Label("Supplier:");
        TextField tfSupplier = new TextField("Default Supplier");
        GridPane gp = new GridPane();
        gp.setHgap(8); gp.setVgap(8);
        gp.addRow(0, lbl, tfSupplier);
        dialog.getDialogPane().setContent(gp);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String supplier = tfSupplier.getText().trim().isEmpty() ? "Unknown" : tfSupplier.getText().trim();

                // ⚠ โมเดล Part ของคุณรับ 4 พารามิเตอร์: name, qty, unit, unitPrice
                List<Part> items = new ArrayList<>();
                items.add(new Part("ตัวอย่างอะไหล่", 1, "ชิ้น", 100.0));

                String id = "PO-" + System.currentTimeMillis();
                PurchaseOrder po = new PurchaseOrder(id, supplier, LocalDate.now());
                for (Part p : items) po.addItem(p);
                return po;
            }
            return null;
        });

        Optional<PurchaseOrder> res = dialog.showAndWait();
        if (res.isEmpty()) return;

        PurchaseOrder newPo = res.get();
        List<PurchaseOrder> all = new ArrayList<>(orders);
        all.add(0, newPo);
        if (safeSaveAll(all)) {
            orders.setAll(all);
            tvOrders.getSelectionModel().select(newPo);
            info("สร้างใบสั่งซื้อสำเร็จ", "PO ID: " + newPo.getId());
        }
    }

    /** กด "รับเข้า (ปิด PO)" → เปลี่ยนสถานะ received=true แล้วบันทึก */
    @FXML
    private void onReceive() {
        PurchaseOrder sel = tvOrders.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("ยังไม่ได้เลือก PO", "โปรดเลือกใบสั่งซื้อก่อน");
            return;
        }
        if (sel.isReceived()) {
            info("สถานะ", "ใบสั่งซื้อนี้ปิดแล้ว");
            return;
        }
        if (!confirm("ยืนยันรับเข้า", "ต้องการปิด PO นี้ใช่หรือไม่?\n\nPO: " + sel.getId())) return;

        sel.markAsReceived();
        if (safeSaveAll(orders)) {
            tvOrders.refresh();
            info("สำเร็จ", "ปิด PO เรียบร้อย");
        }
    }

    /** กด "ยกเลิก" → ลบ PO ออกจากรายการ */
    @FXML
    private void onCancel() {
        PurchaseOrder sel = tvOrders.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("ยังไม่ได้เลือก PO", "โปรดเลือกใบสั่งซื้อก่อน");
            return;
        }
        if (!confirm("ยืนยันยกเลิก", "ต้องการลบ PO นี้ใช่หรือไม่?\n\nPO: " + sel.getId())) return;

        List<PurchaseOrder> all = new ArrayList<>(orders);
        all.remove(sel);
        if (safeSaveAll(all)) {
            orders.setAll(all);
            info("สำเร็จ", "ลบ PO เรียบร้อย");
        }
    }

    // -------------------- Internal helpers --------------------

    /** โหลดข้อมูล PO ทั้งหมดแบบปลอดภัยและ update ตาราง */
    private void safeRefresh() {
        try {
            ensurePoFileExists();
            List<PurchaseOrder> list = repo.findAll();
            orders.setAll(list);
            updateButtons();
        } catch (RuntimeException ex) {
            error("โหลดข้อมูลล้มเหลว", ex);
        }
    }

    /** เซฟข้อมูลทั้งหมดกลับไฟล์ JSON โดยสร้างโฟลเดอร์/ไฟล์ให้ถ้ายังไม่มี */
    private boolean safeSaveAll(List<PurchaseOrder> all) {
        try {
            ensurePoFileExists();
            repo.saveAll(all);
            return true;
        } catch (RuntimeException ex) {
            error("บันทึกข้อมูลล้มเหลว", ex);
            return false;
        }
    }

    /** สร้าง data/purchase_orders.json เป็น [] ถ้าไม่พบ */
    private void ensurePoFileExists() {
        try {
            File dataDir = PO_JSON.getParent().toFile();
            if (!dataDir.exists() && !dataDir.mkdirs()) {
                throw new IllegalStateException("ไม่สามารถสร้างโฟลเดอร์: " + dataDir.getAbsolutePath());
            }
            if (!Files.exists(PO_JSON)) {
                try (FileWriter w = new FileWriter(PO_JSON.toFile())) {
                    w.write("[]");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("เตรียมไฟล์ PO ไม่สำเร็จ: " + PO_JSON, e);
        }
    }

    private void updateButtons() {
        PurchaseOrder sel = tvOrders.getSelectionModel().getSelectedItem();
        boolean hasSel = sel != null;
        btnReceive.setDisable(!hasSel || (hasSel && sel.isReceived()));
        btnCancel.setDisable(!hasSel);
        btnPlace.setDisable(false);
    }

    private String ns(String s) { return s == null ? "" : s; }

    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void warn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void error(String title, Exception ex) {
        ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(String.valueOf(ex.getMessage()));
        String stack = getStackTrace(ex);
        TextArea ta = new TextArea(stack);
        ta.setEditable(false); ta.setWrapText(false);
        ta.setMaxWidth(Double.MAX_VALUE); ta.setMaxHeight(Double.MAX_VALUE);
        a.getDialogPane().setExpandableContent(ta);
        a.showAndWait();
    }

    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title); a.setHeaderText(title); a.setContentText(msg);
        ButtonType ok = new ButtonType("ยืนยัน", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("ยกเลิก", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(ok, cancel);
        return a.showAndWait().filter(bt -> bt == ok).isPresent();
    }

    private String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        while (t != null) {
            sb.append(t).append("\n");
            for (StackTraceElement el : t.getStackTrace()) {
                sb.append("  at ").append(el).append("\n");
            }
            t = t.getCause();
            if (t != null) sb.append("Caused by: ");
        }
        return sb.toString();
    }
}
