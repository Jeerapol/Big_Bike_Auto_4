module com.example.big_bike_auto {
    requires javafx.controls;
    requires javafx.fxml;

    // ไลบรารี UI อื่น ๆ
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    // ✅ เพิ่ม Jackson
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.datatype.jsr310;

    // Export UI และ root package
    exports com.example.big_bike_auto.ui;
    exports com.example.big_bike_auto;

    // เปิดให้ FXMLLoader ใช้ reflection
    opens com.example.big_bike_auto.controller to javafx.fxml;
    opens com.example.big_bike_auto to javafx.fxml;

    // ✅ เปิดให้ Jackson ใช้ reflection (serialize/deserialize)
    opens com.example.big_bike_auto.customer to com.fasterxml.jackson.databind;
    opens com.example.big_bike_auto.common to com.fasterxml.jackson.databind;
}
