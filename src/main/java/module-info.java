module com.example.big_bike_auto {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    // ไลบรารี UI อื่น ๆ
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    // ✅ เพิ่ม Jackson
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.datatype.jsr310;

    // Export UI และ root package (ถ้าต้องการให้ module อื่น import public API)
    exports com.example.big_bike_auto.ui;
    exports com.example.big_bike_auto;

    // เปิดให้ FXMLLoader ใช้ reflection กับ controller
    opens com.example.big_bike_auto.controller to javafx.fxml, javafx.base;
    opens com.example.big_bike_auto to javafx.fxml;

    // ✅ เปิด package ที่ Gson ต้อง reflect
    opens com.example.big_bike_auto.parts to com.google.gson;
    opens com.example.big_bike_auto.po    to com.google.gson;   // << เพิ่มบรรทัดนี้

    // ✅ เปิดให้ Jackson ใช้ reflection
    opens com.example.big_bike_auto.customer to com.fasterxml.jackson.databind;
    opens com.example.big_bike_auto.common   to com.fasterxml.jackson.databind;

}
