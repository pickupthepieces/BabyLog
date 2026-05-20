package app.babylog.nativeapp;

import java.util.Locale;

public final class BabyLogDueDateCalculator {
    public static final int DEFAULT_CYCLE_DAYS = 28;
    public static final int MIN_STANDARD_CYCLE_DAYS = 21;
    public static final int MAX_STANDARD_CYCLE_DAYS = 35;
    public static final double MIN_CRL_MM = 10.0;
    public static final double MAX_CRL_MM = 84.0;
    public static final String CLINICAL_NOTE =
            "临床上两者差异较大时通常以早 B 超为准，请以您的医生意见为最终依据。";

    private BabyLogDueDateCalculator() {
    }

    public static LmpResult fromLmp(String lmpDate, int cycleDays, String todayDate) {
        if (!BabyLogFormatters.isValidDateInput(lmpDate) || !BabyLogFormatters.isValidDateInput(todayDate)) {
            return LmpResult.invalid("请先选择有效的末次月经日期。");
        }
        int normalizedCycle = cycleDays <= 0 ? DEFAULT_CYCLE_DAYS : cycleDays;
        int dueOffsetDays = 280 + (normalizedCycle - DEFAULT_CYCLE_DAYS);
        int gaDays = Math.max(0, BabyLogFormatters.daysBetweenDateInputs(lmpDate, todayDate));
        boolean beyondTypicalRange = gaDays > 42 * 7;
        boolean nonStandardCycle = normalizedCycle < MIN_STANDARD_CYCLE_DAYS || normalizedCycle > MAX_STANDARD_CYCLE_DAYS;
        return new LmpResult(
                true,
                BabyLogFormatters.offsetDateInput(lmpDate, dueOffsetDays),
                gaDays,
                BabyLogFormatters.formatGestationalAge(gaDays),
                normalizedCycle,
                nonStandardCycle,
                beyondTypicalRange,
                ""
        );
    }

    public static CrlResult fromCrl(double crlMm, String examDate) {
        if (!BabyLogFormatters.isValidDateInput(examDate)) {
            return CrlResult.invalid("请先选择有效的检查日期。");
        }
        if (!Double.isFinite(crlMm) || crlMm < MIN_CRL_MM || crlMm > MAX_CRL_MM) {
            return CrlResult.invalid("CRL 超出早期 B 超推算窗口，本工具不做推算。");
        }
        double rawDays = 8.052d * Math.sqrt(crlMm) + 23.73d;
        int roundedDays = (int) Math.round(rawDays);
        return new CrlResult(
                true,
                BabyLogFormatters.offsetDateInput(examDate, 280 - roundedDays),
                roundedDays,
                BabyLogFormatters.formatGestationalAge(roundedDays),
                crlMm,
                rawDays,
                ""
        );
    }

    public static int diffDays(String firstDueDate, String secondDueDate) {
        if (!BabyLogFormatters.isValidDateInput(firstDueDate) || !BabyLogFormatters.isValidDateInput(secondDueDate)) {
            return 0;
        }
        return Math.abs(BabyLogFormatters.daysBetweenDateInputs(firstDueDate, secondDueDate));
    }

    public static String formatCycleWarning(boolean nonStandardCycle) {
        return nonStandardCycle ? "周期不在 21-35 天常见范围内，请谨慎使用 LMP 推算。" : "";
    }

    public static String formatRangeWarning(boolean beyondTypicalRange) {
        return beyondTypicalRange ? "当前孕周已超出常规孕期范围，请以医生确认日期为准。" : "";
    }

    public static String formatCrl(double crlMm) {
        return String.format(Locale.US, "%.1f mm", crlMm);
    }

    public static final class LmpResult {
        public final boolean valid;
        public final String estimatedDueDate;
        public final int gestationalAgeDays;
        public final String gestationalAgeLabel;
        public final int cycleDays;
        public final boolean nonStandardCycle;
        public final boolean beyondTypicalRange;
        public final String message;

        private LmpResult(
                boolean valid,
                String estimatedDueDate,
                int gestationalAgeDays,
                String gestationalAgeLabel,
                int cycleDays,
                boolean nonStandardCycle,
                boolean beyondTypicalRange,
                String message
        ) {
            this.valid = valid;
            this.estimatedDueDate = estimatedDueDate == null ? "" : estimatedDueDate;
            this.gestationalAgeDays = gestationalAgeDays;
            this.gestationalAgeLabel = gestationalAgeLabel == null ? "" : gestationalAgeLabel;
            this.cycleDays = cycleDays;
            this.nonStandardCycle = nonStandardCycle;
            this.beyondTypicalRange = beyondTypicalRange;
            this.message = message == null ? "" : message;
        }

        private static LmpResult invalid(String message) {
            return new LmpResult(false, "", 0, "", DEFAULT_CYCLE_DAYS, false, false, message);
        }
    }

    public static final class CrlResult {
        public final boolean valid;
        public final String estimatedDueDate;
        public final int gestationalAgeDays;
        public final String gestationalAgeLabel;
        public final double crlMm;
        public final double rawGestationalAgeDays;
        public final String message;

        private CrlResult(
                boolean valid,
                String estimatedDueDate,
                int gestationalAgeDays,
                String gestationalAgeLabel,
                double crlMm,
                double rawGestationalAgeDays,
                String message
        ) {
            this.valid = valid;
            this.estimatedDueDate = estimatedDueDate == null ? "" : estimatedDueDate;
            this.gestationalAgeDays = gestationalAgeDays;
            this.gestationalAgeLabel = gestationalAgeLabel == null ? "" : gestationalAgeLabel;
            this.crlMm = crlMm;
            this.rawGestationalAgeDays = rawGestationalAgeDays;
            this.message = message == null ? "" : message;
        }

        private static CrlResult invalid(String message) {
            return new CrlResult(false, "", 0, "", 0d, 0d, message);
        }
    }
}
