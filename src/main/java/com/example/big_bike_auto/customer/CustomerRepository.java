package com.example.big_bike_auto.customer;

import com.example.big_bike_auto.common.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * CustomerRepository (JSON) จัดเก็บข้อมูลลูกค้าในไฟล์ data/customers.json
 * - ใช้ JsonUtil.mapper() เพื่อความสม่ำเสมอ
 * - ปลอดภัยจากไฟล์หาย: สร้างโฟลเดอร์/ไฟล์ให้อัตโนมัติ
 * - ทนทานกับไฟล์ว่าง/ข้อมูลเสีย: คืนค่าเป็นลิสต์ว่าง
 * - Thread-safe (synchronized) ระหว่างการเขียนไฟล์
 */
public class CustomerRepository {
    private final Path dataDir = Path.of("data");
    private final Path dataFile = dataDir.resolve("customers.json");

    /** โหลดลูกค้าทั้งหมด (ถ้าไฟล์ไม่มี/ว่าง -> [] ) */
    public List<Customer> findAll() throws IOException {
        ensureFile();
        try {
            byte[] bytes = Files.readAllBytes(dataFile);
            if (bytes.length == 0) return new ArrayList<>();
            return JsonUtil.mapper().readValue(bytes, new TypeReference<List<Customer>>() {});
        } catch (Exception ex) {
            // ถ้า parse พัง ให้คืนลิสต์ว่างแทน
            return new ArrayList<>();
        }
    }

    /** บันทึกลูกค้าใหม่ (append) */
    public synchronized void append(Customer c) throws IOException {
        List<Customer> all = findAll();
        all.add(c);
        writeAll(all);
    }

    /** คืนลูกค้าล่าสุด N ราย (เรียงตาม registeredAt ใหม่→เก่า) */
    public List<Customer> findRecent(int limit) throws IOException {
        List<Customer> all = findAll();
        all.sort(Comparator.comparing(Customer::getRegisteredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        if (all.size() <= limit) return all;
        return new ArrayList<>(all.subList(0, limit));
    }

    private void writeAll(List<Customer> all) throws IOException {
        ensureFile();
        byte[] bytes = JsonUtil.mapper().writeValueAsBytes(all);
        Files.writeString(dataFile, new String(bytes));
    }

    private void ensureFile() throws IOException {
        if (!Files.exists(dataDir)) Files.createDirectories(dataDir);
        if (!Files.exists(dataFile)) Files.writeString(dataFile, "[]");
    }
}
