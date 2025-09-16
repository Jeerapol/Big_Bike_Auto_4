package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.RouterHub;
import com.example.big_bike_auto.auth.Role;
import com.example.big_bike_auto.auth.SessionContext;
import com.example.big_bike_auto.controller.RepairDetailsController.FileRepairJobRepository;
import com.example.big_bike_auto.controller.RepairDetailsController.RepairJob;
import com.example.big_bike_auto.controller.RepairDetailsController.RepairStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * รายการซ่อม:
 * - โหลดข้อมูลงานซ่อมทั้งหมดจาก repository
 * - แสดง TableView
 * - RBAC: แสดง/ซ่อนปุ่มตาม Role
 * - ดับเบิลคลิกแถว → ไปหน้าแก้ตาม Role
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

    private final FileRepairJobRepository repo = new FileRepairJobRepository();
    private final ObservableList<JobRow> data = FXCollections.observableArrayList();
    private final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    @FXML
    private void initialize() {
        // map column -> getter
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customer"));
        colBike.setCellValueFactory(new PropertyValueFactory<>("bike"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tvJobs.setItems(data);

        // RBAC ปุ่ม
        Role r = SessionContext.getCurrentRole();
        btnEditRepair.setVisible(r == Role.TECHNICIAN || r == Role.ADMIN);
        btnEditRegister.setVisible(r == Role.STAFF || r == Role.ADMIN);

        // ดับเบิลคลิกตาม Role
        tvJobs.setRowFactory(tv -> {
            TableRow<JobRow> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (!row.isEmpty() && ev.getClickCount() == 2) {
                    JobRow jr = row.getItem();
                    if (r == Role.TECHNICIAN) {
                        RouterHub.getRouter().toRepairDetails(jr.uuid);
                    } else if (r == Role.STAFF) {
                        openRegisterEdit(jr.uuid);
                    } else { // ADMIN default -> ไปหน้า RepairDetails
                        RouterHub.getRouter().toRepairDetails(jr.uuid);
                    }
                }
            });
            return row;
        });

        // โหลดข้อมูลครั้งแรก
        loadData();
    }

    @FXML
    private void onRefresh() {
        loadData();
    }

    @FXML
    private void onEditRepair() {
        JobRow sel = tvJobs.getSelectionModel().getSelectedItem();
        if (sel == null) {
            info("ยังไม่ได้เลือกรายการ", "กรุณาเลือกแถวก่อน");
            return;
        }
        RouterHub.getRouter().toRepairDetails(sel.uuid);
    }

    @FXML
    private void onEditRegister() {
        JobRow sel = tvJobs.getSelectionModel().getSelectedItem();
        if (sel == null) {
            info("ยังไม่ได้เลือกรายการ", "กรุณาเลือกแถวก่อน");
            return;
        }
        openRegisterEdit(sel.uuid);
    }

    /** ไปหน้า register ในโหมดแก้ไข (ส่ง jobId ผ่าน RegisterEditContext แบบง่าย) */
    private void openRegisterEdit(UUID jobId) {
        RegisterEditContext.setEditingJobId(jobId);
        RouterHub.getRouter().navigate("register");
    }

    private void loadData() {
        try {
            data.clear();
            List<RepairJob> all = repo.findAll();
            for (RepairJob j : all) data.add(toRow(j));
        } catch (Exception ex) {
            ex.printStackTrace();
            error("โหลดข้อมูลไม่สำเร็จ", ex.getMessage());
        }
    }

    private JobRow toRow(RepairJob j) {
        String date = (j.getReceivedDate() != null) ? DATE.format(j.getReceivedDate()) : "-";
        String status = (j.getStatus() != null) ? j.getStatus().toString() : RepairStatus.RECEIVED.toString();
        return new JobRow(j.getJobId(), safe(j.getCustomerName()), safe(j.getBikeModel()), date, status);
    }

    private static String safe(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    private void info(String h, String c) {
        var a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(h); a.setContentText(c); a.showAndWait();
    }
    private void error(String h, String c) {
        var a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(h); a.setContentText(c); a.showAndWait();
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

    /** ตัวช่วยส่ง jobId ไปหน้า register ตอนแก้ไข (วิธีง่าย) */
    public static final class RegisterEditContext {
        private static UUID editingJobId;
        public static void setEditingJobId(UUID id) { editingJobId = id; }
        public static UUID getEditingJobId() { return editingJobId; }
        public static void clear() { editingJobId = null; }
    }
}
