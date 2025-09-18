module com.example.big_bike_auto {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;

    // UI libraries (ถ้ามีใช้งานจริง)
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    // Jackson (ตามที่พบในโค้ด)
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.datatype.jsr310;

    // Exports (ให้ module อื่น/JavaFX มองเห็น public types)
    exports com.example.big_bike_auto;
    exports com.example.big_bike_auto.controller;
    exports com.example.big_bike_auto.model;
    exports com.example.big_bike_auto.repository;
    exports com.example.big_bike_auto.common;

    // Opens (ให้ reflection ใช้กับ FXML/Serializer)
    opens com.example.big_bike_auto.controller to javafx.fxml, javafx.base;
    opens com.example.big_bike_auto to javafx.fxml;
    opens com.example.big_bike_auto.model to com.google.gson, com.fasterxml.jackson.databind;
    opens com.example.big_bike_auto.common to com.fasterxml.jackson.databind, com.google.gson;
}
