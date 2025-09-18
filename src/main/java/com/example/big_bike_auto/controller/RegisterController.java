package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.common.JsonUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * RegisterController:
 * - ใส่ค่า default วันที่เป็น "วันนี้"
 * - แสดงผลวันที่แบบไทย (พ.ศ.) ใน DatePicker ด้วย StringConverter
 * - เติมตัวเลือกสถานะลง ComboBox
 * - กดบันทึก -> validate, สร้าง customerCode 10 ตัว (A-Z0-9), บันทึกลง data/customers.json
 *
 * Junior Tips:
 *  - fx:id ต้องตรงกับ FXML ทุกตัว ไม่งั้น field จะเป็น null
 *  - DatePicker เก็บค่าเป็น LocalDate (ค.ศ.) แต่เราทำ Converter ให้ "แสดง" เป็น พ.ศ.
 *
 * Senior Notes:
 *  - ใช้ SecureRandom ในการสร้างโค้ดลูกค้า ลด collision
 *  - JsonUtil.ensureJsonArrayFile() เพื่อสร้างไฟล์ [] ถ้ายังไม่มี
 *  - Validate field ขั้นพื้นฐานก่อนบันทึก
 */
public class RegisterController {

    // ===== FXML จากไฟล์ RegisterPage.fxml (ของคุณ) =====
    @FXML private TextField tfCustomerName;
    @FXML private TextField tfPhone;
    @FXML private TextField tfPlate;
    @FXML private TextField tfProvince;
    @FXML private DatePicker dpRegisteredDate;
    @FXML private ComboBox<String> cbStatus;
    @FXML private TextArea taSymptom;
    @FXML private Button btnSave;

    // ===== ค่าคงที่/ตัวช่วย =====
    private static final String CUSTOMERS_JSON = "data/customers.json";

    // รายการสถานะมาตรฐาน (ปรับเพิ่ม/ลดได้ตาม flow ของคุณ)
    private static final List<String> STATUSES = List.of(
            "RECEIVED",      // รับรถแล้ว
            "IN_PROGRESS",   // กำลังซ่อม
            "COMPLETED",     // ซ่อมเสร็จ
            "DELIVERED"      // ส่งมอบแล้ว
    );

    // ฟอร์แมตวันที่ที่ "แสดงผล" ให้ผู้ใช้ (ไทย/พ.ศ.)
    private static final DateTimeFormatter TH_DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // สำหรับสุ่มรหัสลูกค้า (A-Z0-9) 10 ตัว
    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int CUSTOMER_CODE_LEN = 10;

    @FXML
    public void initialize() {
        // 1) ใส่ค่า default เป็น "วันนี้"
        dpRegisteredDate.setValue(LocalDate.now());

        // 1.1) ทำให้ DatePicker "แสดงผล" เป็นไทย/พ.ศ. (แต่ค่าภายในยังเป็น LocalDate ค.ศ.)
        dpRegisteredDate.setConverter(buildThaiBuddhistConverter());

        // 2) เติมสถานะลง ComboBox + เลือกค่าเริ่มต้น
        cbStatus.setItems(FXCollections.observableArrayList(STATUSES));
        cbStatus.setPromptText("เลือกสถานะ");
        cbStatus.getSelectionModel().selectFirst(); // default = RECEIVED

        // 3) bind action ปุ่มบันทึก
        btnSave.setOnAction(e -> onSave());
    }

    /**
     * Converter สำหรับ DatePicker:
     * - toString: แสดงผลเป็น พ.ศ. (dd/MM/yyyy โดยเพิ่มปี +543) ด้วย ThaiBuddhistDate
     * - fromString: แปลงสตริงของผู้ใช้ (dd/MM/yyyy พ.ศ.) กลับเป็น LocalDate (ค.ศ.) โดยลดย้อนปี 543
     */
    private StringConverter<LocalDate> buildThaiBuddhistConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                if (date == null) return "";
                // ใช้ ThaiBuddhistDate เพื่อได้ปีพุทธศักราชโดยอัตโนมัติ
                ThaiBuddhistDate th = ThaiBuddhistDate.from(date);
                // แสดงผลด้วยฟอร์แมต dd/MM/yyyy (ของปีพุทธ)
                return TH_DISPLAY_FMT.format(th);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.trim().isEmpty()) return null;
                // ผู้ใช้พิมพ์ dd/MM/yyyy (ไทย/พ.ศ.) -> parse เป็น LocalDate พ.ศ. แล้วแปลงกลับเป็น ค.ศ.
                // เราจะแยกวัน/เดือน/ปีเองเพื่อเลี่ยง parsing ที่สลับคริสต์/พุทธ
                try {
                    String[] parts = text.trim().split("[/\\-]");
                    if (parts.length != 3) throw new IllegalArgumentException("รูปแบบวันที่ควรเป็น dd/MM/yyyy");
                    int d = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    int buddhistYear = Integer.parseInt(parts[2]);
                    int isoYear = buddhistYear - 543; // แปลง พ.ศ. -> ค.ศ.
                    return LocalDate.of(isoYear, m, d);
                } catch (Exception ex) {
                    // ถ้า parse ไม่ได้ ให้ปล่อย empty -> ผู้ใช้จะเห็นว่าไม่เปลี่ยนค่า
                    showWarn("รูปแบบวันที่ไม่ถูกต้อง", "โปรดใช้รูปแบบ dd/MM/yyyy (เช่น 01/10/2568)");
                    return null;
                }
            }
        };
    }

    /**
     * เมื่อกดบันทึก:
     * - ตรวจความถูกต้องของข้อมูล
     * - สร้าง customerCode ยาว 10 ตัว (A-Z0-9)
     * - ต่อท้ายข้อมูลลง customers.json
     */
    private void onSave() {
        // --- 1) เก็บค่าจากฟอร์ม ---
        String name = safe(tfCustomerName.getText());
        String phone = safe(tfPhone.getText());
        String plate = safe(tfPlate.getText());
        String province = safe(tfProvince.getText());
        LocalDate registeredDate = dpRegisteredDate.getValue();
        String status = cbStatus.getValue(); // จะไม่เป็น null เพราะ selectFirst แล้ว
        String symptom = safe(taSymptom.getText());

        // --- 2) Validate พื้นฐาน ---
        String err = validateForm(name, phone, plate, province, registeredDate, status);
        if (err != null) {
            showWarn("ข้อมูลไม่ครบถ้วน", err);
            return;
        }

        // --- 3) เตรียม JSON object ของลูกค้าใหม่ ---
        // โครงสร้างยึดตาม JSON ที่คุณเคยส่ง (เพิ่ม customerCode 10 ตัว)
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("id", UUID.randomUUID().toString());
        customer.put("customerCode", genCustomerCode(CUSTOMER_CODE_LEN)); // <<<<< โค้ดลูกค้า 10 ตัว
        customer.put("name", name);
        customer.put("phone", phone);
        customer.put("plate", plate);
        customer.put("province", province);
        customer.put("bikeModel", null); // ยังไม่ระบุ
        customer.put("receivedDate", registeredDate); // LocalDate -> JsonUtil จะ serialize เป็น "YYYY-MM-DD"
        customer.put("registeredAt", LocalDateTime.now().toString()); // เวลา ณ ตอนบันทึก
        customer.put("status", status);
        customer.put("symptom", symptom);

        // บล็อก "repair" ให้ว่างเริ่มต้น (สอดคล้องกับโครงเก่า)
        Map<String, Object> repair = new LinkedHashMap<>();
        repair.put("parts", new ArrayList<>());   // [] เริ่มต้น
        repair.put("grandTotal", 0.0);
        repair.put("notes", "");
        repair.put("lastUpdated", LocalDateTime.now().toString());
        customer.put("repair", repair);

        // --- 4) บันทึกลงไฟล์ customers.json (append) ---
        try {
            JsonUtil.ensureJsonArrayFile(CUSTOMERS_JSON);
            // อ่านรายการเดิมทั้งหมดเป็น List<Map>
            List<Map> list = new ArrayList<>(JsonUtil.readList(CUSTOMERS_JSON, Map.class));
            list.add(customer);
            JsonUtil.writeList(CUSTOMERS_JSON, list);

            // --- 5) แจ้งผู้ใช้สำเร็จ + โชว์รหัสลูกค้า ---
            String code = (String) customer.get("customerCode");
            showInfo("บันทึกสำเร็จ", "สร้างข้อมูลลูกค้าเรียบร้อย\nรหัสลูกค้า: " + code + "\n\nนำรหัสดังกล่าวไปใช้ค้นหา/ดูสถานะภายหลังได้");

            // เคลียร์ฟอร์มเบา ๆ (คงวันที่/สถานะไว้ให้)
            tfCustomerName.clear();
            tfPhone.clear();
            tfPlate.clear();
            tfProvince.clear();
            taSymptom.clear();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("บันทึกไม่สำเร็จ", ex);
        }
    }

    // ===== Helpers =====

    // ตรวจความครบถ้วน/รูปแบบง่าย ๆ
    private String validateForm(String name, String phone, String plate, String province, LocalDate date, String status) {
        List<String> problems = new ArrayList<>();
        if (name.isEmpty()) problems.add("กรุณาระบุชื่อลูกค้า");
        if (phone.isEmpty()) problems.add("กรุณาระบุเบอร์โทร");
        if (plate.isEmpty()) problems.add("กรุณาระบุป้ายทะเบียน");
        if (province.isEmpty()) problems.add("กรุณาระบุจังหวัดทะเบียน");
        if (date == null) problems.add("กรุณาระบุวันที่ลงทะเบียน");
        if (status == null || status.isEmpty()) problems.add("กรุณาเลือกสถานะ");

        // ตัวอย่างกฎเพิ่ม: เบอร์โทรให้เป็นตัวเลข 8-12 หลัก (ปรับตามจริงได้)
        if (!phone.isEmpty() && !phone.matches("\\d{8,12}")) {
            problems.add("รูปแบบเบอร์โทรไม่ถูกต้อง (ควรเป็นตัวเลข 8-12 หลัก)");
        }

        return problems.isEmpty() ? null : String.join("\n", problems);
    }

    // แปลง null -> "" กัน NPE
    private String safe(String s) { return s == null ? "" : s.trim(); }

    // สร้างโค้ดลูกค้า A-Z0-9 ความยาว len (เช็คซ้ำเบื้องต้นกับไฟล์เดิม)
    private String genCustomerCode(int len) {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = randomAlnum(len);
            if (!isCodeExists(code)) return code;
        }
        // ถ้ายังชน ให้แน่ใจด้วย timestamp suffix (โอกาสน้อยมาก)
        return randomAlnum(len - 4) + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT).substring(0, 4);
    }

    private String randomAlnum(int len) {
        char[] out = new char[len];
        for (int i = 0; i < len; i++) {
            out[i] = ALPHANUM[RNG.nextInt(ALPHANUM.length)];
        }
        return new String(out);
    }

    private boolean isCodeExists(String code) {
        try {
            JsonUtil.ensureJsonArrayFile(CUSTOMERS_JSON);
            List<Map> list = JsonUtil.readList(CUSTOMERS_JSON, Map.class);
            for (Map m : list) {
                Object v = m.get("customerCode");
                if (v != null && code.equalsIgnoreCase(String.valueOf(v))) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            // ถ้าอ่านไม่ได้ให้ถือว่าไม่มี (ไม่ fail registration)
            return false;
        }
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void showWarn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, Exception ex) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(String.valueOf(ex.getMessage()));
        TextArea ta = new TextArea(getStack(ex));
        ta.setEditable(false);
        ta.setWrapText(false);
        ta.setMaxWidth(Double.MAX_VALUE);
        ta.setMaxHeight(Double.MAX_VALUE);
        a.getDialogPane().setExpandableContent(ta);
        a.showAndWait();
    }

    private String getStack(Throwable t) {
        StringBuilder sb = new StringBuilder();
        while (t != null) {
            sb.append(t).append("\n");
            for (StackTraceElement el : t.getStackTrace()) {
                sb.append("  at ").append(el).append("\n");
            }
            t = t.getCause();
            if (t != null) sb.append("Caused by: ");
        }
        return sb.toString();
    }
}
