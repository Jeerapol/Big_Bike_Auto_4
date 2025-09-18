package com.example.big_bike_auto.parts;

import com.example.big_bike_auto.common.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Repository สำหรับ part_orders.json
 * - ใช้ GsonUtil เพื่อให้รองรับ LocalDate/BigDecimal โดยไม่ชน module
 * - มีการสร้างไฟล์เริ่มต้นถ้ายังไม่เคยมี
 */
public class PartOrderRepository {

    private final File dataFile;
    private final Gson gson = GsonUtil.gson(); // ✅ ใช้ Gson กลาง

    private static final Type LIST_TYPE = new TypeToken<List<PartOrder>>() {}.getType();

    public PartOrderRepository() {
        File dir = new File("data");
        if (!dir.exists()) dir.mkdirs();
        this.dataFile = new File(dir, "part_orders.json");
        if (!dataFile.exists()) {
            try (Writer w = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
                gson.toJson(new ArrayList<>(), LIST_TYPE, w);
            } catch (IOException e) {
                throw new RuntimeException("Cannot initialize part_orders.json", e);
            }
        }
    }

    public synchronized List<PartOrder> findAll() {
        try (Reader r = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            List<PartOrder> list = gson.fromJson(r, LIST_TYPE);
            return list == null ? new ArrayList<>() : list;
        } catch (IOException e) {
            throw new RuntimeException("Read part_orders.json failed", e);
        }
    }

    public synchronized void upsert(PartOrder order) {
        List<PartOrder> list = findAll();
        list.removeIf(x -> Objects.equals(x.getOrderId(), order.getOrderId()));
        list.add(order);
        saveAll(list);
    }

    public synchronized Optional<PartOrder> findById(UUID id) {
        return findAll().stream().filter(x -> x.getOrderId().equals(id)).findFirst();
    }

    private synchronized void saveAll(List<PartOrder> list) {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
            gson.toJson(list, LIST_TYPE, w);
        } catch (IOException e) {
            throw new RuntimeException("Save part_orders.json failed", e);
        }
    }
}
