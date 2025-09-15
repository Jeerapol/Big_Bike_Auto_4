package com.example.big_bike_auto.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;

public class RepairDetailsController {

    @FXML private Label lbJobId;
    @FXML private Label lbCustomerName;
    @FXML private Label lbBikeModel;
    @FXML private Label lbReceivedDate;
    @FXML private ComboBox<RepairStatus> cbStatus;
    @FXML private TextArea taNotes;

    @FXML private TableView<PartUsage> tvParts;
    @FXML private TableColumn<PartUsage, String> colPartName;
    @FXML private TableColumn<PartUsage, Integer> colQty;
    @FXML private TableColumn<PartUsage, String> colUnit;
    @FXML private TableColumn<PartUsage, Double> colUnitPrice;
    @FXML private TableColumn<PartUsage, Double> colTotal;

    @FXML private TextField tfPartName;
    @FXML private TextField tfQty;
    @FXML private TextField tfUnit;
    @FXML private TextField tfUnitPrice;

    @FXML private Label lbGrandTotal;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("th", "TH"));
    private final ObservableList<PartUsage> parts = FXCollections.observableArrayList();
    private UUID currentJobId;

    private final RepairJobRepository repo = new FileRepairJobRepository();

    @FXML
    private void initialize() {
        colPartName.setCellValueFactory(new PropertyValueFactory<>("partName"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        tvParts.setItems(parts);
        cbStatus.setItems(FXCollections.observableArrayList(RepairStatus.values()));
        lbGrandTotal.setText(currencyFormat.format(0.0));
    }

    /** แก้ path → ไปที่โฟลเดอร์ ui */
    public static void openWithJob(UUID jobId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    RepairDetailsController.class.getResource("/com/example/big_bike_auto/ui/RepairDetails.fxml")
            );
            Scene scene = new Scene(loader.load());

            RepairDetailsController controller = loader.getController();
            controller.loadJob(jobId);

            Stage stage = new Stage();
            stage.setTitle("รายละเอียดงานซ่อม");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            showException("ไม่สามารถเปิดหน้า Repair Details", e);
        }
    }

    @FXML
    private void onAddPart(ActionEvent event) {
        try {
            String name = safeTrim(tfPartName.getText());
            String unit = safeTrim(tfUnit.getText());
            int qty = parsePositiveInt(tfQty.getText(), "จำนวนต้องเป็นจำนวนเต็มมากกว่า 0");
            double unitPrice = parseNonNegativeDouble(tfUnitPrice.getText(), "ราคา/หน่วย ต้องเป็นตัวเลข >= 0");

            if (name.isEmpty()) throw new IllegalArgumentException("กรุณากรอกชื่ออะไหล่");
            if (unit.isEmpty()) throw new IllegalArgumentException("กรุณากรอกหน่วย");

            PartUsage part = new PartUsage(name, qty, unit, unitPrice);
            parts.add(part);
            recalcGrandTotal();
            clearPartInputs();
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.WARNING, "ข้อมูลไม่ถูกต้อง", ex.getMessage());
        } catch (Exception ex) {
            showException("เกิดข้อผิดพลาดขณะเพิ่มรายการ", ex);
        }
    }

    @FXML
    private void onRemoveSelectedPart(ActionEvent event) {
        PartUsage sel = tvParts.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.INFORMATION, "ยังไม่ได้เลือกรายการ", "กรุณาเลือกแถวในตารางก่อนลบ");
            return;
        }
        parts.remove(sel);
        recalcGrandTotal();
    }

    @FXML
    private void onSave(ActionEvent event) {
        try {
            if (currentJobId == null) {
                showAlert(Alert.AlertType.ERROR, "ไม่พบรหัสงาน", "ไม่สามารถระบุรหัสงานซ่อมได้");
                return;
            }

            RepairJob job = repo.findById(currentJobId)
                    .orElseThrow(() -> new IllegalStateException("ไม่พบงานซ่อมในระบบ"));

            job.setStatus(cbStatus.getValue());
            job.setNotes(safeTrim(taNotes.getText()));
            job.setParts(new ArrayList<>(parts));

            validateJobBeforeSave(job);
            repo.save(job);

            showAlert(Alert.AlertType.INFORMATION, "บันทึกสำเร็จ", "อัปเดตข้อมูลงานซ่อมเรียบร้อยแล้ว");
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.WARNING, "ข้อมูลไม่ถูกต้อง", ex.getMessage());
        } catch (Exception ex) {
            showException("บันทึกไม่สำเร็จ", ex);
        }
    }

    @FXML
    private void onBack(ActionEvent event) {
        Stage stage = (Stage) tvParts.getScene().getWindow();
        stage.close();
    }

    private void loadJob(UUID jobId) {
        try {
            Optional<RepairJob> opt = repo.findById(jobId);
            if (opt.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "ไม่พบงานซ่อม", "Job ID: " + jobId + " ไม่พบในระบบ");
                return;
            }

            RepairJob job = opt.get();
            currentJobId = job.getJobId();

            lbJobId.setText(job.getJobId().toString());
            lbCustomerName.setText(nullToDash(job.getCustomerName()));
            lbBikeModel.setText(nullToDash(job.getBikeModel()));
            lbReceivedDate.setText(job.getReceivedDate() != null ? job.getReceivedDate().toString() : "-");

            cbStatus.getSelectionModel().select(job.getStatus() != null ? job.getStatus() : RepairStatus.RECEIVED);
            taNotes.setText(job.getNotes() != null ? job.getNotes() : "");

            parts.setAll(job.getParts() != null ? job.getParts() : Collections.emptyList());
            recalcGrandTotal();
        } catch (Exception ex) {
            showException("โหลดงานซ่อมไม่สำเร็จ", ex);
        }
    }

    private void recalcGrandTotal() {
        double grand = parts.stream().mapToDouble(PartUsage::getTotalPrice).sum();
        lbGrandTotal.setText(currencyFormat.format(grand));
    }

    private void clearPartInputs() {
        tfPartName.clear();
        tfQty.clear();
        tfUnit.clear();
        tfUnitPrice.clear();
        tfPartName.requestFocus();
    }

    private void validateJobBeforeSave(RepairJob job) {
        if (job.getStatus() == null) throw new IllegalArgumentException("กรุณาเลือกสถานะงาน");
        for (PartUsage p : job.getParts()) {
            if (safeTrim(p.getPartName()).isEmpty()) {
                throw new IllegalArgumentException("รายการอะไหล่บางรายการไม่มีชื่อ");
            }
            if (p.getQuantity() <= 0) {
                throw new IllegalArgumentException("จำนวนของ '" + p.getPartName() + "' ต้อง > 0");
            }
            if (p.getUnitPrice() < 0) {
                throw new IllegalArgumentException("ราคา/หน่วยของ '" + p.getPartName() + "' ต้อง >= 0");
            }
        }
    }

    private static String safeTrim(String s) { return s == null ? "" : s.trim(); }
    private static String nullToDash(String s) { return (s == null || s.isEmpty()) ? "-" : s; }

    private static int parsePositiveInt(String text, String errorMsg) {
        try {
            int v = Integer.parseInt(safeTrim(text));
            if (v <= 0) throw new NumberFormatException();
            return v;
        } catch (Exception e) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    private static double parseNonNegativeDouble(String text, String errorMsg) {
        try {
            double v = Double.parseDouble(safeTrim(text));
            if (v < 0) throw new NumberFormatException();
            return v;
        } catch (Exception e) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    private static void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle("Repair Details");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static void showException(String header, Exception ex) {
        ex.printStackTrace(System.err);
        showAlert(Alert.AlertType.ERROR, header, ex.getMessage() != null ? ex.getMessage() : ex.toString());
    }

    public enum RepairStatus {
        RECEIVED("รับงานแล้ว"),
        DIAGNOSING("กำลังวิเคราะห์อาการ"),
        WAIT_PARTS("รออะไหล่"),
        REPAIRING("กำลังซ่อม"),
        QA("ตรวจสอบคุณภาพ"),
        COMPLETED("เสร็จสิ้น"),
        CANCELED("ยกเลิก");

        private final String display;
        RepairStatus(String display) { this.display = display; }
        @Override public String toString() { return display; }
    }

    public static class PartUsage implements Serializable {
        private String partName;
        private int quantity;
        private String unit;
        private double unitPrice;
        public PartUsage() { }
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

    public static class RepairJob implements Serializable {
        private UUID jobId;
        private String customerName;
        private String bikeModel;
        private LocalDate receivedDate;
        private RepairStatus status;
        private String notes;
        private List<PartUsage> parts;
        public RepairJob() { }
        public RepairJob(UUID jobId) { this.jobId = jobId; }
        public UUID getJobId() { return jobId; }
        public void setJobId(UUID jobId) { this.jobId = jobId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getBikeModel() { return bikeModel; }
        public void setBikeModel(String bikeModel) { this.bikeModel = bikeModel; }
        public LocalDate getReceivedDate() { return receivedDate; }
        public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
        public RepairStatus getStatus() { return status; }
        public void setStatus(RepairStatus status) { this.status = status; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public List<PartUsage> getParts() { return parts; }
        public void setParts(List<PartUsage> parts) { this.parts = parts; }
    }

    public interface RepairJobRepository {
        Optional<RepairJob> findById(UUID jobId) throws IOException, ClassNotFoundException;
        void save(RepairJob job) throws IOException, ClassNotFoundException;
        List<RepairJob> findAll() throws IOException, ClassNotFoundException;
    }

    public static class FileRepairJobRepository implements RepairJobRepository {
        private final Path dataDir = Path.of(System.getProperty("user.home"), ".bigbike");
        private final Path dataFile = dataDir.resolve("repair_jobs.ser");

        public FileRepairJobRepository() {
            try {
                if (!Files.exists(dataDir)) Files.createDirectories(dataDir);
                if (!Files.exists(dataFile)) {
                    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(dataFile))) {
                        oos.writeObject(new HashMap<UUID, RepairJob>());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("สร้างพื้นที่จัดเก็บไม่สำเร็จ: " + e.getMessage(), e);
            }
        }

        @Override
        public synchronized Optional<RepairJob> findById(UUID jobId) throws IOException, ClassNotFoundException {
            Map<UUID, RepairJob> map = readAll();
            return Optional.ofNullable(map.get(jobId));
        }

        @Override
        public synchronized void save(RepairJob job) throws IOException, ClassNotFoundException {
            if (job.getJobId() == null) throw new IllegalArgumentException("jobId ห้ามเป็นค่าว่าง");
            Map<UUID, RepairJob> map = readAll();
            map.put(job.getJobId(), job);
            writeAll(map);
        }

        @Override
        public synchronized List<RepairJob> findAll() throws IOException, ClassNotFoundException {
            return new ArrayList<>(readAll().values());
        }

        @SuppressWarnings("unchecked")
        private Map<UUID, RepairJob> readAll() throws IOException, ClassNotFoundException {
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(dataFile))) {
                Object obj = ois.readObject();
                if (obj instanceof Map) return (Map<UUID, RepairJob>) obj;
                return new HashMap<>();
            }
        }

        private void writeAll(Map<UUID, RepairJob> map) throws IOException {
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(dataFile))) {
                oos.writeObject(map);
                oos.flush();
            }
        }
    }
}
