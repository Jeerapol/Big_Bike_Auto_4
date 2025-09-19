package com.example.big_bike_auto.service;

import com.example.big_bike_auto.model.PurchaseOrder;
import com.example.big_bike_auto.model.OrderItem;
import com.example.big_bike_auto.model.viewmodel.OrderRow;
import com.example.big_bike_auto.repository.PurchaseOrderRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OrderService – เวอร์ชันเข้ากับโมเดลจริงของโปรเจกต์:
 * PurchaseOrder{id, supplier, orderDate, received:boolean, items: List<OrderItem>}
 *
 * หมายเหตุสำคัญ:
 * - โปรเจกต์นี้มี OrderItem(String sku, String name, String unit, int quantity, double unitPrice)
 *   เราจึงต้องลองคอนสตรักเตอร์ 5 พารามิเตอร์ก่อน (รองรับ unit)
 * - ยังคงมี fallback สำหรับซิกเนเจอร์อื่น ๆ เผื่อการเปลี่ยนแปลงในอนาคต
 */
public class OrderService {

    private final PurchaseOrderRepository orderRepo = new PurchaseOrderRepository();
    private static final DateTimeFormatter ID_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    /* ========= Query ========= */

    public List<PurchaseOrder> listPOs(String status, String supplier, LocalDate from, LocalDate to) {
        String normalized = normalizeStatus(status);

        return orderRepo.findAll().stream()
                .filter(po -> {
                    if (supplier != null && !supplier.isBlank()) {
                        if (!supplier.equals(po.getSupplier())) return false;
                    }
                    if (!"ALL".equals(normalized)) {
                        boolean wantReceived = "RECEIVED".equals(normalized);
                        boolean isReceived = po.isReceived();
                        if (wantReceived && !isReceived) return false;
                        if (!wantReceived && isReceived) return false; // normalized OPEN
                    }
                    if (from != null) {
                        if (po.getOrderDate() == null || po.getOrderDate().isBefore(from)) return false;
                    }
                    if (to != null) {
                        if (po.getOrderDate() == null || po.getOrderDate().isAfter(to)) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(PurchaseOrder::getOrderDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    private String normalizeStatus(String s) {
        if (s == null || s.isBlank()) return "ALL";
        String up = s.toUpperCase(Locale.ROOT);
        return switch (up) {
            case "ALL" -> "ALL";
            case "RECEIVED" -> "RECEIVED";
            case "OPEN", "DRAFT", "SUBMITTED" -> "OPEN";
            default -> "ALL";
        };
    }

    /* ========= Commands ========= */

    /**
     * สร้าง/ต่อเติม PO แบบ OPEN (received=false) ต่อ supplier
     * - ถ้ามี PO ของ supplier ที่ received=false อยู่แล้ว → append รายการ
     * - ถ้าไม่มี → สร้างใหม่ id = PO-YYYYMMDD-XXX
     */
    public void createOrAppendDraftPO(String supplier, List<OrderRow> rows) {
        if (rows == null || rows.isEmpty()) return;

        PurchaseOrder open = findOpenBySupplier(supplier);
        if (open == null) {
            open = new PurchaseOrder(nextId(), supplier, LocalDate.now()); // received=false โดยดีฟอลต์
        }

        for (OrderRow r : rows) {
            if (r.getOrderQty() <= 0) continue;

            // ⚠️ OrderRow ไม่มี unit → เลือก default unit เช่น "pcs"
            String unit = "pcs";

            // ✅ ใช้ factory แบบยืดหยุ่น สร้าง OrderItem ตาม constructor ที่มีจริง
            OrderItem item = createOrderItemFlexible(r.getPartCode(), r.getName(), unit, r.getOrderQty(), 0.0);
            open.addItem(item);
        }

        save(open);
    }

    /** submitPO: โมเดลจริงไม่มีสถานะย่อย → ไม่เปลี่ยนแปลง (กันเผื่อ UX ในอนาคต) */
    public void submitPO(String orderId) {
        PurchaseOrder po = findById(orderId);
        if (po == null) return;
        save(po);
    }

    /** mark เป็น received แล้วบันทึก */
    public void receivePO(String orderId, String note) {
        PurchaseOrder po = findById(orderId);
        if (po == null) return;
        po.markAsReceived();
        save(po);
        // (ถ้าต้องปรับ stock ให้ไปเรียก repository ที่เกี่ยวข้อง)
    }

    /** ส่งออก CSV ง่ายๆ ไปยัง export/purchase_orders.csv */
    public void exportPOsCsv(List<PurchaseOrder> pos) {
        try {
            Path dir = Paths.get("export");
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path file = dir.resolve("purchase_orders.csv");

            try (BufferedWriter w = Files.newBufferedWriter(file)) {
                w.write("id,supplier,status,orderDate,items,totalQty,totalCost");
                w.newLine();
                for (PurchaseOrder po : pos) {
                    int totalQty = totalQuantity(po);
                    double totalCost = totalCost(po);
                    int items = (po.getItems() == null ? 0 : po.getItems().size());
                    String status = po.isReceived() ? "RECEIVED" : "OPEN";
                    String orderDate = po.getOrderDate() == null ? "" : po.getOrderDate().toString();

                    String line = String.join(",",
                            safe(po.getId()),
                            safe(po.getSupplier()),
                            status,
                            orderDate,
                            String.valueOf(items),
                            String.valueOf(totalQty),
                            String.format(Locale.US, "%.2f", totalCost)
                    );
                    w.write(line);
                    w.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Export CSV failed: " + e.getMessage(), e);
        }
    }

    /* ========= Helpers ========= */

    public int totalQuantity(PurchaseOrder po) {
        if (po == null || po.getItems() == null) return 0;
        try {
            return po.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        } catch (NoSuchMethodError | Exception ignored) {
            return 0;
        }
    }

    public double totalCost(PurchaseOrder po) {
        if (po == null) return 0.0;
        return po.getTotalAmount();
    }

    private PurchaseOrder findById(String id) {
        if (id == null) return null;
        return orderRepo.findAll().stream()
                .filter(po -> id.equals(po.getId()))
                .findFirst().orElse(null);
    }

    private PurchaseOrder findOpenBySupplier(String supplier) {
        if (supplier == null) return null;
        return orderRepo.findAll().stream()
                .filter(po -> supplier.equals(po.getSupplier()) && !po.isReceived())
                .findFirst().orElse(null);
    }

    private String nextId() {
        String today = LocalDate.now().format(ID_DAY);
        long countToday = orderRepo.findAll().stream()
                .filter(po -> po.getOrderDate() != null && today.equals(po.getOrderDate().format(ID_DAY)))
                .count();
        return "PO-" + today + "-" + String.format("%03d", countToday + 1);
    }

    private void save(PurchaseOrder po) {
        List<PurchaseOrder> all = new ArrayList<>(orderRepo.findAll());
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (Objects.equals(all.get(i).getId(), po.getId())) {
                all.set(i, po);
                replaced = true;
                break;
            }
        }
        if (!replaced) all.add(po);
        orderRepo.saveAll(all);
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace(",", " ");
    }

    /* ========= Flexible OrderItem Factory (Reflection) ========= */

    /**
     * พยายามสร้าง OrderItem ตาม constructor/signature ที่มีจริงในโปรเจกต์
     * ลำดับที่ลอง:
     * 0) (String, String, String, int, double)  <-- ของโปรเจกต์นี้
     * 1) (String, String, int, double)
     * 2) (String, String, int)
     * 3) (String, int, double)
     * 4) (String, int)
     * 5) () + setters: setPartCode/setName/setQuantity/setUnitPrice ถ้ามี
     */
    private OrderItem createOrderItemFlexible(String partCode, String name, String unit, int quantity, double unitPrice) {
        Class<OrderItem> clazz = OrderItem.class;

        // 0) (String, String, String, int, double)  <-- ตรงกับ OrderItem จริงในโปรเจกต์
        OrderItem inst = tryCtor(clazz,
                new Class[]{String.class, String.class, String.class, int.class, double.class},
                new Object[]{partCode, name, unit, quantity, unitPrice});
        if (inst != null) return inst;

        // 1) (String, String, int, double)
        inst = tryCtor(clazz,
                new Class[]{String.class, String.class, int.class, double.class},
                new Object[]{partCode, name, quantity, unitPrice});
        if (inst != null) return inst;

        // 2) (String, String, int)
        inst = tryCtor(clazz,
                new Class[]{String.class, String.class, int.class},
                new Object[]{partCode, name, quantity});
        if (inst != null) return inst;

        // 3) (String, int, double)
        inst = tryCtor(clazz,
                new Class[]{String.class, int.class, double.class},
                new Object[]{partCode, quantity, unitPrice});
        if (inst != null) return inst;

        // 4) (String, int)
        inst = tryCtor(clazz,
                new Class[]{String.class, int.class},
                new Object[]{partCode, quantity});
        if (inst != null) return inst;

        // 5) () + setters (โปรเจกต์นี้ไม่มี setter สำหรับ sku/name/unit จึงใช้ไม่ครบ)
        inst = tryCtor(clazz, new Class[]{}, new Object[]{});
        if (inst != null) {
            callSetterIfExists(inst, "setPartCode", String.class, partCode);
            callSetterIfExists(inst, "setName", String.class, name);
            callSetterIfExists(inst, "setQuantity", int.class, quantity);
            callSetterIfExists(inst, "setUnitPrice", double.class, unitPrice);
            callSetterIfExists(inst, "setUnit", String.class, unit);
            callSetterIfExists(inst, "setSku", String.class, partCode);
            return inst;
        }

        throw new RuntimeException("Cannot instantiate OrderItem with any known constructor/signature");
    }

    private static <T> T tryCtor(Class<T> cls, Class<?>[] sig, Object[] args) {
        try {
            Constructor<T> c = cls.getDeclaredConstructor(sig);
            c.setAccessible(true);
            return c.newInstance(args);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void callSetterIfExists(Object obj, String name, Class<?> argType, Object value) {
        try {
            Method m = obj.getClass().getMethod(name, argType);
            m.setAccessible(true);
            m.invoke(obj, value);
        } catch (ReflectiveOperationException ignored) {
            // ไม่มี setter ดังกล่าวก็ข้าม
        }
    }
}
