import app.babylog.nativeapp.BabyLogFormatters;

public final class BabyLogFormattersSmokeTest {
    public static void main(String[] args) {
        assertEquals(199, BabyLogFormatters.parseGestationalAgeDays("28+3"));
        assertEquals(196, BabyLogFormatters.parseGestationalAgeDays("28"));
        assertEquals(null, BabyLogFormatters.parseGestationalAgeDays("28+x"));
        assertEquals("28+3 周", BabyLogFormatters.formatGestationalAge(199));
        assertEquals(
                "28+3 周 · EFW 1420 g · BPD 71 mm",
                BabyLogFormatters.formatUltrasoundSummary(199, 1420.0, 71.0)
        );
        assertEquals(
                "B 超手动记录 · 待补充指标",
                BabyLogFormatters.formatUltrasoundSummary(null, null, null)
        );
        assertEquals("https://example.invalid/api", BabyLogFormatters.normalizeBackendBaseUrl(" https://example.invalid/api/ "));
        assertEquals(
                "",
                BabyLogFormatters.formatUltrasoundSoftRangeWarnings(71.0, 260.0, 240.0, 55.0, 1420.0)
        );
        assertEquals(
                "BPD 常用范围 10-120 mm；EFW 常用范围 50-6000 g",
                BabyLogFormatters.formatUltrasoundSoftRangeWarnings(121.0, null, null, null, 6200.0)
        );
        assertEquals(
                "孕周 常用范围 10+0-42+0 周",
                BabyLogFormatters.formatUltrasoundSoftRangeWarnings(60, null, null, null, null, null)
        );
        long now = 1_700_000_000_000L;
        assertEquals("尚未导出", BabyLogFormatters.formatBackupAgeLabel(0L, now));
        assertEquals("今天已导出", BabyLogFormatters.formatBackupAgeLabel(now - 60_000L, now));
        assertEquals("距上次导出 7 天", BabyLogFormatters.formatBackupAgeLabel(now - 7L * 86_400_000L, now));
        assertEquals(0, BabyLogFormatters.backupAgeLevel(now - 6L * 86_400_000L, now));
        assertEquals(1, BabyLogFormatters.backupAgeLevel(now - 7L * 86_400_000L, now));
        assertEquals(2, BabyLogFormatters.backupAgeLevel(now - 30L * 86_400_000L, now));
        assertEquals("baby", BabyLogFormatters.timelineFilterGroup("feed"));
        assertEquals("pregnancy", BabyLogFormatters.timelineFilterGroup("fetal_movement"));
        assertEquals("ultrasound", BabyLogFormatters.timelineFilterGroup("ultrasound"));
        assertEquals("temperature", BabyLogFormatters.timelineFilterGroup("temperature"));
        assertEquals("checkup", BabyLogFormatters.timelineFilterGroup("pregnancy_checkup"));
        if (!BabyLogFormatters.matchesTimelineFilter("feed", "baby")) {
            throw new AssertionError("feed should match baby filter");
        }
        if (BabyLogFormatters.matchesTimelineFilter("feed", "pregnancy")) {
            throw new AssertionError("feed should not match pregnancy filter");
        }
        if (!BabyLogFormatters.isValidDateInput("2026-05-15")) {
            throw new AssertionError("valid date rejected");
        }
        if (BabyLogFormatters.isValidDateInput("2026-99-99")) {
            throw new AssertionError("invalid date accepted");
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }
}
