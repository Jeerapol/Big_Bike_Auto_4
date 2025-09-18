package com.example.big_bike_auto.po;

import com.example.big_bike_auto.common.GsonUtil;
import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

/**
 * บันทึก PO Draft ลงไฟล์ JSON ที่ data/po_drafts/
 * - ตั้งชื่อไฟล์: {supplier}_{yyyyMMdd}_{uuid}.json
 * - ใช้ GsonUtil (รองรับ LocalDate)
 */
public class DraftPoRepository {

    private final File baseDir = new File("data/po_drafts");
    private final Gson gson = GsonUtil.gson();

    public DraftPoRepository() {
        if (!baseDir.exists()) baseDir.mkdirs();
    }

    public File save(PurchaseOrderDraft draft) {
        String date = LocalDate.now().toString().replace("-", "");
        String fileName = sanitize(draft.getSupplier()) + "_" + date + "_" + UUID.randomUUID() + ".json";
        File f = new File(baseDir, fileName);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            gson.toJson(draft, w);
        } catch (IOException e) {
            throw new RuntimeException("Cannot save PO draft: " + f.getAbsolutePath(), e);
        }
        return f;
    }

    private static String sanitize(String s) {
        return s == null ? "UNKNOWN" : s.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
