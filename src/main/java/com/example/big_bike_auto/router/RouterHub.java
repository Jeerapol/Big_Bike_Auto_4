package com.example.big_bike_auto.router;

public final class RouterHub {
    private static Router ROUTER;

    private RouterHub() {}

    public static void setRouter(Router router) { ROUTER = router; }

    public static Router getRouter() {
        if (ROUTER == null) throw new IllegalStateException("Router ยังไม่ได้ set!");
        return ROUTER;
    }

    public static boolean isReady() { return ROUTER != null; }
}
