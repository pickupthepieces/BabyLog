package app.babylog.nativeapp;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class BabyLogFormatters {
    // 这个 App 固定中国一家人使用：所有日期/时间换算钉死在 Asia/Shanghai，
    // 不跟随设备或 CI runner 的默认时区，避免归错天 / 跨午夜边界算错（L-9）。
    private static final TimeZone CN_ZONE = TimeZone.getTimeZone("Asia/Shanghai");

    private BabyLogFormatters() {
    }

    private static SimpleDateFormat cnFormat(String pattern, Locale locale) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, locale);
        format.setTimeZone(CN_ZONE);
        return format;
    }

    public static String nowIso() {
        return cnFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date());
    }

    public static String todayDateInput() {
        return cnFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
    }

    public static String offsetDateInput(String date, int dayDelta) {
        if (!isValidDateInput(date)) {
            return date == null ? "" : date;
        }
        try {
            SimpleDateFormat format = cnFormat("yyyy-MM-dd", Locale.US);
            format.setLenient(false);
            Date parsed = format.parse(date);
            if (parsed == null) {
                return date;
            }
            Calendar calendar = Calendar.getInstance(CN_ZONE);
            calendar.setTime(parsed);
            calendar.add(Calendar.DATE, dayDelta);
            return format.format(calendar.getTime());
        } catch (ParseException ignored) {
            return date;
        }
    }

    public static int daysBetweenDateInputs(String fromDate, String toDate) {
        if (!isValidDateInput(fromDate) || !isValidDateInput(toDate)) {
            return 0;
        }
        try {
            SimpleDateFormat format = cnFormat("yyyy-MM-dd", Locale.US);
            Date from = format.parse(fromDate);
            Date to = format.parse(toDate);
            if (from == null || to == null) {
                return 0;
            }
            return (int) ((to.getTime() - from.getTime()) / 86_400_000L);
        } catch (ParseException ignored) {
            return 0;
        }
    }

    public static String createOccurredAtFromDate(String date) {
        if (!isValidDateInput(date)) {
            return nowIso();
        }
        return date + "T12:00:00.000+0800";
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
        SimpleDateFormat format = cnFormat("yyyy-MM-dd", Locale.US);
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
        String normalized = value.trim()
                .replace(" ", "")
                .replace("＋", "+")
                .replace("周", "+")
                .replace("週", "+")
                .replace("W", "+")
                .replace("w", "+");
        if (normalized.endsWith("+")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
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
            Double parsed = Double.valueOf(value.trim());
            return parsed.isNaN() || parsed.isInfinite() ? null : parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String resolveCareStage(BabyLogDomain.ChildProfile profile, String todayDate) {
        if (profile == null || !profile.setupCompleted) {
            return BabyLogDomain.STAGE_UNKNOWN;
        }
        if (BabyLogDomain.isExplicitStageOverride(profile.stageOverride)) {
            return profile.stageOverride;
        }
        String today = isValidDateInput(todayDate) ? todayDate : todayDateInput();
        if (isValidDateInput(profile.birthDate) && today.compareTo(profile.birthDate) >= 0) {
            return BabyLogDomain.STAGE_BABY;
        }
        if (isValidDateInput(profile.expectedDueDate)) {
            return BabyLogDomain.STAGE_PREGNANCY;
        }
        return BabyLogDomain.STAGE_UNKNOWN;
    }

    public static boolean shouldMutePregnancyDerivedUi(String stage) {
        return BabyLogDomain.STAGE_PREGNANCY_ENDED.equals(stage)
                || BabyLogDomain.STAGE_PAUSED.equals(stage);
    }

    public static String recordDay(String iso) {
        return recordDay(iso, 0);
    }

    public static String recordDay(String iso, int boundaryHour) {
        Date date = parseIso(iso);
        if (date == null) {
            return "";
        }
        int boundedHour = Math.max(0, Math.min(23, boundaryHour));
        long shiftedMillis = date.getTime() - boundedHour * 3_600_000L;
        return cnFormat("yyyy-MM-dd", Locale.US).format(new Date(shiftedMillis));
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

    public static String formatMaternalGlucoseWarning(Double glucoseMmolL, String context) {
        if (glucoseMmolL == null) {
            return "";
        }
        String normalized = context == null ? "" : context.trim();
        double threshold;
        String label;
        if ("fasting".equals(normalized)) {
            threshold = 5.1;
            label = "空腹";
        } else if ("after_1h".equals(normalized)) {
            threshold = 10.0;
            label = "餐后1h";
        } else if ("after_2h".equals(normalized)) {
            threshold = 8.5;
            label = "餐后2h";
        } else {
            return "";
        }
        if (glucoseMmolL <= threshold) {
            return "";
        }
        return label + "血糖高于 " + String.format(Locale.US, "%.1f", threshold) + " mmol/L；非诊断，仅提示，请遵医嘱";
    }

    public static String maternalGlucoseContextLabel(String context) {
        if ("fasting".equals(context)) return "空腹";
        if ("after_1h".equals(context)) return "餐后1h";
        if ("after_2h".equals(context)) return "餐后2h";
        if ("random".equals(context)) return "随机";
        return context == null || context.trim().isEmpty() ? "" : context.trim();
    }

    public static boolean isOutsideSoftRange(Double value, double min, double max) {
        return value != null && (value < min || value > max);
    }

    public static String eventLabel(String eventType) {
        if ("pregnancy_checkup".equals(eventType)) return "产检";
        if ("screening_nt".equals(eventType)) return "NT";
        if ("screening_serum".equals(eventType)) return "唐筛";
        if ("screening_nipt".equals(eventType)) return "无创 DNA";
        if ("screening_anomaly".equals(eventType)) return "大排畸";
        if ("screening_ogtt".equals(eventType)) return "糖耐 OGTT";
        if ("screening_gbs".equals(eventType)) return "GBS";
        if ("screening_nst".equals(eventType)) return "胎心监护";
        if ("ultrasound".equals(eventType)) return "B 超";
        if ("fetal_movement".equals(eventType)) return "胎动";
        if ("contraction".equals(eventType)) return "宫缩";
        if ("maternal_metric".equals(eventType)) return "孕妈指标";
        if ("birth".equals(eventType)) return "出生";
        if ("feed".equals(eventType)) return "喂养";
        if ("breastfeed".equals(eventType)) return "母乳";
        if ("bottle".equals(eventType)) return "奶瓶";
        if ("sleep".equals(eventType)) return "睡眠";
        if ("wake".equals(eventType)) return "起床";
        if ("diaper".equals(eventType)) return "尿布";
        if ("pee".equals(eventType)) return "尿尿";
        if ("poop".equals(eventType)) return "便便";
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
        JSONObject payload = event.payload;
        if ("feed".equals(event.eventType)) {
            return babyCareSummary(
                    event.eventType,
                    localizeFeedType(payload.optString("feedType", "")),
                    payload.has("amountMl") ? formatNumber(payload.optDouble("amountMl")) + " ml" : ""
            );
        }
        if ("temperature".equals(event.eventType)) {
            return babyCareSummary(
                    event.eventType,
                    payload.has("temperatureC") ? formatNumber(payload.optDouble("temperatureC")) + " ℃" : "",
                    localizeMeasureMethod(payload.optString("measureMethod", ""))
            );
        }
        if ("medication".equals(event.eventType)) {
            return babyCareSummary(
                    event.eventType,
                    payload.optString("medicationName", ""),
                    payload.optString("dosage", ""),
                    payload.optString("reason", "")
            );
        }
        String summary = payload.optString("summary", "");
        return summary.isEmpty() ? "手动记录 · 待补充详情" : localizeStoredSummary(summary);
    }

    public static String formatDateTime(String iso) {
        Date date = parseIso(iso);
        if (date == null) {
            return iso == null ? "" : iso;
        }
        return cnFormat("M月d日 HH:mm", Locale.CHINA).format(date);
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
        return cnFormat("HH:mm", Locale.CHINA).format(date);
    }

    public static String formatEventDay(String iso) {
        Date event = parseIso(iso);
        if (event == null) {
            return "";
        }
        SimpleDateFormat dayFormat = cnFormat("yyyy-MM-dd", Locale.US);
        String eventDay = dayFormat.format(event);
        String today = dayFormat.format(new Date());
        long diff = (dateOnlyMillis(today) - dateOnlyMillis(eventDay)) / 86_400_000L;
        if (diff == 0) {
            return "今天";
        }
        if (diff == 1) {
            return "昨天";
        }
        return cnFormat("M月d日", Locale.CHINA).format(event);
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

    public static String relativeTimeFromNow(String iso) {
        return relativeTimeFromNow(iso, System.currentTimeMillis());
    }

    public static String relativeTimeFromNow(String iso, long nowMillis) {
        Date date = parseIso(iso);
        if (date == null) {
            return iso == null ? "" : iso;
        }
        long seconds = Math.max(0L, (nowMillis - date.getTime()) / 1000L);
        if (seconds <= 5L) {
            return "刚刚";
        }
        if (seconds < 60L) {
            return seconds + " 秒前";
        }
        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return minutes + " 分钟前";
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + " 小时前";
        }
        long days = hours / 24L;
        if (days < 7L) {
            return days + " 天前";
        }
        return iso.length() >= 10 ? iso.substring(0, 10) : iso;
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
        if ("pregnancy_checkup".equals(eventType) || isScreeningEventType(eventType)) {
            return "checkup";
        }
        if ("fetal_movement".equals(eventType)
                || "contraction".equals(eventType)
                || "maternal_metric".equals(eventType)) {
            return "pregnancy";
        }
        if ("birth".equals(eventType)
                || "feed".equals(eventType)
                || "breastfeed".equals(eventType)
                || "bottle".equals(eventType)
                || "sleep".equals(eventType)
                || "wake".equals(eventType)
                || "diaper".equals(eventType)
                || "pee".equals(eventType)
                || "poop".equals(eventType)
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
        String group = timelineFilterGroup(eventType);
        if ("pregnancy".equals(filter)) {
            return "pregnancy".equals(group) || "ultrasound".equals(group) || "checkup".equals(group);
        }
        if ("baby".equals(filter)) {
            return "baby".equals(group) || "temperature".equals(group);
        }
        return filter.equals(group);
    }

    public static boolean isScreeningEventType(String eventType) {
        return eventType != null && eventType.startsWith("screening_");
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
                SimpleDateFormat format = cnFormat(pattern, Locale.US);
                return format.parse(iso);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private static long dateOnlyMillis(String value) {
        try {
            return cnFormat("yyyy-MM-dd", Locale.US).parse(value).getTime();
        } catch (ParseException ignored) {
            return 0;
        }
    }

    private static String babyCareSummary(String eventType, String... parts) {
        StringBuilder summary = new StringBuilder(eventLabel(eventType));
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                summary.append(" · ").append(part.trim());
            }
        }
        if (summary.toString().equals(eventLabel(eventType))) {
            summary.append(" · 待补充详情");
        }
        return summary.toString();
    }

    private static String localizeFeedType(String value) {
        if ("bottle".equalsIgnoreCase(value)) return "奶瓶";
        if ("breast".equalsIgnoreCase(value)) return "母乳";
        if ("food".equalsIgnoreCase(value) || "solid".equalsIgnoreCase(value)) return "辅食";
        return value;
    }

    private static String localizeMeasureMethod(String value) {
        if ("axillary".equalsIgnoreCase(value)) return "腋温";
        if ("oral".equalsIgnoreCase(value)) return "口温";
        if ("ear".equalsIgnoreCase(value) || "tympanic".equalsIgnoreCase(value)) return "耳温";
        if ("forehead".equalsIgnoreCase(value)) return "额温";
        if ("rectal".equalsIgnoreCase(value)) return "肛温";
        return value;
    }

    private static String localizeStoredSummary(String summary) {
        return summary
                .replace(" · bottle", " · 奶瓶")
                .replace(" · breast", " · 母乳")
                .replace(" · food", " · 辅食")
                .replace(" · solid", " · 辅食")
                .replace(" · axillary", " · 腋温")
                .replace(" · oral", " · 口温")
                .replace(" · ear", " · 耳温")
                .replace(" · tympanic", " · 耳温")
                .replace(" · forehead", " · 额温")
                .replace(" · rectal", " · 肛温");
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
