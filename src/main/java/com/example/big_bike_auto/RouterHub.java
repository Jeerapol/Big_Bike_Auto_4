package com.example.big_bike_auto;

import java.util.UUID;

/**
 * RouterHub: ตัวกลาง static สำหรับให้ controller อื่น ๆ เรียกใช้งาน Router (instance ของคุณ)
 * - setRouter(..): ให้ HomeController หรือ Main จะเป็นคนตั้งค่า
 * - getRouter(): ดึง Router ที่ตั้งไว้ (ถ้าไม่ได้ตั้งไว้จะ throw เพื่อบอก configuration ผิด)
 * - helper methods: openDashboard/openRegister/openRepairDetails เพื่อให้ code อ่านง่าย
 */
public final class RouterHub {
    private static Router ROUTER; // <- ใช้ Router ของคุณ (ใน package เดียวกัน)

    private RouterHub() {}

    /** ตั้งค่า Router หนึ่งครั้งก่อนใช้งาน ทั้งแอป */
    public static void setRouter(Router router) {
        ROUTER = router;
    }

    /** คืนค่า Router ถ้ายังไม่ตั้งค่าจะ throw เพื่อชี้ปัญหา configuration */
    public static Router getRouter() {
        if (ROUTER == null) {
            throw new IllegalStateException(
                    "Router ยังไม่ได้ตั้งค่า (ต้องเรียก RouterHub.setRouter(...) จาก HomeController/Main ก่อน)");
        }
        return ROUTER;
    }

    // ====== Helper สำหรับ flow การนำทางให้โค้ดอ่านง่าย ======

    /** เปิดหน้า Dashboard */
    public static void openDashboard() {
        getRouter().navigate("dashboard");
    }

    /** เปิดหน้า ลงทะเบียน */
    public static void openRegister() {
        getRouter().navigate("register");
    }

    /** เปิดหน้า รายละเอียดงานซ่อม ตาม jobId */
    public static void openRepairDetails(UUID jobId) {
        getRouter().toRepairDetails(jobId);
    }
}
