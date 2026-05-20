import app.babylog.nativeapp.BabyLogDomain;
import app.babylog.nativeapp.BabyLogFormatters;
import app.babylog.nativeapp.BabyLogService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BabyLogServiceSmokeTest {
    public static void main(String[] args) throws Exception {
        assertEquals(
                "喂养 · 奶瓶 · 120 ml",
                BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.feed("奶瓶", "120", "夜奶")
                )
        );

        assertEquals(
                "睡眠 · 22:10-01:20 · 卧室",
                BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.sleep("22:10", "01:20", "卧室", "")
                )
        );

        assertEquals(
                "尿布 · 便 · 黄色偏稀",
                BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.diaper("便", "黄色偏稀", "")
                )
        );

        assertEquals(
                "体温 · 37.8 ℃ · 腋温",
                BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.temperature("37.8", "腋温", "")
                )
        );

        assertEquals(
                "用药 · 布洛芬 · 2 ml · 发热",
                BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.medication("布洛芬", "2 ml", "发热")
                )
        );
        assertEquals(
                "喂养 · 待补充详情",
                BabyLogService.formatBabyCareSummary(BabyLogService.BabyCareInput.feed("", "", ""))
        );
        assertEquals(
                "奶瓶 · 120 ml",
                BabyLogService.formatBabyCareSummary(BabyLogService.BabyCareInput.quick("bottle", "120 ml", ""))
        );
        assertEquals(
                "尿尿",
                BabyLogService.formatBabyCareSummary(BabyLogService.BabyCareInput.quick("pee", "", ""))
        );
        assertFalse(BabyLogService.hasBabyCareMinimumContent(BabyLogService.BabyCareInput.quick("pee", "", "")));
        assertTrue(BabyLogService.hasBabyCareMinimumContent(BabyLogService.BabyCareInput.quick("pee", "尿量偏多", "")));

        assertEquals(
                "产检 · 市妇幼产科 · 一切正常",
                BabyLogService.formatPregnancySummary(
                        BabyLogService.PregnancyInput.checkup("2026-05-18", "市妇幼产科", "一切正常", "下次 2026-06-16")
                )
        );
        BabyLogService.PregnancyInput structuredCheckup = BabyLogService.PregnancyInput.checkupStructured(
                "2026-05-18",
                "22+5",
                "市妇幼",
                "产科",
                "118",
                "76",
                "60.4",
                "24",
                "88",
                "143",
                "头位",
                "无",
                "白细胞少许",
                "阴性",
                "112",
                "无特殊",
                "一切正常",
                "继续常规产检",
                "2026-06-16",
                "产检报告",
                "血常规照片",
                "复查血常规",
                "/tmp/checkup.jpg",
                "checkup.jpg"
        );
        assertEquals("产科", structuredCheckup.department);
        assertEquals("2026-06-16", structuredCheckup.nextVisitDate);
        assertEquals(
                "产检 · 22+5 周 · 市妇幼 · 血压 118/76 mmHg · 体重 60.4 kg · 胎心 143 bpm · 胎位 头位 · 尿蛋白 阴性 · Hb 112 g/L · 一切正常 · 继续常规产检",
                BabyLogService.formatPregnancySummary(structuredCheckup)
        );
        JSONObject structuredPayload = BabyLogService.buildPregnancyPayload(structuredCheckup);
        assertTrue(BabyLogService.hasPregnancyMinimumContent(structuredCheckup));
        assertFalse(BabyLogService.hasPregnancyMinimumContent(BabyLogService.PregnancyInput.checkup("2026-05-18", "", "", "")));
        assertEquals(159, structuredPayload.optInt("gestationalAgeDays"));
        assertEquals("头位", structuredPayload.optString("fetalPresentation"));
        assertEquals("无", structuredPayload.optString("edema"));
        assertEquals("阴性", structuredPayload.optString("urineProtein"));
        assertEquals(112.0, structuredPayload.optDouble("hemoglobinGL"));
        assertEquals("无特殊", structuredPayload.optString("highRiskFactors"));
        assertEquals("继续常规产检", structuredPayload.optString("treatmentAdvice"));
        assertEquals("产检报告", structuredPayload.optString("reportType"));
        assertEquals("血常规照片", structuredPayload.optString("attachmentNote"));
        Map<String, String> ogttValues = new LinkedHashMap<>();
        ogttValues.put("fastingGlucoseMmolL", "4.8");
        ogttValues.put("oneHourGlucoseMmolL", "8.1");
        ogttValues.put("twoHourGlucoseMmolL", "7.0");
        ogttValues.put("abnormalFlag", "见报告");
        BabyLogService.PregnancyInput ogtt = BabyLogService.PregnancyInput.screening(
                "screening_ogtt",
                "2026-05-18",
                "24+1",
                ogttValues,
                "报告原文记录",
                "/tmp/ogtt.jpg",
                "ogtt.jpg"
        );
        JSONObject ogttPayload = BabyLogService.buildPregnancyPayload(ogtt);
        assertTrue(BabyLogService.hasPregnancyMinimumContent(ogtt));
        assertEquals(169, ogttPayload.optInt("gestationalAgeDays"));
        assertEquals(4.8, ogttPayload.optDouble("fastingGlucoseMmolL"));
        assertEquals("见报告", ogttPayload.optString("abnormalFlag"));
        assertEquals(
                "糖耐 OGTT · 24+1 周 · 空腹 4.8 mmol/L · 1h 8.1 mmol/L · 2h 7 mmol/L · 见报告 · 报告原文记录",
                BabyLogService.formatPregnancySummary(ogtt)
        );
        assertFalse(BabyLogService.hasPregnancyMinimumContent(
                BabyLogService.PregnancyInput.screening("screening_nst", "2026-05-18", "33+0", new LinkedHashMap<>(), "", "", "")
        ));
        assertEquals(
                "胎动 · 20:00-21:00 · 10 次",
                BabyLogService.formatPregnancySummary(
                        BabyLogService.PregnancyInput.fetalMovement("20:00-21:00", "10", "饭后")
                )
        );
        assertEquals(
                "胎动 · 20:04-20:17 · 10 次 · 13 分钟",
                BabyLogService.formatFetalMovementSessionSummary(
                        BabyLogService.FetalMovementSessionInput.create(
                                "2026-05-18T20:04:00.000+0800",
                                "2026-05-18T20:17:00.000+0800",
                                10,
                                13,
                                10,
                                "饭后"
                        )
                )
        );
        assertEquals(
                "宫缩 · 22:10 · 间隔 5 分钟 · 持续 40 秒",
                BabyLogService.formatPregnancySummary(
                        BabyLogService.PregnancyInput.contraction("22:10", "5", "40", "")
                )
        );
        BabyLogService.ContractionEntryInput contractionEntry = BabyLogService.ContractionEntryInput.create(
                "2026-05-18T22:10:00.000+0800",
                "2026-05-18T22:10:42.000+0800",
                42,
                300
        );
        JSONObject contractionPayload = BabyLogService.buildContractionSessionPayload("session-1", contractionEntry);
        assertEquals("session", contractionPayload.optString("entryMode"));
        assertEquals("session-1", contractionPayload.optString("sessionId"));
        assertEquals(42, contractionPayload.optInt("durationSec"));
        assertEquals(300, contractionPayload.optInt("intervalFromPrevSec"));
        assertEquals(
                "宫缩 · 22:10-22:10 · 持续 42 秒 · 距上次 5 分钟",
                BabyLogService.formatContractionSessionSummary(contractionEntry)
        );
        assertEquals(
                "孕妈指标 · 体重 60.4 kg · 血压 118/76 mmHg · 血糖 空腹 5.2 mmol/L",
                BabyLogService.formatMaternalMetricSummary(
                BabyLogService.MaternalMetricInput.create("60.4", "118", "76", "5.2", "fasting", "")
                )
        );
        assertFalse(BabyLogService.hasMaternalMetricMinimumContent(BabyLogService.MaternalMetricInput.create("", "", "", "", "fasting", "")));
        assertTrue(BabyLogService.hasMaternalMetricMinimumContent(BabyLogService.MaternalMetricInput.create("", "", "", "", "fasting", "今天状态稳定")));
        assertEquals(
                "羊水 AFI 12.3 cm · 最大羊水池 5.1 cm · 胎盘 前壁 · 成熟度 I 级 · 胎位 头位 · 脐动脉 S/D 2.5 · PI 0.9 · RI 0.6",
                BabyLogService.formatUltrasoundClinicalDetails(ultrasound("2026-05-18", "22+5", "55", "205", "180", "38", "520", "12.3", "5.1", "前壁", "I 级", "头位", "2.5", "0.9", "0.6", "", ""))
        );
        assertEquals(
                "最大羊水池 3.8 cm · 胎盘 前壁 · 胎儿个数 单胎 · 胎动 可见 · 胎心率 156 bpm · CRL 68 mm · NT 1.6 mm · 宫颈管长度 35 mm · 脐带插入处 居中",
                BabyLogService.formatUltrasoundClinicalDetails(new BabyLogService.UltrasoundInput(
                        "2026-03-14", "", "", "", "", "", "", "", "", "", "", "3.8",
                        "前壁", "", "", "156", "单胎", "可见", "居中",
                        "35", "68", "1.6", "", "", "", "", ""
                ))
        );
        assertFalse(BabyLogService.hasUltrasoundMinimumContent(ultrasound("2026-05-18", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")));
        assertFalse(BabyLogService.hasUltrasoundMinimumContent(ultrasound("2026-05-18", "22+5", "", "", "", "", "", "12.3", "", "前壁", "", "", "", "", "", "", "")));
        assertTrue(BabyLogService.hasUltrasoundMinimumContent(ultrasound("2026-05-18", "22+5", "45", "", "", "", "", "", "", "", "", "", "", "", "", "", "")));
        File ultrasoundPhoto = File.createTempFile("babylog-ultrasound", ".jpg");
        ultrasoundPhoto.deleteOnExit();
        Files.write(ultrasoundPhoto.toPath(), new byte[] { 1, 2, 3 });
        assertTrue(BabyLogService.hasUltrasoundMinimumContent(ultrasound("2026-05-18", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ultrasoundPhoto.getAbsolutePath(), "ultrasound.jpg")));

        BabyLogDomain.ChildProfile birthProfile = BabyLogService.withBirthDateFromBirthEvent(
                BabyLogDomain.ChildProfile.createForNewFamily("栗子", "female", "2026-09-16", "", "auto", true),
                "2026-09-17T08:30:00.000+0800"
        );
        assertEquals("2026-09-17", birthProfile.birthDate);

        BabyLogDomain.BabyLogEvent original = new BabyLogDomain.BabyLogEvent(
                "evt_edit",
                BabyLogDomain.FAMILY_ID,
                BabyLogDomain.CHILD_ID,
                "ultrasound",
                "2026-05-02T12:00:00.000+0800",
                null,
                Arrays.asList("att_existing"),
                "manual",
                "2026-05-02T12:00:00.000+0800",
                "2026-05-02T12:00:00.000+0800",
                BabyLogDomain.UPDATED_BY_LOCAL,
                BabyLogDomain.SCHEMA_VERSION,
                null
        );
        BabyLogDomain.BabyLogEvent edited = BabyLogService.createEditedEvent(
                original,
                "ultrasound",
                null,
                Arrays.asList("att_existing", "att_new")
        );
        assertEquals(original.id, edited.id);
        assertEquals(original.createdAt, edited.createdAt);
        assertEquals(original.occurredAt, edited.occurredAt);
        assertEquals(BabyLogDomain.UPDATED_BY_LOCAL, edited.updatedBy);
        assertTrue(!original.updatedAt.equals(edited.updatedAt));
        assertEquals(2, edited.attachmentIds.size());
        BabyLogDomain.BabyLogEvent movedDate = BabyLogService.createEditedEvent(
                original,
                "ultrasound",
                null,
                Arrays.asList("att_existing"),
                BabyLogFormatters.createOccurredAtFromDate("2026-05-05")
        );
        assertEquals("2026-05-05T12:00:00.000+0800", movedDate.occurredAt);
        assertEquals(original.id, movedDate.id);
        assertEquals(original.createdAt, movedDate.createdAt);

        List<BabyLogDomain.BabyLogEvent> manyEvents = new ArrayList<>();
        for (int i = 0; i < 105; i++) {
            int month = 1 + (i / 28);
            int day = 1 + (i % 28);
            manyEvents.add(new BabyLogDomain.BabyLogEvent(
                    "evt_" + i,
                    BabyLogDomain.FAMILY_ID,
                    BabyLogDomain.CHILD_ID,
                    "ultrasound",
                    String.format("2026-%02d-%02dT12:00:00.000+0800", month, day),
                    null,
                    Arrays.asList(),
                    "manual",
                    "2026-01-01T12:00:00.000+0800",
                    "2026-01-01T12:00:00.000+0800",
                    BabyLogDomain.UPDATED_BY_LOCAL,
                    BabyLogDomain.SCHEMA_VERSION,
                    null
            ));
        }
        List<BabyLogDomain.BabyLogEvent> sortedEvents = BabyLogService.sortEventsNewestFirst(manyEvents);
        assertEquals(105, sortedEvents.size());
        assertEquals("evt_104", sortedEvents.get(0).id);
        assertEquals("evt_0", sortedEvents.get(sortedEvents.size() - 1).id);

        JSONObject validImportData = new JSONObject();
        validImportData.put("events", new JSONArray().put(original.toJson()));
        validImportData.put("attachments", new JSONArray());
        validImportData.put("attachmentBlobs", new JSONArray());
        validImportData.put("syncChanges", new JSONArray().put(BabyLogDomain.createSyncChange("event", original.id, "upsert").toJson()));
        BabyLogService.validateBackupDataForImport(validImportData);

        JSONObject missingEventIdData = new JSONObject(validImportData.toString());
        missingEventIdData.getJSONArray("events").getJSONObject(0).remove("id");
        assertThrows(() -> BabyLogService.validateBackupDataForImport(missingEventIdData));

        JSONObject missingAttachmentBlobData = new JSONObject(validImportData.toString());
        JSONObject attachment = new JSONObject();
        attachment.put("id", "att_missing_blob");
        attachment.put("familyId", BabyLogDomain.FAMILY_ID);
        attachment.put("childId", BabyLogDomain.CHILD_ID);
        attachment.put("kind", "ultrasound");
        attachment.put("originalName", "scan.jpg");
        attachment.put("mimeType", "image/jpeg");
        attachment.put("byteSize", 3);
        attachment.put("localPath", "/tmp/scan.jpg");
        attachment.put("createdAt", "2026-05-02T12:00:00.000+0800");
        attachment.put("updatedAt", "2026-05-02T12:00:00.000+0800");
        attachment.put("updatedBy", BabyLogDomain.UPDATED_BY_LOCAL);
        attachment.put("schemaVersion", BabyLogDomain.SCHEMA_VERSION);
        missingAttachmentBlobData.put("attachments", new JSONArray().put(attachment));
        missingAttachmentBlobData.put("attachmentBlobs", new JSONArray());
        assertThrows(() -> BabyLogService.validateBackupDataForImport(missingAttachmentBlobData));
        JSONObject deletedAttachmentData = new JSONObject(validImportData.toString());
        JSONObject deletedAttachment = new JSONObject(attachment.toString());
        deletedAttachment.put("id", "att_deleted_no_blob");
        deletedAttachment.put("deletedAt", "2026-05-03T12:00:00.000+0800");
        deletedAttachmentData.put("attachments", new JSONArray().put(deletedAttachment));
        deletedAttachmentData.put("attachmentBlobs", new JSONArray());
        BabyLogService.validateBackupDataForImport(deletedAttachmentData);

        JSONObject badSyncData = new JSONObject(validImportData.toString());
        badSyncData.getJSONArray("syncChanges").getJSONObject(0).remove("entityId");
        assertThrows(() -> BabyLogService.validateBackupDataForImport(badSyncData));
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

    private static void assertThrows(ThrowingRunnable action) throws Exception {
        try {
            action.run();
        } catch (Exception expected) {
            return;
        }
        throw new AssertionError("expected exception");
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static BabyLogService.UltrasoundInput ultrasound(
            String examDate,
            String gestationalAge,
            String bpdMm,
            String hcMm,
            String acMm,
            String flMm,
            String efwGram,
            String afiCm,
            String deepestPocketCm,
            String placentaLocation,
            String placentaGrade,
            String fetalPresentation,
            String umbilicalSd,
            String umbilicalPi,
            String umbilicalRi,
            String photoPath,
            String photoName
    ) {
        return new BabyLogService.UltrasoundInput(
                examDate,
                gestationalAge,
                "",
                "",
                "",
                bpdMm,
                hcMm,
                acMm,
                flMm,
                efwGram,
                afiCm,
                deepestPocketCm,
                placentaLocation,
                placentaGrade,
                fetalPresentation,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                umbilicalSd,
                umbilicalPi,
                umbilicalRi,
                photoPath,
                photoName
        );
    }
}
