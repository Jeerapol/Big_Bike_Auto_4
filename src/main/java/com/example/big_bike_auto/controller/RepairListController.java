package com.example.big_bike_auto.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * Controller ของ RepairList.fxml
 * หน้าที่:
 *  - แสดงรายการงานซ่อมทั้งหมด
 *  - ปุ่ม refresh, edit, open stock
 */
public class RepairListController {

    // Table + Columns
    @FXML private TableView<?> tvJobs;
    @FXML private TableColumn<?, ?> colId;
    @FXML private TableColumn<?, ?> colCustomer;
    @FXML private TableColumn<?, ?> colBike;
    @FXML private TableColumn<?, ?> colDate;
    @FXML private TableColumn<?, ?> colStatus;

    @FXML
    public void initialize() {
        // TODO: โหลด repair jobs จาก service
        System.out.println("RepairListController initialized");
    }

    // ปุ่มรีเฟรช
    @FXML
    private void onRefresh() {
        System.out.println("รีเฟรชตารางงานซ่อม");
        // TODO: reload data จาก service
    }

    // ปุ่มแก้ไขรายการซ่อม
    @FXML
    private void onEditRepair() {
        System.out.println("แก้ไขรายการซ่อม");
        // TODO: เปิด RepairDetails.fxml โดยส่งงานที่เลือก
    }

    // ปุ่มแก้ไขข้อมูลลงทะเบียน
    @FXML
    private void onEditRegister() {
        System.out.println("แก้ไขข้อมูลลงทะเบียน");
        // TODO: เปิด Register.fxml
    }

    // ปุ่มเปิดอะไหล่สำหรับงานนี้
    @FXML
    private void onOpenStockForJob() {
        System.out.println("เปิดอะไหล่สำหรับงานนี้");
        // TODO: เปิด InventoryPage.fxml และเลือกเฉพาะ job ปัจจุบัน
    }
}
