import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogFormatters;
import app.babylog.nativeapp.BabyLogDomain;

public final class BabyLogFormattersSmokeTest {
    public static void main(String[] args) {
        assertEquals(199, BabyLogFormatters.parseGestationalAgeDays("28+3"));
        assertEquals(199, BabyLogFormatters.parseGestationalAgeDays("28＋3"));
        assertEquals(199, BabyLogFormatters.parseGestationalAgeDays("28周3"));
        assertEquals(199, BabyLogFormatters.parseGestationalAgeDays("28周3天"));
        assertEquals(199, BabyLogFormatters.parseGestationalAgeDays("28w3"));
        assertEquals(199, BabyLogFormatters.parseGestationalAgeDays("28W3D"));
        assertEquals(196, BabyLogFormatters.parseGestationalAgeDays("28"));
        assertEquals(196, BabyLogFormatters.parseGestationalAgeDays("28周"));
        assertEquals(null, BabyLogFormatters.parseGestationalAgeDays("28+x"));
        assertEquals(null, BabyLogFormatters.parseOptionalNumber("NaN"));
        assertEquals(null, BabyLogFormatters.parseOptionalNumber("Infinity"));
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
        String[][] eventGroups = {
                {"pregnancy_checkup", "checkup"},
                {"screening_nt", "checkup"},
                {"screening_serum", "checkup"},
                {"screening_nipt", "checkup"},
                {"screening_anomaly", "checkup"},
                {"screening_ogtt", "checkup"},
                {"screening_gbs", "checkup"},
                {"screening_nst", "checkup"},
                {"ultrasound", "ultrasound"},
                {"fetal_movement", "pregnancy"},
                {"contraction", "pregnancy"},
                {"maternal_metric", "pregnancy"},
                {"birth", "baby"},
                {"feed", "baby"},
                {"breastfeed", "baby"},
                {"bottle", "baby"},
                {"sleep", "baby"},
                {"wake", "baby"},
                {"diaper", "baby"},
                {"pee", "baby"},
                {"poop", "baby"},
                {"temperature", "temperature"},
                {"medication", "baby"},
                {"illness", "baby"},
                {"growth", "baby"},
                {"child_checkup", "baby"},
                {"vaccine", "baby"},
                {"milestone", "baby"},
                {"note", "all"}
        };
        assertEquals(BabyLogDomain.EVENT_TYPES.length, eventGroups.length);
        for (String[] eventGroup : eventGroups) {
            assertEventGroup(eventGroup[0], eventGroup[1]);
        }
        assertEquals("孕妈指标", BabyLogFormatters.eventLabel("maternal_metric"));
        assertEquals(
                "空腹血糖高于 5.1 mmol/L；非诊断，仅提示，请遵医嘱",
                BabyLogFormatters.formatMaternalGlucoseWarning(5.2, "fasting")
        );
        assertEquals(
                "餐后1h血糖高于 10.0 mmol/L；非诊断，仅提示，请遵医嘱",
                BabyLogFormatters.formatMaternalGlucoseWarning(10.1, "after_1h")
        );
        assertEquals(
                "餐后2h血糖高于 8.5 mmol/L；非诊断，仅提示，请遵医嘱",
                BabyLogFormatters.formatMaternalGlucoseWarning(8.6, "after_2h")
        );
        assertEquals("", BabyLogFormatters.formatMaternalGlucoseWarning(7.0, "random"));
        assertEquals("2026-05-17", BabyLogFormatters.recordDay("2026-05-18T02:30:00.000+0800", 4));
        assertEquals("2026-05-18", BabyLogFormatters.recordDay("2026-05-18T04:00:00.000+0800", 4));
        assertEquals("2026-05-17", BabyLogFormatters.offsetDateInput("2026-05-18", -1));
        assertEquals("2026-05-19", BabyLogFormatters.offsetDateInput("2026-05-18", 1));
        assertEquals("bad-date", BabyLogFormatters.offsetDateInput("bad-date", 1));
        assertEquals(1, BabyLogFormatters.daysBetweenDateInputs("2026-03-08", "2026-03-09"));
        assertEquals(280, BabyLogFormatters.daysBetweenDateInputs("2025-12-10", "2026-09-16"));
        assertEquals(0, BabyLogFormatters.daysBetweenDateInputs("bad-date", "2026-09-16"));
        long nowMillis = BabyLogFormatters.parseIsoMillis("2026-05-25T12:00:00.000+0800");
        assertEquals("刚刚", BabyLogFormatters.relativeTimeFromNow("2026-05-25T11:59:56.000+0800", nowMillis));
        assertEquals("12 秒前", BabyLogFormatters.relativeTimeFromNow("2026-05-25T11:59:48.000+0800", nowMillis));
        assertEquals("5 分钟前", BabyLogFormatters.relativeTimeFromNow("2026-05-25T11:55:00.000+0800", nowMillis));
        assertEquals("3 小时前", BabyLogFormatters.relativeTimeFromNow("2026-05-25T09:00:00.000+0800", nowMillis));
        assertEquals("2 天前", BabyLogFormatters.relativeTimeFromNow("2026-05-23T12:00:00.000+0800", nowMillis));
        assertEquals("2026-05-17", BabyLogFormatters.relativeTimeFromNow("2026-05-17T12:00:00.000+0800", nowMillis));
        assertEquals("pregnancy", BabyLogFormatters.resolveCareStage(
                BabyLogDomain.ChildProfile.createForNewFamily("栗子", "female", "2026-08-05", "", "auto", true),
                "2026-05-18"
        ));
        assertEquals("baby", BabyLogFormatters.resolveCareStage(
                BabyLogDomain.ChildProfile.createForNewFamily("栗子", "female", "2026-08-05", "2026-05-01", "auto", true),
                "2026-05-18"
        ));
        assertEquals("pregnancy", BabyLogFormatters.resolveCareStage(
                BabyLogDomain.ChildProfile.createForNewFamily("栗子", "female", "2026-08-05", "2026-05-01", "pregnancy", true),
                "2026-05-18"
        ));
        assertEquals("unknown", BabyLogFormatters.resolveCareStage(
                BabyLogDomain.ChildProfile.createForNewFamily("", "unknown", "", "", "auto", true),
                "2026-05-18"
        ));
        assertEquals(BabyLogDomain.STAGE_PREGNANCY_ENDED, BabyLogFormatters.resolveCareStage(
                BabyLogDomain.ChildProfile.createForNewFamily("栗子", "female", "2026-08-05", "2026-05-01", BabyLogDomain.STAGE_PREGNANCY_ENDED, true),
                "2026-05-18"
        ));
        assertEquals(BabyLogDomain.STAGE_PAUSED, BabyLogFormatters.resolveCareStage(
                BabyLogDomain.ChildProfile.createForNewFamily("栗子", "female", "2026-08-05", "2026-05-01", BabyLogDomain.STAGE_PAUSED, true),
                "2026-05-18"
        ));
        assertEquals(true, BabyLogFormatters.shouldMutePregnancyDerivedUi(BabyLogDomain.STAGE_PREGNANCY_ENDED));
        assertEquals(true, BabyLogFormatters.shouldMutePregnancyDerivedUi(BabyLogDomain.STAGE_PAUSED));
        assertEquals(false, BabyLogFormatters.shouldMutePregnancyDerivedUi(BabyLogDomain.STAGE_PREGNANCY));
        assertEquals(false, BabyLogFormatters.shouldMutePregnancyDerivedUi(BabyLogDomain.STAGE_BABY));
        if (!BabyLogFormatters.matchesTimelineFilter("feed", "baby")) {
            throw new AssertionError("feed should match baby filter");
        }
        if (!BabyLogFormatters.matchesTimelineFilter("ultrasound", "pregnancy")) {
            throw new AssertionError("ultrasound should match pregnancy filter");
        }
        if (!BabyLogFormatters.matchesTimelineFilter("pregnancy_checkup", "pregnancy")) {
            throw new AssertionError("pregnancy_checkup should match pregnancy filter");
        }
        if (!BabyLogFormatters.matchesTimelineFilter("screening_ogtt", "pregnancy")
                || !BabyLogFormatters.matchesTimelineFilter("screening_ogtt", "checkup")) {
            throw new AssertionError("screening_ogtt should match pregnancy and checkup filters");
        }
        if (!BabyLogFormatters.matchesTimelineFilter("temperature", "baby")) {
            throw new AssertionError("temperature should match baby filter");
        }
        if (BabyLogFormatters.matchesTimelineFilter("feed", "pregnancy")) {
            throw new AssertionError("feed should not match pregnancy filter");
        }
        assertEquals("2026-05-18T12:00:00.000+0800", BabyLogFormatters.createOccurredAtFromDate("2026-05-18"));
        if (!BabyLogFormatters.isValidDateInput("2026-05-15")) {
            throw new AssertionError("valid date rejected");
        }
        if (BabyLogFormatters.isValidDateInput("2026-99-99")) {
            throw new AssertionError("invalid date accepted");
        }
        assertEquals("120ml", BabyLogFormatters.detailOnlySummary("奶瓶 · 120ml", "bottle"));
        assertEquals("", BabyLogFormatters.detailOnlySummary("母乳 · 待补充详情", "breastfeed"));
        assertEquals("", BabyLogFormatters.detailOnlySummary("便便", "poop"));
        assertEquals("", BabyLogFormatters.detailOnlySummary(null, "poop"));
        assertEquals("第一次笑", BabyLogFormatters.detailOnlySummary("里程碑 · 第一次笑", "milestone"));
        assertEquals("自定义摘要", BabyLogFormatters.detailOnlySummary("自定义摘要", "note"));
    }


    private static void assertEventGroup(String eventType, String expectedGroup) {
        assertEquals(expectedGroup, BabyLogFormatters.timelineFilterGroup(eventType));
    }
}
