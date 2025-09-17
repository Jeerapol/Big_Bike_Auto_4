package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.RouterHub;
import com.example.big_bike_auto.customer.Customer;
import com.example.big_bike_auto.customer.CustomerRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * RegisterController (customers.json version)
 * - ตรงกับ FXML เดิม: fx:id = tfCustomerName, tfPhone, tfPlate, tfProvince, dpRegisteredDate, cbStatus, taSymptom, btnSave
 * - บันทึกลง data/customers.json ผ่าน CustomerRepository (Single Source of Truth ฝั่ง Register)
 * - แสดงข้อความสถานะเป็นภาษาไทยให้ตรงกับหน้า Repair (UI เท่านั้น)
 *
 * หมายเหตุ:
 * - นี่เป็นการ "แก้เฉพาะ UI/ข้อความ" ให้ตรงกัน ข้อมูลยังเป็นโมเดล Customer เดิม (Customer.RepairStatus)
 */
public class RegisterController {

    @FXML private TextField tfCustomerName;
    @FXML private TextField tfPhone;
    @FXML private TextField tfPlate;
    @FXML private TextField tfProvince;
    @FXML private DatePicker dpRegisteredDate;
    @FXML private ComboBox<String> cbStatus;
    @FXML private TextArea taSymptom;
    @FXML private Button btnSave;

    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{9,10}");

    @FXML
    private void initialize() {
        // วันที่เริ่มต้น = วันนี้
        dpRegisteredDate.setValue(LocalDate.now());
        dpRegisteredDate.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate date) { return date != null ? date.toString() : ""; }
            @Override public LocalDate fromString(String s) { return (s == null || s.isBlank()) ? null : LocalDate.parse(s); }
        });

        // ✅ ข้อความสถานะให้ตรงกับหน้า Repair (ภาษาไทย)
        cbStatus.getItems().setAll(
                "รับงานแล้ว",          // RECEIVED
                "กำลังวิเคราะห์อาการ",   // DIAGNOSING
                "รออะไหล่",             // WAIT_PARTS
                "กำลังซ่อม",            // REPAIRING
                "ตรวจสอบคุณภาพ",        // QA
                "เสร็จสิ้น"             // DONE (ของฝั่ง Customer)
        );
        cbStatus.getSelectionModel().selectFirst();

        // ให้ช่องเบอร์โทรรับเฉพาะตัวเลขตอนพิมพ์
        tfPhone.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));

        btnSave.setOnAction(e -> onSave());
    }

    /** กดบันทึก */
    private void onSave() {
        var errors = validateForm();
        if (!errors.isEmpty()) {
            showError("กรอกข้อมูลไม่ครบถ้วน", String.join("\n", errors));
            return;
        }

        try {
            // map ข้อความสถานะ (ไทย) -> Enum ของ Customer
            Customer.RepairStatus st = mapUiStatusToCustomerEnum(Objects.toString(cbStatus.getValue(), ""));

            // สร้าง Customer และกำหนดฟิลด์
            Customer c = new Customer();
            c.setId(UUID.randomUUID().toString());            // ระบุตัวตนด้วย UUID
            c.setName(safe(tfCustomerName.getText()));
            c.setPhone(safe(tfPhone.getText()));
            c.setPlate(safe(tfPlate.getText()));
            c.setProvince(safe(tfProvince.getText()));
            c.setBikeModel(null);                              // optional ในโมเดลเดิม
            c.setStatus(st);
            c.setSymptom(safe(taSymptom.getText()));
            c.setReceivedDate(dpRegisteredDate.getValue());    // วันที่รับรถจาก DatePicker
            c.setRegisteredAt(LocalDateTime.now());            // เวลาบันทึกเข้า system

            // เขียนลง customers.json (atomic ภายใน repo)
            CustomerRepository repo = new CustomerRepository();
            repo.upsert(c);

            // แจ้งผล และกลับ Dashboard
            Alert ok = new Alert(AlertType.INFORMATION);
            ok.setTitle("สำเร็จ");
            ok.setHeaderText("บันทึกการลงทะเบียนเรียบร้อย");
            ok.setContentText("กลับไปหน้า Dashboard ได้");
            ok.showAndWait();

            RouterHub.openDashboard();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("บันทึกไม่สำเร็จ", ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }

    // ---------- Helpers & Validation ----------

    /** แปลงข้อความสถานะ (ไทย) จาก UI -> Customer.RepairStatus */
    private static Customer.RepairStatus mapUiStatusToCustomerEnum(String uiText) {
        // normalize
        String t = uiText == null ? "" : uiText.trim().toLowerCase(Locale.ROOT);
        return switch (t) {
            case "กำลังซ่อม" -> Customer.RepairStatus.REPAIRING;
            case "รออะไหล่" -> Customer.RepairStatus.WAIT_PARTS;
            case "กำลังวิเคราะห์อาการ" -> Customer.RepairStatus.DIAGNOSING;
            case "ตรวจสอบคุณภาพ" -> Customer.RepairStatus.QA;
            case "เสร็จสิ้น" -> Customer.RepairStatus.DONE;
            case "รับงานแล้ว" -> Customer.RepairStatus.RECEIVED;
            default -> Customer.RepairStatus.RECEIVED;
        };
    }

    /** ตรวจความครบถ้วนของฟอร์ม */
    private List<String> validateForm() {
        var list = new java.util.ArrayList<String>();
        if (isBlank(tfCustomerName.getText())) list.add("กรุณากรอกชื่อผู้ใช้บริการ");
        if (isBlank(tfPhone.getText()) || !PHONE_PATTERN.matcher(tfPhone.getText()).matches())
            list.add("กรุณากรอกเบอร์โทร 9-10 หลัก (เฉพาะตัวเลข)");
        if (isBlank(tfPlate.getText())) list.add("กรุณากรอกทะเบียนรถ");
        if (isBlank(tfProvince.getText())) list.add("กรุณากรอกจังหวัด");
        if (dpRegisteredDate.getValue() == null) list.add("กรุณาเลือกวันที่ลงทะเบียน/รับรถ");
        if (cbStatus.getValue() == null) list.add("กรุณาเลือกสถานะเริ่มต้น");
        return list;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String safe(String s) { return Objects.toString(s, ""); }

    private void showError(String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("เกิดข้อผิดพลาด");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
