import app.babylog.nativeapp.BabyLogDomain;
import app.babylog.nativeapp.BabyLogVisitSummaryExporter;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public final class BabyLogVisitSummaryExporterSmokeTest {
    public static void main(String[] args) throws Exception {
        BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.createAttachment(
                "document_image",
                "checkup.jpg",
                "image/jpeg",
                1024,
                "/tmp/checkup.jpg"
        );

        JSONObject checkupPayload = new JSONObject()
                .put("checkupDate", "2026-05-18")
                .put("gestationalAgeDays", 160)
                .put("provider", "奉化区妇幼")
                .put("systolicBp", 118)
                .put("diastolicBp", 76)
                .put("weightKg", 60.4)
                .put("fetalHeartRateBpm", 142)
                .put("doctorConclusion", "胎儿发育正常")
                .put("summary", "产检 · 22+6 周");
        BabyLogDomain.BabyLogEvent checkup = BabyLogDomain.createEvent(
                "pregnancy_checkup",
                "2026-05-18T12:00:00+0800",
                checkupPayload,
                Collections.singletonList(attachment.id),
                "manual"
        );

        JSONObject screeningPayload = new JSONObject()
                .put("screeningDate", "2026-05-05")
                .put("riskT21", "1:1000")
                .put("riskLevel", "低风险")
                .put("summary", "唐筛 · 低风险");
        BabyLogDomain.BabyLogEvent screening = BabyLogDomain.createEvent(
                "screening_serum",
                "2026-05-05T12:00:00+0800",
                screeningPayload,
                Collections.emptyList(),
                "manual"
        );

        JSONObject ultrasoundPayload = new JSONObject()
                .put("examDate", "2026-04-20")
                .put("bpdMm", 45)
                .put("hcMm", 176)
                .put("summary", "B 超 · BPD 45 mm");
        BabyLogDomain.BabyLogEvent ultrasound = BabyLogDomain.createEvent(
                "ultrasound",
                "2026-04-20T12:00:00+0800",
                ultrasoundPayload,
                Collections.emptyList(),
                "manual"
        );

        JSONObject contractionOnePayload = new JSONObject()
                .put("entryMode", "session")
                .put("sessionId", "contraction-session-1")
                .put("startIso", "2026-05-19T22:10:00+0800")
                .put("endIso", "2026-05-19T22:10:40+0800")
                .put("durationSec", 40)
                .put("summary", "宫缩 · 22:10-22:10 · 持续 40 秒");
        BabyLogDomain.BabyLogEvent contractionOne = BabyLogDomain.createEvent(
                "contraction",
                "2026-05-19T22:10:40+0800",
                contractionOnePayload,
                Collections.emptyList(),
                "manual"
        );
        JSONObject contractionTwoPayload = new JSONObject()
                .put("entryMode", "session")
                .put("sessionId", "contraction-session-1")
                .put("startIso", "2026-05-19T22:15:00+0800")
                .put("endIso", "2026-05-19T22:15:50+0800")
                .put("durationSec", 50)
                .put("intervalFromPrevSec", 300)
                .put("summary", "宫缩 · 22:15-22:15 · 持续 50 秒");
        BabyLogDomain.BabyLogEvent contractionTwo = BabyLogDomain.createEvent(
                "contraction",
                "2026-05-19T22:15:50+0800",
                contractionTwoPayload,
                Collections.emptyList(),
                "manual"
        );

        List<BabyLogDomain.BabyLogEvent> events = Arrays.asList(ultrasound, screening, checkup, contractionOne, contractionTwo);
        String markdown = BabyLogVisitSummaryExporter.buildMarkdown(
                events,
                Collections.singletonList(attachment),
                "",
                "",
                null
        );

        assertContains(markdown, BabyLogVisitSummaryExporter.DISCLAIMER_LINE);
        assertContains(markdown, "## 2026-05-18 · 产检（22+6 周 · 奉化区妇幼）");
        assertContains(markdown, "血压 118/76 mmHg");
        assertContains(markdown, "体重 60.4 kg");
        assertContains(markdown, "附件 1 张");
        assertContains(markdown, "分级 低风险（报告原文）");
        assertContains(markdown, "21 三体风险 1:1000（报告原文）");
        assertContains(markdown, "## 2026-05-19 · 宫缩会话");
        assertContains(markdown, "共 2 次；平均持续 45 秒；最短 40 秒；最长 50 秒");
        assertContains(markdown, "第 2 次：时间 22:15-22:15；持续 50 秒；距上次 300 秒");
        assertNotContains(markdown, "腹围");

        String filtered = BabyLogVisitSummaryExporter.buildMarkdown(
                events,
                Collections.singletonList(attachment),
                "2026-05-01",
                "2026-05-31",
                new HashSet<>(Collections.singletonList(BabyLogVisitSummaryExporter.CATEGORY_CHECKUP))
        );
        assertContains(filtered, "2026-05-18 · 产检");
        assertNotContains(filtered, "2026-04-20 · B 超");
        assertNotContains(filtered, "唐筛");
    }

    private static void assertContains(String haystack, String needle) {
        if (haystack == null || !haystack.contains(needle)) {
            throw new AssertionError("expected markdown to contain: " + needle + "\nActual:\n" + haystack);
        }
    }

    private static void assertNotContains(String haystack, String needle) {
        if (haystack != null && haystack.contains(needle)) {
            throw new AssertionError("expected markdown not to contain: " + needle + "\nActual:\n" + haystack);
        }
    }
}
