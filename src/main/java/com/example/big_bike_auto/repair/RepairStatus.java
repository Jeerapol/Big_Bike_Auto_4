package com.example.big_bike_auto.repair;

/**
 * RepairStatus:
 * สถานะงานซ่อมแบบตายตัว (Enum) ใช้ร่วมกันทั้ง Register และ Repair Details
 * - การใช้ Enum ลดบั๊กจากการสะกดผิด และทำให้การค้นหา/รีวิวโค้ดง่ายขึ้น
 */
public enum RepairStatus {
    RECEIVED,       // รับรถเข้าระบบ
    DIAGNOSING,     // วินิจฉัยอาการ
    WAIT_PARTS,     // รออะไหล่
    REPAIRING,      // กำลังซ่อม
    QA,             // ตรวจคุณภาพ
    COMPLETED;      // ส่งมอบ/เสร็จสิ้น

    @Override
    public String toString() {
        // แสดงเป็นข้อความอ่านง่าย (ไทย) ใน ComboBox ได้
        return switch (this) {
            case RECEIVED -> "รับรถเข้าระบบ";
            case DIAGNOSING -> "วินิจฉัย";
            case WAIT_PARTS -> "รออะไหล่";
            case REPAIRING -> "กำลังซ่อม";
            case QA -> "ตรวจคุณภาพ";
            case COMPLETED -> "เสร็จสิ้น";
        };
    }
}
