import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogDomain;
import app.babylog.nativeapp.BabyLogException;
import app.babylog.nativeapp.BabyLogBabyDayTimelineSlots;
import app.babylog.nativeapp.BabyLogDailyBabySummary;
import app.babylog.nativeapp.BabyLogFormatters;
import app.babylog.nativeapp.BabyLogRepository;
import app.babylog.nativeapp.BabyLogService;
import app.babylog.nativeapp.BabyLogSyncTrigger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;

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
        BabyLogDomain.BabyLogEvent sleepEvent = sleepEvent("2026-05-25T22:00:00.000+0800", "2026-05-25T23:30:00.000+0800");
        assertEquals(90, BabyLogService.sleepDurationMinutes(sleepEvent).getAsInt());
        assertEquals("1 小时 30 分", BabyLogFormatters.formatSleepDurationLabel(90));
        assertEquals("45 分钟", BabyLogFormatters.formatSleepDurationLabel(45));
        assertEquals("3 分钟", BabyLogFormatters.formatSleepDurationLabel(3));

        BabyLogDomain.BabyLogEvent overnightSleepEvent = sleepEvent(
                "2026-05-25T23:00:00.000+0800",
                "2026-05-26T06:30:00.000+0800"
        );
        assertEquals(450, BabyLogService.sleepDurationMinutes(overnightSleepEvent).getAsInt());
        assertTrue(BabyLogFormatters.eventSummary(overnightSleepEvent).contains("7 小时 30 分"));

        BabyLogDomain.BabyLogEvent openSleepEvent = sleepEvent("2026-05-25T23:00:00.000+0800", "");
        assertFalse(BabyLogService.sleepDurationMinutes(openSleepEvent).isPresent());
        assertTrue(BabyLogFormatters.eventSummary(openSleepEvent).contains("睡眠中"));

        assertEquals(
                "尿布 · 便 · 黄色偏稀",
                BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.diaper("便", "黄色偏稀", "")
                )
        );
        JSONObject diaperPayload = BabyLogService.buildBabyCarePayload(
                BabyLogService.BabyCareInput.diaper("便", "量多", "黄色偏稀", "晨起")
        );
        assertEquals("poop", diaperPayload.optString("diaperKind"));
        assertEquals("黄", diaperPayload.optString("color"));
        assertEquals("稀", diaperPayload.optString("consistency"));
        assertEquals("黄色偏稀", diaperPayload.optString("diaperObservation"));
        assertFalse(diaperPayload.has("summary"));
        assertContains(BabyLogFormatters.eventSummary(
                babyEvent("diaper", "2026-05-25T07:00:00.000+0800", diaperPayload)
        ), "黄色偏稀");
        JSONObject smallPeePayload = BabyLogService.buildBabyCarePayload(
                BabyLogService.BabyCareInput.diaper("小便", "偏多", "", "")
        );
        assertEquals("pee", smallPeePayload.optString("diaperKind"));

        JSONObject oldDiaperPayload = new JSONObject();
        oldDiaperPayload.put("diaperType", "尿");
        oldDiaperPayload.put("diaperDetail", "偏多");
        Map<String, String> oldDiaperDraft = BabyLogService.babyCareDraftFields("diaper", oldDiaperPayload);
        assertEquals("尿", oldDiaperDraft.get("primary"));
        assertEquals("偏多", oldDiaperDraft.get("secondary"));
        assertFalse(oldDiaperDraft.containsKey("tertiary"));
        oldDiaperPayload.put("diaperType", "小便");
        oldDiaperPayload.put("diaperObservation", "黄绿色糊状");
        Map<String, String> legacyDiaperDraft = BabyLogService.babyCareDraftFields("diaper", oldDiaperPayload);
        assertEquals("小便", legacyDiaperDraft.get("primary"));
        assertEquals("黄绿色糊状", legacyDiaperDraft.get("tertiary"));

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
                "成长 · 体重 7.2 kg · 身长 65 cm · 头围 42 cm · 满月儿保",
                BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.growth("7.2", "65", "42", "满月儿保")
                )
        );
        JSONObject growthPayload = BabyLogService.buildBabyCarePayload(
                BabyLogService.BabyCareInput.growth("7.2", "65", "42", "满月儿保")
        );
        assertNear(7.2, growthPayload.optDouble("weightKg"), 0.001);
        assertNear(65.0, growthPayload.optDouble("heightCm"), 0.001);
        assertNear(42.0, growthPayload.optDouble("headCircumferenceCm"), 0.001);
        assertEquals("满月儿保", growthPayload.optString("note"));
        assertFalse(growthPayload.has("summary"));
        String growthSummary = BabyLogFormatters.eventSummary(
                babyEvent("growth", "2026-05-25T18:30:00.000+0800", growthPayload)
        );
        assertContains(growthSummary, "体重 7.2 kg");
        assertContains(growthSummary, "身长 65 cm");
        assertContains(growthSummary, "头围 42 cm");
        Map<String, String> growthDraft = BabyLogService.babyCareDraftFields("growth", growthPayload);
        assertEquals("7.2", growthDraft.get("primary"));
        assertEquals("65", growthDraft.get("secondary"));
        assertEquals("42", growthDraft.get("tertiary"));
        assertEquals("满月儿保", growthDraft.get("note"));
        BabyLogService.BabyCareInput childCheckupInput = BabyLogService.BabyCareInput.childCheckup(
                "7.4",
                "66",
                "42.5",
                "社区儿保",
                "发育记录已核对",
                "2026-07-25",
                "妈妈备注"
        );
        assertEquals(
                "儿保 · 体重 7.4 kg · 身长 66 cm · 头围 42.5 cm · 社区儿保 · 发育记录已核对 · 下次 2026-07-25 · 妈妈备注",
                BabyLogService.formatBabyCareSummary(childCheckupInput)
        );
        JSONObject childCheckupPayload = BabyLogService.buildBabyCarePayload(childCheckupInput);
        assertNear(7.4, childCheckupPayload.optDouble("weightKg"), 0.001);
        assertEquals("社区儿保", childCheckupPayload.optString("checkupInstitution"));
        assertEquals("发育记录已核对", childCheckupPayload.optString("checkupConclusion"));
        assertEquals("2026-07-25", childCheckupPayload.optString("nextCheckupDate"));
        String childCheckupSummary = BabyLogFormatters.eventSummary(
                babyEvent("child_checkup", "2026-05-25T18:45:00.000+0800", childCheckupPayload)
        );
        assertContains(childCheckupSummary, "儿保");
        assertContains(childCheckupSummary, "体重 7.4 kg");
        assertContains(childCheckupSummary, "下次 2026-07-25");
        Map<String, String> childCheckupDraft = BabyLogService.babyCareDraftFields("child_checkup", childCheckupPayload);
        assertEquals("7.4", childCheckupDraft.get("primary"));
        assertEquals("66", childCheckupDraft.get("secondary"));
        assertEquals("42.5", childCheckupDraft.get("tertiary"));
        assertEquals("社区儿保", childCheckupDraft.get("checkupInstitution"));
        assertEquals("发育记录已核对", childCheckupDraft.get("checkupConclusion"));
        assertEquals("2026-07-25", childCheckupDraft.get("nextCheckupDate"));
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

        JSONObject feedSidePayload = BabyLogService.buildBabyCarePayload(
                BabyLogService.BabyCareInput.feed("母乳", "", "L", "亲喂")
        );
        assertEquals("L", feedSidePayload.optString("breastSide"));
        assertFalse(feedSidePayload.has("summary"));
        assertContains(BabyLogFormatters.eventSummary(
                babyEvent("feed", "2026-05-25T08:00:00.000+0800", feedSidePayload)
        ), "左侧");
        JSONObject legacySummaryPayload = new JSONObject(feedSidePayload.toString())
                .put("summary", "旧摘要不应显示");
        String legacySummaryRendered = BabyLogFormatters.eventSummary(
                babyEvent("feed", "2026-05-25T08:00:00.000+0800", legacySummaryPayload)
        );
        assertContains(legacySummaryRendered, "母乳");
        assertNotContains(legacySummaryRendered, "旧摘要不应显示");
        Map<String, String> feedSideDraft = BabyLogService.babyCareDraftFields("feed", feedSidePayload);
        assertEquals("母乳", feedSideDraft.get("primary"));
        assertEquals("L", feedSideDraft.get("tertiary"));

        JSONObject solidFoodPayload = BabyLogService.buildBabyCarePayload(
                BabyLogService.BabyCareInput.feed("辅食", "", "香蕉泥", "")
        );
        assertEquals("香蕉泥", solidFoodPayload.optString("solidFood"));

        JSONObject breastfeedPayload = BabyLogService.buildBabyCarePayload(
                BabyLogService.BabyCareInput.breastfeed("12", "8", "夜间")
        );
        assertEquals(12.0, breastfeedPayload.optDouble("leftMinutes"));
        assertEquals(8.0, breastfeedPayload.optDouble("rightMinutes"));
        assertFalse(breastfeedPayload.has("detail"));
        assertFalse(breastfeedPayload.has("summary"));
        String breastfeedSummary = BabyLogFormatters.eventSummary(
                babyEvent("breastfeed", "2026-05-25T08:30:00.000+0800", breastfeedPayload)
        );
        assertContains(breastfeedSummary, "左 12 分钟");
        assertContains(breastfeedSummary, "右 8 分钟");

        JSONObject bottlePayload = BabyLogService.buildBabyCarePayload(
                BabyLogService.BabyCareInput.bottle("120", "美赞臣", "睡前")
        );
        assertEquals(120.0, bottlePayload.optDouble("amountMl"));
        assertEquals("美赞臣", bottlePayload.optString("brand"));
        assertTrue(BabyLogService.formatBabyCareSummary(
                BabyLogService.BabyCareInput.bottle("120", "美赞臣", "睡前")
        ).contains("美赞臣"));

        JSONObject oldBreastfeedPayload = new JSONObject();
        oldBreastfeedPayload.put("detail", "左侧约十分钟");
        Map<String, String> oldBreastfeedDraft = BabyLogService.babyCareDraftFields("breastfeed", oldBreastfeedPayload);
        assertEquals("左侧约十分钟", oldBreastfeedDraft.get("tertiary"));

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
        assertFalse(structuredPayload.has("summary"));
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
        assertFalse(ogttPayload.has("summary"));
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
        BabyLogService.FetalMovementSessionInput movementSession = BabyLogService.FetalMovementSessionInput.create(
                "2026-05-18T20:04:00.000+0800",
                "2026-05-18T20:17:00.000+0800",
                10,
                13,
                10,
                "饭后"
        );
        JSONObject movementSessionPayload = BabyLogService.buildFetalMovementSessionPayload(movementSession);
        assertFalse(movementSessionPayload.has("summary"));
        assertEquals(
                "胎动 · 20:04-20:17 · 10 次 · 13 分钟",
                BabyLogFormatters.eventSummary(babyEvent("fetal_movement", "2026-05-18T20:17:00.000+0800", movementSessionPayload))
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
        assertFalse(contractionPayload.has("summary"));
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
        JSONObject maternalMetricPayload = BabyLogService.buildMaternalMetricPayload(
                BabyLogService.MaternalMetricInput.create("60.4", "118", "76", "5.2", "fasting", "")
        );
        assertFalse(maternalMetricPayload.has("summary"));
        assertEquals(
                "孕妈指标 · 体重 60.4 kg · 血压 118/76 mmHg · 血糖 空腹 5.2 mmol/L",
                BabyLogFormatters.eventSummary(babyEvent("maternal_metric", "2026-05-18T08:00:00.000+0800", maternalMetricPayload))
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
        Method buildUltrasoundPayload = BabyLogService.class.getDeclaredMethod(
                "buildUltrasoundPayload",
                BabyLogService.UltrasoundInput.class
        );
        buildUltrasoundPayload.setAccessible(true);
        JSONObject ultrasoundPayload = (JSONObject) buildUltrasoundPayload.invoke(
                null,
                ultrasound("2026-05-18", "22+5", "45", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
        );
        assertFalse(ultrasoundPayload.has("summary"));
        assertContains(BabyLogFormatters.eventSummary(
                babyEvent("ultrasound", "2026-05-18T12:00:00.000+0800", ultrasoundPayload)
        ), "BPD 45 mm");

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

        assertRepositorySupportsAtomicEventAttachmentSyncWrites(original);
        assertCreateEventPlansSyncChange(original);
        assertServiceExceptionTypes();
        assertDailyBabySummary();
        assertSuccessfulWriteTriggersSync();
        assertQuickSleepWakeClosure();
        assertBabyCareOccurredTimeBackfill();
        assertQuickUndoUsesTrashDelete();
        assertBackupRoundTripRestoresEventsAttachmentsAndUndo();

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

        JSONObject missingLocalAttachment = new JSONObject(attachment.toString());
        missingLocalAttachment.put("id", "att_missing_local_file");
        missingLocalAttachment.put("localPath", new File("missing/scan.jpg").getAbsolutePath());
        JSONArray sanitizedAttachments = BabyLogService.sanitizeAttachmentsForBackup(
                new JSONArray().put(missingLocalAttachment),
                "2026-05-20T12:00:00.000+0800"
        );
        assertEquals("2026-05-20T12:00:00.000+0800", sanitizedAttachments.getJSONObject(0).optString("deletedAt"));
        JSONObject sanitizedBackupData = new JSONObject(validImportData.toString());
        sanitizedBackupData.put("attachments", sanitizedAttachments);
        sanitizedBackupData.put("attachmentBlobs", new JSONArray());
        BabyLogService.validateBackupDataForImport(sanitizedBackupData);

        JSONObject badSyncData = new JSONObject(validImportData.toString());
        badSyncData.getJSONArray("syncChanges").getJSONObject(0).remove("entityId");
        assertThrows(() -> BabyLogService.validateBackupDataForImport(badSyncData));
    }






    private static BabyLogDomain.BabyLogEvent sleepEvent(String startIso, String endIso) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("sleepStart", startIso);
        if (endIso != null && !endIso.isEmpty()) {
            payload.put("sleepEnd", endIso);
        }
        return BabyLogDomain.createEvent("sleep", startIso, payload, Collections.emptyList(), "manual");
    }

    private static void assertDailyBabySummary() throws Exception {
        BabyLogRepository emptyRepository = BabyLogRepository.forSmokeTest();
        BabyLogDailyBabySummary empty = BabyLogService.forSmokeTest(emptyRepository).dailyBabySummary("2026-05-25");
        assertEquals(0, empty.feedCount);
        assertEquals(0, empty.feedTotalMl);
        assertEquals(0, empty.feedBreastCount);
        assertEquals(0, empty.feedBottleCount);
        assertEquals(0, empty.feedSolidCount);
        assertEquals("", empty.feedLastTime);
        assertEquals("", empty.feedLastType);
        assertEquals(0, empty.feedLastAmountMl);
        assertEquals(0, empty.sleepTotalMinutes);
        assertEquals(0, empty.sleepIncompleteCount);
        assertEquals(0, empty.sleepLongestMinutes);
        assertEquals("", empty.sleepLastTime);
        assertEquals(0, empty.peeCount);
        assertEquals(0, empty.poopCount);
        assertEquals(0, empty.diaperCount);
        assertEquals("", empty.diaperLastKind);
        assertEquals("", empty.diaperLastTime);
        assertTrue(Double.isNaN(empty.temperatureMax));
        assertTrue(Double.isNaN(empty.temperatureMin));
        assertEquals("", empty.temperatureLastTime);
        assertEquals("", empty.medicationLastName);
        assertEquals("", empty.medicationLastTime);
        assertEquals(0, empty.milestoneCount);
        assertTrue(Double.isNaN(empty.growthWeightKg));
        assertTrue(Double.isNaN(empty.growthHeightCm));
        assertTrue(Double.isNaN(empty.growthHeadCircumferenceCm));
        assertEquals("", empty.growthLastTime);

        BabyLogRepository repository = BabyLogRepository.forSmokeTest();
        repository.putEvent(babyEvent("feed", "2026-05-25T08:00:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.feed("奶瓶", "120", ""))));
        repository.putEvent(babyEvent("bottle", "2026-05-25T10:00:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.bottle("90", "A2", ""))));
        repository.putEvent(babyEvent("breastfeed", "2026-05-25T11:00:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.breastfeed("12", "8", ""))));
        repository.putEvent(babyEvent("feed", "2026-05-25T11:20:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.feed("辅食", "", "米糊", ""))));
        repository.putEvent(babyEvent("feed", "2026-05-25T11:40:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.feed("奶瓶", "40", ""))));
        repository.putEvent(sleepEvent("2026-05-25T23:00:00.000+0800", "2026-05-26T06:30:00.000+0800"));
        repository.putEvent(sleepEvent("2026-05-25T12:00:00.000+0800", ""));
        repository.putEvent(babyEvent("pee", "2026-05-25T13:00:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.quick("pee", "偏多", ""))));
        repository.putEvent(babyEvent("poop", "2026-05-25T15:00:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.quick("poop", "软便", ""))));
        repository.putEvent(babyEvent("diaper", "2026-05-25T15:30:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.diaper("尿便混合", "量多", "黄色软便", ""))));
        JSONObject legacySmallPeePayload = new JSONObject();
        legacySmallPeePayload.put("diaperType", "小便");
        repository.putEvent(babyEvent("diaper", "2026-05-25T15:40:00.000+0800", legacySmallPeePayload));
        repository.putEvent(babyEvent("temperature", "2026-05-25T09:00:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.temperature("36.7", "腋温", ""))));
        repository.putEvent(babyEvent("temperature", "2026-05-25T14:00:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.temperature("37.4", "腋温", ""))));
        repository.putEvent(babyEvent("medication", "2026-05-25T16:00:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.medication("布洛芬", "2 ml", ""))));
        JSONObject milestonePayload = new JSONObject();
        milestonePayload.put("detail", "会翻身");
        repository.putEvent(babyEvent("milestone", "2026-05-25T17:00:00.000+0800", milestonePayload));
        repository.putEvent(babyEvent("growth", "2026-05-25T07:30:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.growth("7", "64", "41.5", ""))));
        repository.putEvent(babyEvent("growth", "2026-05-25T18:30:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.growth("7.2", "65", "42", "满月儿保"))));
        repository.putEvent(babyEvent("child_checkup", "2026-05-25T18:45:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.childCheckup("7.4", "66", "42.5", "社区儿保", "", "", ""))));
        repository.putEvent(babyEvent("ultrasound", "2026-05-25T18:00:00.000+0800", new JSONObject()));

        BabyLogService service = BabyLogService.forSmokeTest(repository);
        BabyLogDailyBabySummary day = service.dailyBabySummary("2026-05-25");
        assertEquals("2026-05-25", day.dateInput);
        assertEquals(5, day.feedCount);
        assertEquals(250, day.feedTotalMl);
        assertEquals(1, day.feedBreastCount);
        assertEquals(3, day.feedBottleCount);
        assertEquals(1, day.feedSolidCount);
        assertEquals("2026-05-25T11:40:00.000+0800", day.feedLastTime);
        assertEquals("奶瓶", day.feedLastType);
        assertEquals(40, day.feedLastAmountMl);
        assertEquals(450, day.sleepTotalMinutes);
        assertEquals(1, day.sleepIncompleteCount);
        assertEquals(450, day.sleepLongestMinutes);
        assertEquals("2026-05-26T06:30:00.000+0800", day.sleepLastTime);
        assertEquals(3, day.peeCount);
        assertEquals(2, day.poopCount);
        assertEquals(2, day.diaperCount);
        assertEquals("尿", day.diaperLastKind);
        assertEquals("2026-05-25T15:40:00.000+0800", day.diaperLastTime);
        assertEquals(37.4, day.temperatureMax);
        assertEquals(36.7, day.temperatureMin);
        assertEquals("2026-05-25T14:00:00.000+0800", day.temperatureLastTime);
        assertEquals("布洛芬", day.medicationLastName);
        assertEquals("2026-05-25T16:00:00.000+0800", day.medicationLastTime);
        assertEquals(1, day.milestoneCount);
        assertNear(7.4, day.growthWeightKg, 0.001);
        assertNear(66.0, day.growthHeightCm, 0.001);
        assertNear(42.5, day.growthHeadCircumferenceCm, 0.001);
        assertEquals("2026-05-25T18:45:00.000+0800", day.growthLastTime);

        BabyLogDailyBabySummary nextDay = service.dailyBabySummary("2026-05-26");
        assertEquals(0, nextDay.sleepTotalMinutes);
        assertEquals(0, nextDay.feedCount);
        assertEquals(0, nextDay.feedBreastCount);
        assertEquals(0, nextDay.feedBottleCount);
        assertEquals(0, nextDay.feedSolidCount);
        assertEquals("", nextDay.feedLastType);
        assertEquals(0, nextDay.feedLastAmountMl);
        assertTrue(Double.isNaN(nextDay.growthWeightKg));
        assertEquals("", nextDay.growthLastTime);
    }

    private static BabyLogDomain.BabyLogEvent babyEvent(String eventType, String occurredAt, JSONObject payload) {
        return BabyLogDomain.createEvent(eventType, occurredAt, payload, Collections.emptyList(), "manual");
    }

    private static void assertSuccessfulWriteTriggersSync() throws Exception {
        BabyLogRepository repository = BabyLogRepository.forSmokeTest();
        CountingSyncTrigger trigger = new CountingSyncTrigger();
        BabyLogService service = BabyLogService.forSmokeTest(repository, trigger);
        service.saveEventWithSyncChange(babyEvent(
                "note",
                "2026-06-09T09:00:00.000+0800",
                new JSONObject().put("detail", "sync trigger smoke")
        ));
        assertEquals(1, trigger.count);
    }

    private static void assertQuickSleepWakeClosure() throws Exception {
        BabyLogRepository repository = BabyLogRepository.forSmokeTest();
        BabyLogService service = BabyLogService.forSmokeTest(repository);
        BabyLogService.QuickAction sleepAction = new BabyLogService.QuickAction("睡眠", "", 0, "sleep");
        BabyLogService.QuickAction wakeAction = new BabyLogService.QuickAction("起床", "", 0, "wake");

        BabyLogDomain.BabyLogEvent quickSleep = service.recordQuickEvent(sleepAction);
        assertEquals(quickSleep.occurredAt, quickSleep.payload.optString("sleepStart"));
        String sleepStart = shiftIsoMinutes(quickSleep.occurredAt, -30);
        JSONObject openPayload = new JSONObject(quickSleep.payload.toString());
        openPayload.put("sleepStart", sleepStart);
        BabyLogDomain.BabyLogEvent openSleep = BabyLogService.createEditedEvent(
                quickSleep,
                "sleep",
                openPayload,
                quickSleep.attachmentIds,
                sleepStart
        );
        repository.putEvent(openSleep);

        BabyLogDomain.BabyLogEvent wake = service.recordQuickEvent(wakeAction);
        BabyLogDomain.BabyLogEvent closedSleep = repository.findEventById(openSleep.id);
        assertFalse(wake.payload.has("summary"));
        assertContains(BabyLogFormatters.eventSummary(wake), "已闭合睡眠段");
        assertEquals(wake.occurredAt, closedSleep.payload.optString("sleepEnd"));
        assertTrue(BabyLogService.sleepDurationMinutes(closedSleep).isPresent());
        assertTrue(BabyLogService.sleepDurationMinutes(closedSleep).getAsInt() > 0);
        assertEquals("已闭合睡眠段", wake.payload.optString("note"));
        BabyLogDailyBabySummary summary = service.dailyBabySummary(BabyLogFormatters.recordDay(sleepStart));
        assertTrue(summary.sleepTotalMinutes > 0);

        String closedAt = closedSleep.payload.optString("sleepEnd");
        BabyLogDomain.BabyLogEvent secondWake = service.recordQuickEvent(wakeAction);
        BabyLogDomain.BabyLogEvent afterSecondWake = repository.findEventById(openSleep.id);
        assertEquals(closedAt, afterSecondWake.payload.optString("sleepEnd"));
        assertEquals("", secondWake.payload.optString("note"));
    }

    private static void assertBabyCareOccurredTimeBackfill() throws Exception {
        BabyLogRepository repository = BabyLogRepository.forSmokeTest();
        BabyLogService service = BabyLogService.forSmokeTest(repository);
        BabyLogDomain.BabyLogEvent feed = service.recordBabyCareEvent(
                BabyLogService.BabyCareInput.feed("奶瓶", "120", "").withOccurredTime("03:00"),
                "2026-05-25"
        );
        assertEquals("2026-05-25T03:00:00.000+0800", feed.occurredAt);

        BabyLogDomain.BabyLogEvent yesterdayFeed = service.recordBabyCareEvent(
                BabyLogService.BabyCareInput.feed("奶瓶", "90", "")
                        .withOccurredDate("2026-05-24")
                        .withOccurredTime("20:00"),
                "2026-05-25"
        );
        assertEquals("2026-05-24T20:00:00.000+0800", yesterdayFeed.occurredAt);

        BabyLogBabyDayTimelineSlots.TimelineSlots slots =
                BabyLogBabyDayTimelineSlots.compute(repository.listEvents(), "2026-05-25");
        assertEquals(1, slots.eventPoints.size());
        assertEquals(180, slots.eventPoints.get(0).minuteOfDay);

        BabyLogDomain.BabyLogEvent edited = service.updateBabyCareEvent(
                feed.id,
                BabyLogService.BabyCareInput.feed("奶瓶", "150", "").withOccurredTime("04:15")
        );
        assertEquals("2026-05-25T04:15:00.000+0800", edited.occurredAt);
        assertEquals(feed.createdAt, edited.createdAt);

        BabyLogDomain.BabyLogEvent editedDate = service.updateBabyCareEvent(
                feed.id,
                BabyLogService.BabyCareInput.feed("奶瓶", "150", "")
                        .withOccurredDate("2026-05-26")
                        .withOccurredTime("04:15")
        );
        assertEquals("2026-05-26T04:15:00.000+0800", editedDate.occurredAt);
        assertEquals(feed.createdAt, editedDate.createdAt);
    }

    private static void assertQuickUndoUsesTrashDelete() throws Exception {
        BabyLogRepository repository = BabyLogRepository.forSmokeTest();
        BabyLogService service = BabyLogService.forSmokeTest(repository);
        BabyLogDomain.BabyLogEvent event = service.recordQuickEvent(
                new BabyLogService.QuickAction("尿尿", "一拍即记", 0, "pee")
        );
        assertFalse(event.payload.has("summary"));
        assertEquals("尿尿", BabyLogFormatters.eventSummary(event));
        BabyLogDomain.BabyLogEvent deleted = service.deleteEvent(event.id);
        assertTrue(deleted.deletedAt != null);
        assertEquals(0, service.listTimelineEvents().size());
        assertEquals(1, service.listTrashEvents().size());
        assertEquals(event.id, service.listTrashEvents().get(0).id);
    }

    private static void assertBackupRoundTripRestoresEventsAttachmentsAndUndo() throws Exception {
        BabyLogRepository repository = BabyLogRepository.forSmokeTest();
        BabyLogService service = BabyLogService.forSmokeTest(repository);
        BabyLogDomain.BabyLogEvent feed = babyEvent(
                "feed",
                "2026-06-10T08:00:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.feed("奶瓶", "120", ""))
        );
        BabyLogDomain.BabyLogEvent sleep = sleepEvent(
                "2026-06-10T12:30:00.000+0800",
                "2026-06-10T14:00:00.000+0800"
        );
        BabyLogDomain.BabyLogEvent diaper = babyEvent(
                "diaper",
                "2026-06-10T15:30:00.000+0800",
                BabyLogService.buildBabyCarePayload(BabyLogService.BabyCareInput.diaper("混合", "量中", "黄色软便", ""))
        );
        repository.putEvent(feed);
        repository.putEvent(sleep);
        repository.putEvent(diaper);

        byte[] blobBytes = new byte[] { 11, 12, 13, 14, 15 };
        BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.createAttachment(
                "document_image",
                "backup-smoke.jpg",
                "image/jpeg",
                blobBytes.length,
                ""
        );
        repository.putAttachment(attachment);
        assertTrue(repository.putAttachmentBlobFromRemote(attachment.id, blobBytes, "hash_backup_smoke"));

        String backup = service.createBackupJson();
        JSONObject data = new JSONObject(backup).getJSONObject("data");
        assertEquals(3, data.getJSONArray("events").length());
        assertEquals(1, data.getJSONArray("attachments").length());
        assertEquals(1, data.getJSONArray("attachmentBlobs").length());

        service.clearLocalData();
        assertEquals(0, repository.listEvents().size());
        assertEquals(0, repository.listAttachments().size());
        assertFalse(repository.hasAttachmentBlob(attachment.id));

        assertEquals(3, service.importBackupJson(backup));
        assertTrue(service.hasImportUndoSnapshot());
        assertEquals(3, repository.listEvents().size());
        assertEquals(1, repository.listAttachments().size());
        assertNotNull(repository.findEventById(feed.id));
        assertNotNull(repository.findEventById(sleep.id));
        assertNotNull(repository.findEventById(diaper.id));
        assertTrue(repository.hasAttachmentBlob(attachment.id));
        assertEquals("hash_backup_smoke", repository.attachmentBlobContentHash(attachment.id));
        assertTrue(Arrays.equals(blobBytes, repository.findAttachmentBlobBytes(attachment.id)));

        assertEquals(0, service.undoLastImport());
        assertFalse(service.hasImportUndoSnapshot());
        assertEquals(0, repository.listEvents().size());
        assertEquals(0, repository.listAttachments().size());
        assertFalse(repository.hasAttachmentBlob(attachment.id));
    }

    private static void assertServiceExceptionTypes() throws Exception {
        BabyLogService service = BabyLogService.forSmokeTest(BabyLogRepository.forSmokeTest());
        assertThrowsType(
                BabyLogException.ValidationException.class,
                () -> service.recordBabyCareEvent(BabyLogService.BabyCareInput.feed("", "", ""))
        );
        assertThrowsType(
                BabyLogException.NotFoundException.class,
                () -> service.deleteEvent("missing-event")
        );
        assertThrowsType(
                BabyLogException.ValidationException.class,
                () -> service.saveEventWithSyncChange(null)
        );
        assertThrowsType(
                BabyLogException.ValidationException.class,
                () -> service.importBackupJson("{}")
        );
    }

    private static String shiftIsoMinutes(String iso, int minutes) {
        long millis = BabyLogFormatters.parseIsoMillis(iso);
        if (millis <= 0L) {
            return iso;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return format.format(new Date(millis + minutes * 60_000L));
    }


    private static final class CountingSyncTrigger implements BabyLogSyncTrigger {
        int count;

        @Override
        public void triggerAfterLocalWrite() {
            count += 1;
        }
    }

    private static void assertRepositorySupportsAtomicEventAttachmentSyncWrites(BabyLogDomain.BabyLogEvent event) throws Exception {
        if ("__never__".equals(event.id)) {
            BabyLogRepository repository = null;
            boolean committed = repository.putEventWithAttachmentsAndSyncChanges(
                    event,
                    new ArrayList<BabyLogDomain.AttachmentRecord>(),
                    Arrays.asList(BabyLogDomain.createSyncChange("event", event.id, "upsert"))
            );
            if (committed) {
                throw new AssertionError("compile-time API guard should never execute");
            }
        }
    }

    private static void assertCreateEventPlansSyncChange(BabyLogDomain.BabyLogEvent event) {
        List<BabyLogDomain.SyncChange> changes = BabyLogService.createSyncChangesForEventUpsert(
                event,
                Collections.emptyList(),
                null
        );
        boolean found = false;
        for (BabyLogDomain.SyncChange change : changes) {
            if ("event".equals(change.entityType)
                    && event.id.equals(change.entityId)
                    && "upsert".equals(change.operation)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new AssertionError("created event must produce an upsert sync change");
        }
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
