import app.babylog.nativeapp.BabyLogSmartInput;

public final class BabyLogSmartInputSmokeTest {
    public static void main(String[] args) throws Exception {
        String response = "{"
                + "\"choices\":[{\"message\":{\"content\":\"```json\\n"
                + "{\\\"examDate\\\":\\\"2026-05-18\\\","
                + "\\\"gestationalAge\\\":\\\"28+3\\\","
                + "\\\"bpdMm\\\":\\\"71\\\","
                + "\\\"hcMm\\\":\\\"260.5\\\","
                + "\\\"acMm\\\":\\\"bad\\\","
                + "\\\"flMm\\\":55,"
                + "\\\"efwGram\\\":\\\"1420\\\","
                + "\\\"afiCm\\\":\\\"12.3\\\","
                + "\\\"deepestPocketCm\\\":\\\"5.1\\\","
                + "\\\"placentaLocation\\\":\\\"前壁\\\","
                + "\\\"placentaGrade\\\":\\\"I 级\\\","
                + "\\\"fetalPresentation\\\":\\\"头位\\\","
                + "\\\"umbilicalSd\\\":\\\"2.5\\\","
                + "\\\"umbilicalPi\\\":\\\"0.9\\\","
                + "\\\"umbilicalRi\\\":\\\"0.6\\\","
                + "\\\"warnings\\\":[\\\"数值仅供人工确认\\\"],"
                + "\\\"rawText\\\":\\\"BPD 71mm HC 260.5mm\\\"}"
                + "\\n```\"}}]}";

        BabyLogSmartInput.UltrasoundOcrCandidate candidate =
                BabyLogSmartInput.fromOpenAiVisionResponse(response);

        assertEquals("2026-05-18", candidate.examDate.value);
        assertEquals("28+3", candidate.gestationalAge.value);
        assertEquals(71.0, candidate.bpdMm.value);
        assertEquals(260.5, candidate.hcMm.value);
        assertEquals(null, candidate.acMm.value);
        assertEquals(55.0, candidate.flMm.value);
        assertEquals(1420.0, candidate.efwGram.value);
        assertEquals(12.3, candidate.afiCm.value);
        assertEquals(5.1, candidate.deepestPocketCm.value);
        assertEquals("前壁", candidate.placentaLocation.value);
        assertEquals("I 级", candidate.placentaGrade.value);
        assertEquals("头位", candidate.fetalPresentation.value);
        assertEquals(2.5, candidate.umbilicalSd.value);
        assertEquals(0.9, candidate.umbilicalPi.value);
        assertEquals(0.6, candidate.umbilicalRi.value);
        assertEquals("数值仅供人工确认", candidate.warnings.get(0));
        assertEquals("BPD 71mm HC 260.5mm", candidate.rawText);

        BabyLogSmartInput.UltrasoundOcrCandidate plain =
                BabyLogSmartInput.fromMessageContent("{\"examDate\":\"2026-05-19\",\"bpdMm\":\"NaN\",\"warnings\":\"请复核\"}");
        assertEquals("2026-05-19", plain.examDate.value);
        assertEquals(null, plain.bpdMm.value);
        assertEquals("请复核", plain.warnings.get(0));

        try {
            candidate.warnings.add("probe");
            throw new AssertionError("warnings should be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected immutable candidate list.
        }
        if (candidate.warnings.contains("probe")) {
            throw new AssertionError("warnings should be immutable");
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }
}
