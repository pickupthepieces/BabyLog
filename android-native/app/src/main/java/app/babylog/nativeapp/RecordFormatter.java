package app.babylog.nativeapp;

public final class RecordFormatter {
    private RecordFormatter() {
    }

    public static String ultrasoundSummary(BabyLogRecord record) {
        StringBuilder summary = new StringBuilder();
        appendPart(summary, record.gestationalAge, "孕周 ", "");
        appendPart(summary, record.efw, "EFW ", " g");
        appendPart(summary, record.bpd, "BPD ", " mm");
        appendPart(summary, record.hc, "HC ", " mm");
        appendPart(summary, record.ac, "AC ", " mm");
        appendPart(summary, record.fl, "FL ", " mm");
        if (summary.length() == 0) {
            return "B 超记录 · 待补指标";
        }
        return summary.toString();
    }

    private static void appendPart(StringBuilder builder, String value, String prefix, String suffix) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" · ");
        }
        builder.append(prefix).append(normalized).append(suffix);
    }
}
