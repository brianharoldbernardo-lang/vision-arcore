package com.google.ar.core.examples.java.helloar;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TTSHelper {

    private TextToSpeech tts;
    private static final long COOLDOWN_MS = 3000; // 3 seconds cooldown for navigation
    private long lastAudioTime = 0;
    private String currentLanguage = "en"; // en or fil
    private Map<String, Integer> labelToIndex;
    private Map<String, String> filipinoLabels;

    // Track last spoken object and steering
    private String lastSpokenObject = "";
    private String lastSpokenSteering = "";
    private long lastSpokenTime = 0;
    private String lastObjectSpoken = "";
    private YoloDetector.Direction lastSpokenDirection = null;
    private long lastStopSpokenTime = 0;
    public TTSHelper(Context context) {
        // 1. Initialize label map
        labelToIndex = new HashMap<>();
        labelToIndex.put("Sink", 0);
        labelToIndex.put("Traffic light", 1);
        labelToIndex.put("Bicycle", 2);
        labelToIndex.put("Bus", 3);
        labelToIndex.put("Person", 4);
        labelToIndex.put("Chair", 5);
        labelToIndex.put("Couch", 6);
        labelToIndex.put("Door", 7);
        labelToIndex.put("Street light", 8);
        labelToIndex.put("Bed", 9);
        labelToIndex.put("Refrigerator", 10);
        labelToIndex.put("Motorcycle", 11);
        labelToIndex.put("Table", 12);
        labelToIndex.put("Television", 13);
        labelToIndex.put("Truck", 14);
        labelToIndex.put("Toilet", 15);
        labelToIndex.put("Bench", 16);
        labelToIndex.put("Car", 17);
        labelToIndex.put("Stairs", 18);

        filipinoLabels = new HashMap<>();
        filipinoLabels.put("Sink", "lababo");
        filipinoLabels.put("Traffic light","Ilaw Trapiko");
        filipinoLabels.put("Bicycle","Bisikleta");
        filipinoLabels.put("Bus", "Bus");
        filipinoLabels.put("Person", "Tao");
        filipinoLabels.put("Chair", "Silya");
        filipinoLabels.put("Couch", "Sopa");
        filipinoLabels.put("Door", "Pinto");
        filipinoLabels.put("Street light", "Poste ng Ilaw");
        filipinoLabels.put("Bed", "Kama");
        filipinoLabels.put("Refrigerator", "Refrigerator");
        filipinoLabels.put("Motorcycle","Motorsiklo");
        filipinoLabels.put("Table", "Mesa");
        filipinoLabels.put("Television", "Telebisyon");
        filipinoLabels.put("Truck", "Truck");
        filipinoLabels.put("Toilet", "Kubeta");
        filipinoLabels.put("Bench", "Bangko");
        filipinoLabels.put("Car", "Kotse");
        filipinoLabels.put("Stairs", "Hagdan");

        // 2. Initialize TTS
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                Log.d("TTSHelper", "TTS initialized");
            } else {
                Log.e("TTSHelper", "TTS initialization failed");
            }
        });
    }

    /** Convert label to index */
    public int getClassIndex(String label) {
        if (labelToIndex.containsKey(label)) {
            return labelToIndex.get(label);
        }
        return -1;
    }

    /** Localize label */
    private String getLocalizedLabel(String label) {
        if (currentLanguage.equals("fil") && filipinoLabels.containsKey(label)) {
            return filipinoLabels.get(label);
        }
        return label; // fallback to English
    }

    /** Speak object detection like old system:
     * "Chair detected, 1.5 meters in front of you"
     */
    public void speakDetection(String label, float distance, YoloDetector.Direction direction) {
        String localizedLabel = getLocalizedLabel(label);

        // Direction text
        String directionText = "";
        if (currentLanguage.equals("en")) {
            switch (direction) {
                case LEFT: directionText = "on your left"; break;
                case CENTER: directionText = "in front of you"; break;
                case RIGHT: directionText = "on your right"; break;
            }
        } else { // Filipino
            switch (direction) {
                case LEFT: directionText = "sa kaliwa mo"; break;
                case CENTER: directionText = "sa harap mo"; break;
                case RIGHT: directionText = "sa kanan mo"; break;
            }
        }

        String message;
        if (currentLanguage.equals("en")) {
            message = localizedLabel + " detected, " + String.format("%.1f meters away", distance) + " " + directionText;
        } else {
            message = "Mayroong " + localizedLabel + ", " + String.format("%.1f metro ang layo", distance) + " " + directionText;
        }

        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "DETECTION_CALL");
    }

    /**
     * Speak navigation message for nearest object.
     * Respects cooldown and only repeats if object changes or enough time has passed.
     * Flushes previous TTS like old system.
     */
    /** Speak a detected object with distance and suggested steering */
    /** Speak a detected object with distance and suggested steering */
    public void speakNavigation(String objectLabel, float distance, String steering, YoloDetector.Direction direction) {
        long now = System.currentTimeMillis();
        boolean isStopCommand = "stop".equals(steering);

        // 1. SMART FILTERING & COOLDOWN
        if (isStopCommand) {
            // Only repeat "STOP" every 3 seconds so it doesn't machine-gun
            if (now - lastStopSpokenTime < 3000) return;

            // We do NOT check tts.isSpeaking() here because STOP must
            // interrupt casual messages immediately for safety.
        } else {
            // Normal guidance: Wait 5 seconds AND wait for the previous sentence to finish
            if (now - lastSpokenTime < COOLDOWN_MS) return;
            if (tts != null && tts.isSpeaking()) return;
        }

        String message = "";
        String localizedLabel = getLocalizedLabel(objectLabel);

        // 2. Localize the Steering Direction (English to Filipino)
        String localizedSteering = steering;
        if (currentLanguage.equals("fil")) {
            if ("left".equals(steering)) localizedSteering = "kaliwa";
            else if ("right".equals(steering)) localizedSteering = "kanan";
            else if ("forward".equals(steering)) localizedSteering = "deretso";
        }

        // 3. Format distance based on language
        String distStr = (currentLanguage.equals("en")) ?
                String.format("%.1f meters", distance) : String.format("%.1f metro ang layo", distance);

        // --- Scenario A: FULL STOP ---
        if (isStopCommand) {
            message = (currentLanguage.equals("en")) ?
                    "Stop. Path is blocked near you at " + distStr + "." :
                    "Paki-hinto. May nakaharang malapit sa iyo, " + distStr + ".";
        }

        // --- Scenario B: OBJECT DETECTED + STEERING ---
        else if (objectLabel != null && !objectLabel.equals("Unknown")) {
            String dirText = "";
            if (currentLanguage.equals("en")) {
                switch (direction) {
                    case LEFT:   dirText = "on your left"; break;
                    case CENTER: dirText = "in front of you"; break;
                    case RIGHT:  dirText = "on your right"; break;
                }
                message = localizedLabel + ", " + distStr + " " + dirText + ". Move " + localizedSteering + ".";
            } else {
                switch (direction) {
                    case LEFT:   dirText = "sa kaliwa mo"; break;
                    case CENTER: dirText = "sa harap mo"; break;
                    case RIGHT:  dirText = "sa kanan mo"; break;
                }
                message = "May " + localizedLabel + ", " + distStr + " " + dirText + ". Lumiko sa " + localizedSteering + ".";
            }
        }

        // --- Scenario C: PATH GUIDANCE ONLY ---
        else if (!"forward".equals(steering)) {
            message = (currentLanguage.equals("en")) ?
                    "Path ahead blocked, move " + localizedSteering + "." :
                    "May nakaharang sa daan, lumiko sa " + localizedSteering + ".";
        }

        // 4. Final Execution
        if (!message.isEmpty()) {
            // QUEUE_FLUSH ensures we skip "old" detections and say the freshest info
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "NAV_CALL");

            // Update both timers
            lastSpokenTime = now;
            if (isStopCommand) {
                lastStopSpokenTime = now;
            }
        }
    }



    public void speak(String text) {
        if (tts != null) { // Changed from textToSpeech to tts
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void stop() {
        if (tts != null) { // Changed from textToSpeech to tts
            tts.stop();
        }
    }
    public String getCurrentLanguage() {
        return currentLanguage;
    }


    /** Change TTS language */
    public void setLanguage(String langCode) {
        currentLanguage = langCode;
        if (langCode.equals("en")) tts.setLanguage(Locale.US);
        else if (langCode.equals("fil")) tts.setLanguage(new Locale("fil", "PH"));
    }

    /** Shutdown TTS */
    public void shutdown() {
        if (tts != null) tts.shutdown();
    }
}
