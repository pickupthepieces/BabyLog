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
