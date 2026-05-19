import app.babylog.nativeapp.BabyLogSmartInput;

public final class BabyLogSmartInputSmokeTest {
    public static void main(String[] args) throws Exception {
        String response = "{"
                + "\"choices\":[{\"message\":{\"content\":\"```json\\n"
                + "{\\\"examDate\\\":\\\"2026-05-18\\\","
                + "\\\"hospital\\\":\\\"奉化区妇幼\\\","
                + "\\\"reportTime\\\":\\\"2026-05-18 10:30\\\","
                + "\\\"diagnosisText\\\":\\\"宫内妊娠，单活胎\\\","
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
        assertEquals("奉化区妇幼", candidate.hospital.value);
        assertEquals("2026-05-18 10:30", candidate.reportTime.value);
        assertEquals("宫内妊娠，单活胎", candidate.diagnosisText.value);
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
                        + "\"deepestPocketCm\":\"52mm\","
                        + "\"placentaLocation\":\"前壁\","
                        + "\"placentaGrade\":\"0级\","
                        + "\"fetalPresentation\":\"臀位\","
                        + "\"fetalHeartRateBpm\":\"143bpm\","
                        + "\"fetalCount\":\"单胎\","
                        + "\"fetalMovement\":\"有\","
                        + "\"umbilicalInsertion\":\"居中\","
                        + "\"cervicalLengthMm\":\"36mm\","
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
        assertEquals(5.2, mixed.deepestPocketCm.value);
        assertEquals("前壁", mixed.placentaLocation.value);
        assertEquals("0级", mixed.placentaGrade.value);
        assertEquals("臀位", mixed.fetalPresentation.value);
        assertEquals(143.0, mixed.fetalHeartRateBpm.value);
        assertEquals("单胎", mixed.fetalCount.value);
        assertEquals("有", mixed.fetalMovement.value);
        assertEquals("居中", mixed.umbilicalInsertion.value);
        assertEquals(36.0, mixed.cervicalLengthMm.value);

        BabyLogSmartInput.UltrasoundOcrCandidate early =
                BabyLogSmartInput.fromMessageContent("{"
                        + "\"examDate\":\"2026-03-14\","
                        + "\"gestationalAge\":\"13+3\","
                        + "\"bpdMm\":\"22\","
                        + "\"hcMm\":\"83\","
                        + "\"acMm\":\"63\","
                        + "\"flMm\":\"10\","
                        + "\"crlMm\":\"68\","
                        + "\"ntMm\":\"1.6\","
                        + "\"fetalHeartRateBpm\":\"156\","
                        + "\"deepestPocketCm\":\"38mm\","
                        + "\"placentaLocation\":\"前壁\","
                        + "\"umbilicalInsertion\":\"居中\","
                        + "\"rawText\":\"超声孕龄13+3周；NT 1.6；顶臀径CRL 68；胎心率156；羊水最大平段38\""
                        + "}");
        assertEquals(null, early.gestationalAge.value);
        assertEquals(68.0, early.crlMm.value);
        assertEquals(1.6, early.ntMm.value);
        assertEquals(156.0, early.fetalHeartRateBpm.value);
        assertEquals(3.8, early.deepestPocketCm.value);

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
