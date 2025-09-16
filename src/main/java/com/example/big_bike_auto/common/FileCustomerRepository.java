package com.example.big_bike_auto.customer;


import com.example.big_bike_auto.common.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class FileCustomerRepository {
    private static final Path DATA_DIR = Path.of("data");
    private static final Path FILE_PATH = DATA_DIR.resolve("customers.json");
    private static final Lock WRITE_LOCK = new ReentrantLock();


    public FileCustomerRepository() {
        try {
            if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);
            if (!Files.exists(FILE_PATH)) Files.writeString(FILE_PATH, "[]", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize data directory", e);
        }
    }


    public List<Customer> findAll() {
        try {
            String json = Files.readString(FILE_PATH, StandardCharsets.UTF_8);
            List<Customer> list = JsonUtil.mapper().readValue(json, new TypeReference<List<Customer>>(){});
            if (list == null) return Collections.emptyList();
            return list;
        } catch (IOException e) {
// ไฟล์เสีย/อ่านไม่ได้ -> คืนลิสต์ว่าง
            return Collections.emptyList();
        }
    }


    public List<Customer> findLatest(int limit) {
        List<Customer> all = new ArrayList<>(findAll());
        all.sort(Comparator.comparing(Customer::getRegisteredAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return all.subList(0, Math.min(limit, all.size()));
    }


    public Customer saveNew(String name, String phone, String email) {
        Customer c = new Customer(UUID.randomUUID().toString(), name, phone, email, LocalDateTime.now());
        save(c);
        return c;
    }


    public void save(Customer customer) {
        WRITE_LOCK.lock();
        try {
            List<Customer> list = new ArrayList<>(findAll());
            list.add(customer);
            String out = JsonUtil.mapper().writeValueAsString(list);
// เขียนแบบ atomic: เขียนลง temp แล้วค่อย replace
            Path tmp = FILE_PATH.resolveSibling(FILE_PATH.getFileName() + ".tmp");
            Files.writeString(tmp, out, StandardCharsets.UTF_8);
            Files.move(tmp, FILE_PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write customers.json", e);
        } finally {
            WRITE_LOCK.unlock();
        }
    }
}