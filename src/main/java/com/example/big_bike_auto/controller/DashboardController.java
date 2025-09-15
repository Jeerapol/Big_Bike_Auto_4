package com.example.big_bike_auto.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * DashboardController
 * หน้าสรุปภาพรวมหลังล็อกอิน:
 * - ตัวอย่างสถิติ (mock) และตารางล่าสุด
 * - โค้ดนี้ออกแบบให้ Junior อ่านง่าย: อธิบายทุกขั้นตอน และ handle edge cases
 */
public class DashboardController {

    // พื้นที่สำหรับวาง widget เพิ่มเติมภายหลัง
    @FXML private AnchorPane root;

    // ตัวอย่าง label แสดง stat
    @FXML private Label lblTotalRepairs;
    @FXML private Label lblPendingRepairs;
    @FXML private Label lblInventoryItems;

    // ตารางตัวอย่าง
    @FXML private TableView<RecentItem> tblRecent;
    @FXML private TableColumn<RecentItem, String> colType;
    @FXML private TableColumn<RecentItem, String> colTitle;
    @FXML private TableColumn<RecentItem, String> colWhen;

    /**
     * คลาสข้อมูลสำหรับตาราง (POJO)
     * ใช้เป็น model ง่าย ๆ สำหรับ TableView
     */
    public static class RecentItem {
        private final String type;
        private final String title;
        private final String when;
        public RecentItem(String type, String title, String when) {
            this.type = type;
            this.title = title;
            this.when = when;
        }
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getWhen() { return when; }
    }

    @FXML
    private void initialize() {
        // กำหนดคอลัมน์ให้ตรงกับ getter
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colWhen.setCellValueFactory(new PropertyValueFactory<>("when"));

        // ใส่ค่าตัวอย่าง (mock) สำหรับ demo
        ObservableList<RecentItem> data = FXCollections.observableArrayList(
                new RecentItem("ซ่อม", "เปลี่ยนผ้าเบรก", "วันนี้ 09:30"),
                new RecentItem("สต็อก", "รับเข้าโซ่ 520-114L", "เมื่อวาน"),
                new RecentItem("ซ่อม", "เช็คระยะ 10,000 กม.", "2 วันที่แล้ว")
        );
        tblRecent.setItems(data);

        // สถิติ mock
        lblTotalRepairs.setText("128");
        lblPendingRepairs.setText("7");
        lblInventoryItems.setText("342");

        // หมายเหตุ: Production จริงควรดึงข้อมูลจาก service/database
        // พร้อม handle error (เช่น network/database down) แล้วแสดงข้อความที่ user เข้าใจได้
    }
}
