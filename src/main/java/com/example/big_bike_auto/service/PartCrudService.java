package com.example.big_bike_auto.service;

import com.example.big_bike_auto.model.Part;
import com.example.big_bike_auto.repository.PartRepository;
import com.google.gson.Gson;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * PartCrudService (final)
 * - สร้าง/อัปเดต Part แบบไม่พึ่งพา no-arg ctor:
 *   ลองตามลำดับ: no-arg -> ctor ชนิด String/int/Integer -> Gson.fromJson("{}" , Part.class)
 * - ป้องกันรหัสซ้ำ + validate เบื้องต้น
 */
public class PartCrudService {

    private final PartRepository partRepo = new PartRepository();
    private final Gson gson = new Gson(); // ใช้เป็น fallback allocator

    /** DTO รับค่าจากฟอร์ม */
    public static class PartData {
        public final String code;
        public final String name;
        public final String supplier;
        public final String category;
        public final int minStock;
        public final int inStock;

        public PartData(String code, String name, String supplier, String category, int minStock, int inStock) {
            this.code = code == null ? "" : code.trim();
            this.name = name == null ? "" : name.trim();
            this.supplier = supplier == null ? "" : supplier.trim();
            this.category = category == null ? "" : category.trim();
            this.minStock = Math.max(0, minStock);
            this.inStock = Math.max(0, inStock);
        }
    }

    /** ใช้บนฟอร์มเพื่อเช็ครหัสซ้ำแบบเร็ว */
    public boolean existsPartCode(String code) {
        if (code == null || code.isBlank()) return false;
        String c = code.trim();
        for (Part p : partRepo.findAll()) {
            if (c.equalsIgnoreCase(PartAccessor.getCode(p))) return true;
        }
        return false;
    }

    /** สร้างและบันทึก Part ใหม่ */
    public void createPart(PartData data) {
        validate(data);

        // กันรหัสซ้ำ
        List<Part> all = new ArrayList<>(partRepo.findAll());
        String codeLower = data.code.toLowerCase(Locale.ROOT);
        for (Part p : all) {
            if (codeLower.equals(PartAccessor.getCode(p).toLowerCase(Locale.ROOT))) {
                throw new RuntimeException("รหัสอะไหล่ซ้ำ: " + data.code);
            }
        }

        // สร้าง Part แบบยืดหยุ่น
        Part newPart = instantiatePartFlexible(data);

        // เซ็ตค่าผ่าน setter อีกครั้ง (เผื่อ ctor/fallback เติมมาไม่ครบ)
        PartAccessor.setCode(newPart, data.code);
        PartAccessor.setName(newPart, data.name);
        PartAccessor.setSupplier(newPart, data.supplier);
        PartAccessor.setCategory(newPart, data.category);
        PartAccessor.setMinStock(newPart, data.minStock);
        PartAccessor.setInStock(newPart, data.inStock);

        all.add(newPart);
        partRepo.saveAll(all);
    }

    private void validate(PartData d) {
        if (d.code.isBlank()) throw new RuntimeException("กรุณาระบุรหัสสินค้า");
        if (d.name.isBlank()) throw new RuntimeException("กรุณาระบุชื่อสินค้า");
    }

    /**
     * กลยุทธ์สร้าง Part:
     * 1) no-arg constructor
     * 2) constructor ที่ทุกพารามิเตอร์เป็น String/int/Integer (เรียงตามจำนวนพารามิเตอร์มาก→น้อย)
     * 3) Gson.fromJson("{}", Part.class)  ← ทางหนีทีไล่ (ไม่เรียก ctor)
     */
    private Part instantiatePartFlexible(PartData d) {
        // 1) no-arg
        try {
            Constructor<Part> noArg = Part.class.getDeclaredConstructor();
            noArg.setAccessible(true);
            return noArg.newInstance();
        } catch (NoSuchMethodException ignore) {
            // ไม่มี no-arg → ไปต่อ
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            // มีแต่สร้างไม่สำเร็จ → แจ้งทันที
            throw new RuntimeException("สร้าง Part ด้วย no-arg constructor ไม่สำเร็จ: " + e.getMessage(), e);
        }

        // 2) any-arg (String/int/Integer)
        Constructor<?>[] ctors = Part.class.getDeclaredConstructors();
        Arrays.sort(ctors, Comparator.comparingInt((Constructor<?> c) -> c.getParameterTypes().length).reversed());

        Object[] canonical = new Object[]{
                d.code, d.name, d.supplier, d.category,
                Integer.valueOf(d.minStock), Integer.valueOf(d.inStock)
        };

        for (Constructor<?> ctor : ctors) {
            Class<?>[] pts = ctor.getParameterTypes();
            if (!allSupported(pts)) continue;

            Object[] args = mapArgs(pts, canonical);
            if (args == null) continue;

            try {
                ctor.setAccessible(true);
                Object obj = ctor.newInstance(args);
                return (Part) obj;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
                // ลองตัวถัดไป
            }
        }

        // 3) Fallback ด้วย Gson (ไม่เรียก constructor)
        try {
            Part p = gson.fromJson("{}", Part.class); // จัดสรรอ็อบเจ็กต์เปล่า
            if (p != null) return p;
        } catch (Throwable ignored) {
        }

        // ถ้า Gson ยังไม่ผ่าน ให้บอกแนวทางแก้ชัดเจน
        throw new RuntimeException(
                "ไม่สามารถสร้าง Part ได้: โมเดลไม่มี no-arg constructor และไม่พบ constructor ที่รองรับเฉพาะชนิด String/int/Integer.\n" +
                        "ทางแก้ที่แนะนำ: เพิ่ม public Part() หรือเพิ่ม constructor ที่รับ (String code, String name, String supplier, String category, int minStock, int inStock)"
        );
    }

    private boolean allSupported(Class<?>[] pts) {
        for (Class<?> t : pts) {
            if (!(t == String.class || t == int.class || t == Integer.class)) return false;
        }
        return true;
    }

    private Object[] mapArgs(Class<?>[] pts, Object[] canonical) {
        Object[] out = new Object[pts.length];
        // เรา map แบบตำแหน่ง: เติมค่าจาก canonical ตามชนิดพารามิเตอร์
        int strIdx = 0; // ชี้ไปยัง code,name,supplier,category
        int numIdx = 4; // ชี้ไปยัง minStock,inStock
        for (int i = 0; i < pts.length; i++) {
            Class<?> t = pts[i];
            if (t == String.class) {
                if (strIdx > 3) return null;
                out[i] = canonical[strIdx++];
            } else if (t == int.class || t == Integer.class) {
                if (numIdx > 5) return null;
                Integer v = (Integer) canonical[numIdx++];
                out[i] = (t == int.class) ? v.intValue() : v;
            } else {
                return null;
            }
        }
        return out;
    }

    // ---------- Accessor: เรียก getter/setter อย่างยืดหยุ่น ----------
    private static final class PartAccessor {
        private static final String[] CODE_GETTERS = {"getCode", "getId", "getPartCode", "getSku", "code", "id"};
        private static final String[] CODE_SETTERS = {"setCode", "setId", "setPartCode", "setSku"};
        private static final String[] NAME_SETTERS = {"setName", "setPartName", "setTitle"};
        private static final String[] SUPPLIER_SETTERS = {"setSupplier", "setVendor", "setSupplierName"};
        private static final String[] CATEGORY_SETTERS = {"setCategory", "setType", "setGroup"};
        private static final String[] INSTOCK_SETTERS = {"setInStock", "setStock", "setQuantity", "setQty", "setOnHand"};
        private static final String[] MINSTOCK_SETTERS = {"setMinStock", "setReorderPoint", "setSafetyStock"};

        static String getCode(Part p) { return (String) callFirstNoArg(p, CODE_GETTERS, String.class); }

        static void setCode(Part p, String v) { callFirstOneArg(p, CODE_SETTERS, String.class, v); }
        static void setName(Part p, String v) { callFirstOneArg(p, NAME_SETTERS, String.class, v); }
        static void setSupplier(Part p, String v) { callFirstOneArg(p, SUPPLIER_SETTERS, String.class, v); }
        static void setCategory(Part p, String v) { callFirstOneArg(p, CATEGORY_SETTERS, String.class, v); }
        static void setInStock(Part p, int v) { callFirstOneArgIntOrBoxed(p, INSTOCK_SETTERS, v); }
        static void setMinStock(Part p, int v) { callFirstOneArgIntOrBoxed(p, MINSTOCK_SETTERS, v); }

        private static Object callFirstNoArg(Object target, String[] names, Class<?> expect) {
            Class<?> c = target.getClass();
            for (String n : names) {
                try {
                    Method m = c.getMethod(n);
                    m.setAccessible(true);
                    Object val = m.invoke(target);
                    if (val == null) return "";
                    if (expect.isInstance(val)) return val;
                    if (expect == String.class && val instanceof Number num) return String.valueOf(num);
                } catch (ReflectiveOperationException ignored) {}
            }
            return "";
        }

        private static void callFirstOneArg(Object target, String[] names, Class<?> argType, Object value) {
            Class<?> c = target.getClass();
            for (String n : names) {
                try {
                    Method m = c.getMethod(n, argType);
                    m.setAccessible(true);
                    m.invoke(target, value);
                    return;
                } catch (ReflectiveOperationException ignored) {}
            }
            // ไม่พัง: ถ้าไม่พบ setter กรณี text fields ให้ข้ามได้
        }

        private static void callFirstOneArgIntOrBoxed(Object target, String[] names, int value) {
            Class<?> c = target.getClass();
            for (String n : names) {
                try {
                    Method m = c.getMethod(n, int.class);
                    m.setAccessible(true);
                    m.invoke(target, value);
                    return;
                } catch (ReflectiveOperationException ignored) {}
                try {
                    Method m = c.getMethod(n, Integer.class);
                    m.setAccessible(true);
                    m.invoke(target, Integer.valueOf(value));
                    return;
                } catch (ReflectiveOperationException ignored) {}
            }
            throw new RuntimeException("ไม่พบ setter สำหรับฟิลด์ตัวเลข (" + String.join(", ", names) + ")");
        }
    }
}
