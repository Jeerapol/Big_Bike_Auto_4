package com.example.big_bike_auto.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Controller สำหรับหน้า "ลงทะเบียนผู้ใช้บริการ"
 * - ทำหน้าที่เชื่อมโยง UI (fx:id) กับ logic
 * - ใส่ validation เบื้องต้น (ฟิลด์บังคับ, เบอร์โทรเป็นตัวเลข)
 * - ตั้งค่าเริ่มต้น (วันที่วันนี้, รายการสถานะ)
 *
 * Junior-friendly notes:
 * - @FXML: บอก JavaFX ให้ฉีด (inject) ตัวแปร/เมธอดนี้จาก FXML
 * - initialize(): จะถูกเรียกอัตโนมัติหลังจากโหลด FXML เสร็จ
 */
public class RegisterController {

    // === UI refs ===
    @FXML private TextField tfCustomerName;
    @FXML private TextField tfPhone;
    @FXML private TextField tfPlate;
    @FXML private TextField tfProvince;
    @FXML private DatePicker dpRegisteredDate;
    @FXML private ComboBox<String> cbStatus;
    @FXML private TextArea taSymptom;
    @FXML private Button btnSave;

    // pattern สำหรับเบอร์โทร (ตัวเลขเท่านั้น 9-10 หลัก)
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{9,10}");

    /**
     * เรียกหลังโหลด FXML เสร็จ: ใส่ค่าเริ่มต้น, ตั้งค่า formatter/validator, hookup events
     */
    @FXML
    private void initialize() {
        // 1) ตั้งวันที่เริ่มต้นเป็นวันนี้
        dpRegisteredDate.setValue(LocalDate.now());
        dpRegisteredDate.setConverter(new StringConverter<>() {
            // อธิบาย: ให้ DatePicker ใช้ค่า default ของ JavaFX ก็ได้
            @Override public String toString(LocalDate date) {
                return date != null ? date.toString() : "";
            }
            @Override public LocalDate fromString(String s) {
                return (s == null || s.isBlank()) ? null : LocalDate.parse(s);
            }
        });

        // 2) ใส่รายการสถานะใน ComboBox (แทน MenuButton)
        cbStatus.getItems().setAll(
                "ลงทะเบียน",
                "กำลังซ่อม",
                "ซ่อมเสร็จสิ้น (รอชำระเงิน)",
                "ซ่อมเสร็จสิ้น (ชำระเงินแล้ว)"
        );

        // 3) ให้ tfPhone รับเฉพาะตัวเลข (TextFormatter)
        tfPhone.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().isEmpty()) {
                return change; // ว่างได้ระหว่างพิมพ์ (จะตรวจอีกทีตอนกดบันทึก)
            }
            // รับเฉพาะตัวเลข
            if (change.getControlNewText().matches("\\d*")) {
                return change;
            }
            return null; // ปฏิเสธการเปลี่ยนแปลงที่ไม่ใช่ตัวเลข
        }));

        // 4) คลิกบันทึก → validate แล้ว (จำลอง) บันทึก
        btnSave.setOnAction(e -> onSave());
    }

    /**
     * กดบันทึกข้อมูล:
     * - ตรวจความครบถ้วนของฟิลด์
     * - แสดง Alert ถ้าผิดพลาด หรือ สรุปข้อมูลถ้าสำเร็จ
     */
    private void onSave() {
        var errors = validateForm();
        if (!errors.isEmpty()) {
            showError("กรอกข้อมูลไม่ครบถ้วน", String.join("\n", errors));
            return;
        }

        // TODO: ในงานจริง: สร้าง DTO แล้วส่งต่อไป Service/Repository หรือเรียก API
        String summary = """
                ✅ บันทึกสำเร็จ (ตัวอย่าง)
                - ชื่อลูกค้า: %s
                - เบอร์โทร: %s
                - ป้ายทะเบียน: %s
                - จังหวัดทะเบียน: %s
                - วันที่ลงทะเบียน: %s
                - สถานะ: %s
                - อาการเสีย: %s
                """.formatted(
                safe(tfCustomerName.getText()),
                safe(tfPhone.getText()),
                safe(tfPlate.getText()),
                safe(tfProvince.getText()),
                dpRegisteredDate.getValue(),
                cbStatus.getValue(),
                safe(taSymptom.getText())
        );
        showInfo("ผลการบันทึก", summary);

        // ล้างฟอร์มหรือคงไว้ก็ได้ ตาม UX ที่ต้องการ
        // clearForm();
    }

    /**
     * ตรวจข้อมูลฟอร์มแบบง่าย ๆ
     */
    private List<String> validateForm() {
        new Object(); // no-op เพื่อกัน accidental empty method during refactor
        var builder = new java.util.ArrayList<String>();

        if (isBlank(tfCustomerName.getText())) {
            builder.add("กรุณาระบุชื่อลูกค้า");
        }
        if (isBlank(tfPhone.getText())) {
            builder.add("กรุณาระบุเบอร์โทร");
        } else if (!PHONE_PATTERN.matcher(tfPhone.getText().trim()).matches()) {
            builder.add("เบอร์โทรต้องเป็นตัวเลข 9-10 หลัก");
        }
        if (isBlank(tfPlate.getText())) {
            builder.add("กรุณาระบุป้ายทะเบียน");
        }
        if (isBlank(tfProvince.getText())) {
            builder.add("กรุณาระบุจังหวัดทะเบียน");
        }
        if (dpRegisteredDate.getValue() == null) {
            builder.add("กรุณาเลือกวันที่ลงทะเบียน");
        }
        if (cbStatus.getValue() == null) {
            builder.add("กรุณาเลือกสถานะ");
        }
        // อาการเสีย อนุโลมให้ว่างได้ แต่แนะนำให้กรอก (ถ้าบังคับ ให้ uncomment บรรทัดล่าง)
        // if (isBlank(taSymptom.getText())) builder.add("กรุณาระบุอาการเสีย");

        return builder;
    }

    // ===== Utils =====

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String safe(String s) {
        return Objects.toString(s, "");
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("เกิดข้อผิดพลาด");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("ข้อมูล");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @SuppressWarnings("unused")
    private void clearForm() {
        tfCustomerName.clear();
        tfPhone.clear();
        tfPlate.clear();
        tfProvince.clear();
        dpRegisteredDate.setValue(LocalDate.now());
        cbStatus.getSelectionModel().clearSelection();
        taSymptom.clear();
    }
}
