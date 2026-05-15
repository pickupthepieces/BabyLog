package app.babylog.nativeapp;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class BabyLogFormatters {
    private BabyLogFormatters() {
    }

    public static String nowIso() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date());
    }

    public static String todayDateInput() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
    }

    public static String createOccurredAtFromDate(String date) {
        if (!isValidDateInput(date)) {
            return nowIso();
        }
        String time = new SimpleDateFormat("HH:mm:ss.SSSZ", Locale.US).format(new Date());
        return date + "T" + time;
    }

    public static String normalizeBackendBaseUrl(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public static boolean isValidDateInput(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);
        try {
            format.parse(value);
            return true;
        } catch (ParseException ignored) {
            return false;
        }
    }

    public static Integer parseGestationalAgeDays(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replace(" ", "");
        if (!normalized.matches("\\d{1,2}(\\+[0-6])?")) {
            return null;
        }
        String[] parts = normalized.split("\\+");
        int weeks = Integer.parseInt(parts[0]);
        int days = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return weeks * 7 + days;
    }

    public static String formatGestationalAge(int days) {
        return (days / 7) + "+" + (days % 7) + " 周";
    }

    public static Double parseOptionalNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String formatNumber(double value) {
        if (Math.rint(value) == value) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    public static String formatUltrasoundSummary(JSONObject payload) {
        StringBuilder summary = new StringBuilder();
        if (payload.has("gestationalAgeDays")) {
            appendPart(summary, formatGestationalAge(payload.optInt("gestationalAgeDays")));
        }
        if (payload.has("efwGram")) {
            appendPart(summary, "EFW " + formatNumber(payload.optDouble("efwGram")) + " g");
        }
        if (payload.has("bpdMm")) {
            appendPart(summary, "BPD " + formatNumber(payload.optDouble("bpdMm")) + " mm");
        }
        return summary.length() == 0 ? "B 超手动记录 · 待补充指标" : summary.toString();
    }

    public static String formatUltrasoundSummary(Integer gestationalAgeDays, Double efwGram, Double bpdMm) {
        StringBuilder summary = new StringBuilder();
        if (gestationalAgeDays != null) {
            appendPart(summary, formatGestationalAge(gestationalAgeDays));
        }
        if (efwGram != null) {
            appendPart(summary, "EFW " + formatNumber(efwGram) + " g");
        }
        if (bpdMm != null) {
            appendPart(summary, "BPD " + formatNumber(bpdMm) + " mm");
        }
        return summary.length() == 0 ? "B 超手动记录 · 待补充指标" : summary.toString();
    }

    public static String formatUltrasoundSoftRangeWarnings(
            Double bpdMm,
            Double hcMm,
            Double acMm,
            Double flMm,
            Double efwGram
    ) {
        return formatUltrasoundSoftRangeWarnings(null, bpdMm, hcMm, acMm, flMm, efwGram);
    }

    public static String formatUltrasoundSoftRangeWarnings(
            Integer gestationalAgeDays,
            Double bpdMm,
            Double hcMm,
            Double acMm,
            Double flMm,
            Double efwGram
    ) {
        StringBuilder warnings = new StringBuilder();
        appendGestationalAgeWarning(warnings, gestationalAgeDays);
        appendRangeWarning(warnings, "BPD", bpdMm, 10, 120, "mm");
        appendRangeWarning(warnings, "HC", hcMm, 50, 400, "mm");
        appendRangeWarning(warnings, "AC", acMm, 50, 400, "mm");
        appendRangeWarning(warnings, "FL", flMm, 5, 90, "mm");
        appendRangeWarning(warnings, "EFW", efwGram, 50, 6000, "g");
        return warnings.toString();
    }

    public static boolean isOutsideSoftRange(Double value, double min, double max) {
        return value != null && (value < min || value > max);
    }

    public static String eventLabel(String eventType) {
        if ("pregnancy_checkup".equals(eventType)) return "产检";
        if ("ultrasound".equals(eventType)) return "B 超";
        if ("fetal_movement".equals(eventType)) return "胎动";
        if ("contraction".equals(eventType)) return "宫缩";
        if ("birth".equals(eventType)) return "出生";
        if ("feed".equals(eventType)) return "喂养";
        if ("sleep".equals(eventType)) return "睡眠";
        if ("diaper".equals(eventType)) return "尿布";
        if ("temperature".equals(eventType)) return "体温";
        if ("medication".equals(eventType)) return "用药";
        if ("illness".equals(eventType)) return "不适";
        if ("growth".equals(eventType)) return "成长";
        if ("vaccine".equals(eventType)) return "疫苗";
        if ("milestone".equals(eventType)) return "里程碑";
        return "备注";
    }

    public static String ocrStatusLabel(String status) {
        if ("manual-review-required".equals(status)) return "OCR 待人工确认";
        if ("queued".equals(status)) return "OCR 排队中";
        if ("recognized".equals(status)) return "OCR 已识别";
        if ("failed".equals(status)) return "OCR 失败";
        return "OCR 未启用";
    }

    public static String eventSummary(BabyLogDomain.BabyLogEvent event) {
        String summary = event.payload.optString("summary", "");
        return summary.isEmpty() ? "手动记录 · 待补充详情" : summary;
    }

    public static String formatDateTime(String iso) {
        Date date = parseIso(iso);
        if (date == null) {
            return iso == null ? "" : iso;
        }
        return new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(date);
    }

    public static long parseIsoMillis(String iso) {
        Date date = parseIso(iso);
        return date == null ? 0 : date.getTime();
    }

    public static String formatEventTime(String iso) {
        Date date = parseIso(iso);
        if (date == null) {
            return "--:--";
        }
        return new SimpleDateFormat("HH:mm", Locale.CHINA).format(date);
    }

    public static String formatEventDay(String iso) {
        Date event = parseIso(iso);
        if (event == null) {
            return "";
        }
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String eventDay = dayFormat.format(event);
        String today = dayFormat.format(new Date());
        long diff = (dateOnlyMillis(today) - dateOnlyMillis(eventDay)) / 86_400_000L;
        if (diff == 0) {
            return "今天";
        }
        if (diff == 1) {
            return "昨天";
        }
        return new SimpleDateFormat("M月d日", Locale.CHINA).format(event);
    }

    public static String formatRelativeTime(String iso) {
        Date date = parseIso(iso);
        if (date == null) {
            return "暂无";
        }
        long minutes = Math.max(0, (System.currentTimeMillis() - date.getTime()) / 60_000L);
        if (minutes < 1) {
            return "刚刚";
        }
        if (minutes < 60) {
            return minutes + "m 前";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h 前";
        }
        return formatEventDay(iso);
    }

    public static String formatByteSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.US, "%.1f KB", kb);
        }
        return String.format(Locale.US, "%.1f MB", kb / 1024.0);
    }

    public static String formatBackupAgeLabel(long lastExportMillis, long nowMillis) {
        if (lastExportMillis <= 0) {
            return "尚未导出";
        }
        int days = backupAgeDays(lastExportMillis, nowMillis);
        if (days <= 0) {
            return "今天已导出";
        }
        return "距上次导出 " + days + " 天";
    }

    public static int backupAgeDays(long lastExportMillis, long nowMillis) {
        if (lastExportMillis <= 0 || nowMillis <= lastExportMillis) {
            return 0;
        }
        return (int) ((nowMillis - lastExportMillis) / 86_400_000L);
    }

    public static int backupAgeLevel(long lastExportMillis, long nowMillis) {
        int days = backupAgeDays(lastExportMillis, nowMillis);
        if (lastExportMillis <= 0 || days < 7) {
            return 0;
        }
        if (days >= 30) {
            return 2;
        }
        return 1;
    }

    public static String timelineFilterGroup(String eventType) {
        if ("ultrasound".equals(eventType)) {
            return "ultrasound";
        }
        if ("temperature".equals(eventType)) {
            return "temperature";
        }
        if ("pregnancy_checkup".equals(eventType)) {
            return "checkup";
        }
        if ("fetal_movement".equals(eventType)
                || "contraction".equals(eventType)
                || "birth".equals(eventType)) {
            return "pregnancy";
        }
        if ("feed".equals(eventType)
                || "sleep".equals(eventType)
                || "diaper".equals(eventType)
                || "medication".equals(eventType)
                || "illness".equals(eventType)
                || "growth".equals(eventType)
                || "vaccine".equals(eventType)
                || "milestone".equals(eventType)) {
            return "baby";
        }
        return "all";
    }

    public static boolean matchesTimelineFilter(String eventType, String filter) {
        if (filter == null || filter.isEmpty() || "all".equals(filter)) {
            return true;
        }
        return filter.equals(timelineFilterGroup(eventType));
    }

    private static Date parseIso(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return null;
        }
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                if ("yyyy-MM-dd".equals(pattern)) {
                    format.setTimeZone(TimeZone.getDefault());
                }
                return format.parse(iso);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private static long dateOnlyMillis(String value) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value).getTime();
        } catch (ParseException ignored) {
            return 0;
        }
    }

    private static void appendPart(StringBuilder builder, String value) {
        if (builder.length() > 0) {
            builder.append(" · ");
        }
        builder.append(value);
    }

    private static void appendRangeWarning(
            StringBuilder builder,
            String label,
            Double value,
            double min,
            double max,
            String unit
    ) {
        if (!isOutsideSoftRange(value, min, max)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("；");
        }
        builder.append(label)
                .append(" 常用范围 ")
                .append(formatNumber(min))
                .append("-")
                .append(formatNumber(max))
                .append(" ")
                .append(unit);
    }

    private static void appendGestationalAgeWarning(StringBuilder builder, Integer days) {
        if (days == null || (days >= 70 && days <= 294)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("；");
        }
        builder.append("孕周 常用范围 10+0-42+0 周");
    }
}
