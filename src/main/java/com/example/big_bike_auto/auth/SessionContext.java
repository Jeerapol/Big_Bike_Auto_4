package com.example.big_bike_auto.auth;

/**
 * เก็บสถานะการล็อกอินปัจจุบันแบบง่าย ๆ
 * หมายเหตุ: Production จริงควรผูกกับผู้ใช้/โทเคน ไม่ใช้ static
 */
public final class SessionContext {
    private static volatile Role currentRole = Role.STAFF; // ค่าเริ่มต้น

    private SessionContext() {}

    public static Role getCurrentRole() {
        return currentRole;
    }
    public static void setCurrentRole(Role role) {
        currentRole = (role != null) ? role : Role.STAFF;
    }

    // helper
    public static boolean isAdmin() { return currentRole == Role.ADMIN; }
    public static boolean isStaff() { return currentRole == Role.STAFF; }
    public static boolean isTechnician() { return currentRole == Role.TECHNICIAN; }
}
