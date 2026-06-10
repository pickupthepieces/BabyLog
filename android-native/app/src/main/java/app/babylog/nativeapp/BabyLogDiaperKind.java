package app.babylog.nativeapp;

import org.json.JSONObject;

import java.util.Locale;

final class BabyLogDiaperKind {
    static final String PEE = "pee";
    static final String POOP = "poop";
    static final String BOTH = "both";

    private BabyLogDiaperKind() {
    }

    static String fromPayload(JSONObject payload) {
        if (payload == null) {
            return "";
        }
        String kind = normalize(payload.optString("diaperKind"));
        return isBlank(kind) ? normalize(payload.optString("diaperType")) : kind;
    }

    static String normalize(String raw) {
        if (isBlank(raw)) {
            return "";
        }
        String value = raw.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (BOTH.equals(lower)
                || lower.contains("mixed")
                || lower.contains("both")
                || value.contains("混合")
                || value.contains("尿便")
                || value.contains("大小便")) {
            return BOTH;
        }
        if (PEE.equals(lower)
                || lower.contains("urine")
                || value.contains("小便")
                || value.contains("尿")) {
            return PEE;
        }
        if (POOP.equals(lower)
                || lower.contains("stool")
                || value.contains("大便")
                || value.contains("便")) {
            return POOP;
        }
        return "";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
