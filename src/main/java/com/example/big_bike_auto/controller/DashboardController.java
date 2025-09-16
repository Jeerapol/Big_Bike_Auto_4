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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Dashboard: แสดงสรุปและกิจกรรมล่าสุด
 * - ใช้ lambda cell value factory (ไม่พึ่ง reflection) เพื่อกันคอลัมน์ว่าง
 * - ดับเบิลคลิก/เมนูคลิกขวา เปิดหน้ารายละเอียดงานซ่อม
 */
public class DashboardController {

    @FXML private Label lblTotalRepairs;
    @FXML private Label lblPendingRepairs;
    @FXML private Label lblInventoryItems;

    @FXML private TableView<RecentItem> tblRecent;
    @FXML private TableColumn<RecentItem, String> colType;
    @FXML private TableColumn<RecentItem, String> colTitle;
    @FXML private TableColumn<RecentItem, String> colWhen;

    private final RepairDetailsController.FileRepairJobRepository repo =
            new RepairDetailsController.FileRepairJobRepository();
    private final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    // ชุดสถานะที่ถือว่า "คงค้าง"
    private static final Set<RepairDetailsController.RepairStatus> PENDING_STATUSES =
            EnumSet.of(
                    RepairDetailsController.RepairStatus.RECEIVED,
                    RepairDetailsController.RepairStatus.DIAGNOSING,
                    RepairDetailsController.RepairStatus.WAIT_PARTS,
                    RepairDetailsController.RepairStatus.REPAIRING,
                    RepairDetailsController.RepairStatus.QA
            );

    /** Row model (พก jobId เปิดรายละเอียดได้) */
    public static class RecentItem {
        private final String type;
        private final String title;
        private final String whenText; // เปลี่ยนชื่อจาก when -> whenText กันชนชื่อ/ความสับสน
        private final String jobId;
        public RecentItem(String type, String title, String whenText, String jobId) {
            this.type = type;
            this.title = title;
            this.whenText = whenText;
            this.jobId = jobId;
        }
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getWhenText() { return whenText; }
        public String getJobId() { return jobId; }
        @Override public String toString() { return type + " | " + title + " | " + whenText + " | " + jobId; }
    }

    @FXML
    private void initialize() {
        // ❗ ใช้ lambda แทน PropertyValueFactory ปิดจุดพังเรื่องชื่อพร็อพเพอร์ตี
        colType.setCellValueFactory(cd -> new ReadOnlyStringWrapper(nullToDash(cd.getValue().getType())));
        colTitle.setCellValueFactory(cd -> new ReadOnlyStringWrapper(nullToDash(cd.getValue().getTitle())));
        colWhen.setCellValueFactory(cd -> new ReadOnlyStringWrapper(nullToDash(cd.getValue().getWhenText())));

        // Placeholder บอกผู้ใช้เมื่อยังไม่มีข้อมูล
        tblRecent.setPlaceholder(new Label("ยังไม่มีกิจกรรมล่าสุด"));

        // ดับเบิลคลิกซ้าย หรือ คลิกขวาแล้วเลือก "เปิดรายละเอียด"
        tblRecent.setRowFactory(tv -> {
            TableRow<RecentItem> row = new TableRow<>();

            // ดับเบิลคลิกซ้าย
            row.setOnMouseClicked(event -> {
                if (row.isEmpty()) return;
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    RecentItem item = row.getItem();
                    System.out.println("[DBG] double-click: " + item);
                    openDetailsIfPossible(item);
                }
            });

            // เมนูคลิกขวา (กันพลาด)
            MenuItem open = new MenuItem("เปิดรายละเอียดงานซ่อม");
            open.setOnAction(e -> {
                RecentItem item = row.getItem();
                System.out.println("[DBG] context-open: " + item);
                openDetailsIfPossible(item);
            });
            ContextMenu menu = new ContextMenu(open);
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(menu)
            );
            return row;
        });

        refresh();
    }

    /** โหลดข้อมูลสรุป + เติมรายการล่าสุด (งานซ่อม + ลูกค้าใหม่) */
    private void refresh() {
        try {
            List<RepairDetailsController.RepairJob> all = repo.findAll();

            // 1) ตัวเลขสถิติ
            int total = all.size();
            long pending = all.stream()
                    .filter(j -> j.getStatus() != null && PENDING_STATUSES.contains(j.getStatus()))
                    .count();

            lblTotalRepairs.setText(String.valueOf(total));
            lblPendingRepairs.setText(String.valueOf(pending));
            lblInventoryItems.setText("-"); // ยังไม่มีระบบสต็อก

            // 2) งานซ่อมล่าสุดเรียงใหม่->เก่า
            all.sort(Comparator.comparing(
                    RepairDetailsController.RepairJob::getReceivedDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed());

            ObservableList<RecentItem> data = FXCollections.observableArrayList();
            int limit = Math.min(10, all.size());
            for (int i = 0; i < limit; i++) {
                var j = all.get(i);
                String when = j.getReceivedDate() != null ? DATE.format(j.getReceivedDate()) : "-";
                String type = (j.getStatus() != null) ? j.getStatus().toString() : "งานซ่อม";
                String title = safe(j.getCustomerName()) + " • " + safe(j.getBikeModel());
                data.add(new RecentItem(type, title, when, j.getJobId().toString()));
            }

            // 3) ลูกค้าใหม่ล่าสุด (สูงสุด 5)
            try {
                CustomerRepository cRepo = new CustomerRepository();
                List<Customer> recentCus = cRepo.findRecent(5);
                for (Customer c : recentCus) {
                    String whenCus = (c.getRegisteredAt() != null) ? c.getRegisteredAt().toLocalDate().toString() : "-";
                    String titleCus = (c.getName() != null ? c.getName() : "-") + " • " + (c.getPhone() != null ? c.getPhone() : "-");
                    // id ลูกค้า = jobId (เรากำหนดไว้ตอนลงทะเบียน)
                    data.add(new RecentItem("ลูกค้าใหม่", titleCus, whenCus, c.getId()));
                }
            } catch (Exception ignore) {
                // ถ้าอ่าน customers.json ไม่ได้ ให้ข้ามไปเฉย ๆ
            }

            tblRecent.setItems(data);
            System.out.println("[DBG] recent rows = " + data.size());

        } catch (Exception ex) {
            lblTotalRepairs.setText("0");
            lblPendingRepairs.setText("0");
            lblInventoryItems.setText("-");
            tblRecent.setItems(FXCollections.observableArrayList());
            ex.printStackTrace();
        }
    }

    /** เปิดหน้ารายละเอียดงานซ่อม ถ้า jobId เป็น UUID ถูกต้อง */
    private void openDetailsIfPossible(RecentItem item) {
        try {
            if (item == null || item.getJobId() == null || item.getJobId().isBlank()) {
                System.out.println("[DBG] skip open: item/jobId null");
                return;
            }
            UUID id = UUID.fromString(item.getJobId());
            RouterHub.openRepairDetails(id); // = RouterHub.getRouter().toRepairDetails(id)
        } catch (IllegalArgumentException bad) {
            System.out.println("[DBG] bad jobId: " + item.getJobId());
            // jobId ไม่ใช่ UUID -> ข้าม (อาจเป็นแถวที่ไม่ได้ลิงก์ไปงานซ่อมจริง)
        } catch (Exception any) {
            any.printStackTrace();
        }
    }

    private static String safe(String s) { return (s == null || s.isBlank()) ? "-" : s; }
    private static String nullToDash(String s) { return s == null || s.isBlank() ? "-" : s; }
}
