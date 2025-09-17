package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.RouterHub; // ✅ ใช้สำหรับ navigate กลับเมื่ออยู่บน Primary Stage
import com.example.big_bike_auto.customer.Customer;
import com.example.big_bike_auto.customer.CustomerRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.Serializable;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

/**
 * RepairDetailsController (customers.json only)
 *
 * เป้าหมาย:
 * - อ่าน/เขียนข้อมูลจาก CustomerRepository (data/customers.json) เพียงแหล่งเดียว
 * - แสดง/แก้ไขเฉพาะ "สถานะงาน" และ "โน้ต" แล้วบันทึกกลับ Customer.status / Customer.symptom
 * - ตารางอะไหล่/ยอดรวมเป็นเพียง UI ช่วยคำนวณ (ยังไม่ persist ลง customers.json)
 *
 * หมายเหตุ:
 * - jobId = Customer.id (UUID string) ใช้ค้นหาลูกค้าเป้าหมาย
 * - สถานะ UI เป็นภาษาไทย → map เข้ากับ Customer.RepairStatus
 */
public class RepairDetailsController {

    // ===== UI refs =====
    @FXML private Label lbJobId;
    @FXML private Label lbCustomerName;
    @FXML private Label lbBikeModel;
    @FXML private Label lbReceivedDate;

    @FXML private ComboBox<String> cbStatus;   // ใช้ String (ไทย) แล้ว map -> Customer.RepairStatus
    @FXML private TextArea taNotes;

    @FXML private TableView<PartUsage> tvParts;
    @FXML private TableColumn<PartUsage, String>  colPartName;
    @FXML private TableColumn<PartUsage, Integer> colQty;
    @FXML private TableColumn<PartUsage, String>  colUnit;
    @FXML private TableColumn<PartUsage, Double>  colUnitPrice;
    @FXML private TableColumn<PartUsage, Double>  colTotal;

    @FXML private TextField tfPartName;
    @FXML private TextField tfQty;
    @FXML private TextField tfUnit;
    @FXML private TextField tfUnitPrice;

    @FXML private Label lbGrandTotal;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("th", "TH"));
    private final ObservableList<PartUsage> parts = FXCollections.observableArrayList();

    // ===== State =====
    private String currentCustomerId;     // = Customer.id (UUID string)
    private Customer currentCustomer;     // ลูกค้าปัจจุบัน
    private final CustomerRepository customerRepo = new CustomerRepository();

    // โหมดเปิดหน้าจอ: true = Dialog แยก, false = Primary Stage ผ่าน Router
    private boolean openedAsDialog = false;

    /** ใช้โดยผู้เรียกเพื่อระบุว่าหน้านี้ถูกเปิดแบบ Dialog หรือไม่ */
    public void setOpenedAsDialog(boolean value) { this.openedAsDialog = value; }

    // ===== Lifecycle =====
    @FXML
    private void initialize() {
        // Table mapping (lambda type-safe)
        colPartName.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getPartName()));
        colQty.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getQuantity()));
        colUnit.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getUnit()));
        colUnitPrice.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getUnitPrice()));
        colTotal.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getTotalPrice()));

        // รูปแบบการแสดงผลชิดขวา
        colQty.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.valueOf(v));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
        colUnitPrice.setCellFactory(currencyCell());
        colTotal.setCellFactory(currencyCell());

        tvParts.setItems(parts);
        lbGrandTotal.setText(currencyFormat.format(0.0));

        // รายการสถานะ (ภาษาไทย) ให้ตรงกับ Register/Dashboard
        cbStatus.getItems().setAll(
                "รับงานแล้ว",          // RECEIVED
                "กำลังวิเคราะห์อาการ",   // DIAGNOSING
                "รออะไหล่",             // WAIT_PARTS
                "กำลังซ่อม",            // REPAIRING
                "ตรวจสอบคุณภาพ",        // QA
                "เสร็จสิ้น"             // DONE
        );
    }

    // ===== เปิดหน้าจอด้วย id ลูกค้า (เป็น Dialog แยก) =====
    public static void openWithJobId(UUID jobId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    RepairDetailsController.class.getResource("/com/example/big_bike_auto/ui/RepairDetails.fxml")
            );
            Scene scene = new Scene(loader.load());

            RepairDetailsController controller = loader.getController();
            controller.setOpenedAsDialog(true);  // ✅ บอกว่าเปิดเป็น Dialog
            controller.loadJob(jobId);

            Stage stage = new Stage();
            stage.setTitle("รายละเอียดงานซ่อม");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            showException("ไม่สามารถเปิดหน้า Repair Details", e);
        }
    }

    /** โหลดข้อมูลจาก customers.json โดยใช้ jobId = Customer.id (UUID) */
    public void loadJob(UUID jobId) {
        try {
            this.currentCustomerId = jobId.toString();
            Optional<Customer> opt = customerRepo.findById(currentCustomerId);
            if (opt.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "ไม่พบข้อมูล", "ไม่พบ Customer ID: " + currentCustomerId);
                return;
            }
            this.currentCustomer = opt.get();

            // เติม UI
            lbJobId.setText(currentCustomerId);
            lbCustomerName.setText(nullToDash(currentCustomer.getName()));

            // bikeModel อาจไม่มีในโมเดลเดิม: สังเคราะห์จาก plate/province/phone
            String plate = safe(getSafe(currentCustomer::getPlate));
            String prov  = safe(getSafe(currentCustomer::getProvince));
            String phone = safe(getSafe(currentCustomer::getPhone));
            String model = (plate.isBlank() && prov.isBlank() && phone.isBlank())
                    ? "-" : "%s (%s/%s)".formatted(plate, prov, phone);
            lbBikeModel.setText(model);

            LocalDate received = getSafe(currentCustomer::getReceivedDate);
            if (received == null && currentCustomer.getRegisteredAt() != null) {
                received = currentCustomer.getRegisteredAt().toLocalDate();
            }
            lbReceivedDate.setText(received != null ? received.toString() : "-");

            // map สถานะ enum -> ข้อความไทยใน combobox
            cbStatus.getSelectionModel().select(mapCustomerEnumToUi(currentCustomer.getStatus()));

            // โน้ต = symptom ในโมเดลเดิม
            taNotes.setText(safe(getSafe(currentCustomer::getSymptom)));

            // parts เป็นแค่ UI ไม่ persist
            parts.setAll(new ArrayList<>());
            recalcGrandTotal();

        } catch (Exception ex) {
            showException("โหลดงานซ่อมไม่สำเร็จ", ex);
        }
    }

    // ===== ปุ่มกด =====
    @FXML
    private void onAddPart(ActionEvent event) {
        try {
            String name = safe(tfPartName.getText());
            String unit = safe(tfUnit.getText());
            int qty = parsePositiveInt(tfQty.getText(), "จำนวนต้องเป็นจำนวนเต็ม > 0");
            double unitPrice = parseNonNegativeDouble(tfUnitPrice.getText(), "ราคา/หน่วย ต้องเป็นตัวเลข ≥ 0");

            if (name.isBlank()) throw new IllegalArgumentException("กรุณากรอกชื่ออะไหล่");
            if (unit.isBlank()) throw new IllegalArgumentException("กรุณากรอกหน่วย");

            parts.add(new PartUsage(name, qty, unit, unitPrice));
            clearPartInputs();
            recalcGrandTotal();
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.WARNING, "ข้อมูลไม่ถูกต้อง", ex.getMessage());
        } catch (Exception ex) {
            showException("เพิ่มรายการไม่สำเร็จ", ex);
        }
    }

    @FXML
    private void onRemoveSelectedPart(ActionEvent event) {
        PartUsage sel = tvParts.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.INFORMATION, "ยังไม่ได้เลือกรายการ", "กรุณาเลือกแถวก่อนลบ");
            return;
        }
        parts.remove(sel);
        recalcGrandTotal();
    }

    /** บันทึกกลับ customers.json: อัปเดตเฉพาะ status + notes(symptom) */
    @FXML
    private void onSave(ActionEvent event) {
        try {
            if (currentCustomer == null || currentCustomerId == null) {
                showAlert(Alert.AlertType.ERROR, "ไม่พบข้อมูลลูกค้า", "ไม่สามารถบันทึกได้เพราะไม่มี Customer ปัจจุบัน");
                return;
            }

            // อัปเดตค่าจาก UI
            Customer.RepairStatus newStatus = mapUiToCustomerEnum(cbStatus.getValue());
            currentCustomer.setStatus(newStatus);
            currentCustomer.setSymptom(safe(taNotes.getText()));
            // set registeredAt ถ้ายังไม่มี เพื่อช่วยการเรียงบน Dashboard
            if (currentCustomer.getRegisteredAt() == null) {
                currentCustomer.setRegisteredAt(LocalDateTime.now());
            }

            // เขียนทับลง customers.json
            customerRepo.upsert(currentCustomer);

            showAlert(Alert.AlertType.INFORMATION, "บันทึกสำเร็จ", "อัปเดตสถานะและบันทึกโน้ตเรียบร้อย");
        } catch (Exception ex) {
            showException("บันทึกไม่สำเร็จ", ex);
        }
    }

    /** ปุ่มกลับ: ปิดเฉพาะ Dialog หรือ Navigate กลับ Dashboard เมื่ออยู่บน Primary Stage */
    @FXML
    private void onBack(ActionEvent event) {
        try {
            if (openedAsDialog) {
                // ✅ โหมด Dialog: ปิดเฉพาะหน้าต่างนี้
                Stage stage = (Stage) tvParts.getScene().getWindow();
                stage.close();
            } else {
                // ✅ โหมด Primary Stage: นำทางกลับหน้า Dashboard (หรือหน้า list ตามที่คุณต้องการ)
                RouterHub.openDashboard();
            }
        } catch (Exception ex) {
            // กันกรณี Router ยังไม่พร้อม: อย่างน้อยอย่าปิดทั้งแอป
            Stage stage = (Stage) tvParts.getScene().getWindow();
            if (stage != null) stage.hide();
        }
    }

    // ===== Helpers =====
    private void recalcGrandTotal() {
        double total = parts.stream().mapToDouble(PartUsage::getTotalPrice).sum();
        lbGrandTotal.setText(currencyFormat.format(total));
    }

    private void clearPartInputs() {
        tfPartName.clear();
        tfQty.clear();
        tfUnit.clear();
        tfUnitPrice.clear();
        tfPartName.requestFocus();
    }

    // map: UI text (ไทย) -> Customer.RepairStatus
    private static Customer.RepairStatus mapUiToCustomerEnum(String uiText) {
        String t = uiText == null ? "" : uiText.trim();
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

    // map: Customer.RepairStatus -> UI text (ไทย)
    private static String mapCustomerEnumToUi(Customer.RepairStatus st) {
        if (st == null) return "รับงานแล้ว";
        return switch (st) {
            case REPAIRING -> "กำลังซ่อม";
            case WAIT_PARTS -> "รออะไหล่";
            case DIAGNOSING -> "กำลังวิเคราะห์อาการ";
            case QA -> "ตรวจสอบคุณภาพ";
            case DONE -> "เสร็จสิ้น";
            case RECEIVED -> "รับงานแล้ว";
        };
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
    private static String nullToDash(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    private static int parsePositiveInt(String s, String err) {
        try {
            int v = Integer.parseInt(safe(s));
            if (v <= 0) throw new NumberFormatException();
            return v;
        } catch (Exception e) {
            throw new IllegalArgumentException(err);
        }
    }

    private static double parseNonNegativeDouble(String s, String err) {
        try {
            double v = Double.parseDouble(safe(s));
            if (v < 0) throw new NumberFormatException();
            return v;
        } catch (Exception e) {
            throw new IllegalArgumentException(err);
        }
    }

    private static void showAlert(Alert.AlertType type, String header, String content) {
        Alert a = new Alert(type);
        a.setHeaderText(header);
        a.setContentText(content);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }

    private static void showException(String header, Exception ex) {
        ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(header);
        a.setContentText(ex.getMessage() != null ? ex.getMessage() : ex.toString());
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }

    private static <T> T getSafe(Supplier<T> s) {
        try { return s.get(); } catch (Throwable t) { return null; }
    }

    private javafx.util.Callback<TableColumn<PartUsage, Double>, TableCell<PartUsage, Double>> currencyCell() {
        return col -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) setText(null);
                else setText(currencyFormat.format(v));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        };
    }

    // ===== Part row (UI only; not persisted) =====
    public static class PartUsage implements Serializable {
        private String partName;
        private int quantity;
        private String unit;
        private double unitPrice;

        public PartUsage() {}
        public PartUsage(String partName, int quantity, String unit, double unitPrice) {
            this.partName = partName; this.quantity = quantity; this.unit = unit; this.unitPrice = unitPrice;
        }
        public String getPartName() { return partName; }
        public void setPartName(String partName) { this.partName = partName; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
        public double getTotalPrice() { return unitPrice * quantity; }
    }
}
