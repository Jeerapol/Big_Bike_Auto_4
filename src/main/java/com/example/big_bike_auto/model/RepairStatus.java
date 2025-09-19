package com.example.big_bike_auto.model;

/** สถานะงานซ่อม: code สำหรับบันทึก, labelTH สำหรับแสดงผล */
public enum RepairStatus {
    RECEIVED("RECEIVED", "รับงานแล้ว"),
    IN_PROGRESS("IN_PROGRESS", "กำลังซ่อม"),
    WAITING_PARTS("WAITING_PARTS", "รออะไหล่"),
    COMPLETED("COMPLETED", "เสร็จสิ้น"),
    CANCELLED("CANCELLED", "ยกเลิก");

    private final String code;
    private final String labelTH;

    RepairStatus(String code, String labelTH) {
        this.code = code;
        this.labelTH = labelTH;
    }
    public String code() { return code; }
    public String labelTH() { return labelTH; }

    public static RepairStatus fromCode(String code) {
        if (code == null) return RECEIVED;
        for (RepairStatus s : values()) if (s.code.equalsIgnoreCase(code)) return s;
        return RECEIVED;
    }
}
