package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.model.viewmodel.InventoryRow;
import com.example.big_bike_auto.router.ReceivesParams;
import com.example.big_bike_auto.router.Router;
import com.example.big_bike_auto.router.RouterHub;
import com.example.big_bike_auto.service.InventoryService;
import com.example.big_bike_auto.service.PartCrudService;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * InventoryPageController
 * - เพิ่ม onCreatePart(): เปิดฟอร์มเพิ่มสินค้าใหม่ → validate → บันทึกผ่าน PartCrudService → reload
 * - ปรับ selection mode ของตารางให้เลือกได้หลายแถว
 * - อัปเดตสถานะหลังส่ง Draft เพื่อ UX ที่ชัดเจน
 */
public class InventoryPageController implements ReceivesParams {

    // UI
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSupplier;
    @FXML private ComboBox<String> cbCategory;
    @FXML private CheckBox chkOnlyShortage;

    @FXML private TableView<InventoryRow> tvInventory;
    @FXML private TableColumn<InventoryRow, String> colCode;
    @FXML private TableColumn<InventoryRow, String> colName;
    @FXML private TableColumn<InventoryRow, String> colSupplier;
    @FXML private TableColumn<InventoryRow, String> colCategory;
    @FXML private TableColumn<InventoryRow, Number> colInStock;
    @FXML private TableColumn<InventoryRow, Number> colMinStock;
    @FXML private TableColumn<InventoryRow, Number> colReserved;
    @FXML private TableColumn<InventoryRow, Number> colOnOrder;
    @FXML private TableColumn<InventoryRow, Number> colNeeded;

    @FXML private Label lblTotalParts;
    @FXML private Label lblShortage;
    @FXML private Label lblOnOrder;
    @FXML private Label lblInStockSum;
    @FXML private Label lblStatus;
    @FXML private ProgressIndicator progress;

    // Data
    private final ObservableList<InventoryRow> master = FXCollections.observableArrayList();
    private final ObservableList<InventoryRow> filtered = FXCollections.observableArrayList();

    // Services
    private final InventoryService inventoryService = new InventoryService();
    private final PartCrudService partCrudService = new PartCrudService();
    private Router router;

    @FXML
    private void initialize() {
        router = RouterHub.getRouter();

        // ตั้งค่า columns
        colCode.setCellValueFactory(c -> c.getValue().partCodeProperty());
        colName.setCellValueFactory(c -> c.getValue().nameProperty());
        colSupplier.setCellValueFactory(c -> c.getValue().supplierProperty());
        colCategory.setCellValueFactory(c -> c.getValue().categoryProperty());
        colInStock.setCellValueFactory(c -> c.getValue().inStockProperty());
        colMinStock.setCellValueFactory(c -> c.getValue().minStockProperty());
        colReserved.setCellValueFactory(c -> c.getValue().reservedProperty());
        colOnOrder.setCellValueFactory(c -> c.getValue().onOrderProperty());
        colNeeded.setCellValueFactory(c -> c.getValue().neededProperty());

        tvInventory.setItems(filtered);

        // ✅ ให้ผู้ใช้เลือกได้หลายแถว (ใช้กับการส่งไป Draft)
        tvInventory.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Filter listeners
        txtSearch.textProperty().addListener((obs, oldV, newV) -> applyFilter());
        cbSupplier.valueProperty().addListener((obs, o, n) -> applyFilter());
        cbCategory.valueProperty().addListener((obs, o, n) -> applyFilter());
        chkOnlyShortage.selectedProperty().addListener((obs, o, n) -> applyFilter());

        lblTotalParts.textProperty().bind(Bindings.size(filtered).asString("Parts: %d"));

        loadDataAsync();
    }

    @Override
    public void onParams(Map<String, Object> params) {
        if (params == null) return;
        Object supplier = params.get("supplier");
        if (supplier instanceof String s) cbSupplier.setValue(s);
        Object keyword = params.get("q");
        if (keyword instanceof String q) txtSearch.setText(q);
    }

    @FXML
    private void onRecalculate() { loadDataAsync(); }

    @FXML
    private void onAddToDraft() {
        // เลือกเฉพาะแถวที่ needed > 0 หรือใช้ selection
        List<InventoryRow> rows = tvInventory.getSelectionModel().getSelectedItems();
        if (rows == null || rows.isEmpty()) {
            rows = filtered.stream().filter(r -> r.getNeeded() > 0).toList();
        }
        if (rows.isEmpty()) {
            showInfo("ไม่พบรายการที่ต้องสั่ง");
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("draftFromInventory",
                rows.stream().map(InventoryRow::toDraftLine).toList());

        // ส่งไปหน้า Orders (route name ต้องตรงกับ Router)
        router.navigate("ordersPage", params);

        // ✅ แจ้งสถานะให้ผู้ใช้ทราบจำนวนที่ส่ง
        lblStatus.setText("ส่งไป Draft: " + rows.size() + " รายการ");
    }

    @FXML
    private void onAdjustStock() {
        InventoryRow row = tvInventory.getSelectionModel().getSelectedItem();
        if (row == null) { showInfo("กรุณาเลือก 1 รายการ"); return; }

        TextInputDialog dlg = new TextInputDialog("0");
        dlg.setTitle("Adjust Stock");
        dlg.setHeaderText("กรอกจำนวนที่ต้องการ +เพิ่ม / -ลด");
        dlg.setContentText("จำนวน:");
        Optional<String> result = dlg.showAndWait();
        if (result.isEmpty()) return;

        try {
            int delta = Integer.parseInt(result.get().trim());
            Task<Void> saveTask = new Task<>() {
                @Override protected Void call() {
                    inventoryService.adjustStock(row.getPartCode(), delta);
                    return null;
                }
            };
            beforeTask(saveTask);
            saveTask.setOnSucceeded(e -> {
                afterTask();
                showInfo("บันทึกแล้ว");
                loadDataAsync();
            });
            saveTask.setOnFailed(e -> { afterTask(); showError(saveTask.getException()); });
            new Thread(saveTask, "adjust-stock").start();
        } catch (NumberFormatException ex) {
            showError("จำนวนไม่ถูกต้อง");
        }
    }

    @FXML
    private void onExportCsv() {
        try {
            inventoryService.exportInventoryCsv(new ArrayList<>(filtered));
            showInfo("Export CSV สำเร็จ (ดูที่โฟลเดอร์ export/)");
        } catch (Exception e) {
            showError(e);
        }
    }

    // 🔹 ปุ่มใหม่: เพิ่มสินค้า
    @FXML
    private void onCreatePart() {
        Optional<PartCrudService.PartData> form = showCreatePartDialog();
        if (form.isEmpty()) return;

        PartCrudService.PartData data = form.get();

        // ทำงานหนักใน background
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                partCrudService.createPart(data);
                return null;
            }
        };
        beforeTask(task);
        task.setOnSucceeded(e -> {
            afterTask();
            showInfo("เพิ่มสินค้าเรียบร้อย");
            loadDataAsync();
        });
        task.setOnFailed(e -> {
            afterTask();
            showError(task.getException());
        });
        new Thread(task, "create-part").start();
    }

    /** Dialog ฟอร์มเพิ่มสินค้าใหม่ + validate ขั้นต้น */
    private Optional<PartCrudService.PartData> showCreatePartDialog() {
        Dialog<PartCrudService.PartData> dialog = new Dialog<>();
        dialog.setTitle("เพิ่มสินค้าใหม่");
        dialog.setHeaderText("กรอกข้อมูลอะไหล่");

        ButtonType saveBtnType = new ButtonType("บันทึก", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        TextField tfCode = new TextField();
        TextField tfName = new TextField();
        TextField tfSupplier = new TextField();
        TextField tfCategory = new TextField();
        TextField tfMinStock = new TextField("0");
        TextField tfInStock = new TextField("0");

        // Grid form
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8); grid.setPadding(new Insets(10));
        int r = 0;
        grid.addRow(r++, new Label("รหัส*"), tfCode);
        grid.addRow(r++, new Label("ชื่อสินค้า*"), tfName);
        grid.addRow(r++, new Label("ซัพพลายเออร์"), tfSupplier);
        grid.addRow(r++, new Label("หมวดหมู่"), tfCategory);
        grid.addRow(r++, new Label("ขั้นต่ำ"), tfMinStock);
        grid.addRow(r++, new Label("คงเหลือเริ่มต้น"), tfInStock);

        dialog.getDialogPane().setContent(grid);

        // ปุ่มบันทึก disabled ถ้า code/name ว่าง หรือรหัสชนกับของเดิม
        final Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        saveBtn.setDisable(true);

        Runnable validate = () -> {
            String code = tfCode.getText() == null ? "" : tfCode.getText().trim();
            String name = tfName.getText() == null ? "" : tfName.getText().trim();
            boolean basic = !code.isEmpty() && !name.isEmpty();
            boolean dup = partCrudService.existsPartCode(code);
            saveBtn.setDisable(!basic || dup);
            if (dup) dialog.setHeaderText("กรอกรหัสซ้ำกับรายการที่มีอยู่แล้ว");
            else dialog.setHeaderText("กรอกข้อมูลอะไหล่");
        };
        tfCode.textProperty().addListener((o, a, b) -> validate.run());
        tfName.textProperty().addListener((o, a, b) -> validate.run());
        validate.run();

        dialog.setResultConverter(bt -> {
            if (bt != saveBtnType) return null;
            try {
                int minStock = parseNonNegativeInt(tfMinStock.getText(), "ขั้นต่ำ");
                int inStock = parseNonNegativeInt(tfInStock.getText(), "คงเหลือเริ่มต้น");
                return new PartCrudService.PartData(
                        tfCode.getText(), tfName.getText(), tfSupplier.getText(), tfCategory.getText(),
                        minStock, inStock
                );
            } catch (NumberFormatException ex) {
                showError("ตัวเลขไม่ถูกต้อง: " + ex.getMessage());
                return null;
            }
        });

        Optional<PartCrudService.PartData> res = dialog.showAndWait();
        // ถ้า resultConverter คืน null (เช่นเลขผิด) ให้ถือว่าไม่บันทึก
        return res.filter(Objects::nonNull);
    }

    private int parseNonNegativeInt(String s, String field) {
        String v = s == null ? "0" : s.trim();
        int n = Integer.parseInt(v);
        if (n < 0) throw new NumberFormatException(field + " ต้องไม่ติดลบ");
        return n;
    }

    // ---------- load/filter helpers ----------
    private void loadDataAsync() {
        Task<InventoryData> task = new Task<>() {
            @Override protected InventoryData call() {
                var rows = inventoryService.buildInventoryRows();
                Set<String> suppliers = rows.stream().map(InventoryRow::getSupplier)
                        .filter(x -> x != null && !x.isBlank())
                        .collect(Collectors.toCollection(TreeSet::new));
                Set<String> categories = rows.stream().map(InventoryRow::getCategory)
                        .filter(x -> x != null && !x.isBlank())
                        .collect(Collectors.toCollection(TreeSet::new));

                int shortage = (int) rows.stream().filter(r -> r.getNeeded() > 0).count();
                int onOrder = rows.stream().mapToInt(InventoryRow::getOnOrder).sum();
                int inStockSum = rows.stream().mapToInt(InventoryRow::getInStock).sum();

                return new InventoryData(rows, suppliers, categories, shortage, onOrder, inStockSum);
            }
        };
        beforeTask(task);
        task.setOnSucceeded(e -> {
            afterTask();
            InventoryData d = task.getValue();
            master.setAll(d.rows());
            applyFilter();

            cbSupplier.getItems().setAll(d.suppliers());
            cbCategory.getItems().setAll(d.categories());
            lblShortage.setText("Shortage: " + d.shortage());
            lblOnOrder.setText("On Order: " + d.onOrder());
            lblInStockSum.setText("In Stock Sum: " + d.inStockSum());
            lblStatus.setText("โหลดข้อมูลสำเร็จ");
        });
        task.setOnFailed(e -> { afterTask(); showError(task.getException()); });
        new Thread(task, "load-inventory").start();
    }

    private void applyFilter() {
        final String kw = Optional.ofNullable(txtSearch.getText()).orElse("").trim().toLowerCase();
        final String sup = cbSupplier.getValue();
        final String cat = cbCategory.getValue();
        final boolean onlyShortage = chkOnlyShortage.isSelected();

        var p = (Predicate<InventoryRow>) r -> {
            if (!kw.isEmpty()) {
                String s = (r.getPartCode() + " " + r.getName()).toLowerCase();
                if (!s.contains(kw)) return false;
            }
            if (sup != null && !sup.isBlank() && !Objects.equals(sup, r.getSupplier())) return false;
            if (cat != null && !cat.isBlank() && !Objects.equals(cat, r.getCategory())) return false;
            if (onlyShortage && r.getNeeded() <= 0) return false;
            return true;
        };

        List<InventoryRow> out = master.stream().filter(p).toList();
        filtered.setAll(out);
        lblStatus.setText("แสดง " + out.size() + " รายการ");
    }

    private void beforeTask(Task<?> t) {
        progress.setVisible(true);
        lblStatus.setText("กำลังทำงาน...");
    }
    private void afterTask() { progress.setVisible(false); }
    private void showError(Throwable ex) {
        ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR, String.valueOf(ex.getMessage()), ButtonType.OK);
        a.setHeaderText("เกิดข้อผิดพลาด");
        a.showAndWait();
    }
    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("เกิดข้อผิดพลาด");
        a.showAndWait();
    }
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private record InventoryData(
            List<InventoryRow> rows,
            Set<String> suppliers,
            Set<String> categories,
            int shortage,
            int onOrder,
            int inStockSum
    ) { }
}
