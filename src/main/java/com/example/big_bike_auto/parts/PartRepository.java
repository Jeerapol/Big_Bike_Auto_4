package com.example.big_bike_auto.parts;

import com.example.big_bike_auto.common.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Repository สำหรับ parts.json
 * - ใช้ GsonUtil กลาง (มี BigDecimal adapter)
 * - สร้างไฟล์เริ่มต้นถ้าไม่พบ
 */
public class PartRepository {

    private final File dataFile;
    private final Gson gson = GsonUtil.gson(); // ✅ ใช้ Gson กลาง

    private static final Type LIST_TYPE = new TypeToken<List<Part>>() {}.getType();

    public PartRepository() {
        File dir = new File("data");
        if (!dir.exists()) dir.mkdirs();
        this.dataFile = new File(dir, "parts.json");
        if (!dataFile.exists()) {
            try {
                saveAll(defaultSeed());
            } catch (IOException e) {
                throw new RuntimeException("Cannot initialize parts.json", e);
            }
        }
    }

    public synchronized List<Part> findAll() {
        try (Reader r = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            List<Part> list = gson.fromJson(r, LIST_TYPE);
            return list == null ? new ArrayList<>() : list;
        } catch (IOException e) {
            throw new RuntimeException("Read parts.json failed", e);
        }
    }

    public synchronized Optional<Part> findBySku(String sku) {
        if (sku == null) return Optional.empty();
        return findAll().stream().filter(p -> sku.equalsIgnoreCase(p.getSku())).findFirst();
    }

    public synchronized void upsert(Part part) {
        List<Part> list = findAll();
        list.removeIf(p -> p.getSku().equalsIgnoreCase(part.getSku()));
        list.add(part);
        try {
            saveAll(list);
        } catch (IOException e) {
            throw new RuntimeException("Save parts.json failed", e);
        }
    }

    public synchronized void deleteBySku(String sku) {
        List<Part> list = findAll();
        list.removeIf(p -> p.getSku().equalsIgnoreCase(sku));
        try {
            saveAll(list);
        } catch (IOException e) {
            throw new RuntimeException("Save parts.json failed", e);
        }
    }

    public synchronized void saveAll(List<Part> parts) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
            gson.toJson(parts, LIST_TYPE, w);
        }
    }

    private List<Part> defaultSeed() {
        List<Part> init = new ArrayList<>();
        init.add(new Part("OIL-10W40", "น้ำมันเครื่อง 10W40", "ลิตร", 12, 2, 8, new BigDecimal("180"), "A-Supply"));
        init.add(new Part("BRK-PAD-FR", "ผ้าเบรกหน้า", "ชุด", 5, 1, 3, new BigDecimal("450"), "B-Shop"));
        init.add(new Part("AIR-FLT", "กรองอากาศ", "ชิ้น", 3, 0, 5, new BigDecimal("220"), "A-Supply"));
        return init;
    }
}
