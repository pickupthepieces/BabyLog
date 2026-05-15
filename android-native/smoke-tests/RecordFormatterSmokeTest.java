import app.babylog.nativeapp.BabyLogRecord;
import app.babylog.nativeapp.RecordFormatter;

public final class RecordFormatterSmokeTest {
    public static void main(String[] args) {
        BabyLogRecord record = new BabyLogRecord(
                "id",
                "2026-05-15",
                "28+3",
                "71",
                "",
                "",
                "",
                "1420",
                "",
                "",
                0L
        );
        String summary = RecordFormatter.ultrasoundSummary(record);
        if (!"孕周 28+3 · EFW 1420 g · BPD 71 mm".equals(summary)) {
            throw new AssertionError(summary);
        }

        BabyLogRecord empty = BabyLogRecord.empty();
        if (!"B 超记录 · 待补指标".equals(RecordFormatter.ultrasoundSummary(empty))) {
            throw new AssertionError("empty fallback failed");
        }

        BabyLogRecord corrupt = BabyLogRecord.fromJson(null);
        if (!"B 超记录 · 待补指标".equals(RecordFormatter.ultrasoundSummary(corrupt))) {
            throw new AssertionError("corrupt record fallback failed");
        }
    }
}
