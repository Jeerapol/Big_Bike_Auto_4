package com.example.big_bike_auto.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class InventoryController {

    @FXML private TableView<?> tvInventory;
    @FXML private TableColumn<?, ?> colPartName;
    @FXML private TableColumn<?, ?> colQty;
    @FXML private TableColumn<?, ?> colUnit;
    @FXML private TableColumn<?, ?> colPrice;

    @FXML
    public void initialize() {
        System.out.println("InventoryController initialized");
        // TODO: โหลดข้อมูล inventory จาก service
    }

    @FXML
    private void onRefresh() {
        System.out.println("รีเฟรชตารางสต็อก");
        // TODO: reload data
    }

    @FXML
    private void onAddPart() {
        System.out.println("เพิ่มอะไหล่ใหม่");
        // TODO: แสดง dialog กรอกข้อมูล -> เพิ่มลง table
    }

    @FXML
    private void onRemovePart() {
        System.out.println("ลบอะไหล่ที่เลือก");
        // TODO: ลบ row ที่เลือก
    }
}
