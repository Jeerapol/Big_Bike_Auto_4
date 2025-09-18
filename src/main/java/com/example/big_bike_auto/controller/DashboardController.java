package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.router.RouterHub;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label lblTotalRepairs;
    @FXML private Label lblPendingRepairs;
    @FXML private Label lblInventoryItems;

    @FXML private TableView<Map<String, Object>> tblRecent;
    @FXML private TableColumn<Map<String, Object>, String> colType;
    @FXML private TableColumn<Map<String, Object>, String> colTitle;
    @FXML private TableColumn<Map<String, Object>, String> colWhen;

    private static final Path CUSTOMERS = Paths.get("data", "customers.json");
    private static final Path PARTS     = Paths.get("data", "parts.json");
    private static final Set<String> PENDING = Set.of("RECEIVED", "IN_PROGRESS");

    @FXML
    public void initialize() {
        assert lblTotalRepairs   != null;
        assert lblPendingRepairs != null;
        assert lblInventoryItems != null;
        assert tblRecent         != null;
        assert colType           != null;
        assert colTitle          != null;
        assert colWhen           != null;

        setupRecentTable();

        refresh();
        Platform.runLater(this::refresh);

        // ✅ ดับเบิลคลิกแถวเพื่อเปิดหน้า RepairDetails พร้อมพารามิเตอร์ customerId
        tblRecent.setRowFactory(tv -> {
            TableRow<Map<String, Object>> row = new TableRow<>();
            row.setOnMouseClicked(evt -> {
                if (!row.isEmpty()
                        && evt.getButton() == MouseButton.PRIMARY
                        && evt.getClickCount() == 2) {
                    Map<String, Object> data = row.getItem();
                    String customerId = asStr(data.get("id"));
                    if (!customerId.isBlank()) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("customerId", customerId);
                        RouterHub.getRouter().navigate("repairDetails", params);
                    } else {
                        showWarn("ข้อมูลไม่ครบ", "ไม่พบรหัสงาน (id) ในรายการนี้");
                    }
                }
            });
            return row;
        });
    }

    private void setupRecentTable() {
        colType.setCellValueFactory(row -> new ReadOnlyStringWrapper("งานซ่อม"));
        colTitle.setCellValueFactory(row -> {
            Map<String, Object> m = row.getValue();
            String name  = asStr(m.get("name"));
            String plate = asStr(m.get("plate"));
            String sym   = asStr(m.get("symptom"));
            String text = String.format("%s | %s : %s",
                    name.isBlank() ? "-" : name,
                    plate.isBlank() ? "-" : plate,
                    sym.isBlank() ? "-" : sym
            );
            return new ReadOnlyStringWrapper(text);
        });
        colWhen.setCellValueFactory(row -> new ReadOnlyStringWrapper(pickWhen(row.getValue())));
    }

    private void refresh() {
        List<Map<String, Object>> customers = readJsonArrayAsMap(CUSTOMERS);
        int totalRepairs = customers.size();
        int pendingRepairs = (int) customers.stream()
                .map(m -> asStr(m.get("status")).toUpperCase(Locale.ROOT))
                .filter(PENDING::contains)
                .count();
        int inventoryItems = readJsonArrayAsMap(PARTS).size();

        lblTotalRepairs.setText(String.valueOf(totalRepairs));
        lblPendingRepairs.setText(String.valueOf(pendingRepairs));
        lblInventoryItems.setText(String.valueOf(inventoryItems));

        List<Map<String, Object>> recent = customers.stream()
                .sorted(Comparator.comparing(this::extractInstantForSort,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(50)
                .collect(Collectors.toList());
        tblRecent.getItems().setAll(recent);
    }

    // ------------------- JSON utils -------------------
    private List<Map<String, Object>> readJsonArrayAsMap(Path file) {
        try {
            ensureArrayFile(file);
            String json = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (json.isEmpty()) return List.of();
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> list = gson.fromJson(json, type);
            return list != null ? list : List.of();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void ensureArrayFile(Path file) throws java.io.IOException {
        Path dir = file.getParent();
        if (dir != null && !Files.exists(dir)) Files.createDirectories(dir);
        if (!Files.exists(file)) Files.writeString(file, "[]", StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    }

    private String asStr(Object o) { return o == null ? "" : String.valueOf(o).trim(); }

    private String pickWhen(Map<String, Object> m) {
        Object repairObj = m.get("repair");
        if (repairObj instanceof Map) {
            String s = asStr(((Map<?, ?>) repairObj).get("lastUpdated"));
            if (!s.isBlank()) return prettyWhen(s);
        }
        String received = asStr(m.get("receivedDate"));
        return received.isBlank() ? "-" : prettyWhen(received);
    }

    private String extractInstantForSort(Map<String, Object> m) {
        Object repairObj = m.get("repair");
        if (repairObj instanceof Map) {
            String s = asStr(((Map<?, ?>) repairObj).get("lastUpdated"));
            String k = isoKey(s);
            if (k != null) return k;
        }
        return isoKey(asStr(m.get("receivedDate")));
    }

    private String isoKey(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.length() == 10) { LocalDate.parse(s); return s + "T00:00:00"; }
            else { OffsetDateTime.parse(s); return s; }
        } catch (DateTimeParseException e1) {
            try { java.time.LocalDateTime.parse(s); return s; }
            catch (DateTimeParseException e2) { return null; }
        }
    }

    private String prettyWhen(String s) {
        try {
            if (s.length() == 10) {
                LocalDate d = LocalDate.parse(s);
                return String.format("%04d-%02d-%02d 00:00:00", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
            } else {
                try {
                    java.time.LocalDateTime ldt = OffsetDateTime.parse(s).toLocalDateTime();
                    return ldt.toString().replace('T', ' ').split("\\.")[0];
                } catch (DateTimeParseException e) {
                    return java.time.LocalDateTime.parse(s).toString().replace('T', ' ').split("\\.")[0];
                }
            }
        } catch (Exception ignore) { return s; }
    }

    private void showWarn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(title);
        a.showAndWait();
    }
}
