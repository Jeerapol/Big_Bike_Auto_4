package com.example.big_bike_auto.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

/**
 * หน้ารายการซ่อม (ตัวอย่าง)
 */
public class RepairListController {

    @FXML private ListView<String> listView;

    @FXML
    private void initialize() {
        listView.setItems(FXCollections.observableArrayList(
                "ใบงาน #1001 - เครื่องพิมพ์",
                "ใบงาน #1002 - โน้ตบุ๊ก",
                "ใบงาน #1003 - POS"
        ));
    }
}
