package com.google.ar.core.examples.java.helloar;

import android.util.Log;

public final class debugLogger {

    // ===== MASTER SWITCH =====
    public static final boolean ENABLE_LOGS = true;

    // ===== TAGS =====
    public static final String FRAME = "FRAME_DEBUG";
    public static final String YOLO_RAW = "YOLO_RAW";
    public static final String YOLO_CONVERTED = "YOLO_CONVERTED";
    public static final String FILTER = "FILTER_DEBUG";
    public static final String DEPTH = "DEPTH_DEBUG";
    public static final String DEPTH_ERROR = "DEPTH_ERROR";
    public static final String NAV = "NAV_DEBUG";
    public static final String TTS = "TTS_DEBUG";
    public static final String PERF = "PERF_DEBUG";

    private debugLogger() {
        // Prevent instantiation
    }

    // ===== BASIC LOG METHODS =====

    public static void d(String tag, String message) {
        if (ENABLE_LOGS) {
            Log.d(tag, message);
        }
    }

    public static void e(String tag, String message) {
        if (ENABLE_LOGS) {
            Log.e(tag, message);
        }
    }

    public static void w(String tag, String message) {
        if (ENABLE_LOGS) {
            Log.w(tag, message);
        }
    }

    // ===== DOMAIN-SPECIFIC HELPERS =====

    public static void logDepthMedians(float left, float center, float right, String steering) {
        d(DEPTH,
                String.format(
                        "Median Depths - L: %.2f, C: %.2f, R: %.2f | Steering: %s",
                        left, center, right, steering
                )
        );
    }

    public static void logYoloDetection(String label, float conf, float x, float y, float w, float h) {
        d(YOLO_RAW,
                "class=" + label +
                        " conf=" + conf +
                        " x=" + x +
                        " y=" + y +
                        " w=" + w +
                        " h=" + h
        );
    }

    public static void logNavigationDecision(String label, float distance, String steering) {
        d(NAV,
                "Obstacle=" + label +
                        " distance=" + distance +
                        " steering=" + steering
        );
    }

    public static void logTtsSpoken(String message) {
        d(TTS, "Speaking: " + message);
    }

    public static void logTtsSuppressed(String reason) {
        d(TTS, "Speech suppressed: " + reason);
    }
}
