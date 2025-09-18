package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.RouterHub;
import com.example.big_bike_auto.auth.Role;
import com.example.big_bike_auto.auth.SessionContext;
import com.example.big_bike_auto.customer.Customer;
import com.example.big_bike_auto.customer.CustomerRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RepairListController (customers.json only)
 * - โหลดรายการ "ลูกค้าที่ลงทะเบียนซ่อม" จาก CustomerRepository
 * - แสดงใน TableView พร้อมสถานะ (ข้อความไทย) และวันที่รับงาน
 * - RBAC แสดง/ซ่อนปุ่มตาม Role
 * - ดับเบิลคลิกแถว: TECHNICIAN/ADMIN -> Repair Details, STAFF -> Register (edit)
 *
 * หมายเหตุ:
 * - jobId = customerId (UUID string ใน Customer.id)
 */
public class RepairListController {

    @FXML private TableView<JobRow> tvJobs;
    @FXML private TableColumn<JobRow, String> colId;
    @FXML private TableColumn<JobRow, String> colCustomer;
    @FXML private TableColumn<JobRow, String> colBike;
    @FXML private TableColumn<JobRow, String> colDate;
    @FXML private TableColumn<JobRow, String> colStatus;

    @FXML private Button btnRefresh;
    @FXML private Button btnEditRepair;    // TECHNICIAN + ADMIN
    @FXML private Button btnEditRegister;  // STAFF + ADMIN

    private final CustomerRepository repo = new CustomerRepository();
    private final ObservableList<JobRow> data = FXCollections.observableArrayList();
    private final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private Button btnOpenStockForJob;


    @FXML
    private void initialize() {
        // map column -> getter name ใน JobRow
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customer"));
        colBike.setCellValueFactory(new PropertyValueFactory<>("bike"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tvJobs.setItems(data);

        // RBAC: ซ่อน/แสดงปุ่มตามบทบาท
        Role r = SessionContext.getCurrentRole();
        btnEditRepair.setVisible(r == Role.TECHNICIAN || r == Role.ADMIN);
        btnEditRegister.setVisible(r == Role.STAFF || r == Role.ADMIN);

        // ดับเบิลคลิก: ไปหน้าให้เหมาะกับ Role
        tvJobs.setRowFactory(tv -> {
            TableRow<JobRow> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getClickCount() == 2) {
                    JobRow jr = row.getItem();
                    if (jr.uuid == null) {
                        info("ข้อมูลไม่ครบ", "รหัสงานไม่ถูกต้อง");
                        return;
                    }
                    if (r == Role.TECHNICIAN) {
                        RouterHub.getRouter().toRepairDetails(jr.uuid);
                    } else if (r == Role.STAFF) {
                        openRegisterEdit(jr.uuid);
                    } else { // ADMIN
                        RouterHub.getRouter().toRepairDetails(jr.uuid);
                    }
                }
            });
            return row;
        });

        // โหลดครั้งแรก
        loadData();
    }

    @FXML
    private void onRefresh() {
        loadData();
    }

    @FXML
    private void onEditRepair() {
        JobRow sel = tvJobs.getSelectionModel().getSelectedItem();
        if (sel == null || sel.uuid == null) {
            info("ยังไม่ได้เลือกรายการ", "กรุณาเลือกแถวก่อน");
            return;
        }
        RouterHub.getRouter().toRepairDetails(sel.uuid);
    }

    @FXML
    private void onEditRegister() {
        JobRow sel = tvJobs.getSelectionModel().getSelectedItem();
        if (sel == null || sel.uuid == null) {
            info("ยังไม่ได้เลือกรายการ", "กรุณาเลือกแถวก่อน");
            return;
        }
        openRegisterEdit(sel.uuid);
    }

    /** ไปหน้า register ในโหมดแก้ไข (ส่ง customerId เป็น UUID) */
    private void openRegisterEdit(UUID customerId) {
        RegisterEditContext.setEditingJobId(customerId);
        RouterHub.getRouter().navigate("register");
    }

    /** โหลดข้อมูลจาก customers.json แล้วแปลงเป็นแถวตาราง */
    private void loadData() {
        try {
            data.clear();

            // ดึงทั้งหมดและเรียง: ใหม่สุดก่อน (ตาม registeredAt หรือ receivedDate)
            List<Customer> all = repo.findAll();
            List<Customer> ordered = all.stream()
                    .sorted((a, b) -> {
                        var aTime = a.getRegisteredAt() != null ? a.getRegisteredAt() :
                                (a.getReceivedDate() != null ? a.getReceivedDate().atStartOfDay() : null);
                        var bTime = b.getRegisteredAt() != null ? b.getRegisteredAt() :
                                (b.getReceivedDate() != null ? b.getReceivedDate().atStartOfDay() : null);
                        if (aTime == null && bTime == null) return 0;
                        if (aTime == null) return 1;
                        if (bTime == null) return -1;
                        return bTime.compareTo(aTime); // ใหม่ก่อน
                    })
                    .collect(Collectors.toList());

            for (Customer c : ordered) {
                data.add(toRow(c));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            error("โหลดข้อมูลไม่สำเร็จ", ex.getMessage());
        }
    }

    /** map Customer -> แถวตาราง */
    private JobRow toRow(Customer c) {
        UUID uuid = parseUuid(c.getId());
        String id = uuid != null ? uuid.toString() : "-";
        String name = safe(c.getName());

        // สังเคราะห์ข้อมูลรถเพื่อโชว์: ป้าย/จังหวัด/โทร
        String plate = safe(c.getPlate());
        String prov  = safe(c.getProvince());
        String phone = safe(c.getPhone());
        String bike = (plate.isBlank() && prov.isBlank() && phone.isBlank())
                ? "-" : "%s (%s/%s)".formatted(plate, prov, phone);

        String date = "-";
        if (c.getReceivedDate() != null) date = DATE.format(c.getReceivedDate());
        else if (c.getRegisteredAt() != null) date = DATE.format(c.getRegisteredAt().toLocalDate());

        String status = mapStatusToUi(c.getStatus());

        return new JobRow(uuid, name, bike, date, status);
    }

    // ---------- utilities ----------
    private static UUID parseUuid(String s) {
        try {
            return s != null ? UUID.fromString(s) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String mapStatusToUi(Customer.RepairStatus st) {
        if (st == null) return "รับงานแล้ว";
        return switch (st) {
            case REPAIRING -> "กำลังซ่อม";
            case WAIT_PARTS -> "รออะไหล่";
            case DIAGNOSING -> "กำลังวิเคราะห์อาการ";
            case QA -> "ตรวจสอบคุณภาพ";
            case DONE -> "เสร็จสิ้น";
            case RECEIVED -> "รับงานแล้ว";
        };
    }

    private static String safe(String s) { return (s == null || s.isBlank()) ? "" : s.trim(); }

    private void info(String h, String c) {
        var a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(h);
        a.setContentText(c);
        a.showAndWait();
    }

    private void error(String h, String c) {
        var a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(h);
        a.setContentText(c);
        a.showAndWait();
    }

    /** row model สำหรับ TableView */
    public static class JobRow {
        private final UUID uuid;
        private final String id;
        private final String customer;
        private final String bike;
        private final String date;
        private final String status;

        public JobRow(UUID uuid, String customer, String bike, String date, String status) {
            this.uuid = uuid;
            this.id = uuid != null ? uuid.toString() : "-";
            this.customer = customer;
            this.bike = bike;
            this.date = date;
            this.status = status;
        }

        public String getId() { return id; }
        public String getCustomer() { return customer; }
        public String getBike() { return bike; }
        public String getDate() { return date; }
        public String getStatus() { return status; }
    }

    /** ตัวช่วยส่ง customerId ไปหน้า register ตอนแก้ไข (วิธีง่าย) */
    public static final class RegisterEditContext {
        private static UUID editingJobId;
        public static void setEditingJobId(UUID id) { editingJobId = id; }
        public static UUID getEditingJobId() { return editingJobId; }
        public static void clear() { editingJobId = null; }
    }

    @FXML
    private void onOpenStockForJob() {
        JobRow sel = tvJobs.getSelectionModel().getSelectedItem();
        if (sel == null || sel.uuid == null) {
            info("ยังไม่ได้เลือกรายการ", "กรุณาเลือกแถวก่อน");
            return;
        }
        com.example.big_bike_auto.controller.StockPageController.StockPageContext.setJobId(sel.uuid);
        RouterHub.getRouter().navigate("stock");
    }
}
