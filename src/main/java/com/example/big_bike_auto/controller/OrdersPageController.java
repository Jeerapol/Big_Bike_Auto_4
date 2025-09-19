package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.model.PurchaseOrder;
import com.example.big_bike_auto.model.viewmodel.OrderRow;
import com.example.big_bike_auto.model.viewmodel.InventoryRow;
import com.example.big_bike_auto.router.ReceivesParams;
import com.example.big_bike_auto.service.OrderService;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class OrdersPageController implements ReceivesParams {

    // Backlog tab
    @FXML private TextField txtSearchDraft;
    @FXML private ComboBox<String> cbSupplierDraft;
    @FXML private TableView<OrderRow> tvDraft;
    @FXML private TableColumn<OrderRow, String> colDSupplier;
    @FXML private TableColumn<OrderRow, String> colDCode;
    @FXML private TableColumn<OrderRow, String> colDName;
    @FXML private TableColumn<OrderRow, Number> colDNeeded;
    @FXML private TableColumn<OrderRow, Number> colDOrderQty;

    // POs tab
    @FXML private ComboBox<String> cbStatus;
    @FXML private ComboBox<String> cbSupplierPo;
    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private TableView<PurchaseOrder> tvPOs;
    @FXML private TableColumn<PurchaseOrder, String> colPoNo;
    @FXML private TableColumn<PurchaseOrder, String> colPoSupplier;
    @FXML private TableColumn<PurchaseOrder, String> colPoStatus;
    @FXML private TableColumn<PurchaseOrder, String> colPoCreated;
    @FXML private TableColumn<PurchaseOrder, String> colPoItems;
    @FXML private TableColumn<PurchaseOrder, String> colPoQty;
    @FXML private TableColumn<PurchaseOrder, String> colPoCost;

    @FXML private Label lblStatus;
    @FXML private ProgressIndicator progress;

    // Data
    private final ObservableList<OrderRow> draftMaster = FXCollections.observableArrayList();
    private final ObservableList<OrderRow> draftFiltered = FXCollections.observableArrayList();
    private final ObservableList<PurchaseOrder> poList = FXCollections.observableArrayList();

    private final OrderService orderService = new OrderService();

    @FXML
    private void initialize() {
        // Draft columns
        colDSupplier.setCellValueFactory(c -> c.getValue().supplierProperty());
        colDCode.setCellValueFactory(c -> c.getValue().partCodeProperty());
        colDName.setCellValueFactory(c -> c.getValue().nameProperty());
        colDNeeded.setCellValueFactory(c -> c.getValue().neededProperty());

        // ทำให้ OrderQty เป็น editable integer cell
        tvDraft.setEditable(true);
        colDOrderQty.setCellValueFactory(c -> c.getValue().orderQtyProperty());
        colDOrderQty.setCellFactory(tc -> new IntegerEditingCell());
        colDOrderQty.setOnEditCommit(ev -> {
            OrderRow r = ev.getRowValue();
            int val = Math.max(0, ev.getNewValue().intValue());
            r.setOrderQty(val);
        });

        tvDraft.setItems(draftFiltered);

        // ✅ ให้เลือกได้หลายแถว (รองรับการลบเป็นชุดในอนาคต ฯลฯ)
        tvDraft.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        txtSearchDraft.textProperty().addListener((o, ov, nv) -> applyDraftFilter());
        cbSupplierDraft.valueProperty().addListener((o, ov, nv) -> applyDraftFilter());

        // PO columns
        colPoNo.setCellValueFactory(po -> new ReadOnlyStringWrapper(po.getValue().getId()));
        colPoSupplier.setCellValueFactory(po -> new ReadOnlyStringWrapper(po.getValue().getSupplier()));
        colPoStatus.setCellValueFactory(po -> {
            String status = po.getValue().isReceived() ? "RECEIVED" : "OPEN";
            return new ReadOnlyStringWrapper(status);
        });
        colPoCreated.setCellValueFactory(po -> new ReadOnlyStringWrapper(
                po.getValue().getOrderDate() == null ? "" : po.getValue().getOrderDate().toString()
        ));
        colPoItems.setCellValueFactory(po -> new ReadOnlyStringWrapper(
                String.valueOf(po.getValue().getItems() == null ? 0 : po.getValue().getItems().size())
        ));
        // รวมจำนวนชิ้น
        colPoQty.setCellValueFactory(po -> new ReadOnlyStringWrapper(
                String.valueOf(
                        po.getValue().getItems() == null ? 0 :
                                po.getValue().getItems().stream().mapToInt(it -> it.getQuantity()).sum()
                )
        ));
        // รวมต้นทุนทั้งหมด
        colPoCost.setCellValueFactory(po -> new ReadOnlyStringWrapper(
                String.format("%.2f", po.getValue().getTotalAmount())
        ));

        tvPOs.setItems(poList);
        cbStatus.getItems().addAll("ALL", "OPEN", "RECEIVED");
        cbStatus.setValue("ALL");

        // โหลด PO รอบแรก
        reloadPOsAsync();
    }

    @Override
    public void onParams(Map<String, Object> params) {
        if (params == null) return;
        // รับ draft lines มาจาก Inventory
        Object x = params.get("draftFromInventory");
        if (x instanceof List<?> list) {
            List<OrderRow> rows = list.stream().map(obj -> {
                if (obj instanceof InventoryRow.DraftLine dl) {
                    int needed = Math.max(0, dl.getOrderQty());
                    int orderQty = needed; // default เท่ากับ needed แก้ได้ในตาราง
                    return new OrderRow(dl.getSupplier(), dl.getPartCode(), dl.getName(), needed, orderQty);
                }
                return null;
            }).filter(Objects::nonNull).toList();
            addDraftRows(rows); // เติมเข้า backlog
            // (ถ้ามี TabPane ให้สลับไปแท็บ Backlog ที่นี่)
        }
    }

    // ---- Draft actions ----
    @FXML
    private void onClearDraft() {
        draftMaster.clear();
        draftFiltered.clear();
        cbSupplierDraft.getSelectionModel().clearSelection();
        txtSearchDraft.clear();
        lblStatus.setText("เคลียร์ Draft แล้ว");
    }

    @FXML
    private void onCreatePOs() {
        if (draftMaster.isEmpty()) {
            showInfo("ไม่มีรายการใน Draft");
            return;
        }
        // สร้าง PO ต่อ supplier แบบ background
        Task<Integer> t = new Task<>() {
            @Override protected Integer call() {
                Map<String, List<OrderRow>> grouped = draftMaster.stream()
                        .collect(Collectors.groupingBy(OrderRow::getSupplier, TreeMap::new, Collectors.toList()));
                int count = 0;
                for (var e : grouped.entrySet()) {
                    if (e.getValue().isEmpty()) continue;
                    orderService.createOrAppendDraftPO(e.getKey(), e.getValue());
                    count++;
                }
                // ❗️ห้ามแก้ UI/ObservableList ที่นี่ (background)
                return count;
            }
        };
        beforeTask(t);
        t.setOnSucceeded(ev -> {
            afterTask();
            // ✅ เคลียร์ Draft บน FX Thread เท่านั้น
            draftMaster.clear();
            draftFiltered.clear();
            cbSupplierDraft.getItems().clear();
            txtSearchDraft.clear();

            lblStatus.setText("สร้าง PO ต่อ supplier จำนวน " + t.getValue() + " ฉบับ");
            reloadPOsAsync();      // โหลดรายการ PO ใหม่
            applyDraftFilter();    // รีเฟรชมุมมอง Draft
        });
        t.setOnFailed(ev -> {
            afterTask();
            showError(t.getException());
        });
        new Thread(t, "create-pos").start();
    }

    // ---- PO actions ----
    @FXML
    private void onReloadPOs() {
        reloadPOsAsync();
    }

    @FXML
    private void onSubmitPO() {
        PurchaseOrder po = tvPOs.getSelectionModel().getSelectedItem();
        if (po == null) { showInfo("กรุณาเลือก PO"); return; }
        Task<Void> t = new Task<>() {
            @Override protected Void call() {
                orderService.submitPO(po.getId());
                return null;
            }
        };
        beforeTask(t);
        t.setOnSucceeded(e -> { afterTask(); lblStatus.setText("Submit PO เรียบร้อย"); reloadPOsAsync(); });
        t.setOnFailed(e -> { afterTask(); showError(t.getException()); });
        new Thread(t, "submit-po").start();
    }

    @FXML
    private void onMarkReceived() {
        PurchaseOrder po = tvPOs.getSelectionModel().getSelectedItem();
        if (po == null) { showInfo("กรุณาเลือก PO"); return; }
        TextInputDialog dlg = new TextInputDialog("");
        dlg.setTitle("Mark Received");
        dlg.setHeaderText("หมายเหตุ/เลขใบส่งของ (ถ้ามี)");
        dlg.setContentText("Note:");
        String note = dlg.showAndWait().orElse("");
        Task<Void> t = new Task<>() {
            @Override protected Void call() {
                orderService.receivePO(po.getId(), note);
                return null;
            }
        };
        beforeTask(t);
        t.setOnSucceeded(e -> { afterTask(); lblStatus.setText("รับเข้าเรียบร้อย"); reloadPOsAsync(); });
        t.setOnFailed(e -> { afterTask(); showError(t.getException()); });
        new Thread(t, "receive-po").start();
    }

    @FXML
    private void onExportCsv() {
        try {
            orderService.exportPOsCsv(new ArrayList<>(poList));
            showInfo("Export CSV สำเร็จ (ดูโฟลเดอร์ export/)");
        } catch (Exception e) {
            showError(e);
        }
    }

    // ---- PO loading with filter ----
    private void reloadPOsAsync() {
        final String status = cbStatus.getValue();
        final String supplier = cbSupplierPo.getValue();
        final LocalDate from = dpFrom.getValue();
        final LocalDate to = dpTo.getValue();

        Task<List<PurchaseOrder>> t = new Task<>() {
            @Override protected List<PurchaseOrder> call() {
                return orderService.listPOs(status, supplier, from, to);
            }
        };
        beforeTask(t);
        t.setOnSucceeded(e -> {
            afterTask();
            poList.setAll(t.getValue());
            // เติม combo supplier จากรายการ
            Set<String> sups = poList.stream().map(PurchaseOrder::getSupplier)
                    .filter(s -> s != null && !s.isBlank())
                    .collect(Collectors.toCollection(TreeSet::new));
            cbSupplierPo.getItems().setAll(sups);
            lblStatus.setText("โหลด PO: " + poList.size());
        });
        t.setOnFailed(e -> { afterTask(); showError(t.getException()); });
        new Thread(t, "reload-pos").start();
    }

    // ---- Helpers ----
    private void addDraftRows(List<OrderRow> rows) {
        draftMaster.addAll(rows);
        // เติม supplier filter
        Set<String> sups = draftMaster.stream().map(OrderRow::getSupplier)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));
        cbSupplierDraft.getItems().setAll(sups);
        applyDraftFilter();
        lblStatus.setText("เพิ่ม Draft: " + rows.size() + " รายการ");
    }

    private void applyDraftFilter() {
        final String kw = Optional.ofNullable(txtSearchDraft.getText()).orElse("").trim().toLowerCase();
        final String sup = cbSupplierDraft.getValue();
        Predicate<OrderRow> p = r -> {
            if (!kw.isEmpty()) {
                if (!(r.getPartCode().toLowerCase().contains(kw) || r.getName().toLowerCase().contains(kw)))
                    return false;
            }
            if (sup != null && !sup.isBlank() && !Objects.equals(sup, r.getSupplier())) return false;
            return true;
        };
        draftFiltered.setAll(draftMaster.stream().filter(p).toList());
    }

    private void beforeTask(Task<?> t) {
        progress.setVisible(true);
        lblStatus.setText("กำลังทำงาน...");
    }
    private void afterTask() {
        progress.setVisible(false);
    }
    private void showError(Throwable ex) {
        ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR, String.valueOf(ex.getMessage()), ButtonType.OK);
        a.setHeaderText("เกิดข้อผิดพลาด");
        a.showAndWait();
    }
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    /**
     * IntegerEditingCell: cell editor สำหรับจำนวนสั่งซื้อ (รองรับแก้ไขตัวเลข)
     */
    private static class IntegerEditingCell extends TableCell<OrderRow, Number> {
        private final TextField textField = new TextField();

        public IntegerEditingCell() {
            textField.setOnAction(e -> commitEditSafe());
            textField.focusedProperty().addListener((obs, was, is) -> {
                if (!is) commitEditSafe();
            });
        }
        @Override
        public void startEdit() {
            super.startEdit();
            if (isEmpty()) return;
            textField.setText(getItem() == null ? "0" : String.valueOf(getItem().intValue()));
            setGraphic(textField);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            textField.requestFocus();
            textField.selectAll();
        }
        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setContentDisplay(ContentDisplay.TEXT_ONLY);
            setGraphic(null);
        }
        @Override
        protected void updateItem(Number item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                setGraphic(textField);
                setText(null);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            } else {
                setText(String.valueOf(item == null ? 0 : item.intValue()));
                setGraphic(null);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }
        }
        private void commitEditSafe() {
            try {
                int val = Integer.parseInt(textField.getText().trim());
                if (val < 0) val = 0;
                commitEdit(val);
            } catch (NumberFormatException ignored) {
                cancelEdit();
            }
        }
    }
}
