import app.babylog.nativeapp.BabyLogSmartTextClient;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BabyLogSmartTextClientSmokeTest {
    public static void main(String[] args) throws Exception {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("primary", "检查日期");
        fields.put("secondary", "医院");
        fields.put("note", "备注");

        String response = "{"
                + "\"choices\":[{\"message\":{\"content\":\"```json\\n"
                + "{\\\"values\\\":{"
                + "\\\"primary\\\":\\\"2026-05-18\\\","
                + "\\\"secondary\\\":\\\"奉化区妇幼\\\","
                + "\\\"extra\\\":\\\"不允许字段\\\"},"
                + "\\\"warnings\\\":[\\\"日期需核对\\\"],"
                + "\\\"rawText\\\":\\\"今天在奉化区妇幼做产检\\\"}"
                + "\\n```\"}}]}";

        BabyLogSmartTextClient.SmartFillCandidate candidate =
                BabyLogSmartTextClient.parseSmartFillResponse(response, fields.keySet(), "fallback");

        assertEquals("2026-05-18", candidate.values.get("primary"));
        assertEquals("奉化区妇幼", candidate.values.get("secondary"));
        assertEquals(null, candidate.values.get("extra"));
        assertEquals("日期需核对", candidate.warnings.get(0));
        assertEquals("今天在奉化区妇幼做产检", candidate.rawText);

        String plain = "{\"message\":{\"content\":\"{\\\"values\\\":{\\\"note\\\":\\\"饭后一小时血糖 8.8\\\"}}\"}}";
        BabyLogSmartTextClient.SmartFillCandidate simple =
                BabyLogSmartTextClient.parseSmartFillResponse(plain, fields.keySet(), "fallback");
        assertEquals("饭后一小时血糖 8.8", simple.values.get("note"));

        try {
            candidate.values.put("primary", "probe");
            throw new AssertionError("values should be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected immutable candidate map.
        }

        Map<String, Map<String, String>> forms = new LinkedHashMap<>();
        Map<String, String> maternalMetricFields = new LinkedHashMap<>();
        maternalMetricFields.put("weightKg", "体重 kg");
        maternalMetricFields.put("glucoseMmolL", "血糖 mmol/L");
        maternalMetricFields.put("glucoseContext", "血糖情境");
        forms.put("maternal_metric", maternalMetricFields);
        Map<String, String> ultrasoundFields = new LinkedHashMap<>();
        ultrasoundFields.put("examDate", "检查日期");
        ultrasoundFields.put("bpdMm", "BPD mm");
        forms.put("ultrasound", ultrasoundFields);

        String entryResponse = "{"
                + "\"choices\":[{\"message\":{\"content\":\"{"
                + "\\\"eventType\\\":\\\"maternal_metric\\\","
                + "\\\"values\\\":{"
                + "\\\"weightKg\\\":\\\"61.2\\\","
                + "\\\"glucoseMmolL\\\":\\\"8.8\\\","
                + "\\\"glucoseContext\\\":\\\"after_1h\\\","
                + "\\\"bpdMm\\\":\\\"45\\\","
                + "\\\"extra\\\":\\\"drop\\\"},"
                + "\\\"warnings\\\":[\\\"血糖需核对测量时间\\\"],"
                + "\\\"rawText\\\":\\\"饭后一小时血糖8.8，体重61.2\\\"}\"}}]}";
        BabyLogSmartTextClient.SmartEntryCandidate entry =
                BabyLogSmartTextClient.parseSmartEntryResponse(entryResponse, forms, "fallback entry");
        assertEquals("maternal_metric", entry.eventType);
        assertEquals("61.2", entry.values.get("weightKg"));
        assertEquals("8.8", entry.values.get("glucoseMmolL"));
        assertEquals("after_1h", entry.values.get("glucoseContext"));
        assertEquals(null, entry.values.get("bpdMm"));
        assertEquals(null, entry.values.get("extra"));
        assertEquals("血糖需核对测量时间", entry.warnings.get(0));

        String unknownType = "{\"message\":{\"content\":\"{\\\"eventType\\\":\\\"unknown\\\",\\\"values\\\":{\\\"weightKg\\\":\\\"62\\\"}}\"}}";
        BabyLogSmartTextClient.SmartEntryCandidate unknown =
                BabyLogSmartTextClient.parseSmartEntryResponse(unknownType, forms, "fallback");
        assertEquals("", unknown.eventType);
        assertEquals(null, unknown.values.get("weightKg"));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }
}
