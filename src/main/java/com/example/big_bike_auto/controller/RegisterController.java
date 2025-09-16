package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.RouterHub;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Controller: หน้าลงทะเบียน
 * Flow ที่ต้องการ: กดบันทึกสำเร็จ => แจ้งเตือน => กลับไปหน้า Dashboard (ไม่เปิดหน้า Repair Details ตรงนี้)
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
        // ตั้งค่า date picker และตัวกรองเบอร์โทร (ตัวเลขเท่านั้น)
        dpRegisteredDate.setValue(LocalDate.now());
        dpRegisteredDate.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate date) { return date != null ? date.toString() : ""; }
            @Override public LocalDate fromString(String s) { return (s == null || s.isBlank()) ? null : LocalDate.parse(s); }
        });

        cbStatus.getItems().setAll(
                "ลงทะเบียน",
                "กำลังซ่อม",
                "ซ่อมเสร็จสิ้น (รอชำระเงิน)",
                "ซ่อมเสร็จสิ้น (ชำระเงินแล้ว)"
        );

        tfPhone.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getControlNewText().isEmpty()) return change;
            return change.getControlNewText().matches("\\d*") ? change : null;
        }));

        btnSave.setOnAction(e -> onSave());
    }

    /** เมื่อผู้ใช้กดบันทึก: validate -> สร้าง job -> save -> บันทึก customer -> แจ้งเตือน -> กลับ Dashboard */
    private void onSave() {
        var errors = validateForm();
        if (!errors.isEmpty()) {
            showError("กรอกข้อมูลไม่ครบถ้วน", String.join("\n", errors));
            return;
        }

        try {
            // 1) สร้างงานซ่อม (job) จากข้อมูลฟอร์ม
            UUID jobId = UUID.randomUUID();
            var job = new RepairDetailsController.RepairJob(jobId);
            job.setCustomerName(safe(tfCustomerName.getText()));
            job.setBikeModel("%s (%s/%s)".formatted(
                    safe(tfPlate.getText()),
                    safe(tfProvince.getText()),
                    safe(tfPhone.getText())
            ));
            job.setReceivedDate(dpRegisteredDate.getValue());
            job.setStatus(RepairDetailsController.RepairStatus.RECEIVED);
            job.setNotes(safe(taSymptom.getText()));
            job.setParts(new java.util.ArrayList<>());

            // 2) เซฟลง storage (repo เดิมของคุณ)
            var repo = new RepairDetailsController.FileRepairJobRepository();
            repo.save(job);

            // 3) บันทึกลูกค้าเป็น JSON (ไม่ให้ล่มถ้าเขียนไฟล์ไม่ได้)
            try {
                com.example.big_bike_auto.customer.CustomerRepository cRepo =
                        new com.example.big_bike_auto.customer.CustomerRepository();
                com.example.big_bike_auto.customer.Customer c =
                        new com.example.big_bike_auto.customer.Customer(
                                jobId.toString(),                            // ใช้ jobId เป็น id เพื่อโยงไป repair details ได้
                                safe(tfCustomerName.getText()),
                                safe(tfPhone.getText()),
                                null,                                        // ยังไม่มี email ในฟอร์ม
                                java.time.LocalDateTime.now()
                        );
                cRepo.append(c);
            } catch (Exception cx) {
                System.err.println("WARN: เขียน customers.json ไม่ได้: " + cx.getMessage());
            }

            // 4) แจ้งเตือนสำเร็จ + กลับ Dashboard
            Alert ok = new Alert(AlertType.INFORMATION);
            ok.setTitle("สำเร็จ");
            ok.setHeaderText("บันทึกการลงทะเบียนเรียบร้อย");
            ok.setContentText("กลับไปหน้า Dashboard แล้วเลือกงานเพื่อเปิดรายละเอียดได้");
            ok.showAndWait();

            // ★ กลับ Dashboard ผ่าน Router ของคุณ
            RouterHub.openDashboard(); // = RouterHub.getRouter().navigate("dashboard")

        } catch (Exception ex) {
            showError("บันทึกไม่สำเร็จ", ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** ตรวจความถูกต้องของฟอร์ม */
    private List<String> validateForm() {
        var list = new java.util.ArrayList<String>();
        if (isBlank(tfCustomerName.getText())) list.add("กรุณากรอกชื่อผู้ใช้บริการ");
        if (isBlank(tfPhone.getText()) || !PHONE_PATTERN.matcher(tfPhone.getText()).matches()) list.add("กรุณากรอกเบอร์โทร 9-10 หลัก");
        if (isBlank(tfPlate.getText())) list.add("กรุณากรอกทะเบียนรถ");
        if (isBlank(tfProvince.getText())) list.add("กรุณากรอกจังหวัด");
        if (dpRegisteredDate.getValue() == null) list.add("กรุณาเลือกวันที่รับรถ");
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
