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
                + "\\\"rawText\\\":\\\"BPD 71mm HC 260.5mm EFW 1420g\\\"}"
                + "\\n```\"}}]}";

        BabyLogSmartInput.UltrasoundOcrCandidate candidate =
                BabyLogSmartInput.fromOpenAiVisionResponse(response);

        assertEquals("2026-05-18", candidate.examDate.value);
        assertEquals(null, candidate.gestationalAge.value);
        assertEquals(71.0, candidate.bpdMm.value);
        assertEquals(260.5, candidate.hcMm.value);
        assertEquals(null, candidate.acMm.value);
        assertEquals(55.0, candidate.flMm.value);
        assertEquals(1420.0, candidate.efwGram.value);
        assertEquals(null, candidate.afiCm.value);
        assertEquals(null, candidate.deepestPocketCm.value);
        assertEquals(null, candidate.placentaLocation.value);
        assertEquals(null, candidate.placentaGrade.value);
        assertEquals(null, candidate.fetalPresentation.value);
        assertEquals(null, candidate.umbilicalSd.value);
        assertEquals(null, candidate.umbilicalPi.value);
        assertEquals(null, candidate.umbilicalRi.value);
        assertEquals("数值仅供人工确认", candidate.warnings.get(0));
        assertEquals("BPD 71mm HC 260.5mm EFW 1420g", candidate.rawText);

        BabyLogSmartInput.UltrasoundOcrCandidate plain =
                BabyLogSmartInput.fromMessageContent("{\"examDate\":\"2026-05-19\",\"bpdMm\":\"NaN\",\"warnings\":\"请复核\"}");
        assertEquals("2026-05-19", plain.examDate.value);
        assertEquals(null, plain.bpdMm.value);
        assertEquals("请复核", plain.warnings.get(0));

        BabyLogSmartInput.UltrasoundOcrCandidate wrapped =
                BabyLogSmartInput.fromMessageContent("识别结果如下：{\"gestationalAge\":\"29+1\",\"efwGram\":1510} 请人工核对。");
        assertEquals(null, wrapped.gestationalAge.value);
        assertEquals(null, wrapped.efwGram.value);

        BabyLogSmartInput.UltrasoundOcrCandidate mixed =
                BabyLogSmartInput.fromMessageContent("{"
                        + "\"examDate\":\"2026-05-02\","
                        + "\"gestationalAge\":\"36\","
                        + "\"bpdMm\":\"45\","
                        + "\"hcMm\":\"176\","
                        + "\"acMm\":\"158\","
                        + "\"flMm\":\"31\","
                        + "\"efwGram\":\"1435\","
                        + "\"afiCm\":\"52\","
                        + "\"deepestPocketCm\":\"7.1\","
                        + "\"placentaLocation\":\"前壁\","
                        + "\"rawText\":\"胎心率143bpm；羊水最大平段52mm；宫颈管长度36mm；侧脑室后角宽约7.1mm\""
                        + "}");
        assertEquals("2026-05-02", mixed.examDate.value);
        assertEquals(null, mixed.gestationalAge.value);
        assertEquals(45.0, mixed.bpdMm.value);
        assertEquals(176.0, mixed.hcMm.value);
        assertEquals(158.0, mixed.acMm.value);
        assertEquals(31.0, mixed.flMm.value);
        assertEquals(null, mixed.efwGram.value);
        assertEquals(null, mixed.afiCm.value);
        assertEquals(null, mixed.deepestPocketCm.value);
        assertEquals(null, mixed.placentaLocation.value);

        BabyLogSmartInput.UltrasoundOcrCandidate unitText =
                BabyLogSmartInput.fromMessageContent("{\"result\":{"
                        + "\"examDate\":\"2026-05-02\","
                        + "\"bpdMm\":\"4.5 cm\","
                        + "\"hcMm\":\"176mm\","
                        + "\"acMm\":\"15.8 cm\","
                        + "\"flMm\":\"31 mm\","
                        + "\"efwGram\":\"344 g\","
                        + "\"rawText\":\"双顶径45mm 头围176mm 腹围158mm 股骨长31mm EFW 344g\""
                        + "}}");
        assertEquals("2026-05-02", unitText.examDate.value);
        assertEquals(45.0, unitText.bpdMm.value);
        assertEquals(176.0, unitText.hcMm.value);
        assertEquals(158.0, unitText.acMm.value);
        assertEquals(31.0, unitText.flMm.value);
        assertEquals(344.0, unitText.efwGram.value);

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
