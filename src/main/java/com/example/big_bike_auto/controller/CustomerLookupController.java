package com.example.big_bike_auto.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;


public class CustomerLookupController {

    // input
    @FXML private TextField tfCode;

    // header labels
    @FXML private Label lbCode, lbName, lbPlate, lbStatus, lbUpdated, lbReceived, lbBike;

    // notes
    @FXML private TextArea taNotes;

    // parts table
    @FXML private TableView<PartRow> tvParts;
    @FXML private TableColumn<PartRow, String> colPartName, colUnit, colQty, colUnitPrice, colTotal;

    // total
    @FXML private Label lbGrandTotal;

    private static final Path CUSTOMERS = Paths.get("data", "customers.json");
    private final ObservableList<PartRow> parts = FXCollections.observableArrayList();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final NumberFormat moneyFmt = NumberFormat.getNumberInstance(new Locale("th","TH"));

    @FXML
    public void initialize() {
        moneyFmt.setMinimumFractionDigits(0);
        moneyFmt.setMaximumFractionDigits(2);

        // map columns
        colPartName.setCellValueFactory(d -> d.getValue().partNameProperty());
        colUnit.setCellValueFactory(d -> d.getValue().unitProperty());
        colQty.setCellValueFactory(d -> new SimpleStringProperty(formatQty(d.getValue().getQuantity())));
        colUnitPrice.setCellValueFactory(d -> new SimpleStringProperty(formatMoney(d.getValue().getUnitPrice())));
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(formatMoney(d.getValue().getTotal())));
        tvParts.setItems(parts);

        // Enter ในช่อง code = กดค้นหา
        tfCode.setOnAction(e -> onSearch());
        clearUI();
    }

    @FXML
    private void onSearch() {
        String code = tfCode.getText() == null ? "" : tfCode.getText().trim();
        if (code.isEmpty()) {
            warn("กรุณากรอกโค้ดลูกค้า", "ใส่รหัส 10 หลัก เช่น CLCXWSI89B");
            return;
        }
        Map<String, Object> found = findByCustomerCode(code);
        if (found == null) {
            clearUI();
            warn("ไม่พบข้อมูล", "ไม่มีรายการที่ customerCode = " + code);
            return;
        }
        fillUI(found);
    }

    @FXML
    private void onClear() {
        tfCode.clear();
        clearUI();
        tfCode.requestFocus();
    }

    // ----------------- core logic -----------------

    /** อ่าน customers.json แล้วหา record ที่ customerCode ตรง */
    private Map<String, Object> findByCustomerCode(String code) {
        List<Map<String, Object>> list = readCustomers();
        for (Map<String, Object> m : list) {
            String c = str(m.get("customerCode"));
            if (!c.isEmpty() && c.equalsIgnoreCase(code)) return m;
        }
        return null;
    }

    private List<Map<String, Object>> readCustomers() {
        try {
            ensureArrayFile(CUSTOMERS);
            String json = Files.readString(CUSTOMERS, StandardCharsets.UTF_8).trim();
            if (json.isEmpty()) return List.of();
            var type = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> list = gson.fromJson(json, type);
            return list != null ? list : List.of();
        } catch (Exception e) {
            // ไม่ให้ล้มทั้งหน้า
            return List.of();
        }
    }

    private void ensureArrayFile(Path p) throws java.io.IOException {
        if (p.getParent() != null && !Files.exists(p.getParent())) Files.createDirectories(p.getParent());
        if (!Files.exists(p)) Files.writeString(p, "[]", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    }

    // ----------------- UI helpers -----------------

    private void clearUI() {
        lbCode.setText("-");
        lbName.setText("-");
        lbPlate.setText("-");
        lbStatus.setText("-");
        lbUpdated.setText("-");
        lbReceived.setText("-");
        lbBike.setText("-");
        taNotes.clear();
        parts.clear();
        lbGrandTotal.setText("0");
    }

    @SuppressWarnings("unchecked")
    private void fillUI(Map<String, Object> m) {
        lbCode.setText(str(m.get("customerCode")));
        lbName.setText(str(m.get("name")));
        lbPlate.setText(str(m.get("plate")));
        lbStatus.setText(str(m.get("status")));
        lbReceived.setText(prettyDate(str(m.get("receivedDate"))));
        lbBike.setText(str(m.get("bikeModel")));

        Map<String, Object> repair = (m.get("repair") instanceof Map) ? (Map<String, Object>) m.get("repair") : null;
        String updated = repair != null ? str(repair.get("lastUpdated")) : "";
        lbUpdated.setText(prettyDateTime(updated));

        taNotes.setText(repair != null ? str(repair.get("notes")) : "");

        parts.clear();
        double sum = 0;
        if (repair != null && repair.get("parts") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?,?> pm) {
                    String name = str(pm.get("partName"));
                    double qty = toDouble(pm.get("quantity"));
                    String unit = str(pm.get("unit"));
                    double price = toDouble(pm.get("unitPrice"));
                    if (!name.isEmpty()) {
                        parts.add(new PartRow(name, qty, unit, price));
                        sum += qty * price;
                    }
                }
            }
        }
        lbGrandTotal.setText(formatMoney(sum));
    }

    // ----------------- utils -----------------

    private String str(Object o) { return o == null ? "" : String.valueOf(o).trim(); }

    private String formatMoney(double v) { return moneyFmt.format(v); }

    private String formatQty(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long)Math.rint(v));
        return formatMoney(v);
    }

    private double toDouble(Object o) {
        if (o == null) return 0;
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    /** yyyy-MM-dd -> dd/MM/yyyy ; ไม่ได้ก็คืนเดิม/ "-" */
    private String prettyDate(String s) {
        if (s == null || s.isBlank()) return "-";
        try {
            LocalDate d = LocalDate.parse(s);
            return String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
        } catch (Exception e) {
            return s;
        }
    }

    /** ISO date-time หรือ yyyy-MM-dd -> รูปแบบอ่านง่าย */
    private String prettyDateTime(String s) {
        if (s == null || s.isBlank()) return "-";
        // กรณีเป็น date อย่างเดียว
        if (s.length() == 10) return prettyDate(s) + " 00:00:00";
        try {
            return OffsetDateTime.parse(s).toLocalDateTime().toString().replace('T',' ').split("\\.")[0];
        } catch (DateTimeParseException e) {
            try {
                return java.time.LocalDateTime.parse(s).toString().replace('T',' ').split("\\.")[0];
            } catch (DateTimeParseException ex) {
                return s;
            }
        }
    }

    private void warn(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(header);
        a.showAndWait();
    }

    // Table model
    public static class PartRow {
        private final StringProperty partName = new SimpleStringProperty();
        private final DoubleProperty quantity = new SimpleDoubleProperty(0);
        private final StringProperty unit = new SimpleStringProperty();
        private final DoubleProperty unitPrice = new SimpleDoubleProperty(0);

        public PartRow(String name, double qty, String unit, double price) {
            this.partName.set(name);
            this.quantity.set(qty);
            this.unit.set(unit);
            this.unitPrice.set(price);
        }

        public StringProperty partNameProperty() { return partName; }
        public StringProperty unitProperty() { return unit; }
        public double getQuantity() { return quantity.get(); }
        public double getUnitPrice() { return unitPrice.get(); }
        public double getTotal() { return getQuantity() * getUnitPrice(); }
    }
}
