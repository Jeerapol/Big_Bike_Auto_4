package com.example.big_bike_auto.service;

import com.example.big_bike_auto.model.Part;
import com.example.big_bike_auto.model.PurchaseOrder;
import com.example.big_bike_auto.model.OrderItem;
import com.example.big_bike_auto.model.viewmodel.InventoryRow;
import com.example.big_bike_auto.repository.PartRepository;
import com.example.big_bike_auto.repository.PurchaseOrderRepository;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;


public class InventoryService {

    private final PartRepository partRepo = new PartRepository();
    private final PurchaseOrderRepository poRepo = new PurchaseOrderRepository();

    /** โหลดข้อมูล + คำนวณ KPI แล้วแปลงเป็น InventoryRow */
    public List<InventoryRow> buildInventoryRows() {
        List<Part> parts = new ArrayList<>(partRepo.findAll());

        List<PurchaseOrder> pos = new ArrayList<>(poRepo.findAll());
        Map<String, Integer> onOrderByPart = pos.stream()
                .filter(po -> !po.isReceived())
                .flatMap(po -> po.getItems().stream())
                .collect(Collectors.groupingBy(
                        it -> OrderItemAccessor.getPartCode(it),
                        Collectors.summingInt(OrderItemAccessor::getQuantity)
                ));

        Map<String, Integer> reservedByPart = Collections.emptyMap();

        List<InventoryRow> rows = new ArrayList<>(parts.size());
        for (Part p : parts) {
            String code = safe(PartAccessor.getCode(p));
            String name = safe(PartAccessor.getName(p));
            String supplier = safe(PartAccessor.getSupplier(p));
            String category = safe(PartAccessor.getCategory(p));

            int inStock = nz(PartAccessor.getInStock(p));
            int minStock = nz(PartAccessor.getMinStock(p));
            int reserved = nz(reservedByPart.getOrDefault(code, 0));
            int onOrder = nz(onOrderByPart.getOrDefault(code, 0));
            int needed = Math.max(0, minStock - (inStock + onOrder - reserved));

            rows.add(new InventoryRow(
                    code, name, supplier, category,
                    inStock, minStock, reserved, onOrder, needed
            ));
        }
        rows.sort(Comparator.comparingInt(InventoryRow::getNeeded).reversed()
                .thenComparing(InventoryRow::getPartCode));
        return rows;
    }

    /** ปรับสต็อกของ Part ตาม partCode (เพิ่ม/ลด) */
    public void adjustStock(String partCode, int delta) {
        if (partCode == null || partCode.isBlank() || delta == 0) return;

        List<Part> all = new ArrayList<>(partRepo.findAll());
        boolean found = false;
        for (Part p : all) {
            if (partCode.equals(PartAccessor.getCode(p))) {
                int now = nz(PartAccessor.getInStock(p));
                int next = Math.max(0, now + delta);
                PartAccessor.setInStock(p, next);
                found = true;
                break;
            }
        }
        if (!found) throw new RuntimeException("ไม่พบ partCode: " + partCode);
        partRepo.saveAll(all);
    }

    /** Export ตาราง inventory เป็น CSV */
    public void exportInventoryCsv(List<InventoryRow> rows) {
        try {
            Path dir = Paths.get("export");
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path file = dir.resolve("inventory.csv");
            try (BufferedWriter w = Files.newBufferedWriter(file)) {
                w.write("code,name,supplier,category,inStock,minStock,reserved,onOrder,needed");
                w.newLine();
                for (InventoryRow r : rows) {
                    String line = String.join(",",
                            csv(r.getPartCode()),
                            csv(r.getName()),
                            csv(r.getSupplier()),
                            csv(r.getCategory()),
                            String.valueOf(r.getInStock()),
                            String.valueOf(r.getMinStock()),
                            String.valueOf(r.getReserved()),
                            String.valueOf(r.getOnOrder()),
                            String.valueOf(r.getNeeded())
                    );
                    w.write(line);
                    w.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Export inventory CSV failed: " + e.getMessage(), e);
        }
    }

    private static int nz(Integer i) { return i == null ? 0 : i; }
    private static String safe(String s) { return s == null ? "" : s; }
    private static String csv(String s) { return s == null ? "" : s.replace(",", " "); }

    /* ============================================================
     * Reflection Adapters (รองรับทั้ง method และ field)
     * ============================================================ */

    private static final class PartAccessor {
        private static final String[] CODE = {"getCode","getId","getPartCode","getSku","code","id"};
        private static final String[] NAME = {"getName","getPartName","getTitle","name"};
        private static final String[] SUPPLIER = {"getSupplier","getVendor","getSupplierName","supplier"};
        private static final String[] CATEGORY = {"getCategory","getType","getGroup","category"};
        private static final String[] INSTOCK = {"getInStock","getStock","getQuantity","getQty","getOnHand","inStock","stock","quantity","qty"};
        private static final String[] MINSTOCK = {"getMinStock","getReorderPoint","getSafetyStock","minStock","reorderPoint","safetyStock"};
        private static final String[] INSTOCK_SET = {"setInStock","setStock","setQuantity","setQty","setOnHand"};

        static String getCode(Part p)     { return (String) readFirst(p, CODE, String.class, ""); }
        static String getName(Part p)     { return (String) readFirst(p, NAME, String.class, ""); }
        static String getSupplier(Part p) { return (String) readFirst(p, SUPPLIER, String.class, ""); }
        static String getCategory(Part p) { return (String) readFirst(p, CATEGORY, String.class, ""); }

        static Integer getInStock(Part p) {
            Number n = (Number) readFirst(p, INSTOCK, Number.class, 0);
            return n == null ? 0 : n.intValue();
        }
        static Integer getMinStock(Part p) {
            Number n = (Number) readFirst(p, MINSTOCK, Number.class, 0);
            return n == null ? 0 : n.intValue();
        }

        static void setInStock(Part p, int value) {
            // 1) ลอง setter ก่อน
            for (String m : INSTOCK_SET) {
                try {
                    Method method = p.getClass().getMethod(m, int.class);
                    method.setAccessible(true);
                    method.invoke(p, value);
                    return;
                } catch (ReflectiveOperationException ignored) {}
                try {
                    Method method = p.getClass().getMethod(m, Integer.class);
                    method.setAccessible(true);
                    method.invoke(p, Integer.valueOf(value));
                    return;
                } catch (ReflectiveOperationException ignored) {}
            }
            // 2) ถ้าไม่มี setter ให้ลองเขียนลง field โดยตรง
            for (String fName : new String[]{"inStock","stock","quantity","qty","onHand"}) {
                try {
                    Field f = fieldOf(p.getClass(), fName);
                    if (f == null) continue;
                    f.setAccessible(true);
                    if (f.getType() == int.class) { f.setInt(p, value); return; }
                    if (f.getType() == Integer.class) { f.set(p, Integer.valueOf(value)); return; }
                } catch (ReflectiveOperationException ignored) {}
            }
            throw new RuntimeException("ไม่พบช่องทางปรับ inStock (setter/field) ใน Part");
        }

        /** อ่านจาก method ก่อน ถ้าไม่เจอให้ลอง field ชื่อเดียวกัน */
        private static Object readFirst(Object target, String[] names, Class<?> expect, Object defaultVal) {
            Class<?> cls = target.getClass();
            // 1) methods
            for (String name : names) {
                try {
                    Method m = cls.getMethod(name);
                    m.setAccessible(true);
                    Object val = m.invoke(target);
                    if (val == null) return defaultVal;
                    if (expect.isInstance(val)) return val;
                    if (expect == String.class && val instanceof Number num) return String.valueOf(num);
                    if (expect == Number.class && val instanceof String s) {
                        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
                    }
                } catch (ReflectiveOperationException ignored) {}
            }
            // 2) fields
            for (String name : names) {
                try {
                    Field f = fieldOf(cls, name);
                    if (f == null) continue;
                    f.setAccessible(true);
                    Object val = f.get(target);
                    if (val == null) return defaultVal;
                    if (expect.isInstance(val)) return val;
                    if (expect == String.class && val instanceof Number num) return String.valueOf(num);
                    if (expect == Number.class && val instanceof String s) {
                        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
                    }
                } catch (ReflectiveOperationException ignored) {}
            }
            return defaultVal;
        }

        private static Field fieldOf(Class<?> cls, String name) {
            // รองรับชื่อ field แบบตรง ๆ: name "code" "id" ฯลฯ
            try { return cls.getDeclaredField(name); } catch (NoSuchFieldException e) { /* ignore */ }
            // เผื่อบางชื่อขึ้นต้นด้วย get* เราจะ map เป็นชื่อ field แบบ lowerCamel
            if (name.startsWith("get") && name.length() > 3) {
                String f = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                try { return cls.getDeclaredField(f); } catch (NoSuchFieldException e) { /* ignore */ }
            }
            return null;
        }
    }

    private static final class OrderItemAccessor {
        private static final String[] PARTCODE = {"getPartCode","getCode","getSku","getItemCode","partCode","code","sku"};
        private static final String[] QTY = {"getQuantity","getQty","getAmount","quantity","qty"};

        static String getPartCode(OrderItem it) {
            return (String) readFirst(it, PARTCODE, String.class, "");
        }
        static int getQuantity(OrderItem it) {
            Number n = (Number) readFirst(it, QTY, Number.class, 0);
            return n == null ? 0 : n.intValue();
        }

        private static Object readFirst(Object target, String[] names, Class<?> expect, Object defaultVal) {
            Class<?> cls = target.getClass();
            for (String name : names) {
                try {
                    Method m = cls.getMethod(name);
                    m.setAccessible(true);
                    Object val = m.invoke(target);
                    if (val == null) return defaultVal;
                    if (expect.isInstance(val)) return val;
                    if (expect == String.class && val instanceof Number num) return String.valueOf(num);
                    if (expect == Number.class && val instanceof String s) {
                        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
                    }
                } catch (ReflectiveOperationException ignored) {}
                try { // field fallback
                    Field f = cls.getDeclaredField(name);
                    f.setAccessible(true);
                    Object val = f.get(target);
                    if (val == null) return defaultVal;
                    if (expect.isInstance(val)) return val;
                } catch (ReflectiveOperationException ignored) {}
            }
            return defaultVal;
        }
    }
}
