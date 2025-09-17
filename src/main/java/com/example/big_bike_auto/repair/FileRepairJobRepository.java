package com.example.big_bike_auto.repair;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * FileRepairJobRepository:
 * เก็บงานซ่อมแบบ "ไฟล์ละ 1 งาน" (ไฟล์ .ser) ในโฟลเดอร์ data/repairs
 * - เลือก per-file เพื่อหลีกเลี่ยง merge-conflict เวลาหลายงานถูกแก้พร้อมกัน
 * - ใช้ ReadWriteLock เพื่อความปลอดภัยเมื่อเข้าถึงพร้อมกันหลายเธรด
 * - เขียนแบบ atomic: เขียนไฟล์ชั่วคราวแล้วค่อย replace
 */
public class FileRepairJobRepository {

    private static final String DATA_DIR = "data/repairs";
    private static final String EXT = ".ser";

    private final Path baseDir;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public FileRepairJobRepository() {
        this(Paths.get(DATA_DIR));
    }

    public FileRepairJobRepository(Path dir) {
        this.baseDir = dir;
        ensureDir();
    }

    // สร้างโฟลเดอร์หากยังไม่มี
    private void ensureDir() {
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("ไม่สามารถสร้างโฟลเดอร์เก็บข้อมูล: " + baseDir, e);
        }
    }

    private Path fileOfId(String id) {
        return baseDir.resolve(id + EXT);
    }

    /**
     * สร้างหรืออัปเดตงานซ่อมแบบ atomic
     * - เขียนไปที่ไฟล์ temp แล้ว replace
     */
    public void saveOrUpdate(RepairJob job) {
        Objects.requireNonNull(job, "job == null");
        Objects.requireNonNull(job.getId(), "job.id == null");

        lock.writeLock().lock();
        try {
            Path target = fileOfId(job.getId());
            Path tmp = baseDir.resolve(job.getId() + EXT + ".tmp");

            // เขียน object ไปไฟล์ temp
            try (OutputStream fos = Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(job);
                oos.flush();
            }

            // แทนที่แบบ atomic (ถ้าระบบไฟล์รองรับ)
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        } catch (IOException e) {
            throw new RuntimeException("บันทึกงานซ่อมไม่สำเร็จ: " + job.getId(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** อ่านงานซ่อมตาม id */
    public Optional<RepairJob> findById(String id) {
        Objects.requireNonNull(id, "id == null");
        lock.readLock().lock();
        try {
            Path p = fileOfId(id);
            if (!Files.exists(p)) return Optional.empty();
            try (InputStream fis = Files.newInputStream(p, StandardOpenOption.READ);
                 BufferedInputStream bis = new BufferedInputStream(fis);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                Object obj = ois.readObject();
                if (obj instanceof RepairJob rj) return Optional.of(rj);
                return Optional.empty();
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException("อ่านงานซ่อมล้มเหลว: " + id, e);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /** ลบงานซ่อมตาม id */
    public boolean deleteById(String id) {
        Objects.requireNonNull(id, "id == null");
        lock.writeLock().lock();
        try {
            Path p = fileOfId(id);
            if (!Files.exists(p)) return false;
            Files.delete(p);
            return true;
        } catch (IOException e) {
            throw new RuntimeException("ลบงานซ่อมล้มเหลว: " + id, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * ดึงรายการงานซ่อมทั้งหมด (หรือจำนวนล่าสุด)
     * - ใช้เวลาจริงจะ optimize โดยแคช/ดัชนีในหน่วยความจำได้
     */
    public List<RepairJob> listAll(int limitNewestFirst) {
        lock.readLock().lock();
        try {
            List<Path> files = Files.list(baseDir)
                    .filter(p -> p.getFileName().toString().endsWith(EXT))
                    .sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed())
                    .collect(Collectors.toList());

            if (limitNewestFirst > 0 && files.size() > limitNewestFirst) {
                files = files.subList(0, limitNewestFirst);
            }

            List<RepairJob> out = new ArrayList<>();
            for (Path p : files) {
                try (InputStream fis = Files.newInputStream(p, StandardOpenOption.READ);
                     BufferedInputStream bis = new BufferedInputStream(fis);
                     ObjectInputStream ois = new ObjectInputStream(bis)) {
                    Object obj = ois.readObject();
                    if (obj instanceof RepairJob rj) {
                        out.add(rj);
                    }
                } catch (Exception ignore) {
                    // บางไฟล์เสียหาย: ข้ามแล้วไปต่อ (สามารถล็อกเก็บได้)
                }
            }
            return out;

        } catch (IOException e) {
            throw new RuntimeException("อ่านรายการงานซ่อมล้มเหลว", e);
        } finally {
            lock.readLock().unlock();
        }
    }
}
