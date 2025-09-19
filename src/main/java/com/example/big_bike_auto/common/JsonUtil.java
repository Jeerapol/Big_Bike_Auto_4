package com.example.big_bike_auto.common;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;


public final class JsonUtil {

    private JsonUtil() {}

    // --- กำหนดรูปแบบวันที่กลางของระบบ (ปรับได้ตามต้องการ) ---
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    // --- ลงทะเบียน TypeAdapter สำหรับ LocalDate ---
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
                @Override
                public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
                    // แปลง LocalDate -> "YYYY-MM-DD"
                    return src == null ? JsonNull.INSTANCE : new JsonPrimitive(DATE_FMT.format(src));
                }
            })
            .registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
                @Override
                public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    if (json == null || json.isJsonNull()) return null;
                    // รับทั้งรูปแบบ String ISO และตัวเลข epoch(ถ้ามีการใช้ในอนาคต, ที่นี่เราโฟกัส String)
                    try {
                        return LocalDate.parse(json.getAsString(), DATE_FMT);
                    } catch (Exception ex) {
                        // เผื่อข้อมูลเก่าๆ ที่ไม่ใช่ ISO ให้ error ชัดเจน
                        throw new JsonParseException("ไม่สามารถ parse LocalDate จาก: " + json, ex);
                    }
                }
            })
            .serializeNulls() // เก็บ null ตามจริง (แล้วแต่ policy ของโปรเจกต์)
            .setPrettyPrinting()
            .create();


    public static <T> List<T> readList(String filePath, Class<T> elementType) {
        try {
            File f = new File(filePath);
            if (!f.exists()) {
                // ไม่มีไฟล์ → คืนลิสต์ว่าง
                return List.of();
            }
            // ถ้าไฟล์มีอยู่แต่ขนาด 0 → คืนลิสต์ว่าง (กัน JsonSyntaxException)
            if (f.length() == 0) {
                return List.of();
            }

            try (Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                Type listType = TypeToken.getParameterized(List.class, elementType).getType();
                List<T> list = GSON.fromJson(r, listType);
                return list != null ? list : List.of();
            }
        } catch (Exception e) {
            // ห่อเป็น RuntimeException พร้อมข้อความชัดเจน
            throw new RuntimeException("Error reading JSON file: " + filePath, e);
        }
    }


    public static <T> void writeList(String filePath, List<T> list) {
        try {
            File f = new File(filePath);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("ไม่สามารถสร้างโฟลเดอร์: " + parent.getAbsolutePath());
            }
            try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8))) {
                GSON.toJson(list != null ? list : List.of(), w);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing JSON file: " + filePath, e);
        }
    }


    public static void ensureJsonArrayFile(String filePath) {
        try {
            File f = new File(filePath);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("ไม่สามารถสร้างโฟลเดอร์: " + parent.getAbsolutePath());
            }
            if (!f.exists()) {
                Files.writeString(f.toPath(), "[]", StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("เตรียมไฟล์ JSON ไม่สำเร็จ: " + filePath, e);
        }
    }
}
