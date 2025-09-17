package com.example.big_bike_auto.customer;

import com.example.big_bike_auto.common.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * เก็บ/อ่านจาก data/customers.json
 * - เขียนแบบ atomic (.tmp → replace)
 * - มี updateRepair(customerId, repairInfo)
 */
public class CustomerRepository {

    private static final Path DATA_DIR  = Path.of("data");
    private static final Path FILE_PATH = DATA_DIR.resolve("customers.json");
    private static final ReentrantLock WRITE_LOCK = new ReentrantLock();

    public CustomerRepository() {
        try {
            if (!Files.exists(DATA_DIR)) Files.createDirectories(DATA_DIR);
            if (!Files.exists(FILE_PATH)) Files.writeString(FILE_PATH, "[]", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize data dir", e);
        }
    }

    public List<Customer> findAll() {
        try {
            String json = Files.readString(FILE_PATH, StandardCharsets.UTF_8);
            List<Customer> list = JsonUtil.mapper().readValue(json, new TypeReference<List<Customer>>() {});
            return list != null ? list : Collections.emptyList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public Optional<Customer> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        return findAll().stream().filter(c -> id.equals(c.getId())).findFirst();
    }

    /** upsert = เพิ่มใหม่ถ้าไม่มี / แทนที่ถ้ามี id ซ้ำ */
    public void upsert(Customer c) {
        if (c.getId() == null || c.getId().isBlank())
            throw new IllegalArgumentException("Customer.id is required");

        WRITE_LOCK.lock();
        try {
            List<Customer> list = new ArrayList<>(findAll());
            boolean replaced = false;
            for (int i = 0; i < list.size(); i++) {
                if (Objects.equals(list.get(i).getId(), c.getId())) {
                    list.set(i, c);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) list.add(c);
            writeAll(list);
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    /** อัปเดตเฉพาะ repair ของลูกค้า + timestamp + (ออปชัน) สถานะรวม */
    public void updateRepair(String customerId, Customer.RepairInfo repair, Customer.RepairStatus newStatusOrNull) {
        WRITE_LOCK.lock();
        try {
            List<Customer> list = new ArrayList<>(findAll());
            boolean found = false;
            for (int i = 0; i < list.size(); i++) {
                Customer c = list.get(i);
                if (Objects.equals(c.getId(), customerId)) {
                    if (repair != null) {
                        repair.setLastUpdated(LocalDateTime.now());
                        // คำนวณ grandTotal ตรงนี้ ถ้าไม่ได้ตั้งมาก่อน
                        if (repair.getGrandTotal() == null && repair.getParts() != null) {
                            double sum = repair.getParts().stream()
                                    .mapToDouble(p -> (p.getQuantity() == null ? 0 : p.getQuantity()) *
                                            (p.getUnitPrice() == null ? 0 : p.getUnitPrice()))
                                    .sum();
                            repair.setGrandTotal(sum);
                        }
                    }
                    c.setRepair(repair);
                    if (newStatusOrNull != null) c.setStatus(newStatusOrNull);
                    list.set(i, c);
                    found = true;
                    break;
                }
            }
            if (!found) throw new IllegalArgumentException("Customer not found: " + customerId);
            writeAll(list);
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    private void writeAll(List<Customer> list) {
        try {
            String out = JsonUtil.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(list);
            Path tmp = FILE_PATH.resolveSibling(FILE_PATH.getFileName() + ".tmp");
            Files.writeString(tmp, out, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, FILE_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write customers.json", e);
        }
    }
}
