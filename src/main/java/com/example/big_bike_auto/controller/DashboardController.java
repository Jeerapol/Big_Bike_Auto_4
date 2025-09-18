package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.RouterHub;
import com.example.big_bike_auto.customer.Customer;
import com.example.big_bike_auto.customer.CustomerRepository;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Dashboard ใหม่:
 * - ดึงข้อมูลทั้งหมดจาก customers.json
 * - นับ total/pending จากสถานะของ Customer
 * - กิจกรรมล่าสุด = ลูกค้าเรียงใหม่→เก่า
 */
public class DashboardController {

    @FXML private Label lblTotalRepairs;
    @FXML private Label lblPendingRepairs;
    @FXML private Label lblInventoryItems;

    @FXML private TableView<RecentItem> tblRecent;
    @FXML private TableColumn<RecentItem, String> colType;
    @FXML private TableColumn<RecentItem, String> colTitle;
    @FXML private TableColumn<RecentItem, String> colWhen;

    private final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** แถวในตาราง (ถือ id ลูกค้าเพื่อเปิดรายละเอียด) */
    public static class RecentItem {
        private final String type;
        private final String title;
        private final String whenText;
        private final String customerId;
        public RecentItem(String type, String title, String whenText, String customerId) {
            this.type = type; this.title = title; this.whenText = whenText; this.customerId = customerId;
        }
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getWhenText() { return whenText; }
        public String getCustomerId() { return customerId; }
    }

    @FXML
    private void initialize() {
        colType.setCellValueFactory(cd -> new ReadOnlyStringWrapper(nullToDash(cd.getValue().getType())));
        colTitle.setCellValueFactory(cd -> new ReadOnlyStringWrapper(nullToDash(cd.getValue().getTitle())));
        colWhen.setCellValueFactory(cd -> new ReadOnlyStringWrapper(nullToDash(cd.getValue().getWhenText())));

        tblRecent.setPlaceholder(new Label("ยังไม่มีกิจกรรมล่าสุด"));
        tblRecent.setRowFactory(tv -> {
            TableRow<RecentItem> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (row.isEmpty()) return;
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    openDetailsIfPossible(row.getItem());
                }
            });
            return row;
        });

        refresh();
    }

    private void refresh() {
        try {
            List<Customer> all = new CustomerRepository().findAll();

            // ตัวเลขสถิติ
            int total = all.size();
            long pending = all.stream().filter(c ->
                    c.getStatus() == Customer.RepairStatus.RECEIVED ||
                            c.getStatus() == Customer.RepairStatus.DIAGNOSING ||
                            c.getStatus() == Customer.RepairStatus.WAIT_PARTS ||
                            c.getStatus() == Customer.RepairStatus.REPAIRING ||
                            c.getStatus() == Customer.RepairStatus.QA
            ).count();

            lblTotalRepairs.setText(String.valueOf(total));
            lblPendingRepairs.setText(String.valueOf(pending));
            long itemsNeeded = all.stream()
                    .filter(c -> c.getStatus() != Customer.RepairStatus.DONE)
                    .filter(c -> c.getRepair() != null && c.getRepair().getParts() != null)
                    .flatMap(c -> c.getRepair().getParts().stream())
                    .mapToLong(p -> p.getQuantity() == null ? 0 : p.getQuantity())
                    .sum();

            lblInventoryItems.setText(String.valueOf(itemsNeeded));


            // ระบุชนิด Customer ให้ lambda ชัดเจน
            all.sort(Comparator.comparing(
                    (Customer c) -> c.getRegisteredAt() != null ? c.getRegisteredAt()
                            : (c.getReceivedDate() != null ? c.getReceivedDate().atStartOfDay() : null),
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed());

            ObservableList<RecentItem> data = FXCollections.observableArrayList();
            int limit = Math.min(20, all.size());
            for (int i = 0; i < limit; i++) {
                Customer c = all.get(i);
                String type = mapType(c.getStatus());
                String title = safe(c.getName()) + " • " + safe(c.getPlate())
                        + " (" + safe(c.getProvince()) + "/" + safe(c.getPhone()) + ")";
                String when = c.getReceivedDate() != null ? DATE.format(c.getReceivedDate())
                        : (c.getRegisteredAt() != null ? c.getRegisteredAt().toLocalDate().toString() : "-");
                data.add(new RecentItem(type, title, when, c.getId()));
            }
            tblRecent.setItems(data);

        } catch (Exception e) {
            lblTotalRepairs.setText("0");
            lblPendingRepairs.setText("0");
            lblInventoryItems.setText("-");
            tblRecent.setItems(FXCollections.observableArrayList());
            e.printStackTrace();
        }
    }


    private void openDetailsIfPossible(RecentItem item) {
        try {
            if (item == null || item.getCustomerId() == null || item.getCustomerId().isBlank()) return;
            UUID id = UUID.fromString(item.getCustomerId());
            // สมมติ RouterHub รองรับเปิดด้วย customer id (ถ้ายังไม่รองรับ ให้ปรับ Router/Details ตามนี้)
            RouterHub.openRepairDetails(id);
        } catch (Exception ignore) {
            // ถ้า Router ยังต้องการประเภท RepairJob จริง ๆ ให้เปลี่ยนไปเปิดหน้า CustomerDetails แทน
        }
    }

    private static String mapType(Customer.RepairStatus st) {
        if (st == null) return "งานซ่อม";
        return switch (st) {
            case RECEIVED -> "รับงานแล้ว";
            case DIAGNOSING -> "วินิจฉัย";
            case WAIT_PARTS -> "รออะไหล่";
            case REPAIRING -> "กำลังซ่อม";
            case QA -> "ตรวจคุณภาพ";
            case DONE -> "เสร็จสิ้น";
        };
    }

    private static String safe(String s) { return (s == null || s.isBlank()) ? "-" : s; }
    private static String nullToDash(String s) { return (s == null || s.isBlank()) ? "-" : s; }
}
