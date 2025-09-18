package com.example.big_bike_auto.controller;

import com.example.big_bike_auto.RouterHub;
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

public class RepairDetailsController {

    @FXML private Label lbJobId;
    @FXML private Label lbCustomerName;
    @FXML private Label lbBikeModel;
    @FXML private Label lbReceivedDate;

    @FXML private ComboBox<String> cbStatus;   // แสดงข้อความไทย แล้ว map -> Customer.RepairStatus
    @FXML private TextArea taNotes;            // notes ของงานซ่อม (persist ที่ RepairInfo.notes)

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

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("th","TH"));
    private final ObservableList<PartUsage> parts = FXCollections.observableArrayList();

    private String currentCustomerId;     // = Customer.id
    private Customer currentCustomer;
    private final CustomerRepository customerRepo = new CustomerRepository();

    private boolean openedAsDialog = false;
    public void setOpenedAsDialog(boolean value) { this.openedAsDialog = value; }

    // -------------------- init --------------------
    @FXML
    private void initialize() {
        colPartName.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getPartName()));
        colQty.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getQuantity()));
        colUnit.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getUnit()));
        colUnitPrice.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getUnitPrice()));
        colTotal.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getTotalPrice()));

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

        cbStatus.getItems().setAll(
                "รับงานแล้ว","กำลังวิเคราะห์อาการ","รออะไหล่","กำลังซ่อม","ตรวจสอบคุณภาพ","เสร็จสิ้น"
        );
    }

    // เปิดเป็น dialog แยก
    public static void openWithJobId(UUID jobId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    RepairDetailsController.class.getResource("/com/example/big_bike_auto/ui/RepairDetails.fxml")
            );
            Scene scene = new Scene(loader.load());
            RepairDetailsController c = loader.getController();
            c.setOpenedAsDialog(true);
            c.loadJob(jobId);

            Stage stage = new Stage();
            stage.setTitle("รายละเอียดงานซ่อม");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            showException("ไม่สามารถเปิดหน้า Repair Details", e);
        }
    }

    // โหลดข้อมูลลูกค้าจาก customers.json
    public void loadJob(UUID jobId) {
        try {
            currentCustomerId = jobId.toString();
            Optional<Customer> opt = customerRepo.findById(currentCustomerId);
            if (opt.isEmpty()) { showAlert(Alert.AlertType.ERROR,"ไม่พบข้อมูล","ไม่พบ Customer ID: "+currentCustomerId); return; }
            currentCustomer = opt.get();

            lbJobId.setText(currentCustomerId);
            lbCustomerName.setText(nullOrDash(currentCustomer.getName()));

            String plate = safe(getSafe(currentCustomer::getPlate));
            String prov  = safe(getSafe(currentCustomer::getProvince));
            String phone = safe(getSafe(currentCustomer::getPhone));
            lbBikeModel.setText((plate.isBlank() && prov.isBlank() && phone.isBlank())
                    ? "-" : "%s (%s/%s)".formatted(plate, prov, phone));

            LocalDate received = getSafe(currentCustomer::getReceivedDate);
            if (received == null && currentCustomer.getRegisteredAt() != null) {
                received = currentCustomer.getRegisteredAt().toLocalDate();
            }
            lbReceivedDate.setText(received != null ? received.toString() : "-");

            cbStatus.getSelectionModel().select(enumToUi(currentCustomer.getStatus()));

            // ✅ โหลด RepairInfo (parts + notes)
            parts.clear();
            Customer.RepairInfo ri = currentCustomer.getRepair();
            if (ri != null) {
                taNotes.setText(safe(ri.getNotes()));
                if (ri.getParts() != null) {
                    for (Customer.PartItem pi : ri.getParts()) {
                        parts.add(new PartUsage(
                                safe(pi.getPartName()),
                                pi.getQuantity() == null ? 0 : pi.getQuantity(),
                                safe(pi.getUnit()),
                                pi.getUnitPrice() == null ? 0.0 : pi.getUnitPrice()
                        ));
                    }
                }
                // ใช้ grandTotal ที่มี ถ้ามี
                double gt = ri.getGrandTotal() != null ? ri.getGrandTotal()
                        : parts.stream().mapToDouble(PartUsage::getTotalPrice).sum();
                lbGrandTotal.setText(currencyFormat.format(gt));
            } else {
                taNotes.setText(""); // fallback: ถ้ายังไม่เคยมี repair
                recalcGrandTotal();
            }
        } catch (Exception ex) {
            showException("โหลดงานซ่อมไม่สำเร็จ", ex);
        }
    }

    // -------------------- actions --------------------
    @FXML
    private void onAddPart(ActionEvent e) {
        try {
            String name = safe(tfPartName.getText());
            String unit = safe(tfUnit.getText());
            int qty = parsePositiveInt(tfQty.getText(), "จำนวนต้องเป็นจำนวนเต็ม > 0");
            double price = parseNonNegativeDouble(tfUnitPrice.getText(), "ราคา/หน่วย ต้องเป็นตัวเลข ≥ 0");
            if (name.isBlank()) throw new IllegalArgumentException("กรุณากรอกชื่ออะไหล่");
            if (unit.isBlank()) throw new IllegalArgumentException("กรุณากรอกหน่วย");

            parts.add(new PartUsage(name, qty, unit, price));
            clearPartInputs();
            recalcGrandTotal();
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.WARNING, "ข้อมูลไม่ถูกต้อง", ex.getMessage());
        } catch (Exception ex) {
            showException("เพิ่มรายการไม่สำเร็จ", ex);
        }
    }

    @FXML
    private void onRemoveSelectedPart(ActionEvent e) {
        PartUsage sel = tvParts.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert(Alert.AlertType.INFORMATION,"ยังไม่ได้เลือกรายการ","กรุณาเลือกแถวก่อนลบ"); return; }
        parts.remove(sel);
        recalcGrandTotal();
    }

    @FXML
    private void onSave(ActionEvent e) {
        try {
            if (currentCustomerId == null) {
                showAlert(Alert.AlertType.ERROR,"ไม่พบข้อมูลลูกค้า","ไม่มี Customer ปัจจุบัน"); return;
            }

            // ถ้ายังไม่เคยตั้ง registeredAt -> ตั้งครั้งแรกไว้ให้ (เพื่อการเรียง/แสดงผล)
            if (currentCustomer != null && currentCustomer.getRegisteredAt() == null) {
                currentCustomer.setRegisteredAt(LocalDateTime.now());
                customerRepo.upsert(currentCustomer); // ตั้ง timestamp ไว้รอบเดียว
            }

            // สถานะใหม่
            Customer.RepairStatus newStatus = uiToEnum(cbStatus.getValue());

            // ✅ สร้าง RepairInfo จาก UI แล้ว persist
            Customer.RepairInfo ri = new Customer.RepairInfo();
            ri.setNotes(safe(taNotes.getText()));

            List<Customer.PartItem> toSave = new ArrayList<>();
            double grand = 0.0;
            for (PartUsage p : parts) {
                toSave.add(new Customer.PartItem(p.getPartName(), p.getQuantity(), p.getUnit(), p.getUnitPrice()));
                grand += p.getTotalPrice();
            }
            ri.setParts(toSave);
            ri.setGrandTotal(grand);
            // lastUpdated จะถูกเซ็ตใน repository (updateRepair)

            customerRepo.updateRepair(currentCustomerId, ri, newStatus);

            showAlert(Alert.AlertType.INFORMATION,"บันทึกสำเร็จ","อัปเดตงานซ่อมเรียบร้อยแล้ว");
        } catch (Exception ex) {
            showException("บันทึกไม่สำเร็จ", ex);
        }
    }

    @FXML
    private void onBack(ActionEvent e) {
        try {
            if (openedAsDialog) {
                ((Stage) tvParts.getScene().getWindow()).close();
            } else {
                RouterHub.openDashboard();
            }
        } catch (Exception ex) {
            Stage st = (Stage) tvParts.getScene().getWindow();
            if (st != null) st.hide();
        }
    }

    // -------------------- helpers --------------------
    private void recalcGrandTotal() {
        double total = parts.stream().mapToDouble(PartUsage::getTotalPrice).sum();
        lbGrandTotal.setText(currencyFormat.format(total));
    }
    private void clearPartInputs() {
        tfPartName.clear(); tfQty.clear(); tfUnit.clear(); tfUnitPrice.clear(); tfPartName.requestFocus();
    }

    private static Customer.RepairStatus uiToEnum(String ui) {
        String t = ui == null ? "" : ui.trim();
        return switch (t) {
            case "กำลังซ่อม" -> Customer.RepairStatus.REPAIRING;
            case "รออะไหล่" -> Customer.RepairStatus.WAIT_PARTS;
            case "กำลังวิเคราะห์อาการ" -> Customer.RepairStatus.DIAGNOSING;
            case "ตรวจสอบคุณภาพ" -> Customer.RepairStatus.QA;
            case "เสร็จสิ้น" -> Customer.RepairStatus.DONE; // NOTE: ถ้า enum คุณคือ DONE ให้แก้เป็น DONE
            default -> Customer.RepairStatus.RECEIVED;
        };
    }
    private static String enumToUi(Customer.RepairStatus st) {
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

    private static String safe(String s){ return s == null ? "" : s.trim(); }
    private static String nullOrDash(String s){ return (s == null || s.isBlank()) ? "-" : s; }
    private static int parsePositiveInt(String s,String err){ try{ int v=Integer.parseInt(safe(s)); if(v<=0) throw new NumberFormatException(); return v;}catch(Exception e){ throw new IllegalArgumentException(err);} }
    private static double parseNonNegativeDouble(String s,String err){ try{ double v=Double.parseDouble(safe(s)); if(v<0) throw new NumberFormatException(); return v;}catch(Exception e){ throw new IllegalArgumentException(err);} }

    private static void showAlert(Alert.AlertType t,String h,String c){ Alert a=new Alert(t); a.setHeaderText(h); a.setContentText(c); a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE); a.showAndWait();}
    private static void showException(String h, Exception ex){ ex.printStackTrace(); Alert a=new Alert(Alert.AlertType.ERROR); a.setHeaderText(h); a.setContentText(ex.getMessage()!=null? ex.getMessage(): ex.toString()); a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE); a.showAndWait(); }
    private static <T> T getSafe(Supplier<T> s){ try{ return s.get(); }catch(Throwable t){ return null; } }

    private javafx.util.Callback<TableColumn<PartUsage, Double>, TableCell<PartUsage, Double>> currencyCell(){
        return col -> new TableCell<>(){
            @Override protected void updateItem(Double v, boolean empty){
                super.updateItem(v, empty);
                setText(empty || v==null ? null : currencyFormat.format(v));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        };
    }

    // Row model บน UI
    public static class PartUsage implements Serializable {
        private String partName;
        private int quantity;
        private String unit;
        private double unitPrice;
        public PartUsage(){}
        public PartUsage(String partName,int quantity,String unit,double unitPrice){
            this.partName=partName; this.quantity=quantity; this.unit=unit; this.unitPrice=unitPrice;
        }
        public String getPartName(){ return partName; }
        public void setPartName(String partName){ this.partName=partName; }
        public int getQuantity(){ return quantity; }
        public void setQuantity(int quantity){ this.quantity=quantity; }
        public String getUnit(){ return unit; }
        public void setUnit(String unit){ this.unit=unit; }
        public double getUnitPrice(){ return unitPrice; }
        public void setUnitPrice(double unitPrice){ this.unitPrice=unitPrice; }
        public double getTotalPrice(){ return unitPrice*quantity; }
    }
}
