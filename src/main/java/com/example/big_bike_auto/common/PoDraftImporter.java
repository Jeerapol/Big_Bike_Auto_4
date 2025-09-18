package com.example.big_bike_auto.common;

import com.example.big_bike_auto.parts.PartOrder;
import com.example.big_bike_auto.parts.PartOrderRepository;
import com.google.gson.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * PoDraftImporter
 * - นำเข้าไฟล์เก่าจาก data/po_drafts/* (draft/placed/received/cancelled)
 * - แปลงเป็น PartOrder แล้วบันทึกผ่าน PartOrderRepository -> part_orders.json
 * - Idempotent: ใช้ UUID จากชื่อไฟล์เพื่อกันซ้ำ (รันซ้ำไม่เพิ่มรายการเดิม)
 */
public final class PoDraftImporter {

    private PoDraftImporter() {}

    /** คืนจำนวนรายการที่นำเข้า */
    public static int importFromPoDrafts(PartOrderRepository repo) {
        File baseDir = new File("data/po_drafts");
        if (!baseDir.exists() || !baseDir.isDirectory()) return 0;

        File placedDir   = new File(baseDir, "placed");
        File receivedDir = new File(baseDir, "received");
        File cancelledDir= new File(baseDir, "cancelled");

        int count = 0;
        count += scanDir(repo, baseDir,      PartOrder.Status.DRAFT);
        count += scanDir(repo, placedDir,    PartOrder.Status.PLACED);
        count += scanDir(repo, receivedDir,  PartOrder.Status.RECEIVED);
        count += scanDir(repo, cancelledDir, PartOrder.Status.CANCELED);
        return count;
    }

    private static int scanDir(PartOrderRepository repo, File dir, PartOrder.Status status) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return 0;
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) return 0;

        int imported = 0;
        for (File f : files) {
            try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();

                String supplier = getAsString(obj, "supplier", "UNKNOWN");
                LocalDate created = parseLocalDate(getAsString(obj, "createdDate", null));
                if (created == null) created = LocalDate.now();

                List<PartOrder.Line> lines = new ArrayList<>();
                if (obj.has("items") && obj.get("items").isJsonArray()) {
                    JsonArray arr = obj.get("items").getAsJsonArray();
                    for (JsonElement el : arr) {
                        JsonObject it = el.getAsJsonObject();
                        String sku  = getAsString(it, "sku", "-");
                        String name = getAsString(it, "name", "-");
                        int qty     = getAsInt(it, "quantity", getAsInt(it, "qty", 0));
                        BigDecimal unitCost = parseBigDecimal(getAsString(it, "unitCost", "0"));
                        lines.add(new PartOrder.Line(sku, name, qty, unitCost));
                    }
                }
                if (lines.isEmpty()) continue;

                // สร้าง UUID คงที่จาก path ชื่อไฟล์ (รันซ้ำจะชนตัวเดิม)
                UUID id = UUID.nameUUIDFromBytes(f.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
                if (repo.findById(id).isPresent()) continue; // มีแล้ว ข้าม

                PartOrder po = new PartOrder(id, supplier, created, status);
                po.setLines(lines);
                repo.upsert(po);
                imported++;
            } catch (Exception ex) {
                // ไฟล์เพี้ยน ข้าม แต่ไม่ล้มทั้งหน้า
                ex.printStackTrace();
            }
        }
        return imported;
    }

    private static String getAsString(JsonObject o, String k, String def) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? def : e.getAsString();
    }
    private static int getAsInt(JsonObject o, String k, int def) {
        JsonElement e = o.get(k);
        if (e == null || e.isJsonNull()) return def;
        try { return e.getAsInt(); } catch(Exception ex) { return def; }
    }
    private static BigDecimal parseBigDecimal(String s) {
        try { return new BigDecimal(s); } catch(Exception ex) { return BigDecimal.ZERO; }
    }
    private static LocalDate parseLocalDate(String s) {
        try { return s == null ? null : LocalDate.parse(s); } catch(Exception ex) { return null; }
    }
}
