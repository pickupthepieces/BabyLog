import app.babylog.nativeapp.BabyLogSmartVisionClient;
import app.babylog.nativeapp.BabyLogSmartApi;

import java.io.File;

public final class BabyLogSmartVisionClientSmokeTest {
    public static void main(String[] args) throws Exception {
        assertEquals("https://api.example.com/v1/chat/completions",
                BabyLogSmartApi.resolveChatCompletionsUrl("https://api.example.com"));
        assertEquals("https://api.example.com/v1/chat/completions",
                BabyLogSmartApi.resolveChatCompletionsUrl("https://api.example.com/v1"));
        assertEquals("https://api.example.com/custom/chat/completions",
                BabyLogSmartApi.resolveChatCompletionsUrl("https://api.example.com/custom/chat/completions"));
        String prompt = BabyLogSmartVisionClient.ultrasoundRecognitionPrompt();
        assertTrue(prompt.contains("bpdMm"));
        assertTrue(prompt.contains("fetalHeartRateBpm"));
        assertTrue(prompt.contains("deepestPocketCm"));
        assertTrue(prompt.contains("hospital"));
        assertTrue(prompt.contains("diagnosisText"));
        assertTrue(prompt.contains("crlMm"));
        assertTrue(prompt.contains("ntMm"));
        assertFalse(prompt.contains("gestationalAge"));

        String documentPrompt = BabyLogSmartVisionClient.pregnancyDocumentRecognitionPrompt();
        assertTrue(documentPrompt.contains("eventType"));
        assertTrue(documentPrompt.contains("ultrasound"));
        assertTrue(documentPrompt.contains("pregnancy_checkup"));
        assertTrue(documentPrompt.contains("screening_ogtt"));
        assertTrue(documentPrompt.contains("bpdMm"));
        assertTrue(documentPrompt.contains("hemoglobinGL"));
        assertTrue(documentPrompt.contains("fastingGlucoseMmolL"));
        assertTrue(documentPrompt.contains("不要识别姓名"));

        String ultrasoundResponse = "{"
                + "\"choices\":[{\"message\":{\"content\":\"{"
                + "\\\"eventType\\\":\\\"ultrasound\\\","
                + "\\\"values\\\":{\\\"examDate\\\":\\\"2026-05-02\\\",\\\"bpdMm\\\":\\\"45\\\",\\\"rawText\\\":\\\"BPD 45mm\\\"},"
                + "\\\"warnings\\\":[\\\"请核对\\\"],"
                + "\\\"rawText\\\":\\\"BPD 45mm\\\"}\"}}]}";
        BabyLogSmartVisionClient.PregnancyDocumentOcrCandidate ultrasound =
                BabyLogSmartVisionClient.parsePregnancyDocumentResponse(ultrasoundResponse);
        assertEquals("ultrasound", ultrasound.eventType);
        assertEquals(45.0, ultrasound.ultrasound.bpdMm.value);
        assertEquals(null, ultrasound.checkup);

        String checkupResponse = "{"
                + "\"choices\":[{\"message\":{\"content\":\"{"
                + "\\\"eventType\\\":\\\"pregnancy_checkup\\\","
                + "\\\"values\\\":{\\\"primary\\\":\\\"2026-05-18\\\",\\\"hemoglobinGL\\\":\\\"112\\\",\\\"extra\\\":\\\"drop\\\"},"
                + "\\\"warnings\\\":[\\\"人工核对\\\"],"
                + "\\\"rawText\\\":\\\"Hb 112\\\"}\"}}]}";
        BabyLogSmartVisionClient.PregnancyDocumentOcrCandidate checkup =
                BabyLogSmartVisionClient.parsePregnancyDocumentResponse(checkupResponse);
        assertEquals("pregnancy_checkup", checkup.eventType);
        assertEquals("112", checkup.checkup.values.get("hemoglobinGL"));
        assertEquals(null, checkup.checkup.values.get("extra"));
        assertEquals(null, checkup.ultrasound);

        String ogttResponse = "{"
                + "\"choices\":[{\"message\":{\"content\":\"{"
                + "\\\"eventType\\\":\\\"screening_ogtt\\\","
                + "\\\"values\\\":{\\\"primary\\\":\\\"2026-05-18\\\",\\\"fastingGlucoseMmolL\\\":\\\"4.8\\\",\\\"extra\\\":\\\"drop\\\"},"
                + "\\\"warnings\\\":[\\\"照报告核对\\\"],"
                + "\\\"rawText\\\":\\\"OGTT 空腹 4.8\\\"}\"}}]}";
        BabyLogSmartVisionClient.PregnancyDocumentOcrCandidate ogtt =
                BabyLogSmartVisionClient.parsePregnancyDocumentResponse(ogttResponse);
        assertEquals("screening_ogtt", ogtt.eventType);
        assertEquals("4.8", ogtt.checkup.values.get("fastingGlucoseMmolL"));
        assertEquals(null, ogtt.checkup.values.get("extra"));
        assertEquals(null, ogtt.ultrasound);

        RecordingPreparer preparer = new RecordingPreparer();
        BabyLogSmartVisionClient client = new BabyLogSmartVisionClient(preparer);
        File source = new File("source-ultrasound.jpg");
        File prepared = client.prepareImageForUpload(source);
        assertEquals(source, preparer.seen);
        assertEquals(new File("prepared-ultrasound.jpg"), prepared);
        assertEquals(1, preparer.calls);

        StringBuilder longError = new StringBuilder("bad key sk-12345678901234567890 ");
        for (int i = 0; i < 700; i++) {
            longError.append('x');
        }
        String formatted = BabyLogSmartApi.formatApiErrorMessage(403, longError.toString());
        assertTrue(formatted.contains("模型 API 返回 403"));
        assertFalse(formatted.contains("sk-12345678901234567890"));
        assertTrue(formatted.length() < 620);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean actual) {
        if (!actual) {
            throw new AssertionError("expected true but got false");
        }
    }

    private static void assertFalse(boolean actual) {
        if (actual) {
            throw new AssertionError("expected false but got true");
        }
    }

    private static final class RecordingPreparer implements BabyLogSmartVisionClient.UploadImagePreparer {
        int calls;
        File seen;

        @Override
        public File prepare(File source) {
            calls++;
            seen = source;
            return new File("prepared-ultrasound.jpg");
        }
    }
}
