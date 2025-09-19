package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.model.RepairStatus;
import com.example.big_bike_auto.router.ReceivesParams;
import com.example.big_bike_auto.router.RouterHub;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


public class RepairDetailsController implements ReceivesParams {

    // ===== top info =====
    @FXML private Label lbJobId;
    @FXML private Label lbCustomerName;
    @FXML private Label lbBikeModel;
    @FXML private Label lbReceivedDate;
    @FXML private ComboBox<RepairStatus> cbStatus; // ใช้ enum สำหรับค่าจริง

    // ===== note =====
    @FXML private TextArea taNotes;

    // ===== parts table =====
    @FXML private TableView<PartRow> tvParts;
    @FXML private TableColumn<PartRow, String> colPartName;
    @FXML private TableColumn<PartRow, String> colQty;
    @FXML private TableColumn<PartRow, String> colUnit;
    @FXML private TableColumn<PartRow, String> colUnitPrice;
    @FXML private TableColumn<PartRow, String> colTotal;

    @FXML private TextField tfPartName;
    @FXML private TextField tfQty;
    @FXML private TextField tfUnit;
    @FXML private TextField tfUnitPrice;

    @FXML private Label lbGrandTotal;

    // ===== data =====
    private static final Path CUSTOMERS = Paths.get("data", "customers.json");
    private final ObservableList<PartRow> parts = FXCollections.observableArrayList();
    private Map<String, Object> currentCustomer; // map ของลูกค้าที่กำลังแก้
    private List<Map<String, Object>> allCustomers; // ทั้งไฟล์
    private String currentId;

    private static final List<RepairStatus> STATUSES = List.of(
            RepairStatus.RECEIVED,
            RepairStatus.IN_PROGRESS,
            RepairStatus.WAITING_PARTS,
            RepairStatus.COMPLETED,
            RepairStatus.CANCELLED
    );

    @FXML
    public void initialize() {
        // map columns
        colPartName.setCellValueFactory(data -> data.getValue().partNameProperty());
        colQty.setCellValueFactory(data -> new SimpleStringProperty(formatQty(data.getValue().getQuantity())));
        colUnit.setCellValueFactory(data -> data.getValue().unitProperty());
        colUnitPrice.setCellValueFactory(data -> new SimpleStringProperty(formatMoney(data.getValue().getUnitPrice())));
        colTotal.setCellValueFactory(data -> new SimpleStringProperty(formatMoney(data.getValue().getTotal())));
        tvParts.setItems(parts);

        // ComboBox: แสดงเป็นภาษาไทย แต่ค่าเป็น enum
        cbStatus.setConverter(new StringConverter<>() {
            @Override public String toString(RepairStatus s) {
                return (s == null) ? "" : s.labelTH();
            }
            @Override public RepairStatus fromString(String str) {
                return STATUSES.stream()
                        .filter(s -> Objects.equals(s.labelTH(), str))
                        .findFirst()
                        .orElse(RepairStatus.RECEIVED);
            }
        });
        cbStatus.getItems().setAll(STATUSES);

        updateGrandTotalLabel();
    }

    // ===== ReceivesParams: รับ customerId =====
    @Override
    public void onParams(Map<String, Object> params) {
        String id = safe(params == null ? null : params.get("customerId"));
        if (id.isBlank()) {
            showError("พารามิเตอร์ไม่ถูกต้อง", "ไม่พบ customerId");
            return;
        }
        this.currentId = id;
        loadCustomerById(id);
    }

    // ===== actions from buttons =====
    @FXML
    private void onAddPart() {
        String name = tfPartName.getText() == null ? "" : tfPartName.getText().trim();
        String unit = tfUnit.getText() == null ? "" : tfUnit.getText().trim();
        double qty = parseDouble(tfQty.getText(), "จำนวน");
        double unitPrice = parseDouble(tfUnitPrice.getText(), "ราคา/หน่วย");
        if (name.isBlank()) {
            warn("ข้อมูลไม่ครบ", "กรุณากรอกชื่ออะไหล่");
            return;
        }
        if (qty <= 0 || unitPrice < 0) return; // แจ้งแล้วใน parse

        parts.add(new PartRow(name, qty, unit, unitPrice));
        clearPartInputs();
        updateGrandTotalLabel();
    }

    @FXML
    private void onRemoveSelectedPart() {
        PartRow sel = tvParts.getSelectionModel().getSelectedItem();
        if (sel == null) {
            warn("ยังไม่ได้เลือก", "โปรดเลือกรายการในตารางก่อน");
            return;
        }
        parts.remove(sel);
        updateGrandTotalLabel();
    }

    @FXML
    private void onSave() {
        if (currentCustomer == null || allCustomers == null) {
            warn("ไม่พร้อมบันทึก", "ยังไม่พบข้อมูลลูกค้า");
            return;
        }
        // ✅ บันทึกเป็น code (String) ของสถานะ
        RepairStatus st = cbStatus.getValue();
        setMap(currentCustomer, "status", st == null ? RepairStatus.RECEIVED.code() : st.code());

        Map<String, Object> repair = asMap(currentCustomer.get("repair"));
        if (repair == null) {
            repair = new LinkedHashMap<>();
            currentCustomer.put("repair", repair);
        }
        setMap(repair, "notes", taNotes.getText());
        setMap(repair, "parts", parts.stream().map(PartRow::toMap).collect(Collectors.toList()));
        setMap(repair, "grandTotal", parts.stream().mapToDouble(PartRow::getTotal).sum());
        setMap(repair, "lastUpdated", java.time.LocalDateTime.now().toString());

        writeCustomers(allCustomers);
        info("บันทึกสำเร็จ", "อัปเดตข้อมูลงานซ่อมแล้ว");
    }

    @FXML
    private void onBack() {
        RouterHub.getRouter().navigate("dashboard");
    }

    /**
     * ⭐ เมธอดที่ FXML เรียกใช้: ส่ง “คำค้นอะไหล่” ไปหน้า Inventory
     * เหมาะเมื่อสถานะเป็น “รออะไหล่” ให้ทีมไป match part จริงแล้วส่งต่อ Draft PO
     */
    @FXML
    private void onSendShortageToOrders() {
        if (parts.isEmpty()) {
            warn("ไม่มีรายการ", "ยังไม่มีรายการอะไหล่ในตาราง");
            return;
        }
        String q = parts.stream()
                .map(PartRow::getPartName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining(" "));
        if (q.isBlank()) {
            warn("ไม่มีคำค้น", "กรุณาระบุชื่ออะไหล่อย่างน้อย 1 รายการ");
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("q", q);
        RouterHub.getRouter().navigate("inventory", params);
    }

    // ===== load/save helpers =====
    @SuppressWarnings("unchecked")
    private void loadCustomerById(String id) {
        this.allCustomers = readCustomers();
        this.currentCustomer = allCustomers.stream()
                .filter(m -> id.equals(safe(m.get("id"))))
                .findFirst()
                .orElse(null);

        if (currentCustomer == null) {
            showError("ไม่พบข้อมูล", "ไม่พบลูกค้า/งานซ่อม id=" + id);
            return;
        }

        // ---- หัวงาน ----
        lbJobId.setText(safe(currentCustomer.get("id")));
        lbCustomerName.setText(safe(currentCustomer.getOrDefault("name", "-")));
        lbBikeModel.setText(safe(currentCustomer.getOrDefault("bikeModel", "-")));
        String received = safe(currentCustomer.get("receivedDate"));
        if (!received.isBlank()) {
            try {
                LocalDate d = LocalDate.parse(received);
                lbReceivedDate.setText(String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear()));
            } catch (Exception ignore) { lbReceivedDate.setText(received); }
        } else lbReceivedDate.setText("-");

        // ---- สถานะ: เก็บเป็น code (String) → แปลงเป็น enum แล้ว select ----
        String statusCode = safe(currentCustomer.get("status"));
        RepairStatus statusEnum = RepairStatus.fromCode(statusCode);
        cbStatus.getSelectionModel().select(statusEnum);

        // ---- repair ----
        Map<String, Object> repair = asMap(currentCustomer.get("repair"));
        String notes = repair == null ? "" : safe(repair.get("notes"));
        taNotes.setText(notes);

        parts.clear();
        if (repair != null && repair.get("parts") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?,?> m) {
                    String partName = safe(m.get("partName"));
                    double quantity  = asDouble(m.get("quantity"));
                    String unit      = safe(m.get("unit"));
                    double unitPrice = asDouble(m.get("unitPrice"));
                    if (!partName.isBlank()) parts.add(new PartRow(partName, quantity, unit, unitPrice));
                }
            }
        }
        updateGrandTotalLabel();
    }

    private List<Map<String, Object>> readCustomers() {
        try {
            ensureArrayFile(CUSTOMERS);
            String json = Files.readString(CUSTOMERS, StandardCharsets.UTF_8).trim();
            if (json.isEmpty()) return new ArrayList<>();
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> list = gson.fromJson(json, type);
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private void writeCustomers(List<Map<String, Object>> list) {
        try {
            ensureArrayFile(CUSTOMERS);
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(list);
            Files.writeString(CUSTOMERS, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ex) {
            showError("บันทึกไม่สำเร็จ", ex.getMessage());
        }
    }

    private void ensureArrayFile(Path file) throws java.io.IOException {
        Path dir = file.getParent();
        if (dir != null && !Files.exists(dir)) Files.createDirectories(dir);
        if (!Files.exists(file)) Files.writeString(file, "[]", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    }

    // ===== misc utils =====
    private String safe(Object o) { return o == null ? "" : String.valueOf(o).trim(); }
    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) { return (o instanceof Map) ? (Map<String, Object>) o : null; }

    private void setMap(Map<String, Object> m, String k, Object v) {
        if (v == null) m.remove(k); else m.put(k, v);
    }

    private void clearPartInputs() {
        tfPartName.clear(); tfQty.clear(); tfUnit.clear(); tfUnitPrice.clear();
        tfPartName.requestFocus();
    }

    private double parseDouble(String s, String fieldName) {
        try {
            double d = Double.parseDouble(s == null ? "" : s.trim());
            if (d < 0) throw new NumberFormatException("negative");
            return d;
        } catch (Exception ex) {
            warn("ข้อมูลไม่ถูกต้อง", fieldName + " ต้องเป็นตัวเลข >= 0");
            return -1;
        }
    }

    private double asDouble(Object o) {
        if (o == null) return 0;
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    private String formatMoney(double v) {
        NumberFormat f = NumberFormat.getNumberInstance(new Locale("th", "TH"));
        f.setMinimumFractionDigits(0); f.setMaximumFractionDigits(2);
        return f.format(v);
    }
    private String formatQty(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long) Math.rint(v));
        return formatMoney(v);
    }

    private void updateGrandTotalLabel() {
        double sum = parts.stream().mapToDouble(PartRow::getTotal).sum();
        lbGrandTotal.setText(formatMoney(sum));
    }

    private void warn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }
    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }
    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }

    // ===== row model for TableView =====
    public static class PartRow {
        private final StringProperty partName = new SimpleStringProperty();
        private final DoubleProperty quantity = new SimpleDoubleProperty(1);
        private final StringProperty unit = new SimpleStringProperty();
        private final DoubleProperty unitPrice = new SimpleDoubleProperty(0);

        public PartRow(String partName, double quantity, String unit, double unitPrice) {
            this.partName.set(partName);
            this.quantity.set(quantity);
            this.unit.set(unit);
            this.unitPrice.set(unitPrice);
        }

        public StringProperty partNameProperty() { return partName; }
        public String getPartName() { return partName.get(); } // ใช้รวมคำค้นส่งไปหน้า Inventory
        public StringProperty unitProperty() { return unit; }
        public double getQuantity() { return quantity.get(); }
        public double getUnitPrice() { return unitPrice.get(); }
        public double getTotal() { return getQuantity() * getUnitPrice(); }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("partName", partName.get());
            m.put("quantity", getQuantity());
            m.put("unit", unit.get());
            m.put("unitPrice", getUnitPrice());
            return m;
        }
    }
}
