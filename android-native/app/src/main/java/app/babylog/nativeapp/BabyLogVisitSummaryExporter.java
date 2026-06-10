package app.babylog.nativeapp;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BabyLogVisitSummaryExporter {
    public static final String CATEGORY_CHECKUP = "checkup";
    public static final String CATEGORY_ULTRASOUND = "ultrasound";
    public static final String CATEGORY_MATERNAL_METRIC = "maternal_metric";
    public static final String CATEGORY_SCREENING = "screening";
    public static final String CATEGORY_FETAL_MOVEMENT = "fetal_movement";
    public static final String CATEGORY_CONTRACTION = "contraction";
    public static final String DISCLAIMER_LINE = "> 仅家庭记录摘要，所有数值/分级由用户或报告原文录入，未经医学判读。本应用非医疗器械。";
    public static final List<String> DEFAULT_CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            CATEGORY_CHECKUP,
            CATEGORY_ULTRASOUND,
            CATEGORY_MATERNAL_METRIC,
            CATEGORY_SCREENING,
            CATEGORY_FETAL_MOVEMENT,
            CATEGORY_CONTRACTION
    ));

    private BabyLogVisitSummaryExporter() {
    }

    public static String buildMarkdown(
            List<BabyLogDomain.BabyLogEvent> sourceEvents,
            List<BabyLogDomain.AttachmentRecord> sourceAttachments,
            String startDate,
            String endDate,
            Set<String> selectedCategories
    ) {
        return buildMarkdown(
                sourceEvents,
                sourceAttachments,
                startDate,
                endDate,
                selectedCategories,
                Collections.<BabyLogPreVisitQuestionStore.Question>emptyList()
        );
    }

    public static String buildMarkdown(
            List<BabyLogDomain.BabyLogEvent> sourceEvents,
            List<BabyLogDomain.AttachmentRecord> sourceAttachments,
            String startDate,
            String endDate,
            Set<String> selectedCategories,
            List<BabyLogPreVisitQuestionStore.Question> preVisitQuestions
    ) {
        List<BabyLogDomain.BabyLogEvent> events = sourceEvents == null
                ? Collections.emptyList()
                : BabyLogService.sortEventsNewestFirst(sourceEvents);
        Map<String, BabyLogDomain.AttachmentRecord> attachments = indexAttachments(sourceAttachments);
        Set<String> categories = selectedCategories == null
                ? new HashSet<>(DEFAULT_CATEGORIES)
                : new HashSet<>(selectedCategories);

        List<BabyLogDomain.BabyLogEvent> included = new ArrayList<>();
        for (BabyLogDomain.BabyLogEvent event : events) {
            if (event == null || event.deletedAt != null) {
                continue;
            }
            String category = categoryForEvent(event.eventType);
            if (category.isEmpty() || !categories.contains(category)) {
                continue;
            }
            String date = eventDateToken(event);
            if (!inRange(date, startDate, endDate)) {
                continue;
            }
            included.add(event);
        }

        StringBuilder markdown = new StringBuilder();
        markdown.append("# 栗记复诊汇总（").append(rangeLabel(startDate, endDate)).append("）\n");
        markdown.append(DISCLAIMER_LINE).append("\n");

        appendPreVisitQuestionSection(markdown, preVisitQuestions);
        appendDoctorInstructionSection(markdown, included);
        appendPendingReviewSection(markdown, included);
        appendReportExcerptSection(markdown, included);

        if (included.isEmpty()) {
            markdown.append("\n暂无符合条件的记录。\n");
            return markdown.toString();
        }

        Set<String> renderedContractionSessions = new HashSet<>();
        for (BabyLogDomain.BabyLogEvent event : included) {
            String sessionId = contractionSessionId(event);
            if (!sessionId.isEmpty()) {
                if (renderedContractionSessions.add(sessionId)) {
                    appendContractionSession(markdown, sessionId, contractionSessionEvents(included, sessionId));
                }
            } else {
                appendEvent(markdown, event, attachmentCount(event, attachments));
            }
        }
        return markdown.toString().trim() + "\n";
    }

    public static String categoryForEvent(String eventType) {
        if ("pregnancy_checkup".equals(eventType)) {
            return CATEGORY_CHECKUP;
        }
        if ("ultrasound".equals(eventType)) {
            return CATEGORY_ULTRASOUND;
        }
        if ("maternal_metric".equals(eventType)) {
            return CATEGORY_MATERNAL_METRIC;
        }
        if (BabyLogFormatters.isScreeningEventType(eventType)) {
            return CATEGORY_SCREENING;
        }
        if ("fetal_movement".equals(eventType)) {
            return CATEGORY_FETAL_MOVEMENT;
        }
        if ("contraction".equals(eventType)) {
            return CATEGORY_CONTRACTION;
        }
        return "";
    }

    public static String categoryLabel(String category) {
        if (CATEGORY_CHECKUP.equals(category)) return "产检";
        if (CATEGORY_ULTRASOUND.equals(category)) return "B 超";
        if (CATEGORY_MATERNAL_METRIC.equals(category)) return "孕妈指标";
        if (CATEGORY_SCREENING.equals(category)) return "筛查专项";
        if (CATEGORY_FETAL_MOVEMENT.equals(category)) return "胎动";
        if (CATEGORY_CONTRACTION.equals(category)) return "宫缩";
        return category == null ? "" : category;
    }

    private static void appendPreVisitQuestionSection(
            StringBuilder markdown,
            List<BabyLogPreVisitQuestionStore.Question> questions
    ) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        markdown.append("\n## 想问医生的问题\n");
        for (BabyLogPreVisitQuestionStore.Question question : questions) {
            if (question == null || isBlank(question.text)) {
                continue;
            }
            String prefix = isBlank(question.visitDate) ? "" : question.visitDate + "：";
            appendBullet(markdown, prefix + question.text);
        }
    }

    private static void appendDoctorInstructionSection(
            StringBuilder markdown,
            List<BabyLogDomain.BabyLogEvent> events
    ) {
        List<String> lines = new ArrayList<>();
        for (BabyLogDomain.BabyLogEvent event : events) {
            if (event == null || !"pregnancy_checkup".equals(event.eventType)) {
                continue;
            }
            JSONObject payload = event.payload == null ? new JSONObject() : event.payload;
            List<String> values = new ArrayList<>();
            addIfNotBlank(values, textWithLabel(payload, "doctorConclusion", "医生结论", false));
            addIfNotBlank(values, textWithLabel(payload, "diagnosisText", "医生结论", false));
            addIfNotBlank(values, textWithLabel(payload, "treatmentAdvice", "处理建议", false));
            if (!values.isEmpty()) {
                lines.add(eventDateToken(event) + "：" + join(values, "；"));
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        markdown.append("\n## 医生嘱咐\n");
        for (String line : lines) {
            appendBullet(markdown, line);
        }
    }

    private static void appendPendingReviewSection(
            StringBuilder markdown,
            List<BabyLogDomain.BabyLogEvent> events
    ) {
        List<String> lines = new ArrayList<>();
        for (BabyLogDomain.BabyLogEvent event : events) {
            if (event == null || event.payload == null) {
                continue;
            }
            int reviewCount = event.payload.optInt("reviewCount", 0);
            boolean hasMarker = containsReportOriginalMarker(event.payload);
            boolean hasReviewText = containsAnyPayloadText(event.payload, "待复核", "需核对");
            if (reviewCount > 0 || hasMarker || hasReviewText) {
                List<String> values = new ArrayList<>();
                if (reviewCount > 0) {
                    values.add("待核对 " + reviewCount + " 项");
                }
                if (hasMarker || hasReviewText) {
                    values.add("含需人工核对字段（报告原文）");
                }
                lines.add(eventDateToken(event) + " · " + BabyLogFormatters.eventLabel(event.eventType) + "：" + join(values, "；"));
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        markdown.append("\n## 待复核\n");
        for (String line : lines) {
            appendBullet(markdown, line);
        }
    }

    private static void appendReportExcerptSection(
            StringBuilder markdown,
            List<BabyLogDomain.BabyLogEvent> events
    ) {
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        for (BabyLogDomain.BabyLogEvent event : events) {
            if (event == null || event.payload == null) {
                continue;
            }
            for (String excerpt : reportExcerptLines(event)) {
                if (!isBlank(excerpt)) {
                    lines.add(eventDateToken(event) + " · " + BabyLogFormatters.eventLabel(event.eventType) + "：" + excerpt);
                }
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        markdown.append("\n## 报告原文摘录\n");
        for (String line : lines) {
            appendBullet(markdown, line);
        }
    }

    private static void appendEvent(StringBuilder markdown, BabyLogDomain.BabyLogEvent event, int attachmentCount) {
        JSONObject payload = event.payload == null ? new JSONObject() : event.payload;
        String date = eventDateToken(event);
        String title = date + " · " + BabyLogFormatters.eventLabel(event.eventType);
        String titleMeta = titleMeta(payload, event.eventType);
        markdown.append("\n## ").append(title);
        if (!isBlank(titleMeta)) {
            markdown.append("（").append(titleMeta).append("）");
        }
        markdown.append("\n");

        List<String> lines = eventLines(event, payload);
        if (lines.isEmpty()) {
            String summary = BabyLogFormatters.eventSummary(event);
            if (!isBlank(summary) && !summary.contains("待补充详情")) {
                lines.add(summary);
            }
        }
        for (String line : lines) {
            appendBullet(markdown, line);
        }
        if (attachmentCount > 0) {
            appendBullet(markdown, "附件 " + attachmentCount + " 张");
        }
    }

    private static void appendContractionSession(
            StringBuilder markdown,
            String sessionId,
            List<BabyLogDomain.BabyLogEvent> sessionEvents
    ) {
        if (sessionEvents.isEmpty()) {
            return;
        }
        BabyLogDomain.BabyLogEvent first = sessionEvents.get(0);
        markdown.append("\n## ").append(eventDateToken(first)).append(" · 宫缩会话\n");
        List<Integer> durations = new ArrayList<>();
        for (BabyLogDomain.BabyLogEvent event : sessionEvents) {
            JSONObject payload = event.payload == null ? new JSONObject() : event.payload;
            if (payload.has("durationSec")) {
                durations.add(payload.optInt("durationSec"));
            } else if (payload.has("durationSeconds")) {
                durations.add((int) Math.round(payload.optDouble("durationSeconds")));
            }
        }
        if (!durations.isEmpty()) {
            int min = Collections.min(durations);
            int max = Collections.max(durations);
            int sum = 0;
            for (Integer duration : durations) {
                sum += duration == null ? 0 : duration;
            }
            appendBullet(markdown, "共 " + sessionEvents.size() + " 次；平均持续 " + Math.round((double) sum / durations.size()) + " 秒；最短 " + min + " 秒；最长 " + max + " 秒");
        } else {
            appendBullet(markdown, "共 " + sessionEvents.size() + " 次");
        }
        for (int i = 0; i < sessionEvents.size(); i++) {
            JSONObject payload = sessionEvents.get(i).payload == null ? new JSONObject() : sessionEvents.get(i).payload;
            List<String> values = new ArrayList<>();
            String start = BabyLogFormatters.formatEventTime(payload.optString("startIso"));
            String end = BabyLogFormatters.formatEventTime(payload.optString("endIso"));
            if (!"--:--".equals(start) || !"--:--".equals(end)) {
                values.add("时间 " + start + ("--:--".equals(end) ? "" : "-" + end));
            }
            if (payload.has("durationSec")) {
                values.add("持续 " + payload.optInt("durationSec") + " 秒");
            }
            if (payload.has("intervalFromPrevSec")) {
                values.add("距上次 " + payload.optInt("intervalFromPrevSec") + " 秒");
            }
            if (!values.isEmpty()) {
                appendBullet(markdown, "第 " + (i + 1) + " 次：" + join(values, "；"));
            }
        }
    }

    private static List<String> eventLines(BabyLogDomain.BabyLogEvent event, JSONObject payload) {
        if ("pregnancy_checkup".equals(event.eventType)) {
            return checkupLines(payload);
        }
        if ("ultrasound".equals(event.eventType)) {
            return ultrasoundLines(payload);
        }
        if ("maternal_metric".equals(event.eventType)) {
            return maternalMetricLines(payload);
        }
        if (BabyLogFormatters.isScreeningEventType(event.eventType)) {
            return screeningLines(event.eventType, payload);
        }
        if ("fetal_movement".equals(event.eventType)) {
            return fetalMovementLines(payload);
        }
        if ("contraction".equals(event.eventType)) {
            return contractionLines(payload);
        }
        return Collections.emptyList();
    }

    private static List<String> checkupLines(JSONObject payload) {
        List<String> lines = new ArrayList<>();
        List<String> vitals = new ArrayList<>();
        String bp = bloodPressure(payload);
        addIfNotBlank(vitals, bp);
        addIfNotBlank(vitals, valueWithUnit(payload, "weightKg", "体重", "kg"));
        addIfNotBlank(vitals, valueWithUnit(payload, "fundalHeightCm", "宫高", "cm"));
        addIfNotBlank(vitals, valueWithUnit(payload, "abdominalCircumferenceCm", "腹围", "cm"));
        addIfNotBlank(vitals, valueWithUnit(payload, "fetalHeartRateBpm", "胎心率", "bpm"));
        addIfNotBlank(vitals, textWithLabel(payload, "fetalPresentation", "胎位", false));
        addIfNotBlank(vitals, textWithLabel(payload, "edema", "水肿", false));
        addIfNotBlank(vitals, textWithLabel(payload, "urineRoutine", "尿常规", false));
        addIfNotBlank(vitals, textWithLabel(payload, "urineProtein", "尿蛋白", false));
        addIfNotBlank(vitals, valueWithUnit(payload, "hemoglobinGL", "血红蛋白", "g/L"));
        if (!vitals.isEmpty()) {
            lines.add(join(vitals, "；"));
        }

        List<String> findings = new ArrayList<>();
        addIfNotBlank(findings, textWithLabel(payload, "department", "科室", false));
        addIfNotBlank(findings, textWithLabel(payload, "reportType", "报告类型", false));
        addIfNotBlank(findings, textWithLabel(payload, "highRiskFactors", "高危因素", false));
        addIfNotBlank(findings, textWithLabel(payload, "doctorConclusion", "医生结论", false));
        addIfNotBlank(findings, textWithLabel(payload, "treatmentAdvice", "处理建议", false));
        String nextVisit = text(payload, "nextVisitDate");
        if (!isBlank(nextVisit)) {
            String note = text(payload, "nextVisitNote");
            findings.add("下次产检 " + nextVisit + (isBlank(note) || nextVisit.equals(note) ? "" : "（" + note + "）"));
        }
        addIfNotBlank(findings, textWithLabel(payload, "note", "备注", false));
        addIfNotBlank(findings, textWithLabel(payload, "attachmentNote", "附件备注", false));
        if (!findings.isEmpty()) {
            lines.add(join(findings, "；"));
        }
        return lines;
    }

    private static List<String> ultrasoundLines(JSONObject payload) {
        List<String> lines = new ArrayList<>();
        List<String> growth = new ArrayList<>();
        addIfNotBlank(growth, valueWithUnit(payload, "bpdMm", "BPD", "mm"));
        addIfNotBlank(growth, valueWithUnit(payload, "hcMm", "HC", "mm"));
        addIfNotBlank(growth, valueWithUnit(payload, "acMm", "AC", "mm"));
        addIfNotBlank(growth, valueWithUnit(payload, "flMm", "FL", "mm"));
        addIfNotBlank(growth, valueWithUnit(payload, "efwGram", "EFW", "g"));
        if (!growth.isEmpty()) {
            lines.add(join(growth, "，"));
        }

        List<String> clinical = new ArrayList<>();
        addIfNotBlank(clinical, valueWithUnit(payload, "afiCm", "AFI", "cm"));
        addIfNotBlank(clinical, valueWithUnit(payload, "deepestPocketCm", "最大羊水池", "cm"));
        addIfNotBlank(clinical, textWithLabel(payload, "placentaLocation", "胎盘位置", false));
        addIfNotBlank(clinical, textWithLabel(payload, "placentaGrade", "胎盘成熟度", false));
        addIfNotBlank(clinical, textWithLabel(payload, "fetalPresentation", "胎位", false));
        addIfNotBlank(clinical, valueWithUnit(payload, "fetalHeartRateBpm", "胎心率", "bpm"));
        addIfNotBlank(clinical, textWithLabel(payload, "fetalCount", "胎儿个数", false));
        addIfNotBlank(clinical, textWithLabel(payload, "fetalMovement", "胎动", false));
        addIfNotBlank(clinical, textWithLabel(payload, "umbilicalInsertion", "脐带插入处", false));
        addIfNotBlank(clinical, valueWithUnit(payload, "cervicalLengthMm", "宫颈管长度", "mm"));
        addIfNotBlank(clinical, valueWithUnit(payload, "crlMm", "CRL", "mm"));
        addIfNotBlank(clinical, valueWithUnit(payload, "ntMm", "NT", "mm"));
        addIfNotBlank(clinical, valueWithUnit(payload, "umbilicalSd", "脐血流 S/D", ""));
        addIfNotBlank(clinical, valueWithUnit(payload, "umbilicalPi", "脐血流 PI", ""));
        addIfNotBlank(clinical, valueWithUnit(payload, "umbilicalRi", "脐血流 RI", ""));
        if (!clinical.isEmpty()) {
            lines.add(join(clinical, "；"));
        }

        addIfNotBlank(lines, textWithLabel(payload, "diagnosisText", "超声诊断", false));
        addIfNotBlank(lines, textWithLabel(payload, "clinicalDetails", "备注", false));
        return lines;
    }

    private static List<String> maternalMetricLines(JSONObject payload) {
        List<String> lines = new ArrayList<>();
        List<String> metrics = new ArrayList<>();
        addIfNotBlank(metrics, valueWithUnit(payload, "weightKg", "体重", "kg"));
        addIfNotBlank(metrics, bloodPressure(payload));
        String glucose = valueWithUnit(payload, "glucoseMmolL", "血糖", "mmol/L");
        if (!isBlank(glucose)) {
            String context = BabyLogFormatters.maternalGlucoseContextLabel(text(payload, "glucoseContext"));
            metrics.add(isBlank(context) ? glucose : glucose.replace("血糖 ", "血糖（" + context + "）"));
        }
        if (!metrics.isEmpty()) {
            lines.add(join(metrics, "；"));
        }
        addIfNotBlank(lines, textWithLabel(payload, "note", "备注", false));
        return lines;
    }

    private static List<String> screeningLines(String eventType, JSONObject payload) {
        List<String> lines = new ArrayList<>();
        List<String> values = new ArrayList<>();
        if ("screening_nt".equals(eventType)) {
            addIfNotBlank(values, valueWithUnit(payload, "ntMm", "NT", "mm"));
            addIfNotBlank(values, reportText(payload, "conclusion", "结论"));
        } else if ("screening_serum".equals(eventType)) {
            addIfNotBlank(values, reportText(payload, "riskLevel", "分级"));
            addIfNotBlank(values, reportText(payload, "riskT21", "21 三体风险"));
            addIfNotBlank(values, reportText(payload, "riskT18", "18 三体风险"));
            addIfNotBlank(values, reportText(payload, "riskOntd", "开放性神经管风险"));
        } else if ("screening_nipt".equals(eventType)) {
            addIfNotBlank(values, reportText(payload, "t21Result", "T21"));
            addIfNotBlank(values, reportText(payload, "t18Result", "T18"));
            addIfNotBlank(values, reportText(payload, "t13Result", "T13"));
            addIfNotBlank(values, reportText(payload, "sexChromosome", "性染色体"));
            addIfNotBlank(values, reportText(payload, "conclusion", "结论"));
        } else if ("screening_anomaly".equals(eventType)) {
            addIfNotBlank(values, reportText(payload, "structureConclusion", "结构结论"));
            addIfNotBlank(values, reportText(payload, "conclusion", "结论"));
        } else if ("screening_ogtt".equals(eventType)) {
            addIfNotBlank(values, valueWithUnit(payload, "fastingGlucoseMmolL", "空腹", "mmol/L"));
            addIfNotBlank(values, valueWithUnit(payload, "oneHourGlucoseMmolL", "1h", "mmol/L"));
            addIfNotBlank(values, valueWithUnit(payload, "twoHourGlucoseMmolL", "2h", "mmol/L"));
            addIfNotBlank(values, reportText(payload, "abnormalFlag", "报告标注"));
        } else if ("screening_gbs".equals(eventType)) {
            addIfNotBlank(values, reportText(payload, "gbsResult", "GBS"));
        } else if ("screening_nst".equals(eventType)) {
            addIfNotBlank(values, reportText(payload, "nstResult", "胎心监护"));
        }
        if (!values.isEmpty()) {
            lines.add(join(values, "；"));
        }
        addIfNotBlank(lines, textWithLabel(payload, "note", "备注", false));
        addIfNotBlank(lines, textWithLabel(payload, "attachmentNote", "附件备注", false));
        return lines;
    }

    private static List<String> reportExcerptLines(BabyLogDomain.BabyLogEvent event) {
        JSONObject payload = event.payload == null ? new JSONObject() : event.payload;
        List<String> lines = new ArrayList<>();
        if (BabyLogFormatters.isScreeningEventType(event.eventType)) {
            for (String line : screeningLines(event.eventType, payload)) {
                if (line.contains("报告原文")) {
                    lines.add(line);
                }
            }
        } else if ("ultrasound".equals(event.eventType)) {
            addIfNotBlank(lines, reportText(payload, "diagnosisText", "超声诊断"));
            addIfNotBlank(lines, reportText(payload, "clinicalDetails", "备注"));
            addIfNotBlank(lines, reportText(payload, "warningText", "待核对提示"));
            addIfNotBlank(lines, reportText(payload, "structureConclusion", "结构结论"));
            addIfNotBlank(lines, reportText(payload, "conclusion", "结论"));
        }
        return lines;
    }

    private static List<String> fetalMovementLines(JSONObject payload) {
        List<String> lines = new ArrayList<>();
        List<String> values = new ArrayList<>();
        addIfNotBlank(values, textWithLabel(payload, "movementWindow", "时段", false));
        addIfNotBlank(values, valueWithUnit(payload, "movementCount", "胎动", "次"));
        addIfNotBlank(values, textWithLabel(payload, "startedAt", "开始", false));
        addIfNotBlank(values, textWithLabel(payload, "endedAt", "结束", false));
        addIfNotBlank(values, valueWithUnit(payload, "durationMinutes", "持续", "分钟"));
        if (!values.isEmpty()) {
            lines.add(join(values, "；"));
        }
        addIfNotBlank(lines, textWithLabel(payload, "note", "备注", false));
        return lines;
    }

    private static List<String> contractionLines(JSONObject payload) {
        List<String> lines = new ArrayList<>();
        List<String> values = new ArrayList<>();
        addIfNotBlank(values, textWithLabel(payload, "contractionStart", "开始", false));
        addIfNotBlank(values, textWithLabel(payload, "startIso", "开始", false));
        addIfNotBlank(values, textWithLabel(payload, "endIso", "结束", false));
        addIfNotBlank(values, valueWithUnit(payload, "intervalMinutes", "间隔", "分钟"));
        addIfNotBlank(values, valueWithUnit(payload, "intervalFromPrevSec", "距上次", "秒"));
        addIfNotBlank(values, valueWithUnit(payload, "durationSeconds", "持续", "秒"));
        addIfNotBlank(values, valueWithUnit(payload, "durationSec", "持续", "秒"));
        if (!values.isEmpty()) {
            lines.add(join(values, "；"));
        }
        addIfNotBlank(lines, textWithLabel(payload, "note", "备注", false));
        return lines;
    }

    private static String contractionSessionId(BabyLogDomain.BabyLogEvent event) {
        if (event == null || !"contraction".equals(event.eventType) || event.payload == null) {
            return "";
        }
        return text(event.payload, "sessionId");
    }

    private static List<BabyLogDomain.BabyLogEvent> contractionSessionEvents(
            List<BabyLogDomain.BabyLogEvent> events,
            String sessionId
    ) {
        List<BabyLogDomain.BabyLogEvent> result = new ArrayList<>();
        if (isBlank(sessionId)) {
            return result;
        }
        for (BabyLogDomain.BabyLogEvent event : events) {
            if (sessionId.equals(contractionSessionId(event))) {
                result.add(event);
            }
        }
        Collections.sort(result, new Comparator<BabyLogDomain.BabyLogEvent>() {
            @Override
            public int compare(BabyLogDomain.BabyLogEvent left, BabyLogDomain.BabyLogEvent right) {
                return Long.compare(contractionStartMillis(left), contractionStartMillis(right));
            }
        });
        return result;
    }

    private static long contractionStartMillis(BabyLogDomain.BabyLogEvent event) {
        if (event == null) {
            return 0L;
        }
        JSONObject payload = event.payload == null ? new JSONObject() : event.payload;
        long start = BabyLogFormatters.parseIsoMillis(payload.optString("startIso"));
        return start > 0L ? start : BabyLogFormatters.parseIsoMillis(event.occurredAt);
    }

    private static String titleMeta(JSONObject payload, String eventType) {
        List<String> meta = new ArrayList<>();
        addIfNotBlank(meta, gestationalAge(payload));
        if ("ultrasound".equals(eventType)) {
            addIfNotBlank(meta, text(payload, "hospital"));
        } else {
            addIfNotBlank(meta, text(payload, "provider"));
        }
        return join(meta, " · ");
    }

    private static String gestationalAge(JSONObject payload) {
        if (payload == null) {
            return "";
        }
        if (payload.has("gestationalAgeDays")) {
            return BabyLogFormatters.formatGestationalAge(payload.optInt("gestationalAgeDays"));
        }
        return text(payload, "gestationalAge");
    }

    private static String bloodPressure(JSONObject payload) {
        String systolic = number(payload, "systolicBp");
        String diastolic = number(payload, "diastolicBp");
        if (isBlank(systolic) || isBlank(diastolic)) {
            return "";
        }
        return "血压 " + systolic + "/" + diastolic + " mmHg";
    }

    private static String valueWithUnit(JSONObject payload, String key, String label, String unit) {
        String value = number(payload, key);
        if (isBlank(value)) {
            return "";
        }
        return label + " " + value + (isBlank(unit) ? "" : " " + unit);
    }

    private static String reportText(JSONObject payload, String key, String label) {
        return textWithLabel(payload, key, label, true);
    }

    private static String textWithLabel(JSONObject payload, String key, String label, boolean reportOriginal) {
        String value = text(payload, key);
        if (isBlank(value)) {
            return "";
        }
        return label + " " + value + (reportOriginal ? "（报告原文）" : "");
    }

    private static boolean containsReportOriginalMarker(JSONObject payload) {
        return containsAnyPayloadText(payload, "报告原文");
    }

    private static boolean containsAnyPayloadText(JSONObject payload, String... needles) {
        if (payload == null || needles == null || needles.length == 0) {
            return false;
        }
        java.util.Iterator<String> keys = payload.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = payload.opt(key);
            if (value == null || JSONObject.NULL.equals(value)) {
                continue;
            }
            String text = String.valueOf(value);
            for (String needle : needles) {
                if (!isBlank(needle) && text.contains(needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String text(JSONObject payload, String key) {
        if (payload == null || !payload.has(key) || payload.isNull(key)) {
            return "";
        }
        String value = payload.optString(key, "").trim();
        return isBlank(value) || "/".equals(value) || "-".equals(value) || "—".equals(value) ? "" : value;
    }

    private static String number(JSONObject payload, String key) {
        if (payload == null || !payload.has(key) || payload.isNull(key)) {
            return "";
        }
        Object value = payload.opt(key);
        if (value instanceof Number) {
            return BabyLogFormatters.formatNumber(((Number) value).doubleValue());
        }
        Double parsed = BabyLogFormatters.parseOptionalNumber(String.valueOf(value));
        return parsed == null ? "" : BabyLogFormatters.formatNumber(parsed);
    }

    private static String eventDateToken(BabyLogDomain.BabyLogEvent event) {
        String payloadDate = payloadDateToken(event);
        if (!isBlank(payloadDate)) {
            return payloadDate;
        }
        return dateToken(event == null ? "" : event.occurredAt);
    }

    private static String payloadDateToken(BabyLogDomain.BabyLogEvent event) {
        if (event == null || event.payload == null) {
            return "";
        }
        String[] keys = {"examDate", "checkupDate", "screeningDate"};
        for (String key : keys) {
            String value = dateToken(text(event.payload, key));
            if (!isBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private static String dateToken(String value) {
        if (value == null || value.length() < 10) {
            return "";
        }
        return value.substring(0, 10);
    }

    private static boolean inRange(String eventDate, String startDate, String endDate) {
        if (!isBlank(startDate) && !isBlank(eventDate) && eventDate.compareTo(startDate) < 0) {
            return false;
        }
        if (!isBlank(endDate) && !isBlank(eventDate) && eventDate.compareTo(endDate) > 0) {
            return false;
        }
        return true;
    }

    private static String rangeLabel(String startDate, String endDate) {
        if (!isBlank(startDate) && !isBlank(endDate)) {
            return startDate + " 至 " + endDate;
        }
        if (!isBlank(startDate)) {
            return startDate + " 起";
        }
        if (!isBlank(endDate)) {
            return "截至 " + endDate;
        }
        return "全部记录";
    }

    private static int attachmentCount(BabyLogDomain.BabyLogEvent event, Map<String, BabyLogDomain.AttachmentRecord> attachments) {
        if (event == null || event.attachmentIds == null || attachments == null || attachments.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String id : event.attachmentIds) {
            BabyLogDomain.AttachmentRecord attachment = attachments.get(id);
            if (attachment != null && attachment.deletedAt == null) {
                count++;
            }
        }
        return count;
    }

    private static Map<String, BabyLogDomain.AttachmentRecord> indexAttachments(List<BabyLogDomain.AttachmentRecord> attachments) {
        Map<String, BabyLogDomain.AttachmentRecord> indexed = new HashMap<>();
        if (attachments == null) {
            return indexed;
        }
        for (BabyLogDomain.AttachmentRecord attachment : attachments) {
            if (attachment != null) {
                indexed.put(attachment.id, attachment);
            }
        }
        return indexed;
    }

    private static void appendBullet(StringBuilder markdown, String line) {
        if (!isBlank(line)) {
            markdown.append("- ").append(line.trim()).append("\n");
        }
    }

    private static void addIfNotBlank(List<String> target, String value) {
        if (!isBlank(value)) {
            target.add(value.trim());
        }
    }

    private static String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (isBlank(value)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(value.trim());
        }
        return builder.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
