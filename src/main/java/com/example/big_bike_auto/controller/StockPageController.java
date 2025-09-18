package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.parts.Part;
import com.example.big_bike_auto.parts.PartRepository;
import com.example.big_bike_auto.po.DraftPoRepository;
import com.example.big_bike_auto.po.PurchaseOrderDraft;
import com.example.big_bike_auto.service.ReorderService;
import com.example.big_bike_auto.service.ReorderService.Suggestion;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;                     // ✅ ใส่ @FXML ให้ initialize และปุ่ม
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.Optional;
import java.util.UUID;

public class StockPageController {

    // ---- FXML Fields ----
    @FXML public TableView<Part> tblParts;
    @FXML public TableColumn<Part, String> colSku;
    @FXML public TableColumn<Part, String> colName;
    @FXML public TableColumn<Part, String> colUnit;
    @FXML public TableColumn<Part, Number> colOnHand;
    @FXML public TableColumn<Part, Number> colReserved;
    @FXML public TableColumn<Part, Number> colAvailable;
    @FXML public TableColumn<Part, Number> colMinStock;
    @FXML public TableColumn<Part, BigDecimal> colLastCost;
    @FXML public TableColumn<Part, String> colSupplier;
    @FXML public TextField txtSearch;
    @FXML public Label lblStatus;

    private final PartRepository partRepo = new PartRepository();
    private final ReorderService reorderService = new ReorderService();
    private final DraftPoRepository draftRepo = new DraftPoRepository();

    private final ObservableList<Part> partsData = FXCollections.observableArrayList();

    // ✅ ให้ JavaFX เรียกแน่ ๆ
    @FXML
    public void initialize() {
        colSku.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getSku()));
        colName.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getName()));
        colUnit.setCellValueFactory(c -> new ReadOnlyStringWrapper(nvl(c.getValue().getUnit(), "")));
        colOnHand.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().getOnHand()));
        colReserved.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().getReserved()));
        colAvailable.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(
                Math.max(0, c.getValue().getOnHand() - c.getValue().getReserved())));
        colMinStock.setCellValueFactory(c -> new ReadOnlyIntegerWrapper(c.getValue().getMinStock()));
        colLastCost.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().getLastCost() == null ? BigDecimal.ZERO : c.getValue().getLastCost()));
        colSupplier.setCellValueFactory(c -> new ReadOnlyStringWrapper(nvl(c.getValue().getSupplier(), "")));

        // แสดงว่าเปิดมาจากงานซ่อมหรือไม่ (optional)
        StockPageContext.getJobId().ifPresent(id -> {
            if (lblStatus != null) lblStatus.setText("โหมดใช้อะไหล่สำหรับงาน: " + id);
        });

        refreshTable();
    }

    private void refreshTable() {
        partsData.setAll(partRepo.findAll());
        tblParts.setItems(partsData);
        lblStatus.setText("โหลดข้อมูลแล้ว: " + partsData.size() + " รายการ");
    }

    @FXML
    public void onRefresh() {
        refreshTable();
    }

    @FXML
    public void onSearch() {
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            tblParts.setItems(partsData);
            lblStatus.setText("แสดงทั้งหมด");
            return;
        }
        List<Part> filtered = partsData.stream()
                .filter(p -> p.getSku().toLowerCase().contains(q) ||
                        p.getName().toLowerCase().contains(q))
                .collect(Collectors.toList());
        tblParts.setItems(FXCollections.observableArrayList(filtered));
        lblStatus.setText("ผลลัพธ์ค้นหา: " + filtered.size() + " รายการ");
    }

    // ---- แนะนำสั่งซื้อ ----
    @FXML
    public void onBuildReorder() {
        try {
            List<Part> all = partRepo.findAll();
            int safetyStock = 20;
            List<Suggestion> suggestions = reorderService.buildSuggestions(all, safetyStock);
            if (suggestions.isEmpty()) {
                info("ไม่มีรายการที่ต้องสั่งซื้อ", "Available ครบตาม Min แล้ว");
                return;
            }
            showReorderDialogAndSave(suggestions);
        } catch (Exception ex) {
            ex.printStackTrace();
            error("คำนวณคำแนะนำล้มเหลว", ex.getMessage());
        }
    }

    // ---- รับเข้า (เติมสต็อก) ให้กับรายการที่เลือก ----
    @FXML
    public void onReceive() {
        Part sel = tblParts.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("ยังไม่ได้เลือกรายการ", "กรุณาเลือกอะไหล่ในตารางก่อน");
            return;
        }
        TextInputDialog dlg = new TextInputDialog("1");
        dlg.setTitle("รับเข้า (เติมสต็อก)");
        dlg.setHeaderText("SKU: " + sel.getSku() + " - " + sel.getName());
        dlg.setContentText("จำนวนที่รับเข้า (+):");
        Optional<String> res = dlg.showAndWait();
        if (res.isEmpty()) return;
        int qty;
        try {
            qty = Integer.parseInt(res.get().trim());
        } catch (NumberFormatException e) {
            warn("จำนวนไม่ถูกต้อง", "กรุณากรอกเป็นเลขจำนวนเต็ม");
            return;
        }
        if (qty <= 0) {
            warn("จำนวนต้องมากกว่า 0", "ค่าที่กรอก: " + qty);
            return;
        }
        // อัปเดตสต็อก
        sel.setOnHand(sel.getOnHand() + qty);
        partRepo.upsert(sel);
        refreshTable();
        info("สำเร็จ", "รับเข้าจำนวน " + qty + " หน่วยแล้ว");
    }

    // ---- ปรับยอด (Adjust) ตั้งค่าคงเหลือใหม่แบบตรง ๆ ----
    @FXML
    public void onAdjust() {
        Part sel = tblParts.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("ยังไม่ได้เลือกรายการ", "กรุณาเลือกอะไหล่ในตารางก่อน");
            return;
        }
        TextInputDialog dlg = new TextInputDialog(String.valueOf(sel.getOnHand()));
        dlg.setTitle("ปรับยอดคงเหลือ (Adjust)");
        dlg.setHeaderText("SKU: " + sel.getSku() + " - " + sel.getName());
        dlg.setContentText("ตั้งค่าคงเหลือ (On hand) ใหม่เป็น:");
        Optional<String> res = dlg.showAndWait();
        if (res.isEmpty()) return;
        int newOnHand;
        try {
            newOnHand = Integer.parseInt(res.get().trim());
        } catch (NumberFormatException e) {
            warn("จำนวนไม่ถูกต้อง", "กรุณากรอกเป็นเลขจำนวนเต็ม");
            return;
        }
        if (newOnHand < 0) {
            warn("จำนวนต้องไม่ติดลบ", "ค่าที่กรอก: " + newOnHand);
            return;
        }
        sel.setOnHand(newOnHand);
        // กัน reserved > onHand
        if (sel.getReserved() > sel.getOnHand()) {
            sel.setReserved(sel.getOnHand());
        }
        partRepo.upsert(sel);
        refreshTable();
        info("สำเร็จ", "ตั้งค่าคงเหลือเป็น " + newOnHand + " หน่วยแล้ว");
    }

    // === Dialog แสดงคำแนะนำให้ผู้ใช้ตรวจ/แก้ ก่อนบันทึก ===
    private void showReorderDialogAndSave(List<Suggestion> suggestions) {
        Stage dialog = new Stage();
        dialog.setTitle("แนะนำสั่งซื้อ (PO Draft) – ตรวจสอบก่อนบันทึก");
        dialog.initModality(Modality.APPLICATION_MODAL);

        TableView<Row> table = new TableView<>();
        ObservableList<Row> rows = FXCollections.observableArrayList();
        for (Suggestion s : suggestions) rows.add(new Row(s));
        table.setItems(rows);

        TableColumn<Row, String> cSupplier = mkCol("ผู้ขาย", 140, r -> r.supplier);
        TableColumn<Row, String> cSku      = mkCol("SKU", 120, r -> r.sku);
        TableColumn<Row, String> cName     = mkCol("ชื่อ", 220, r -> r.name);
        TableColumn<Row, Number> cAvail    = mkColNum("เบิกได้", 90, r -> r.available);
        TableColumn<Row, Number> cMin      = mkColNum("Min", 70, r -> r.minStock);
        TableColumn<Row, Number> cMoq      = mkColNum("MOQ", 70, r -> r.moq);
        TableColumn<Row, Number> cPack     = mkColNum("Pack", 70, r -> r.pack);
        TableColumn<Row, Number> cQty      = mkEditableIntCol("สั่ง", 90, r -> r.qty);
        cQty.setEditable(true);
        TableColumn<Row, String> cUnit     = mkCol("หน่วย", 70, r -> r.unit);
        TableColumn<Row, String> cCost     = mkCol("ต้นทุน", 100, r -> r.costText);
        TableColumn<Row, String> cSub      = mkCol("รวม", 100, r -> r.subTotalText);
        TableColumn<Row, String> cReason   = mkCol("เหตุผล", 220, r -> r.reason);

        table.getColumns().addAll(
                cSupplier, cSku, cName, cAvail, cMin, cMoq, cPack, cQty, cUnit, cCost, cSub, cReason
        );
        table.setEditable(true);

        Label lblTotal = new Label();
        Runnable recompute = () -> {
            BigDecimal grand = rows.stream().map(r -> r.subTotal.get())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblTotal.setText("ยอดรวมทั้งหมด: " + grand.setScale(2, RoundingMode.HALF_UP));
        };
        recompute.run();

        rows.forEach(r -> r.qty.addListener((obs, o, n) -> {
            r.recalc();
            recompute.run();
        }));

        Button btnSave = new Button("บันทึกเป็นฉบับร่าง PO");
        btnSave.setOnAction(e -> {
            try {
                Map<String, PurchaseOrderDraft> bySupplier = new LinkedHashMap<>();
                for (Row r : rows) {
                    if (r.qty.get() <= 0) continue;
                    PurchaseOrderDraft draft = bySupplier.computeIfAbsent(
                            r.supplier.get(), PurchaseOrderDraft::new);
                    draft.getItems().add(new PurchaseOrderDraft.Item(
                            r.sku.get(), r.name.get(), r.unit.get(), r.qty.get(),
                            r.unitCost, r.subTotal.get()
                    ));
                }
                if (bySupplier.isEmpty()) {
                    warn("ไม่มีรายการจะบันทึก", "ปริมาณเป็น 0 ทั้งหมด?");
                    return;
                }
                StringBuilder savedFiles = new StringBuilder();
                bySupplier.forEach((sup, draft) -> {
                    draft.recomputeTotal();
                    var file = draftRepo.save(draft);
                    savedFiles.append(file.getName()).append("\n");
                });
                info("บันทึกสำเร็จ", savedFiles.toString());
                lblStatus.setText("บันทึก PO Draft สำเร็จ (" + bySupplier.size() + " ผู้ขาย)");
                dialog.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                error("บันทึก PO Draft ล้มเหลว", ex.getMessage());
            }
        });

        Button btnCancel = new Button("ยกเลิก");
        btnCancel.setOnAction(e -> dialog.close());

        HBox footer = new HBox(10, lblTotal, new HBox(), btnSave, btnCancel);
        HBox.setMargin(lblTotal, new Insets(8));
        footer.setPadding(new Insets(8));
        footer.setStyle("-fx-alignment: CENTER_RIGHT;");

        BorderPane root = new BorderPane(table, null, null, footer, null);
        root.setPadding(new Insets(8));
        dialog.setScene(new Scene(root, 1100, 600));
        dialog.showAndWait();
    }

    // ----- Utilities & inner Row model -----

    private static class Row {
        StringProperty supplier = new SimpleStringProperty();
        StringProperty sku = new SimpleStringProperty();
        StringProperty name = new SimpleStringProperty();
        StringProperty unit = new SimpleStringProperty();
        IntegerProperty available = new SimpleIntegerProperty();
        IntegerProperty minStock = new SimpleIntegerProperty();
        IntegerProperty moq = new SimpleIntegerProperty();
        IntegerProperty pack = new SimpleIntegerProperty();
        IntegerProperty qty = new SimpleIntegerProperty();
        ObjectProperty<BigDecimal> subTotal = new SimpleObjectProperty<>(BigDecimal.ZERO);
        StringProperty costText = new SimpleStringProperty();
        StringProperty subTotalText = new SimpleStringProperty();
        StringProperty reason = new SimpleStringProperty();

        BigDecimal unitCost = BigDecimal.ZERO;

        Row(Suggestion s) {
            supplier.set(s.supplier);
            sku.set(s.sku);
            name.set(s.name);
            unit.set(s.unit);
            available.set(s.available);
            minStock.set(s.minStock);
            moq.set(s.moq);
            pack.set(s.packSize);
            qty.set(s.suggestQty);
            unitCost = s.lastCost == null ? BigDecimal.ZERO : s.lastCost;
            subTotal.set(s.subTotal);
            costText.set(unitCost.setScale(2, RoundingMode.HALF_UP).toPlainString());
            subTotalText.set(s.subTotal.setScale(2, RoundingMode.HALF_UP).toPlainString());
            reason.set(s.reason);
            qty.addListener((obs, o, n) -> recalc());
        }

        void recalc() {
            BigDecimal sub = unitCost.multiply(BigDecimal.valueOf(qty.get()))
                    .setScale(2, RoundingMode.HALF_UP);
            subTotal.set(sub);
            subTotalText.set(sub.toPlainString());
        }
    }

    private static TableColumn<Row, String> mkCol(String title, double w,
                                                  Function<Row, ObservableValue<String>> extractor) {
        TableColumn<Row, String> c = new TableColumn<>(title);
        c.setPrefWidth(w);
        c.setCellValueFactory(cd -> extractor.apply(cd.getValue()));
        return c;
    }

    private static TableColumn<Row, Number> mkColNum(String title, double w,
                                                     Function<Row, ObservableValue<Number>> extractor) {
        TableColumn<Row, Number> c = new TableColumn<>(title);
        c.setPrefWidth(w);
        c.setCellValueFactory(cd -> extractor.apply(cd.getValue()));
        return c;
    }

    private static TableColumn<Row, Number> mkEditableIntCol(String title, double w,
                                                             Function<Row, ObservableValue<Number>> extractor) {
        TableColumn<Row, Number> c = new TableColumn<>(title);
        c.setPrefWidth(w);
        c.setCellValueFactory(cd -> extractor.apply(cd.getValue()));
        c.setCellFactory(col -> new TextFieldTableCell<>(new StringConverter<Number>() {
            @Override public String toString(Number object) { return object == null ? "0" : object.toString(); }
            @Override public Number fromString(String string) {
                try {
                    int v = Integer.parseInt(string.trim());
                    return Math.max(0, v);
                } catch (Exception e) {
                    return 0;
                }
            }
        }));
        c.setOnEditCommit(ev -> {
            Row row = ev.getRowValue();
            int newVal = ev.getNewValue() == null ? 0 : ev.getNewValue().intValue();
            int moq = row.moq.get();
            int pack = Math.max(1, row.pack.get());
            if (newVal < moq) newVal = moq;
            int k = newVal / pack;
            if (newVal % pack != 0) k++;
            newVal = k * pack;
            row.qty.set(newVal);
            row.recalc();
        });
        return c;
    }

    private static void info(String h, String c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(h);
        a.setContentText(c);
        a.showAndWait();
    }
    private static void warn(String h, String c) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(h);
        a.setContentText(c);
        a.showAndWait();
    }
    private static void error(String h, String c) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(h);
        a.setContentText(c);
        a.showAndWait();
    }
    private static String nvl(String s, String d) { return (s == null || s.isBlank()) ? d : s; }

    // ==== Context สำหรับเปิดมาจากงานซ่อม ====
    public static final class StockPageContext {
        private static UUID jobId;
        public static void setJobId(UUID id) { jobId = id; }
        public static Optional<UUID> getJobId() { return Optional.ofNullable(jobId); }
        public static void clear() { jobId = null; }
    }
}
