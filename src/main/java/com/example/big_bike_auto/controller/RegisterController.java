package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.common.JsonUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.StringConverter;

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
 * - เติมตัวเลือกสถานะ + รุ่นรถลง ComboBox (รุ่นรถเป็น editable)
 * - กดบันทึก -> validate, สร้าง customerCode 10 ตัว (A-Z0-9), บันทึกลง data/customers.json
 * - ✅ หลังบันทึกสำเร็จ: แสดง Alert พร้อมปุ่ม "คัดลอกรหัส" เพื่อ copy ไป Clipboard
 */
public class RegisterController {

    // ===== FXML =====
    @FXML private TextField tfCustomerName;
    @FXML private TextField tfPhone;
    @FXML private TextField tfPlate;
    @FXML private TextField tfProvince;
    @FXML private DatePicker dpRegisteredDate;
    @FXML private ComboBox<String> cbStatus;
    @FXML private ComboBox<String> cbBikeModel;   // รุ่นรถ (editable)
    @FXML private TextArea taSymptom;
    @FXML private Button btnSave;

    // ===== ค่าคงที่/ตัวช่วย =====
    private static final String CUSTOMERS_JSON = "data/customers.json";

    private static final List<String> STATUSES = List.of(
            "RECEIVED", "IN_PROGRESS", "COMPLETED", "DELIVERED"
    );

    // รายการรุ่นรถแนะนำ (ปรับแก้ได้)
    private static final List<String> BIKE_MODELS = List.of(
            "Honda Wave 110i", "Honda Click 125i", "Honda PCX 160",
            "Yamaha GT125", "Yamaha NMAX 155", "Yamaha Aerox 155",
            "Suzuki Smash 115", "Suzuki Raider R150",
            "Kawasaki Z250", "Kawasaki Ninja 400",
            "GPX Demon", "GPX Razer",
            "อื่น ๆ (พิมพ์เอง)"
    );

    private static final DateTimeFormatter TH_DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int CUSTOMER_CODE_LEN = 10;

    @FXML
    public void initialize() {
        // 1) วันที่ default = วันนี้ + แสดงแบบ พ.ศ.
        dpRegisteredDate.setValue(LocalDate.now());
        dpRegisteredDate.setConverter(buildThaiBuddhistConverter());

        // 2) สถานะ
        cbStatus.setItems(FXCollections.observableArrayList(STATUSES));
        cbStatus.setPromptText("เลือกสถานะ");
        cbStatus.getSelectionModel().selectFirst();

        // 3) รุ่นรถ
        cbBikeModel.setItems(FXCollections.observableArrayList(BIKE_MODELS));
        cbBikeModel.setEditable(true);
        cbBikeModel.setPromptText("เลือกหรือพิมพ์รุ่นรถ");

        // 4) ปุ่มบันทึก
        btnSave.setOnAction(e -> onSave());
    }

    /** Converter DatePicker: แสดง/รับ พ.ศ. แต่เก็บเป็น LocalDate ค.ศ. */
    private StringConverter<LocalDate> buildThaiBuddhistConverter() {
        return new StringConverter<>() {
            @Override public String toString(LocalDate date) {
                if (date == null) return "";
                ThaiBuddhistDate th = ThaiBuddhistDate.from(date);
                return TH_DISPLAY_FMT.format(th);
            }
            @Override public LocalDate fromString(String text) {
                if (text == null || text.trim().isEmpty()) return null;
                try {
                    String[] parts = text.trim().split("[/\\-]");
                    if (parts.length != 3) throw new IllegalArgumentException("รูปแบบวันที่ควรเป็น dd/MM/yyyy");
                    int d = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    int buddhistYear = Integer.parseInt(parts[2]);
                    int isoYear = buddhistYear - 543;
                    return LocalDate.of(isoYear, m, d);
                } catch (Exception ex) {
                    showWarn("รูปแบบวันที่ไม่ถูกต้อง", "โปรดใช้ dd/MM/yyyy (เช่น 01/10/2568)");
                    return null;
                }
            }
        };
    }

    /** กดบันทึก: ตรวจ-สร้าง code-เขียนไฟล์ + Alert พร้อมปุ่มคัดลอก */
    private void onSave() {
        String name = safe(tfCustomerName.getText());
        String phone = safe(tfPhone.getText());
        String plate = safe(tfPlate.getText());
        String province = safe(tfProvince.getText());
        LocalDate registeredDate = dpRegisteredDate.getValue();
        String status = cbStatus.getValue();
        String symptom = safe(taSymptom.getText());
        String bikeModel = getBikeModelText(); // อ่านค่าจาก ComboBox (รองรับกรอกเอง)

        String err = validateForm(name, phone, plate, province, registeredDate, status, bikeModel);
        if (err != null) { showWarn("ข้อมูลไม่ครบถ้วน", err); return; }

        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("id", UUID.randomUUID().toString());
        customer.put("customerCode", genCustomerCode(CUSTOMER_CODE_LEN));
        customer.put("name", name);
        customer.put("phone", phone);
        customer.put("plate", plate);
        customer.put("province", province);
        customer.put("bikeModel", bikeModel);
        customer.put("receivedDate", registeredDate);
        customer.put("registeredAt", LocalDateTime.now().toString());
        customer.put("status", status);
        customer.put("symptom", symptom);

        Map<String, Object> repair = new LinkedHashMap<>();
        repair.put("parts", new ArrayList<>());
        repair.put("grandTotal", 0.0);
        repair.put("notes", "");
        repair.put("lastUpdated", LocalDateTime.now().toString());
        customer.put("repair", repair);

        try {
            JsonUtil.ensureJsonArrayFile(CUSTOMERS_JSON);
            java.util.List<Map> list = new ArrayList<>(JsonUtil.readList(CUSTOMERS_JSON, Map.class));
            list.add(customer);
            JsonUtil.writeList(CUSTOMERS_JSON, list);

            String code = (String) customer.get("customerCode");

            // ✅ Alert พร้อมปุ่ม "คัดลอกรหัส"
            showSavedWithCopy(code);

            // เคลียร์ฟอร์มบางส่วน
            tfCustomerName.clear();
            tfPhone.clear();
            tfPlate.clear();
            tfProvince.clear();
            taSymptom.clear();
            cbBikeModel.getEditor().clear();
            cbBikeModel.getSelectionModel().clearSelection();

        } catch (Exception ex) {
            ex.printStackTrace();
            showError("บันทึกไม่สำเร็จ", ex);
        }
    }

    /** ดึงข้อความจาก ComboBox รุ่นรถ (รองรับทั้งเลือกจากรายการและพิมพ์เอง) */
    private String getBikeModelText() {
        String val = cbBikeModel.getValue();
        if (val != null && !val.isBlank()) return val.trim();
        String typed = cbBikeModel.getEditor() != null ? cbBikeModel.getEditor().getText() : "";
        return safe(typed);
    }

    // ===== Alert แบบมีปุ่มคัดลอก =====
    private void showSavedWithCopy(String code) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("บันทึกสำเร็จ");
        alert.setHeaderText("สร้างข้อมูลลูกค้าเรียบร้อย");
        alert.setContentText("รหัสลูกค้า: " + code);

        ButtonType copyBtn = new ButtonType("คัดลอกรหัส", ButtonBar.ButtonData.LEFT);
        ButtonType okBtn   = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(copyBtn, okBtn);

        // แสดง และเช็กว่ากดปุ่มไหน
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == copyBtn) {
            Clipboard cb = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(code);
            cb.setContent(content);

            // แจ้งสั้น ๆ ว่าคัดลอกแล้ว
            Alert done = new Alert(Alert.AlertType.INFORMATION, "คัดลอก \"" + code + "\" แล้ว", ButtonType.OK);
            done.setHeaderText(null);
            done.showAndWait();
        }
    }

    // ===== Validation / Utils =====
    private String validateForm(String name, String phone, String plate, String province,
                                LocalDate date, String status, String bikeModel) {
        List<String> problems = new ArrayList<>();
        if (name.isEmpty()) problems.add("กรุณาระบุชื่อลูกค้า");
        if (phone.isEmpty()) problems.add("กรุณาระบุเบอร์โทร");
        if (plate.isEmpty()) problems.add("กรุณาระบุป้ายทะเบียน");
        if (province.isEmpty()) problems.add("กรุณาระบุจังหวัดทะเบียน");
        if (date == null) problems.add("กรุณาระบุวันที่ลงทะเบียน");
        if (status == null || status.isEmpty()) problems.add("กรุณาเลือกสถานะ");
        if (bikeModel.isEmpty()) problems.add("กรุณาระบุรุ่นรถ");

        if (!phone.isEmpty() && !phone.matches("\\d{8,12}")) {
            problems.add("รูปแบบเบอร์โทรไม่ถูกต้อง (ควรเป็นตัวเลข 8-12 หลัก)");
        }
        return problems.isEmpty() ? null : String.join("\n", problems);
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private String genCustomerCode(int len) {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = randomAlnum(len);
            if (!isCodeExists(code)) return code;
        }
        return randomAlnum(len - 4)
                + Long.toString(System.currentTimeMillis(), 36)
                .toUpperCase(Locale.ROOT)
                .substring(0, 4);
    }
    private String randomAlnum(int len) {
        char[] out = new char[len];
        for (int i = 0; i < len; i++) out[i] = ALPHANUM[RNG.nextInt(ALPHANUM.length)];
        return new String(out);
    }
    private boolean isCodeExists(String code) {
        try {
            JsonUtil.ensureJsonArrayFile(CUSTOMERS_JSON);
            java.util.List<Map> list = JsonUtil.readList(CUSTOMERS_JSON, Map.class);
            for (Map m : list) {
                Object v = m.get("customerCode");
                if (v != null && code.equalsIgnoreCase(String.valueOf(v))) return true;
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private void showWarn(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title); a.setHeaderText(title); a.setContentText(msg);
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
