package app.babylog.nativeapp;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class BabyLogBabyCareRecords {
    private BabyLogBabyCareRecords() {
    }

    static JSONObject buildPayload(BabyLogService.BabyCareInput input) throws JSONException {
        JSONObject payload = new JSONObject();
        if ("feed".equals(input.eventType)) {
            putStringIfNotBlank(payload, "feedType", input.primary);
            putNumberIfNotNull(payload, "amountMl", BabyLogFormatters.parseOptionalNumber(input.secondary));
            if (isBreastMilkFeed(input.primary)) {
                putStringIfNotBlank(payload, "breastSide", normalizeBreastSide(input.tertiary));
            } else if (isSolidFoodFeed(input.primary)) {
                putStringIfNotBlank(payload, "solidFood", input.tertiary);
            } else if (!isBlank(normalizeBreastSide(input.tertiary))) {
                putStringIfNotBlank(payload, "breastSide", normalizeBreastSide(input.tertiary));
            } else {
                putStringIfNotBlank(payload, "solidFood", input.tertiary);
            }
            putStringIfNotBlank(payload, "note", input.note);
        } else if ("sleep".equals(input.eventType)) {
            putStringIfNotBlank(payload, "sleepStart", input.primary);
            putStringIfNotBlank(payload, "sleepEnd", input.secondary);
            putStringIfNotBlank(payload, "sleepPlace", input.tertiary);
            putStringIfNotBlank(payload, "note", input.note);
        } else if ("diaper".equals(input.eventType)) {
            putStringIfNotBlank(payload, "diaperType", input.primary);
            putStringIfNotBlank(payload, "diaperKind", BabyLogDiaperKind.normalize(input.primary));
            putStringIfNotBlank(payload, "diaperDetail", input.secondary);
            putStringIfNotBlank(payload, "diaperObservation", input.tertiary);
            putStringIfNotBlank(payload, "color", normalizeDiaperColor(input.tertiary));
            putStringIfNotBlank(payload, "consistency", normalizeDiaperConsistency(input.tertiary));
            putStringIfNotBlank(payload, "note", input.note);
        } else if ("breastfeed".equals(input.eventType)) {
            Double leftMinutes = BabyLogFormatters.parseOptionalNumber(input.primary);
            Double rightMinutes = BabyLogFormatters.parseOptionalNumber(input.secondary);
            putNumberIfNotNull(payload, "leftMinutes", leftMinutes);
            putNumberIfNotNull(payload, "rightMinutes", rightMinutes);
            if (leftMinutes == null && rightMinutes == null) {
                putStringIfNotBlank(payload, "detail", input.primary);
            }
            putStringIfNotBlank(payload, "note", input.note);
        } else if ("bottle".equals(input.eventType)) {
            Double amountMl = BabyLogFormatters.parseOptionalNumber(input.primary);
            putNumberIfNotNull(payload, "amountMl", amountMl);
            if (amountMl == null) {
                putStringIfNotBlank(payload, "detail", input.primary);
            }
            putStringIfNotBlank(payload, "brand", input.secondary);
            putStringIfNotBlank(payload, "note", input.note);
        } else if ("temperature".equals(input.eventType)) {
            Double temperature = BabyLogFormatters.parseOptionalNumber(input.primary);
            putNumberIfNotNull(payload, "temperatureC", temperature);
            putStringIfNotBlank(payload, "measureMethod", input.secondary);
            putStringIfNotBlank(payload, "note", input.note);
        } else if ("medication".equals(input.eventType)) {
            putStringIfNotBlank(payload, "medicationName", input.primary);
            putStringIfNotBlank(payload, "dosage", input.secondary);
            putStringIfNotBlank(payload, "reason", input.tertiary);
            putStringIfNotBlank(payload, "note", input.note);
        } else if ("growth".equals(input.eventType) || "child_checkup".equals(input.eventType)) {
            putNumberIfNotNull(payload, "weightKg", BabyLogFormatters.parseOptionalNumber(input.primary));
            putNumberIfNotNull(payload, "heightCm", BabyLogFormatters.parseOptionalNumber(input.secondary));
            putNumberIfNotNull(payload, "headCircumferenceCm", BabyLogFormatters.parseOptionalNumber(input.tertiary));
            if ("child_checkup".equals(input.eventType)) {
                putStringIfNotBlank(payload, "checkupInstitution", input.checkupInstitution);
                putStringIfNotBlank(payload, "checkupConclusion", input.checkupConclusion);
                if (BabyLogFormatters.isValidDateInput(input.nextCheckupDate)) {
                    putStringIfNotBlank(payload, "nextCheckupDate", input.nextCheckupDate);
                }
            }
            putStringIfNotBlank(payload, "note", input.note);
        } else {
            putStringIfNotBlank(payload, "detail", input.primary);
            putStringIfNotBlank(payload, "note", input.secondary);
        }
        return payload;
    }

    static String formatSummary(BabyLogService.BabyCareInput input) {
        StringBuilder summary = new StringBuilder(BabyLogFormatters.eventLabel(input.eventType));
        if ("feed".equals(input.eventType)) {
            appendSummary(summary, input.primary);
            Double amount = BabyLogFormatters.parseOptionalNumber(input.secondary);
            if (amount != null) {
                appendSummary(summary, BabyLogFormatters.formatNumber(amount) + " ml");
            }
            appendSummary(summary, input.tertiary);
        } else if ("sleep".equals(input.eventType)) {
            if (!isBlank(input.primary) && !isBlank(input.secondary)) {
                appendSummary(summary, input.primary.trim() + "-" + input.secondary.trim());
            }
            appendSummary(summary, input.tertiary);
        } else if ("diaper".equals(input.eventType)) {
            appendSummary(summary, input.primary);
            appendSummary(summary, input.secondary);
            appendSummary(summary, input.tertiary);
        } else if ("breastfeed".equals(input.eventType)) {
            Double left = BabyLogFormatters.parseOptionalNumber(input.primary);
            Double right = BabyLogFormatters.parseOptionalNumber(input.secondary);
            if (left != null) {
                appendSummary(summary, "左 " + BabyLogFormatters.formatNumber(left) + " 分钟");
            }
            if (right != null) {
                appendSummary(summary, "右 " + BabyLogFormatters.formatNumber(right) + " 分钟");
            }
            appendSummary(summary, input.note);
        } else if ("bottle".equals(input.eventType)) {
            Double amount = BabyLogFormatters.parseOptionalNumber(input.primary);
            appendSummary(summary, amount == null ? input.primary : BabyLogFormatters.formatNumber(amount) + " ml");
            appendSummary(summary, input.secondary);
            appendSummary(summary, input.note);
        } else if ("temperature".equals(input.eventType)) {
            Double temperature = BabyLogFormatters.parseOptionalNumber(input.primary);
            if (temperature != null) {
                appendSummary(summary, BabyLogFormatters.formatNumber(temperature) + " ℃");
            }
            appendSummary(summary, input.secondary);
        } else if ("medication".equals(input.eventType)) {
            appendSummary(summary, input.primary);
            appendSummary(summary, input.secondary);
            appendSummary(summary, input.tertiary);
        } else if ("growth".equals(input.eventType) || "child_checkup".equals(input.eventType)) {
            appendGrowthSummary(summary, BabyLogFormatters.parseOptionalNumber(input.primary), "体重", "kg");
            appendGrowthSummary(summary, BabyLogFormatters.parseOptionalNumber(input.secondary), "身长", "cm");
            appendGrowthSummary(summary, BabyLogFormatters.parseOptionalNumber(input.tertiary), "头围", "cm");
            if ("child_checkup".equals(input.eventType)) {
                appendSummary(summary, input.checkupInstitution);
                appendSummary(summary, input.checkupConclusion);
                if (!isBlank(input.nextCheckupDate)) {
                    appendSummary(summary, "下次 " + input.nextCheckupDate);
                }
            }
            appendSummary(summary, input.note);
        } else {
            appendSummary(summary, input.primary);
            appendSummary(summary, input.secondary);
            if (summary.toString().equals(BabyLogFormatters.eventLabel(input.eventType))) {
                return summary.toString();
            }
        }
        if (summary.toString().equals(BabyLogFormatters.eventLabel(input.eventType))) {
            summary.append(" · 待补充详情");
        }
        return summary.toString();
    }

    static Map<String, String> draftFields(String eventType, JSONObject payload) {
        Map<String, String> values = new LinkedHashMap<>();
        if (payload == null) {
            return values;
        }
        if ("feed".equals(eventType)) {
            putDraftField(values, "primary", payload.optString("feedType"));
            putDraftField(values, "secondary", payloadNumberText(payload, "amountMl"));
            putDraftField(values, "tertiary", payload.optString("breastSide", payload.optString("solidFood")));
            putDraftField(values, "note", payload.optString("note"));
        } else if ("breastfeed".equals(eventType)) {
            putDraftField(values, "primary", payloadNumberText(payload, "leftMinutes"));
            putDraftField(values, "secondary", payloadNumberText(payload, "rightMinutes"));
            String legacyDetail = payload.optString("detail");
            putDraftField(values, "tertiary", payload.optString("note").isEmpty() ? legacyDetail : payload.optString("note"));
        } else if ("bottle".equals(eventType)) {
            putDraftField(values, "primary", payloadNumberText(payload, "amountMl"));
            putDraftField(values, "secondary", payload.optString("brand", payload.optString("detail")));
            putDraftField(values, "tertiary", payload.optString("note"));
        } else if ("sleep".equals(eventType)) {
            putDraftField(values, "primary", payload.optString("sleepStart"));
            putDraftField(values, "secondary", payload.optString("sleepEnd"));
            putDraftField(values, "tertiary", payload.optString("sleepPlace"));
            putDraftField(values, "note", payload.optString("note"));
        } else if ("diaper".equals(eventType)) {
            putDraftField(values, "primary", payload.optString("diaperType"));
            putDraftField(values, "secondary", payload.optString("diaperDetail"));
            putDraftField(values, "tertiary", payload.optString("diaperObservation",
                    joinNonBlank(payload.optString("color"), payload.optString("consistency"))));
            putDraftField(values, "note", payload.optString("note"));
        } else if ("temperature".equals(eventType)) {
            putDraftField(values, "primary", payloadNumberText(payload, "temperatureC"));
            putDraftField(values, "secondary", payload.optString("measureMethod"));
            putDraftField(values, "note", payload.optString("note"));
        } else if ("medication".equals(eventType)) {
            putDraftField(values, "primary", payload.optString("medicationName"));
            putDraftField(values, "secondary", payload.optString("dosage"));
            putDraftField(values, "tertiary", payload.optString("reason"));
        } else if ("growth".equals(eventType) || "child_checkup".equals(eventType)) {
            putDraftField(values, "primary", payloadNumberText(payload, "weightKg"));
            putDraftField(values, "secondary", payloadNumberText(payload, "heightCm"));
            putDraftField(values, "tertiary", payloadNumberText(payload, "headCircumferenceCm"));
            if ("child_checkup".equals(eventType)) {
                putDraftField(values, "checkupInstitution", payload.optString("checkupInstitution"));
                putDraftField(values, "checkupConclusion", payload.optString("checkupConclusion"));
                putDraftField(values, "nextCheckupDate", payload.optString("nextCheckupDate"));
            }
            putDraftField(values, "note", payload.optString("note"));
        } else {
            putDraftField(values, "primary", payload.optString("detail"));
            putDraftField(values, "secondary", payload.optString("note"));
        }
        return values;
    }

    static boolean hasMinimumContent(BabyLogService.BabyCareInput input) {
        if (input == null) {
            return false;
        }
        return !isBlank(input.primary)
                || !isBlank(input.secondary)
                || !isBlank(input.tertiary)
                || !isBlank(input.note)
                || !isBlank(input.checkupInstitution)
                || !isBlank(input.checkupConclusion)
                || !isBlank(input.nextCheckupDate);
    }

    private static void putStringIfNotBlank(JSONObject payload, String key, String value) throws JSONException {
        if (!isBlank(value)) {
            payload.put(key, value.trim());
        }
    }

    private static void putNumberIfNotNull(JSONObject payload, String key, Double value) throws JSONException {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private static void appendSummary(StringBuilder summary, String value) {
        if (isBlank(value)) {
            return;
        }
        if (summary.length() > 0) {
            summary.append(" · ");
        }
        summary.append(value.trim());
    }

    private static void appendGrowthSummary(StringBuilder summary, Double value, String label, String unit) {
        if (value != null) {
            appendSummary(summary, label + " " + BabyLogFormatters.formatNumber(value) + " " + unit);
        }
    }

    private static void putDraftField(Map<String, String> values, String key, String value) {
        if (!isBlank(value)) {
            values.put(key, value.trim());
        }
    }

    private static String payloadNumberText(JSONObject payload, String key) {
        if (payload == null || !payload.has(key)) {
            return "";
        }
        double value = payload.optDouble(key, Double.NaN);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "";
        }
        return BabyLogFormatters.formatNumber(value);
    }

    private static boolean isBreastMilkFeed(String feedType) {
        return !isBlank(feedType) && feedType.contains("母乳");
    }

    private static boolean isSolidFoodFeed(String feedType) {
        return !isBlank(feedType) && feedType.contains("辅");
    }

    private static String normalizeBreastSide(String raw) {
        if (isBlank(raw)) {
            return "";
        }
        String trimmed = raw.trim();
        String value = trimmed.toUpperCase(Locale.ROOT);
        if ("左".equals(trimmed) || "LEFT".equals(value) || "L".equals(value)) {
            return "L";
        }
        if ("右".equals(trimmed) || "RIGHT".equals(value) || "R".equals(value)) {
            return "R";
        }
        if ("双侧".equals(trimmed) || "两侧".equals(trimmed) || "BOTH".equals(value)) {
            return "BOTH";
        }
        return trimmed;
    }

    private static String normalizeDiaperColor(String raw) {
        if (isBlank(raw)) {
            return "";
        }
        String normalized = raw.trim();
        if (normalized.startsWith("黄")) return "黄";
        if (normalized.startsWith("绿")) return "绿";
        if (normalized.startsWith("黑")) return "黑";
        if (normalized.startsWith("红")) return "红";
        if (normalized.startsWith("白")) return "白";
        if (normalized.startsWith("其它") || normalized.startsWith("其他")) return "其它";
        return "";
    }

    private static String normalizeDiaperConsistency(String raw) {
        if (isBlank(raw)) {
            return "";
        }
        String normalized = raw.trim();
        if (normalized.contains("水样")) return "水样";
        if (normalized.contains("稀")) return "稀";
        if (normalized.contains("软")) return "软便";
        if (normalized.contains("成型")) return "成型";
        return "";
    }

    private static String joinNonBlank(String first, String second) {
        boolean hasFirst = !isBlank(first);
        boolean hasSecond = !isBlank(second);
        if (hasFirst && hasSecond) {
            return first.trim() + " / " + second.trim();
        }
        if (hasFirst) {
            return first.trim();
        }
        if (hasSecond) {
            return second.trim();
        }
        return "";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
