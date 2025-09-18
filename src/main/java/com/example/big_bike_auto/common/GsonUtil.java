package com.example.big_bike_auto.common;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * GsonUtil
 * - รวมการตั้งค่า Gson ไว้ที่เดียว (Singleton)
 * - ลงทะเบียน TypeAdapter สำหรับ java.time และ BigDecimal
 * - จุดประสงค์: เลี่ยงการใช้ reflection เข้า java.time ที่โดน module block
 */
public final class GsonUtil {

    // ใช้ format มาตรฐาน ISO-8601 เพื่อความเข้ากันได้ข้ามระบบ
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // --- TypeAdapter สำหรับ LocalDate ---
    private static final JsonSerializer<LocalDate> LOCAL_DATE_SER = (src, typeOfSrc, context) ->
            src == null ? JsonNull.INSTANCE : new JsonPrimitive(DATE_FMT.format(src));

    private static final JsonDeserializer<LocalDate> LOCAL_DATE_DESER = (json, typeOfT, context) -> {
        if (json == null || json.isJsonNull()) return null;
        String s = json.getAsString().trim();
        if (s.isEmpty()) return null;
        // parsing ตรง ๆ ด้วย ISO
        return LocalDate.parse(s, DATE_FMT);
    };

    // --- TypeAdapter สำหรับ LocalDateTime ---
    private static final JsonSerializer<LocalDateTime> LOCAL_DATETIME_SER = (src, t, c) ->
            src == null ? JsonNull.INSTANCE : new JsonPrimitive(DATETIME_FMT.format(src));

    private static final JsonDeserializer<LocalDateTime> LOCAL_DATETIME_DESER = (json, t, c) -> {
        if (json == null || json.isJsonNull()) return null;
        String s = json.getAsString().trim();
        if (s.isEmpty()) return null;
        // รองรับรูปแบบ ISO หลัก และ fallback กรณีมี milliseconds หรือ timezone ถูก strip ไป (อย่างง่าย)
        try {
            return LocalDateTime.parse(s, DATETIME_FMT);
        } catch (Exception ex) {
            // Fallback แบบปลอดภัย: ลองตัดส่วนเกินเช่น .SSS หรือ 'Z'
            String cleaned = s.replace("Z", "");
            int dot = cleaned.indexOf('.');
            if (dot > 0) cleaned = cleaned.substring(0, dot);
            return LocalDateTime.parse(cleaned, DATETIME_FMT);
        }
    };

    // --- TypeAdapter สำหรับ BigDecimal ---
    private static final JsonSerializer<BigDecimal> BIGDEC_SER = (src, t, c) ->
            src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toPlainString());
    private static final JsonDeserializer<BigDecimal> BIGDEC_DESER = (json, t, c) ->
            json == null || json.isJsonNull() ? null : new BigDecimal(json.getAsString());

    // สร้าง Gson เพียงอินสแตนซ์เดียว ใช้ร่วมกันทั้งแอป
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, LOCAL_DATE_SER)
            .registerTypeAdapter(LocalDate.class, LOCAL_DATE_DESER)
            .registerTypeAdapter(LocalDateTime.class, LOCAL_DATETIME_SER)
            .registerTypeAdapter(LocalDateTime.class, LOCAL_DATETIME_DESER)
            .registerTypeAdapter(BigDecimal.class, BIGDEC_SER)
            .registerTypeAdapter(BigDecimal.class, BIGDEC_DESER)
            .setPrettyPrinting()
            .create();

    private GsonUtil() { /* no-op */ }

    /**
     * คืน Gson ที่ตั้งค่าพร้อมใช้งาน
     */
    public static Gson gson() {
        return GSON;
    }
}
