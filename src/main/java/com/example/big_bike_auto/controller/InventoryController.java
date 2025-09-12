package com.example.big_bike_auto.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * หน้าสินค้าคงคลัง (ตัวอย่าง)
 * - ใส่ TableView แบบ dummy
 */
public class InventoryController {

    @FXML private TableView<String> table;
    @FXML private TableColumn<String, String> colName;

    @FXML
    private void initialize() {
        colName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()));
        table.setItems(FXCollections.observableArrayList("อะไหล่ A", "อะไหล่ B", "อะไหล่ C"));
    }
}
