module com.example.big_bike_auto {
    requires javafx.controls;
    requires javafx.fxml;

    // ใช้ไลบรารีเหล่านี้จริงจึง requires
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    // ให้ JavaFX launcher (javafx.graphics) เรียกคลาส Application ในแพ็กเกจ ui ได้
    exports com.example.big_bike_auto.ui;

    // ถ้าคุณมีคลาส public อื่นใน root package ที่อยากให้ module อื่นเห็น ค่อย exports เพิ่ม
     exports com.example.big_bike_auto;

    // ให้ FXMLLoader เข้าถึง @FXML ใน controller (reflection)
    opens com.example.big_bike_auto.controller to javafx.fxml;
    opens com.example.big_bike_auto to javafx.fxml;

    // ถ้ามี FXML ที่ fx:controller อยู่ในแพ็กเกจอื่นเพิ่มเติม ให้ opens เพิ่มตามนั้น
}
