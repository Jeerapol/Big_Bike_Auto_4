package com.example.big_bike_auto.auth;

/** บทบาทผู้ใช้ (Role-Based Access Control) */
public enum Role {
    ADMIN,        // เห็น/ทำได้ทุกอย่าง
    STAFF,        // แก้ไขข้อมูลลงทะเบียน
    TECHNICIAN;   // แก้ไขรายละเอียดซ่อม

    /** แปลงข้อความจาก UI เป็น Role อย่างปลอดภัย */
    public static Role fromString(String s) {
        if (s == null) return STAFF;
        switch (s.trim().toUpperCase()) {
            case "ADMIN": return ADMIN;
            case "TECHNICIAN": return TECHNICIAN;
            default: return STAFF;
        }
    }
}
