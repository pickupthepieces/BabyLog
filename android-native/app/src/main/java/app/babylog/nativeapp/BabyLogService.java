package app.babylog.nativeapp;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BabyLogService {
    public static final String BACKUP_FORMAT = "babylog.backup";
    public static final int BACKUP_VERSION = 1;
    public static final int TRASH_RETENTION_DAYS = 7;
    private static final String LAST_IMPORT_UNDO_FILE = "last-import-undo.json";
    private static final long TRASH_RETENTION_MS = TRASH_RETENTION_DAYS * 24L * 60L * 60L * 1000L;

    private final Context context;
    private final BabyLogRepository repository;

    public BabyLogService(Context context, BabyLogRepository repository) {
        this.context = context.getApplicationContext();
        this.repository = repository;
    }

    public BabyLogDomain.BabyLogEvent recordQuickEvent(QuickAction action) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("summary", action.label);
        payload.put("quickAction", action.label);
        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                action.eventType,
                BabyLogFormatters.nowIso(),
                payload,
                Collections.emptyList(),
                "manual"
        );
        BabyLogDomain.ChildProfile profileUpdate = "birth".equals(event.eventType)
                ? withBirthDateFromBirthEvent(repository.loadChildProfile(), event.occurredAt)
                : null;
        saveEventWithAttachmentsAndOptionalChildProfile(event, Collections.emptyList(), profileUpdate);
        return event;
    }

    public static BabyLogDomain.ChildProfile withBirthDateFromBirthEvent(
            BabyLogDomain.ChildProfile profile,
            String occurredAt
    ) {
        String birthDate = occurredAt == null || occurredAt.length() < 10
                ? BabyLogFormatters.todayDateInput()
                : occurredAt.substring(0, 10);
        return (profile == null ? BabyLogDomain.ChildProfile.empty() : profile).withBirthDate(birthDate);
    }

    public BabyLogDomain.BabyLogEvent recordBabyCareEvent(BabyCareInput input) throws JSONException {
        if (!hasBabyCareMinimumContent(input)) {
            throw new IllegalArgumentException("请至少填写一项记录内容");
        }
        JSONObject payload = buildBabyCarePayload(input);
        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                input.eventType,
                BabyLogFormatters.nowIso(),
                payload,
                Collections.emptyList(),
                "manual"
        );
        saveEventWithSyncChange(event);
        return event;
    }

    public BabyLogDomain.BabyLogEvent updateBabyCareEvent(String eventId, BabyCareInput input) throws JSONException {
        if (!hasBabyCareMinimumContent(input)) {
            throw new IllegalArgumentException("请至少填写一项记录内容");
        }
        BabyLogDomain.BabyLogEvent existing = requireEditableEvent(eventId, input.eventType);
        BabyLogDomain.BabyLogEvent event = createEditedEvent(
                existing,
                input.eventType,
                buildBabyCarePayload(input),
                existing.attachmentIds
        );
        saveEventWithSyncChange(event);
        return event;
    }

    public BabyLogDomain.BabyLogEvent recordPregnancyEvent(PregnancyInput input) throws JSONException {
        if (!hasPregnancyMinimumContent(input)) {
            throw new IllegalArgumentException("请至少填写一项记录内容");
        }
        JSONObject payload = buildPregnancyPayload(input);
        String occurredAt = isPregnancyDocumentEvent(input.eventType) && BabyLogFormatters.isValidDateInput(input.primary)
                ? BabyLogFormatters.createOccurredAtFromDate(input.primary)
                : BabyLogFormatters.nowIso();
        List<BabyLogDomain.AttachmentRecord> attachments = createPregnancyAttachments(input);
        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                input.eventType,
                occurredAt,
                payload,
                attachmentIdsFromRecords(attachments),
                "manual"
        );
        saveEventWithAttachmentsAndSyncChanges(event, attachments);
        return event;
    }

    public BabyLogDomain.BabyLogEvent updatePregnancyEvent(String eventId, PregnancyInput input) throws JSONException {
        if (!hasPregnancyMinimumContent(input)) {
            throw new IllegalArgumentException("请至少填写一项记录内容");
        }
        BabyLogDomain.BabyLogEvent existing = requireEditableEvent(eventId, input.eventType);
        List<String> attachmentIds = new ArrayList<>(existing.attachmentIds);
        List<BabyLogDomain.AttachmentRecord> attachments = createPregnancyAttachments(input);
        attachmentIds.addAll(attachmentIdsFromRecords(attachments));
        String occurredAt = isPregnancyDocumentEvent(input.eventType) && BabyLogFormatters.isValidDateInput(input.primary)
                ? BabyLogFormatters.createOccurredAtFromDate(input.primary)
                : existing.occurredAt;
        JSONObject payload = buildPregnancyPayload(input);
        if ("contraction".equals(input.eventType)) {
            preserveContractionSessionFields(existing.payload, payload);
        }
        BabyLogDomain.BabyLogEvent event = createEditedEvent(
                existing,
                input.eventType,
                payload,
                attachmentIds,
                occurredAt
        );
        saveEventWithAttachmentsAndSyncChanges(event, attachments);
        return event;
    }

    public BabyLogDomain.BabyLogEvent recordFetalMovementSession(FetalMovementSessionInput input) throws JSONException {
        JSONObject payload = buildFetalMovementSessionPayload(input);
        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                "fetal_movement",
                isBlank(input.endedAtIso) ? BabyLogFormatters.nowIso() : input.endedAtIso,
                payload,
                Collections.emptyList(),
                "manual"
        );
        saveEventWithSyncChange(event);
        return event;
    }

    public List<BabyLogDomain.BabyLogEvent> recordContractionSession(ContractionSessionInput input) throws JSONException {
        if (input == null || input.entries.isEmpty()) {
            throw new IllegalArgumentException("请至少记录一次宫缩");
        }
        List<BabyLogDomain.BabyLogEvent> events = new ArrayList<>();
        List<BabyLogDomain.SyncChange> changes = new ArrayList<>();
        for (ContractionEntryInput entry : input.entries) {
            JSONObject payload = buildContractionSessionPayload(input.sessionId, entry);
            BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                    "contraction",
                    isBlank(entry.endIso) ? BabyLogFormatters.nowIso() : entry.endIso,
                    payload,
                    Collections.emptyList(),
                    "manual"
            );
            events.add(event);
            changes.add(BabyLogDomain.createSyncChange("event", event.id, "upsert"));
        }
        if (!repository.putEventsWithSyncChanges(events, changes)) {
            throw new JSONException("保存宫缩会话失败");
        }
        onSuccessfulWrite();
        return events;
    }

    public BabyLogDomain.BabyLogEvent recordMaternalMetric(MaternalMetricInput input) throws JSONException {
        if (!hasMaternalMetricMinimumContent(input)) {
            throw new IllegalArgumentException("请至少填写一项孕妈指标或备注");
        }
        JSONObject payload = buildMaternalMetricPayload(input);
        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                "maternal_metric",
                BabyLogFormatters.nowIso(),
                payload,
                Collections.emptyList(),
                "manual"
        );
        saveEventWithSyncChange(event);
        return event;
    }

    public BabyLogDomain.BabyLogEvent updateMaternalMetric(String eventId, MaternalMetricInput input) throws JSONException {
        if (!hasMaternalMetricMinimumContent(input)) {
            throw new IllegalArgumentException("请至少填写一项孕妈指标或备注");
        }
        BabyLogDomain.BabyLogEvent existing = requireEditableEvent(eventId, "maternal_metric");
        BabyLogDomain.BabyLogEvent event = createEditedEvent(
                existing,
                "maternal_metric",
                buildMaternalMetricPayload(input),
                existing.attachmentIds
        );
        saveEventWithSyncChange(event);
        return event;
    }

    public BabyLogDomain.BabyLogEvent deleteEvent(String eventId) throws JSONException {
        BabyLogDomain.BabyLogEvent event = repository.findEventById(eventId);
        if (event == null || event.deletedAt != null) {
            throw new JSONException("记录不存在或已删除");
        }
        String deletedAt = BabyLogFormatters.nowIso();
        BabyLogDomain.BabyLogEvent deleted = event.withDeletedAt(deletedAt);
        List<BabyLogDomain.AttachmentRecord> attachments = new ArrayList<>();
        List<BabyLogDomain.SyncChange> changes = new ArrayList<>();
        changes.add(BabyLogDomain.createSyncChange("event", deleted.id, "delete"));
        for (String attachmentId : event.attachmentIds) {
            BabyLogDomain.AttachmentRecord attachment = repository.findAttachmentById(attachmentId);
            if (attachment == null || attachment.deletedAt != null) {
                continue;
            }
            attachments.add(attachment.withDeletedAt(deletedAt));
            changes.add(BabyLogDomain.createSyncChange("attachment", attachment.id, "delete"));
        }
        BabyLogDomain.ChildProfile profileUpdate = null;
        if ("birth".equals(event.eventType)) {
            profileUpdate = repository.loadChildProfile().withBirthDate("");
            changes.add(BabyLogDomain.createSyncChange("childProfile", profileUpdate.id, "upsert"));
        }
        if (!repository.putEventProfileAttachmentsAndSyncChanges(deleted, profileUpdate, attachments, changes)) {
            throw new JSONException("删除记录失败");
        }
        onSuccessfulWrite();
        return deleted;
    }

    public BabyLogDomain.BabyLogEvent restoreEvent(String eventId) throws JSONException {
        BabyLogDomain.BabyLogEvent event = repository.findEventById(eventId);
        if (event == null || event.deletedAt == null) {
            throw new JSONException("记录不存在或不在回收站");
        }
        String restoredAt = BabyLogFormatters.nowIso();
        BabyLogDomain.BabyLogEvent restored = event.withRestoredAt(restoredAt);
        List<BabyLogDomain.AttachmentRecord> attachments = new ArrayList<>();
        List<BabyLogDomain.SyncChange> changes = new ArrayList<>();
        changes.add(BabyLogDomain.createSyncChange("event", restored.id, "upsert"));
        for (String attachmentId : event.attachmentIds) {
            BabyLogDomain.AttachmentRecord attachment = repository.findAttachmentById(attachmentId);
            if (attachment == null || attachment.deletedAt == null) {
                continue;
            }
            attachments.add(attachment.withRestoredAt(restoredAt));
            changes.add(BabyLogDomain.createSyncChange("attachment", attachment.id, "upsert"));
        }
        BabyLogDomain.ChildProfile profileUpdate = null;
        if ("birth".equals(restored.eventType)) {
            profileUpdate = withBirthDateFromBirthEvent(repository.loadChildProfile(), restored.occurredAt);
            changes.add(BabyLogDomain.createSyncChange("childProfile", profileUpdate.id, "upsert"));
        }
        if (!repository.putEventProfileAttachmentsAndSyncChanges(restored, profileUpdate, attachments, changes)) {
            throw new JSONException("恢复记录失败");
        }
        onSuccessfulWrite();
        return restored;
    }

    private void saveChildProfileWithSync(BabyLogDomain.ChildProfile profile) throws JSONException {
        BabyLogDomain.ChildProfile next = profile == null ? BabyLogDomain.ChildProfile.empty() : profile;
        repository.saveChildProfile(next);
        repository.putSyncChange(BabyLogDomain.createSyncChange("childProfile", next.id, "upsert"));
        onSuccessfulWrite();
    }

    public boolean saveEventWithSyncChange(BabyLogDomain.BabyLogEvent event) throws JSONException {
        return saveEventWithAttachmentsAndSyncChanges(event, Collections.emptyList());
    }

    private boolean saveEventWithAttachmentsAndSyncChanges(
            BabyLogDomain.BabyLogEvent event,
            List<BabyLogDomain.AttachmentRecord> attachments
    ) throws JSONException {
        return saveEventWithAttachmentsAndOptionalChildProfile(event, attachments, null);
    }

    private boolean saveEventWithAttachmentsAndOptionalChildProfile(
            BabyLogDomain.BabyLogEvent event,
            List<BabyLogDomain.AttachmentRecord> attachments,
            BabyLogDomain.ChildProfile childProfile
    ) throws JSONException {
        if (event == null) {
            throw new JSONException("记录不能为空");
        }
        List<BabyLogDomain.SyncChange> changes = createSyncChangesForEventUpsert(event, attachments, childProfile);
        boolean ok = repository.putEventProfileAttachmentsAndSyncChanges(event, childProfile, attachments, changes);
        if (!ok) {
            throw new JSONException("保存记录失败");
        }
        onSuccessfulWrite();
        return true;
    }

    private void onSuccessfulWrite() {
        BabyLogSyncPushWorker.enqueueIfConfigured(context);
    }

    public static List<BabyLogDomain.SyncChange> createSyncChangesForEventUpsert(
            BabyLogDomain.BabyLogEvent event,
            List<BabyLogDomain.AttachmentRecord> attachments,
            BabyLogDomain.ChildProfile childProfile
    ) {
        List<BabyLogDomain.SyncChange> changes = new ArrayList<>();
        changes.add(BabyLogDomain.createSyncChange("event", event.id, "upsert"));
        if (childProfile != null) {
            changes.add(BabyLogDomain.createSyncChange("childProfile", childProfile.id, "upsert"));
        }
        if (attachments != null) {
            for (BabyLogDomain.AttachmentRecord attachment : attachments) {
                if (attachment != null) {
                    changes.add(BabyLogDomain.createSyncChange("attachment", attachment.id, "upsert"));
                }
            }
        }
        return changes;
    }

    public int purgeExpiredTrash() {
        String now = BabyLogFormatters.nowIso();
        int count = 0;
        for (BabyLogDomain.BabyLogEvent event : repository.listDeletedEvents()) {
            if (!isTrashExpired(event.deletedAt, now)) {
                continue;
            }
            for (String attachmentId : event.attachmentIds) {
                BabyLogDomain.AttachmentRecord attachment = repository.findAttachmentById(attachmentId);
                if (attachment != null) {
                    deleteLocalFile(attachment.localPath);
                    repository.hardDeleteAttachment(attachment.id);
                }
            }
            repository.hardDeleteEvent(event.id);
            count += 1;
        }
        return count;
    }

    public List<BabyLogDomain.BabyLogEvent> listTrashEvents() {
        List<BabyLogDomain.BabyLogEvent> events = repository.listDeletedEvents();
        Collections.sort(events, (left, right) -> Long.compare(parseTime(right.deletedAt), parseTime(left.deletedAt)));
        return events;
    }

    public static boolean isTrashExpired(String deletedAt, String nowIso) {
        long deleted = BabyLogFormatters.parseIsoMillis(deletedAt);
        long now = BabyLogFormatters.parseIsoMillis(nowIso);
        return deleted > 0 && now > 0 && now - deleted >= TRASH_RETENTION_MS;
    }

    public static int trashRemainingDays(String deletedAt, String nowIso) {
        long deleted = BabyLogFormatters.parseIsoMillis(deletedAt);
        long now = BabyLogFormatters.parseIsoMillis(nowIso);
        if (deleted <= 0 || now <= 0) {
            return TRASH_RETENTION_DAYS;
        }
        long remaining = TRASH_RETENTION_MS - Math.max(0L, now - deleted);
        if (remaining <= 0L) {
            return 0;
        }
        return (int) Math.ceil(remaining / (24.0 * 60.0 * 60.0 * 1000.0));
    }

    public static JSONObject buildBabyCarePayload(BabyCareInput input) throws JSONException {
        JSONObject payload = new JSONObject();

        if ("feed".equals(input.eventType)) {
            putStringIfNotBlank(payload, "feedType", input.primary);
            putNumberIfNotNull(payload, "amountMl", BabyLogFormatters.parseOptionalNumber(input.secondary));
            putStringIfNotBlank(payload, "note", input.note);
        } else if ("sleep".equals(input.eventType)) {
            putStringIfNotBlank(payload, "sleepStart", input.primary);
            putStringIfNotBlank(payload, "sleepEnd", input.secondary);
            putStringIfNotBlank(payload, "sleepPlace", input.tertiary);
            putStringIfNotBlank(payload, "note", input.note);
        } else if ("diaper".equals(input.eventType)) {
            putStringIfNotBlank(payload, "diaperType", input.primary);
            putStringIfNotBlank(payload, "diaperDetail", input.secondary);
            putStringIfNotBlank(payload, "note", input.note);
        } else if ("temperature".equals(input.eventType)) {
            Double temperature = BabyLogFormatters.parseOptionalNumber(input.primary);
            putNumberIfNotNull(payload, "temperatureC", temperature);
            putStringIfNotBlank(payload, "measureMethod", input.secondary);
            putStringIfNotBlank(payload, "note", input.note);
        } else if ("medication".equals(input.eventType)) {
            putStringIfNotBlank(payload, "medicationName", input.primary);
            putStringIfNotBlank(payload, "dosage", input.secondary);
            putStringIfNotBlank(payload, "reason", input.tertiary);
            putStringIfNotBlank(payload, "note", input.note);
        } else {
            putStringIfNotBlank(payload, "detail", input.primary);
            putStringIfNotBlank(payload, "note", input.secondary);
        }

        payload.put("summary", formatBabyCareSummary(input));
        return payload;
    }

    public static JSONObject buildPregnancyPayload(PregnancyInput input) throws JSONException {
        JSONObject payload = new JSONObject();

        if ("pregnancy_checkup".equals(input.eventType)) {
            putStringIfNotBlank(payload, "checkupDate", input.primary);
            Integer gestationalAgeDays = BabyLogFormatters.parseGestationalAgeDays(input.gestationalAge);
            if (gestationalAgeDays != null) {
                payload.put("gestationalAgeDays", gestationalAgeDays);
            }
            putStringIfNotBlank(payload, "provider", input.secondary);
            putStringIfNotBlank(payload, "department", input.department);
            putNumberIfNotNull(payload, "systolicBp", BabyLogFormatters.parseOptionalNumber(input.systolicBp));
            putNumberIfNotNull(payload, "diastolicBp", BabyLogFormatters.parseOptionalNumber(input.diastolicBp));
            putNumberIfNotNull(payload, "weightKg", BabyLogFormatters.parseOptionalNumber(input.weightKg));
            putNumberIfNotNull(payload, "fundalHeightCm", BabyLogFormatters.parseOptionalNumber(input.fundalHeightCm));
            putNumberIfNotNull(payload, "abdominalCircumferenceCm", BabyLogFormatters.parseOptionalNumber(input.abdominalCircumferenceCm));
            putNumberIfNotNull(payload, "fetalHeartRateBpm", BabyLogFormatters.parseOptionalNumber(input.fetalHeartRateBpm));
            putStringIfNotBlank(payload, "fetalPresentation", input.fetalPresentation);
            putStringIfNotBlank(payload, "edema", input.edema);
            putStringIfNotBlank(payload, "urineRoutine", input.urineRoutine);
            putStringIfNotBlank(payload, "urineProtein", input.urineProtein);
            putNumberIfNotNull(payload, "hemoglobinGL", BabyLogFormatters.parseOptionalNumber(input.hemoglobinGL));
            putStringIfNotBlank(payload, "highRiskFactors", input.highRiskFactors);
            putStringIfNotBlank(payload, "doctorConclusion", input.tertiary);
            putStringIfNotBlank(payload, "finding", input.tertiary);
            putStringIfNotBlank(payload, "treatmentAdvice", input.treatmentAdvice);
            putStringIfNotBlank(payload, "nextVisitDate", input.nextVisitDate);
            putStringIfNotBlank(payload, "nextVisitNote", input.nextVisitDate.isEmpty() ? input.note : input.nextVisitDate);
            putStringIfNotBlank(payload, "reportType", input.reportType);
            putStringIfNotBlank(payload, "attachmentNote", input.attachmentNote);
            putStringIfNotBlank(payload, "note", input.note);
        } else if (isScreeningEventType(input.eventType)) {
            putStringIfNotBlank(payload, "screeningDate", input.primary);
            Integer gestationalAgeDays = BabyLogFormatters.parseGestationalAgeDays(input.gestationalAge);
            if (gestationalAgeDays != null) {
                payload.put("gestationalAgeDays", gestationalAgeDays);
            }
            for (Map.Entry<String, String> entry : input.screeningValues.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (isBlank(key) || isBlank(value)) {
                    continue;
                }
                if (isScreeningNumericField(key)) {
                    putNumberIfNotNull(payload, key, BabyLogFormatters.parseOptionalNumber(value));
                } else {
                    putStringIfNotBlank(payload, key, value);
                }
            }
            putStringIfNotBlank(payload, "note", input.note);
            putStringIfNotBlank(payload, "attachmentNote", input.attachmentNote);
        } else if ("fetal_movement".equals(input.eventType)) {
            putStringIfNotBlank(payload, "movementWindow", input.primary);
            putNumberIfNotNull(payload, "movementCount", BabyLogFormatters.parseOptionalNumber(input.secondary));
            putStringIfNotBlank(payload, "note", input.note);
        } else if ("contraction".equals(input.eventType)) {
            putStringIfNotBlank(payload, "contractionStart", input.primary);
            putNumberIfNotNull(payload, "intervalMinutes", BabyLogFormatters.parseOptionalNumber(input.secondary));
            putNumberIfNotNull(payload, "durationSeconds", BabyLogFormatters.parseOptionalNumber(input.tertiary));
            putStringIfNotBlank(payload, "note", input.note);
        }

        payload.put("summary", formatPregnancySummary(input));
        return payload;
    }

    public static JSONObject buildFetalMovementSessionPayload(FetalMovementSessionInput input) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("entryMode", "session");
        putStringIfNotBlank(payload, "startedAt", input.startedAtIso);
        putStringIfNotBlank(payload, "endedAt", input.endedAtIso);
        payload.put("movementCount", input.count);
        payload.put("durationMinutes", input.durationMinutes);
        payload.put("targetCount", input.targetCount);
        putStringIfNotBlank(payload, "note", input.note);
        payload.put("summary", formatFetalMovementSessionSummary(input));
        return payload;
    }

    public static JSONObject buildContractionSessionPayload(String sessionId, ContractionEntryInput input) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("entryMode", "session");
        putStringIfNotBlank(payload, "sessionId", sessionId);
        putStringIfNotBlank(payload, "startIso", input.startIso);
        putStringIfNotBlank(payload, "endIso", input.endIso);
        putStringIfNotBlank(payload, "contractionStart", BabyLogFormatters.formatEventTime(input.startIso));
        payload.put("durationSec", Math.max(0, input.durationSec));
        if (input.intervalFromPrevSec != null && input.intervalFromPrevSec > 0) {
            payload.put("intervalFromPrevSec", input.intervalFromPrevSec);
        }
        payload.put("summary", formatContractionSessionSummary(input));
        return payload;
    }

    public static JSONObject buildMaternalMetricPayload(MaternalMetricInput input) throws JSONException {
        JSONObject payload = new JSONObject();
        Double weight = BabyLogFormatters.parseOptionalNumber(input.weightKg);
        Double systolic = BabyLogFormatters.parseOptionalNumber(input.systolicBp);
        Double diastolic = BabyLogFormatters.parseOptionalNumber(input.diastolicBp);
        Double glucose = BabyLogFormatters.parseOptionalNumber(input.glucoseMmolL);
        putNumberIfNotNull(payload, "weightKg", weight);
        putNumberIfNotNull(payload, "systolicBp", systolic);
        putNumberIfNotNull(payload, "diastolicBp", diastolic);
        putNumberIfNotNull(payload, "glucoseMmolL", glucose);
        putStringIfNotBlank(payload, "glucoseContext", input.glucoseContext);
        putStringIfNotBlank(payload, "note", input.note);
        String warning = BabyLogFormatters.formatMaternalGlucoseWarning(glucose, input.glucoseContext);
        putStringIfNotBlank(payload, "warningText", warning);
        payload.put("summary", formatMaternalMetricSummary(input));
        return payload;
    }

    public static String formatBabyCareSummary(BabyCareInput input) {
        StringBuilder summary = new StringBuilder(BabyLogFormatters.eventLabel(input.eventType));
        if ("feed".equals(input.eventType)) {
            appendSummary(summary, input.primary);
            Double amount = BabyLogFormatters.parseOptionalNumber(input.secondary);
            if (amount != null) {
                appendSummary(summary, BabyLogFormatters.formatNumber(amount) + " ml");
            }
        } else if ("sleep".equals(input.eventType)) {
            if (!isBlank(input.primary) && !isBlank(input.secondary)) {
                appendSummary(summary, input.primary.trim() + "-" + input.secondary.trim());
            }
            appendSummary(summary, input.tertiary);
        } else if ("diaper".equals(input.eventType)) {
            appendSummary(summary, input.primary);
            appendSummary(summary, input.secondary);
        } else if ("temperature".equals(input.eventType)) {
            Double temperature = BabyLogFormatters.parseOptionalNumber(input.primary);
            if (temperature != null) {
                appendSummary(summary, BabyLogFormatters.formatNumber(temperature) + " ℃");
            }
            appendSummary(summary, input.secondary);
        } else if ("medication".equals(input.eventType)) {
            appendSummary(summary, input.primary);
            appendSummary(summary, input.secondary);
            appendSummary(summary, input.tertiary);
        } else {
            appendSummary(summary, input.primary);
            appendSummary(summary, input.secondary);
            if (summary.toString().equals(BabyLogFormatters.eventLabel(input.eventType))) {
                return summary.toString();
            }
        }
        if (summary.toString().equals(BabyLogFormatters.eventLabel(input.eventType))) {
            summary.append(" · 待补充详情");
        }
        return summary.toString();
    }

    public static String formatPregnancySummary(PregnancyInput input) {
        StringBuilder summary = new StringBuilder(BabyLogFormatters.eventLabel(input.eventType));
        if ("pregnancy_checkup".equals(input.eventType)) {
            Integer gestationalAgeDays = BabyLogFormatters.parseGestationalAgeDays(input.gestationalAge);
            if (gestationalAgeDays != null) {
                appendSummary(summary, BabyLogFormatters.formatGestationalAge(gestationalAgeDays));
            }
            appendSummary(summary, input.secondary);
            Double systolic = BabyLogFormatters.parseOptionalNumber(input.systolicBp);
            Double diastolic = BabyLogFormatters.parseOptionalNumber(input.diastolicBp);
            if (systolic != null && diastolic != null) {
                appendSummary(summary, "血压 " + BabyLogFormatters.formatNumber(systolic) + "/" + BabyLogFormatters.formatNumber(diastolic) + " mmHg");
            }
            Double weight = BabyLogFormatters.parseOptionalNumber(input.weightKg);
            if (weight != null) {
                appendSummary(summary, "体重 " + BabyLogFormatters.formatNumber(weight) + " kg");
            }
            Double fetalHeartRate = BabyLogFormatters.parseOptionalNumber(input.fetalHeartRateBpm);
            if (fetalHeartRate != null) {
                appendSummary(summary, "胎心 " + BabyLogFormatters.formatNumber(fetalHeartRate) + " bpm");
            }
            if (!isBlank(input.fetalPresentation)) {
                appendSummary(summary, "胎位 " + input.fetalPresentation);
            }
            if (!isBlank(input.urineProtein)) {
                appendSummary(summary, "尿蛋白 " + input.urineProtein);
            }
            Double hemoglobin = BabyLogFormatters.parseOptionalNumber(input.hemoglobinGL);
            if (hemoglobin != null) {
                appendSummary(summary, "Hb " + BabyLogFormatters.formatNumber(hemoglobin) + " g/L");
            }
            appendSummary(summary, input.tertiary);
            appendSummary(summary, input.treatmentAdvice);
        } else if (isScreeningEventType(input.eventType)) {
            Integer gestationalAgeDays = BabyLogFormatters.parseGestationalAgeDays(input.gestationalAge);
            if (gestationalAgeDays != null) {
                appendSummary(summary, BabyLogFormatters.formatGestationalAge(gestationalAgeDays));
            }
            appendScreeningSummary(summary, input);
        } else if ("fetal_movement".equals(input.eventType)) {
            appendSummary(summary, input.primary);
            Double count = BabyLogFormatters.parseOptionalNumber(input.secondary);
            if (count != null) {
                appendSummary(summary, BabyLogFormatters.formatNumber(count) + " 次");
            }
        } else if ("contraction".equals(input.eventType)) {
            appendSummary(summary, input.primary);
            Double interval = BabyLogFormatters.parseOptionalNumber(input.secondary);
            if (interval != null) {
                appendSummary(summary, "间隔 " + BabyLogFormatters.formatNumber(interval) + " 分钟");
            }
            Double duration = BabyLogFormatters.parseOptionalNumber(input.tertiary);
            if (duration != null) {
                appendSummary(summary, "持续 " + BabyLogFormatters.formatNumber(duration) + " 秒");
            }
        }
        if (summary.toString().equals(BabyLogFormatters.eventLabel(input.eventType))) {
            summary.append(" · 待补充详情");
        }
        return summary.toString();
    }

    public static String formatFetalMovementSessionSummary(FetalMovementSessionInput input) {
        StringBuilder summary = new StringBuilder(BabyLogFormatters.eventLabel("fetal_movement"));
        String window = formatSessionWindow(input.startedAtIso, input.endedAtIso);
        appendSummary(summary, window);
        if (input.count > 0) {
            appendSummary(summary, input.count + " 次");
        }
        if (input.durationMinutes > 0) {
            appendSummary(summary, input.durationMinutes + " 分钟");
        }
        if (summary.toString().equals(BabyLogFormatters.eventLabel("fetal_movement"))) {
            summary.append(" · 待补充详情");
        }
        return summary.toString();
    }

    public static String formatContractionSessionSummary(ContractionEntryInput input) {
        StringBuilder summary = new StringBuilder(BabyLogFormatters.eventLabel("contraction"));
        String window = formatSessionWindow(input.startIso, input.endIso);
        appendSummary(summary, window);
        if (input.durationSec > 0) {
            appendSummary(summary, "持续 " + input.durationSec + " 秒");
        }
        if (input.intervalFromPrevSec != null && input.intervalFromPrevSec > 0) {
            appendSummary(summary, "距上次 " + formatSecondsAsMinutes(input.intervalFromPrevSec));
        }
        if (summary.toString().equals(BabyLogFormatters.eventLabel("contraction"))) {
            summary.append(" · 待补充详情");
        }
        return summary.toString();
    }

    public static boolean isScreeningEventType(String eventType) {
        return BabyLogFormatters.isScreeningEventType(eventType);
    }

    public static boolean isPregnancyDocumentEvent(String eventType) {
        return "pregnancy_checkup".equals(eventType) || isScreeningEventType(eventType);
    }

    private static boolean isScreeningNumericField(String key) {
        return "ntMm".equals(key)
                || "fastingGlucoseMmolL".equals(key)
                || "oneHourGlucoseMmolL".equals(key)
                || "twoHourGlucoseMmolL".equals(key);
    }

    private static void appendScreeningSummary(StringBuilder summary, PregnancyInput input) {
        if ("screening_nt".equals(input.eventType)) {
            appendNumberSummary(summary, "NT", input.screeningValues.get("ntMm"), "mm");
            appendSummary(summary, input.screeningValues.get("conclusion"));
        } else if ("screening_serum".equals(input.eventType)) {
            appendSummary(summary, input.screeningValues.get("riskLevel"));
            appendSummary(summary, withLabel("T21", input.screeningValues.get("riskT21")));
            appendSummary(summary, withLabel("T18", input.screeningValues.get("riskT18")));
            appendSummary(summary, withLabel("ONTD", input.screeningValues.get("riskOntd")));
        } else if ("screening_nipt".equals(input.eventType)) {
            appendSummary(summary, withLabel("T21", input.screeningValues.get("t21Result")));
            appendSummary(summary, withLabel("T18", input.screeningValues.get("t18Result")));
            appendSummary(summary, withLabel("T13", input.screeningValues.get("t13Result")));
            appendSummary(summary, input.screeningValues.get("conclusion"));
        } else if ("screening_anomaly".equals(input.eventType)) {
            appendSummary(summary, input.screeningValues.get("structureConclusion"));
            appendSummary(summary, input.screeningValues.get("conclusion"));
        } else if ("screening_ogtt".equals(input.eventType)) {
            appendNumberSummary(summary, "空腹", input.screeningValues.get("fastingGlucoseMmolL"), "mmol/L");
            appendNumberSummary(summary, "1h", input.screeningValues.get("oneHourGlucoseMmolL"), "mmol/L");
            appendNumberSummary(summary, "2h", input.screeningValues.get("twoHourGlucoseMmolL"), "mmol/L");
            appendSummary(summary, input.screeningValues.get("abnormalFlag"));
        } else if ("screening_gbs".equals(input.eventType)) {
            appendSummary(summary, input.screeningValues.get("gbsResult"));
        } else if ("screening_nst".equals(input.eventType)) {
            appendSummary(summary, input.screeningValues.get("nstResult"));
        }
        appendSummary(summary, input.note);
    }

    private static void appendNumberSummary(StringBuilder summary, String label, String rawValue, String unit) {
        Double value = BabyLogFormatters.parseOptionalNumber(rawValue);
        if (value != null) {
            appendSummary(summary, label + " " + BabyLogFormatters.formatNumber(value) + " " + unit);
        }
    }

    public static String formatUltrasoundClinicalDetails(UltrasoundInput input) {
        StringBuilder summary = new StringBuilder();
        Double afi = BabyLogFormatters.parseOptionalNumber(input.afiCm);
        if (afi != null) {
            appendSummary(summary, "羊水 AFI " + BabyLogFormatters.formatNumber(afi) + " cm");
        }
        Double deepestPocket = BabyLogFormatters.parseOptionalNumber(input.deepestPocketCm);
        if (deepestPocket != null) {
            appendSummary(summary, "最大羊水池 " + BabyLogFormatters.formatNumber(deepestPocket) + " cm");
        }
        appendSummary(summary, withLabel("胎盘", input.placentaLocation));
        appendSummary(summary, withLabel("成熟度", input.placentaGrade));
        appendSummary(summary, withLabel("胎位", input.fetalPresentation));
        appendSummary(summary, withLabel("医院", input.hospital));
        appendSummary(summary, withLabel("报告时间", input.reportTime));
        appendSummary(summary, withLabel("诊断", input.diagnosisText));
        appendSummary(summary, withLabel("胎儿个数", input.fetalCount));
        appendSummary(summary, withLabel("胎动", input.fetalMovement));
        Double fetalHeartRate = BabyLogFormatters.parseOptionalNumber(input.fetalHeartRateBpm);
        if (fetalHeartRate != null) {
            appendSummary(summary, "胎心率 " + BabyLogFormatters.formatNumber(fetalHeartRate) + " bpm");
        }
        Double crl = BabyLogFormatters.parseOptionalNumber(input.crlMm);
        if (crl != null) {
            appendSummary(summary, "CRL " + BabyLogFormatters.formatNumber(crl) + " mm");
        }
        Double nt = BabyLogFormatters.parseOptionalNumber(input.ntMm);
        if (nt != null) {
            appendSummary(summary, "NT " + BabyLogFormatters.formatNumber(nt) + " mm");
        }
        Double cervicalLength = BabyLogFormatters.parseOptionalNumber(input.cervicalLengthMm);
        if (cervicalLength != null) {
            appendSummary(summary, "宫颈管长度 " + BabyLogFormatters.formatNumber(cervicalLength) + " mm");
        }
        appendSummary(summary, withLabel("脐带插入处", input.umbilicalInsertion));
        Double sd = BabyLogFormatters.parseOptionalNumber(input.umbilicalSd);
        if (sd != null) {
            appendSummary(summary, "脐动脉 S/D " + BabyLogFormatters.formatNumber(sd));
        }
        Double pi = BabyLogFormatters.parseOptionalNumber(input.umbilicalPi);
        if (pi != null) {
            appendSummary(summary, "PI " + BabyLogFormatters.formatNumber(pi));
        }
        Double ri = BabyLogFormatters.parseOptionalNumber(input.umbilicalRi);
        if (ri != null) {
            appendSummary(summary, "RI " + BabyLogFormatters.formatNumber(ri));
        }
        return summary.toString();
    }

    public static boolean hasUltrasoundMinimumContent(UltrasoundInput input) {
        if (input == null) {
            return false;
        }
        return hasUsableUltrasoundPhoto(input.photoPath)
                || BabyLogFormatters.parseOptionalNumber(input.bpdMm) != null
                || BabyLogFormatters.parseOptionalNumber(input.hcMm) != null
                || BabyLogFormatters.parseOptionalNumber(input.acMm) != null
                || BabyLogFormatters.parseOptionalNumber(input.flMm) != null
                || BabyLogFormatters.parseOptionalNumber(input.efwGram) != null;
    }

    public static boolean hasBabyCareMinimumContent(BabyCareInput input) {
        if (input == null) {
            return false;
        }
        return !isBlank(input.primary)
                || !isBlank(input.secondary)
                || !isBlank(input.tertiary)
                || !isBlank(input.note);
    }

    public static boolean hasPregnancyMinimumContent(PregnancyInput input) {
        if (input == null) {
            return false;
        }
        if ("pregnancy_checkup".equals(input.eventType)) {
            return !isBlank(input.secondary)
                    || !isBlank(input.department)
                    || !isBlank(input.systolicBp)
                    || !isBlank(input.diastolicBp)
                    || !isBlank(input.weightKg)
                    || !isBlank(input.fundalHeightCm)
                    || !isBlank(input.abdominalCircumferenceCm)
                    || !isBlank(input.fetalHeartRateBpm)
                    || !isBlank(input.fetalPresentation)
                    || !isBlank(input.edema)
                    || !isBlank(input.urineRoutine)
                    || !isBlank(input.urineProtein)
                    || !isBlank(input.hemoglobinGL)
                    || !isBlank(input.highRiskFactors)
                    || !isBlank(input.tertiary)
                    || !isBlank(input.treatmentAdvice)
                    || !isBlank(input.nextVisitDate)
                    || !isBlank(input.reportType)
                    || !isBlank(input.attachmentNote)
                    || !isBlank(input.note)
                    || !isBlank(input.attachmentPath);
        }
        if (isScreeningEventType(input.eventType)) {
            for (String value : input.screeningValues.values()) {
                if (!isBlank(value)) {
                    return true;
                }
            }
            return !isBlank(input.note)
                    || !isBlank(input.attachmentNote)
                    || !isBlank(input.attachmentPath);
        }
        return !isBlank(input.primary)
                || !isBlank(input.secondary)
                || !isBlank(input.tertiary)
                || !isBlank(input.note);
    }

    public static boolean hasMaternalMetricMinimumContent(MaternalMetricInput input) {
        if (input == null) {
            return false;
        }
        return !isBlank(input.weightKg)
                || !isBlank(input.systolicBp)
                || !isBlank(input.diastolicBp)
                || !isBlank(input.glucoseMmolL)
                || !isBlank(input.note);
    }

    private static boolean hasUsableUltrasoundPhoto(String photoPath) {
        if (isBlank(photoPath)) {
            return false;
        }
        File image = new File(photoPath);
        return image.isFile() && image.length() > 0;
    }

    public static String formatMaternalMetricSummary(MaternalMetricInput input) {
        StringBuilder summary = new StringBuilder(BabyLogFormatters.eventLabel("maternal_metric"));
        Double weight = BabyLogFormatters.parseOptionalNumber(input.weightKg);
        if (weight != null) {
            appendSummary(summary, "体重 " + BabyLogFormatters.formatNumber(weight) + " kg");
        }
        Double systolic = BabyLogFormatters.parseOptionalNumber(input.systolicBp);
        Double diastolic = BabyLogFormatters.parseOptionalNumber(input.diastolicBp);
        if (systolic != null && diastolic != null) {
            appendSummary(summary, "血压 " + BabyLogFormatters.formatNumber(systolic) + "/" + BabyLogFormatters.formatNumber(diastolic) + " mmHg");
        }
        Double glucose = BabyLogFormatters.parseOptionalNumber(input.glucoseMmolL);
        if (glucose != null) {
            String context = BabyLogFormatters.maternalGlucoseContextLabel(input.glucoseContext);
            String prefix = context.isEmpty() ? "血糖 " : "血糖 " + context + " ";
            appendSummary(summary, prefix + BabyLogFormatters.formatNumber(glucose) + " mmol/L");
        }
        if (summary.toString().equals(BabyLogFormatters.eventLabel("maternal_metric"))) {
            summary.append(" · 待补充详情");
        }
        return summary.toString();
    }

    public BabyLogDomain.BabyLogEvent recordUltrasound(UltrasoundInput input) throws JSONException {
        if (!hasUltrasoundMinimumContent(input)) {
            throw new IllegalArgumentException("请先选择 B 超单图片，或填写至少一个生长指标");
        }
        List<BabyLogDomain.AttachmentRecord> attachments = createUltrasoundAttachments(input);
        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                "ultrasound",
                BabyLogFormatters.createOccurredAtFromDate(input.examDate),
                buildUltrasoundPayload(input),
                attachmentIdsFromRecords(attachments),
                "manual"
        );
        saveEventWithAttachmentsAndSyncChanges(event, attachments);
        return event;
    }

    public BabyLogDomain.BabyLogEvent updateUltrasound(String eventId, UltrasoundInput input) throws JSONException {
        BabyLogDomain.BabyLogEvent existing = requireEditableEvent(eventId, "ultrasound");
        if (!hasUltrasoundMinimumContent(input) && existing.attachmentIds.isEmpty()) {
            throw new IllegalArgumentException("请先选择 B 超单图片，或填写至少一个生长指标");
        }
        List<String> attachmentIds = new ArrayList<>(existing.attachmentIds);
        List<BabyLogDomain.AttachmentRecord> attachments = createUltrasoundAttachments(input);
        attachmentIds.addAll(attachmentIdsFromRecords(attachments));
        String occurredAt = BabyLogFormatters.isValidDateInput(input.examDate)
                ? BabyLogFormatters.createOccurredAtFromDate(input.examDate)
                : existing.occurredAt;
        BabyLogDomain.BabyLogEvent event = createEditedEvent(
                existing,
                "ultrasound",
                buildUltrasoundPayload(input),
                attachmentIds,
                occurredAt
        );
        saveEventWithAttachmentsAndSyncChanges(event, attachments);
        return event;
    }

    public static BabyLogDomain.BabyLogEvent createEditedEvent(
            BabyLogDomain.BabyLogEvent existing,
            String expectedEventType,
            JSONObject payload,
            List<String> attachmentIds
    ) throws JSONException {
        return createEditedEvent(existing, expectedEventType, payload, attachmentIds, existing == null ? null : existing.occurredAt);
    }

    public static BabyLogDomain.BabyLogEvent createEditedEvent(
            BabyLogDomain.BabyLogEvent existing,
            String expectedEventType,
            JSONObject payload,
            List<String> attachmentIds,
            String occurredAt
    ) throws JSONException {
        if (existing == null || existing.deletedAt != null) {
            throw new JSONException("记录不存在或已删除");
        }
        if (!existing.eventType.equals(expectedEventType)) {
            throw new JSONException("记录类型不匹配");
        }
        return new BabyLogDomain.BabyLogEvent(
                existing.id,
                existing.familyId,
                existing.childId,
                existing.eventType,
                isBlank(occurredAt) ? existing.occurredAt : occurredAt,
                payload == null ? existing.payload : payload,
                attachmentIds == null ? new ArrayList<>() : new ArrayList<>(attachmentIds),
                existing.source,
                existing.createdAt,
                BabyLogFormatters.nowIso(),
                BabyLogDomain.UPDATED_BY_LOCAL,
                existing.schemaVersion,
                existing.deletedAt
        );
    }

    private BabyLogDomain.BabyLogEvent requireEditableEvent(String eventId, String expectedEventType) throws JSONException {
        BabyLogDomain.BabyLogEvent existing = repository.findEventById(eventId);
        if (existing == null || existing.deletedAt != null) {
            throw new JSONException("记录不存在或已删除");
        }
        if (!expectedEventType.equals(existing.eventType)) {
            throw new JSONException("记录类型不匹配");
        }
        return existing;
    }

    private List<BabyLogDomain.AttachmentRecord> createPregnancyAttachments(PregnancyInput input) throws JSONException {
        List<BabyLogDomain.AttachmentRecord> attachments = new ArrayList<>();
        if (!isPregnancyDocumentEvent(input.eventType) || isBlank(input.attachmentPath)) {
            return attachments;
        }
        File image = new File(input.attachmentPath);
        if (image.exists() && image.length() > 0) {
            BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.createAttachment(
                    "document_image",
                    isBlank(input.attachmentName) ? image.getName() : input.attachmentName,
                    "image/jpeg",
                    image.length(),
                    input.attachmentPath
            );
            attachments.add(attachment);
        }
        return attachments;
    }

    private List<BabyLogDomain.AttachmentRecord> createUltrasoundAttachments(UltrasoundInput input) throws JSONException {
        List<BabyLogDomain.AttachmentRecord> attachments = new ArrayList<>();
        if (input.photoPath != null && !input.photoPath.isEmpty()) {
            File image = new File(input.photoPath);
            if (image.exists() && image.length() > 0) {
                BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.createAttachment(
                        "ultrasound_image",
                        input.photoName == null || input.photoName.isEmpty() ? image.getName() : input.photoName,
                        "image/jpeg",
                        image.length(),
                        input.photoPath
                );
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    private static List<String> attachmentIdsFromRecords(List<BabyLogDomain.AttachmentRecord> attachments) {
        List<String> ids = new ArrayList<>();
        if (attachments == null) {
            return ids;
        }
        for (BabyLogDomain.AttachmentRecord attachment : attachments) {
            if (attachment != null) {
                ids.add(attachment.id);
            }
        }
        return ids;
    }

    private static JSONObject buildUltrasoundPayload(UltrasoundInput input) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("examDate", input.examDate);
        Double bpd = BabyLogFormatters.parseOptionalNumber(input.bpdMm);
        Double hc = BabyLogFormatters.parseOptionalNumber(input.hcMm);
        Double ac = BabyLogFormatters.parseOptionalNumber(input.acMm);
        Double fl = BabyLogFormatters.parseOptionalNumber(input.flMm);
        Double efw = BabyLogFormatters.parseOptionalNumber(input.efwGram);
        if (efw == null) {
            efw = BabyLogFetalGrowthReference.estimateEfwHadlock3Gram(bpd, ac, fl);
            if (efw != null) {
                payload.put("efwMethod", "hadlock3");
            }
        }
        putIfNotNull(payload, "gestationalAgeDays", BabyLogFormatters.parseGestationalAgeDays(input.gestationalAge));
        putIfNotNull(payload, "bpdMm", bpd);
        putIfNotNull(payload, "hcMm", hc);
        putIfNotNull(payload, "acMm", ac);
        putIfNotNull(payload, "flMm", fl);
        putIfNotNull(payload, "efwGram", efw);
        putStringIfNotBlank(payload, "hospital", input.hospital);
        putStringIfNotBlank(payload, "reportTime", input.reportTime);
        putStringIfNotBlank(payload, "diagnosisText", input.diagnosisText);
        putIfNotNull(payload, "afiCm", BabyLogFormatters.parseOptionalNumber(input.afiCm));
        putIfNotNull(payload, "deepestPocketCm", BabyLogFormatters.parseOptionalNumber(input.deepestPocketCm));
        putStringIfNotBlank(payload, "placentaLocation", input.placentaLocation);
        putStringIfNotBlank(payload, "placentaGrade", input.placentaGrade);
        putStringIfNotBlank(payload, "fetalPresentation", input.fetalPresentation);
        putIfNotNull(payload, "fetalHeartRateBpm", BabyLogFormatters.parseOptionalNumber(input.fetalHeartRateBpm));
        putStringIfNotBlank(payload, "fetalCount", input.fetalCount);
        putStringIfNotBlank(payload, "fetalMovement", input.fetalMovement);
        putStringIfNotBlank(payload, "umbilicalInsertion", input.umbilicalInsertion);
        putIfNotNull(payload, "cervicalLengthMm", BabyLogFormatters.parseOptionalNumber(input.cervicalLengthMm));
        putIfNotNull(payload, "crlMm", BabyLogFormatters.parseOptionalNumber(input.crlMm));
        putIfNotNull(payload, "ntMm", BabyLogFormatters.parseOptionalNumber(input.ntMm));
        putIfNotNull(payload, "umbilicalSd", BabyLogFormatters.parseOptionalNumber(input.umbilicalSd));
        putIfNotNull(payload, "umbilicalPi", BabyLogFormatters.parseOptionalNumber(input.umbilicalPi));
        putIfNotNull(payload, "umbilicalRi", BabyLogFormatters.parseOptionalNumber(input.umbilicalRi));
        String summary = BabyLogFormatters.formatUltrasoundSummary(payload);
        payload.put("summary", summary);
        putStringIfNotBlank(payload, "clinicalDetails", formatUltrasoundClinicalDetails(input));
        return payload;
    }

    public DashboardSnapshot loadDashboard() {
        List<BabyLogDomain.BabyLogEvent> events = listRecentEvents(20);
        List<BabyLogDomain.AttachmentRecord> attachments = listAttachmentsNewestFirst();
        List<BabyLogDomain.SyncChange> changes = repository.listSyncChanges();
        int pending = 0;
        int failed = 0;
        int synced = 0;
        for (BabyLogDomain.SyncChange change : changes) {
            if ("pending".equals(change.status) || "failed".equals(change.status)) {
                pending += 1;
            }
            if ("failed".equals(change.status)) {
                failed += 1;
            }
            if ("synced".equals(change.status)) {
                synced += 1;
            }
        }
        return new DashboardSnapshot(
                events,
                attachments,
                summarizeToday(),
                pending,
                failed,
                synced,
                repository.loadSyncLastPulledAt(),
                repository.loadRemoteUpdateBannerCount(),
                repository.estimateLocalBytes() + estimateAttachmentBytes(),
                context.getFilesDir().getUsableSpace()
        );
    }

    public void dismissRemoteUpdateBanner() {
        repository.dismissRemoteUpdateBanner();
    }

    public List<BabyLogDomain.BabyLogEvent> listRecentEvents(int limit) {
        List<BabyLogDomain.BabyLogEvent> events = sortEventsNewestFirst(repository.listEvents());
        return events.size() <= limit ? events : new ArrayList<>(events.subList(0, limit));
    }

    public List<BabyLogDomain.BabyLogEvent> listTimelineEvents() {
        return sortEventsNewestFirst(repository.listEvents());
    }

    public static List<BabyLogDomain.BabyLogEvent> sortEventsNewestFirst(List<BabyLogDomain.BabyLogEvent> source) {
        List<BabyLogDomain.BabyLogEvent> events = new ArrayList<>(source);
        Collections.sort(events, (left, right) -> Long.compare(parseTime(right.occurredAt), parseTime(left.occurredAt)));
        return events;
    }

    public List<BabyLogDomain.AttachmentRecord> listAttachmentsNewestFirst() {
        List<BabyLogDomain.AttachmentRecord> attachments = repository.listAttachments();
        Collections.sort(attachments, (left, right) -> Long.compare(parseTime(right.createdAt), parseTime(left.createdAt)));
        return attachments;
    }

    public Map<String, Integer> summarizeToday() {
        Map<String, Integer> counts = new HashMap<>();
        for (String type : BabyLogDomain.EVENT_TYPES) {
            counts.put(type, 0);
        }
        String today = BabyLogFormatters.todayDateInput();
        for (BabyLogDomain.BabyLogEvent event : repository.listEvents()) {
            if (event.occurredAt != null && event.occurredAt.startsWith(today)) {
                Integer current = counts.get(event.eventType);
                counts.put(event.eventType, (current == null ? 0 : current) + 1);
            }
        }
        return counts;
    }

    public String createBackupJson() throws JSONException, IOException {
        JSONObject data = new JSONObject();
        data.put("familyProfiles", repository.exportFamilyProfiles());
        data.put("childProfiles", repository.exportChildProfiles());
        data.put("familyMembers", repository.exportFamilyMembers());
        data.put("events", repository.exportEvents());
        JSONArray attachments = sanitizeAttachmentsForBackup(repository.exportAttachments(), BabyLogFormatters.nowIso());
        data.put("attachments", attachments);
        data.put("attachmentBlobs", createAttachmentBlobBackup(attachments));
        data.put("syncChanges", repository.exportSyncChanges());
        validateBackupDataForImport(data);

        JSONObject backup = new JSONObject();
        backup.put("format", BACKUP_FORMAT);
        backup.put("version", BACKUP_VERSION);
        backup.put("exportedAt", BabyLogFormatters.nowIso());
        backup.put("data", data);
        return backup.toString(2);
    }

    public int importBackupJson(String raw) throws JSONException, IOException {
        return importBackupJson(raw, true);
    }

    public boolean hasImportUndoSnapshot() {
        File file = importUndoSnapshotFile();
        return file.isFile() && file.length() > 0;
    }

    public int undoLastImport() throws JSONException, IOException {
        File file = importUndoSnapshotFile();
        if (!file.isFile()) {
            throw new IOException("没有可撤销的导入快照");
        }
        String raw = new String(readBytes(file), StandardCharsets.UTF_8);
        int count = importBackupJson(raw, false);
        file.delete();
        return count;
    }

    private int importBackupJson(String raw, boolean createUndoSnapshot) throws JSONException, IOException {
        JSONObject backup = new JSONObject(raw);
        if (!BACKUP_FORMAT.equals(backup.optString("format"))) {
            throw new JSONException("Invalid BabyLog backup format");
        }
        if (backup.optInt("version") != BACKUP_VERSION) {
            throw new JSONException("Unsupported backup version");
        }
        JSONObject data = backup.optJSONObject("data");
        if (data == null || data.optJSONArray("events") == null) {
            throw new JSONException("Invalid BabyLog backup data");
        }
        validateBackupDataForImport(data);
        if (createUndoSnapshot) {
            writeImportUndoSnapshot(createBackupJson());
        }
        JSONArray attachments = data.optJSONArray("attachments");
        JSONArray restoredAttachments = restoreAttachmentBlobs(attachments, data.optJSONArray("attachmentBlobs"));
        boolean imported = repository.importData(
                data.optJSONArray("familyProfiles"),
                data.optJSONArray("childProfiles"),
                data.optJSONArray("familyMembers"),
                data.optJSONArray("events"),
                restoredAttachments,
                data.optJSONArray("syncChanges")
        );
        if (!imported) {
            throw new IOException("导入写入失败，原数据未确认替换");
        }
        return data.optJSONArray("events").length();
    }

    public static void validateBackupDataForImport(JSONObject data) throws JSONException {
        if (data == null) {
            throw new JSONException("Invalid BabyLog backup data");
        }
        JSONArray events = data.optJSONArray("events");
        if (events == null) {
            throw new JSONException("Invalid BabyLog backup events");
        }
        validateEvents(events);
        validateProfiles(data.optJSONArray("familyProfiles"), "familyProfiles");
        validateProfiles(data.optJSONArray("childProfiles"), "childProfiles");
        validateProfiles(data.optJSONArray("familyMembers"), "familyMembers");
        validateAttachments(data.optJSONArray("attachments"), data.optJSONArray("attachmentBlobs"));
        validateSyncChanges(data.optJSONArray("syncChanges"));
    }

    public String copyImageUriToPrivateFile(Uri uri, String nameHint) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new IOException("无法读取图片");
        }
        File raw = createCameraCaptureFile("selected-original.jpg");
        try (InputStream in = input; FileOutputStream out = new FileOutputStream(raw)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        try {
            return compressImageFileToPrivateFile(raw, nameHint == null ? "selected.jpg" : nameHint);
        } finally {
            raw.delete();
        }
    }

    public String compressImageFileToPrivateFile(File source, String nameHint) throws IOException {
        File output = createAttachmentFile(nameHint == null ? "scan.jpg" : nameHint);
        BabyLogImageUtils.compressFileToJpeg(source, output);
        return output.getAbsolutePath();
    }

    public File createCameraCaptureFile(String nameHint) throws IOException {
        File base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File dir = new File(base == null ? context.getFilesDir() : base, "camera-captures");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建拍照目录");
        }
        String safeName = nameHint == null ? "scan.jpg" : nameHint.replaceAll("[^A-Za-z0-9._-]", "_");
        return File.createTempFile("babylog-", "-" + safeName, dir);
    }

    public File createAttachmentFile(String nameHint) {
        File dir = new File(context.getFilesDir(), "attachments");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String safeName = nameHint == null ? "scan.jpg" : nameHint.replaceAll("[^A-Za-z0-9._-]", "_");
        return new File(dir, UUID.randomUUID() + "-" + safeName);
    }

    public void clearLocalData() {
        deleteRecursively(new File(context.getFilesDir(), "attachments"));
        deleteRecursively(new File(context.getFilesDir(), "camera-captures"));
        File pictureBase = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (pictureBase != null) {
            deleteRecursively(new File(pictureBase, "camera-captures"));
        }
        repository.clearLocalData();
    }

    private long estimateAttachmentBytes() {
        return directoryBytes(new File(context.getFilesDir(), "attachments"));
    }

    private void deleteLocalFile(String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        File file = new File(path);
        if (file.isFile()) {
            file.delete();
        }
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    private long directoryBytes(File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        if (file.isFile()) {
            return file.length();
        }
        long bytes = 0;
        File[] children = file.listFiles();
        if (children == null) {
            return 0;
        }
        for (File child : children) {
            bytes += directoryBytes(child);
        }
        return bytes;
    }

    public static JSONArray sanitizeAttachmentsForBackup(JSONArray attachments, String missingAt) throws JSONException {
        JSONArray sanitized = new JSONArray();
        if (attachments == null) {
            return sanitized;
        }
        String timestamp = isBlank(missingAt) ? BabyLogFormatters.nowIso() : missingAt;
        for (int i = 0; i < attachments.length(); i++) {
            JSONObject json = attachments.optJSONObject(i);
            if (json == null) {
                continue;
            }
            JSONObject copy = new JSONObject(json.toString());
            BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.AttachmentRecord.fromJson(copy);
            if (attachment != null && attachment.deletedAt == null) {
                File file = new File(attachment.localPath);
                if (!file.isFile()) {
                    copy.put("deletedAt", timestamp);
                    copy.put("updatedAt", timestamp);
                    copy.put("ocrStatus", "missing-local-file");
                }
            }
            sanitized.put(copy);
        }
        return sanitized;
    }

    private JSONArray createAttachmentBlobBackup(JSONArray attachments) throws IOException, JSONException {
        JSONArray blobs = new JSONArray();
        if (attachments == null) {
            return blobs;
        }
        for (int i = 0; i < attachments.length(); i++) {
            BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.AttachmentRecord.fromJson(attachments.optJSONObject(i));
            if (attachment == null) {
                continue;
            }
            if (attachment.deletedAt != null) {
                continue;
            }
            File file = new File(attachment.localPath);
            if (!file.isFile()) {
                throw new IOException("附件文件不存在：" + attachment.originalName);
            }
            JSONObject blob = new JSONObject();
            blob.put("familyId", attachment.familyId);
            blob.put("attachmentId", attachment.id);
            blob.put("mimeType", attachment.mimeType);
            blob.put("byteSize", file.length());
            blob.put("createdAt", attachment.createdAt);
            blob.put("dataBase64", Base64.encodeToString(readBytes(file), Base64.NO_WRAP));
            blobs.put(blob);
        }
        return blobs;
    }

    private JSONArray restoreAttachmentBlobs(JSONArray attachments, JSONArray blobs) throws JSONException, IOException {
        if (attachments == null) {
            attachments = new JSONArray();
        }
        if (blobs == null) {
            return attachments;
        }

        Map<String, String> restoredPaths = new HashMap<>();
        for (int i = 0; i < blobs.length(); i++) {
            JSONObject blob = blobs.optJSONObject(i);
            if (blob == null) {
                continue;
            }
            String attachmentId = blob.optString("attachmentId");
            byte[] bytes = Base64.decode(blob.optString("dataBase64", ""), Base64.DEFAULT);
            File file = createAttachmentFile(attachmentId + ".jpg");
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(bytes);
            }
            restoredPaths.put(attachmentId, file.getAbsolutePath());
        }

        JSONArray restored = new JSONArray();
        for (int i = 0; i < attachments.length(); i++) {
            JSONObject attachment = attachments.optJSONObject(i);
            if (attachment == null) {
                continue;
            }
            String path = restoredPaths.get(attachment.optString("id"));
            if (path != null) {
                attachment.put("localPath", path);
                attachment.put("localBlobKey", path);
            }
            restored.put(attachment);
        }
        return restored;
    }

    private File importUndoSnapshotFile() {
        return new File(context.getFilesDir(), LAST_IMPORT_UNDO_FILE);
    }

    private void writeImportUndoSnapshot(String raw) throws IOException {
        try (FileOutputStream output = new FileOutputStream(importUndoSnapshotFile())) {
            output.write(raw.getBytes(StandardCharsets.UTF_8));
        }
    }

    private byte[] readBytes(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static void putIfNotNull(JSONObject payload, String key, Object value) throws JSONException {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private static void putStringIfNotBlank(JSONObject payload, String key, String value) throws JSONException {
        if (!isBlank(value)) {
            payload.put(key, value.trim());
        }
    }

    private static void putNumberIfNotNull(JSONObject payload, String key, Double value) throws JSONException {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private static void appendSummary(StringBuilder summary, String value) {
        if (isBlank(value)) {
            return;
        }
        if (summary.length() > 0) {
            summary.append(" · ");
        }
        summary.append(value.trim());
    }

    private static String withLabel(String label, String value) {
        return isBlank(value) ? "" : label + " " + value.trim();
    }

    private static String formatSessionWindow(String startedAtIso, String endedAtIso) {
        if (isBlank(startedAtIso) && isBlank(endedAtIso)) {
            return "";
        }
        String start = BabyLogFormatters.formatEventTime(startedAtIso);
        String end = BabyLogFormatters.formatEventTime(endedAtIso);
        if ("--:--".equals(start) && "--:--".equals(end)) {
            return "";
        }
        if ("--:--".equals(start)) {
            return end;
        }
        if ("--:--".equals(end)) {
            return start;
        }
        return start + "-" + end;
    }

    private static String formatSecondsAsMinutes(int seconds) {
        if (seconds <= 0) {
            return "";
        }
        int minutes = seconds / 60;
        int rest = seconds % 60;
        if (rest == 0) {
            return minutes + " 分钟";
        }
        if (minutes == 0) {
            return rest + " 秒";
        }
        return minutes + " 分 " + rest + " 秒";
    }

    private static void preserveContractionSessionFields(JSONObject existingPayload, JSONObject newPayload) throws JSONException {
        if (existingPayload == null || newPayload == null || isBlank(existingPayload.optString("sessionId"))) {
            return;
        }
        putStringIfNotBlank(newPayload, "entryMode", existingPayload.optString("entryMode"));
        putStringIfNotBlank(newPayload, "sessionId", existingPayload.optString("sessionId"));
        putStringIfNotBlank(newPayload, "startIso", existingPayload.optString("startIso"));
        putStringIfNotBlank(newPayload, "endIso", existingPayload.optString("endIso"));
        Double duration = newPayload.has("durationSeconds")
                ? newPayload.optDouble("durationSeconds")
                : (existingPayload.has("durationSec") ? existingPayload.optDouble("durationSec") : null);
        putNumberIfNotNull(newPayload, "durationSec", duration);
        Double intervalMinutes = newPayload.has("intervalMinutes") ? newPayload.optDouble("intervalMinutes") : null;
        if (intervalMinutes != null && !Double.isNaN(intervalMinutes) && !Double.isInfinite(intervalMinutes)) {
            newPayload.put("intervalFromPrevSec", Math.max(0, Math.round(intervalMinutes * 60.0)));
        } else if (existingPayload.has("intervalFromPrevSec")) {
            newPayload.put("intervalFromPrevSec", existingPayload.optInt("intervalFromPrevSec"));
        }
    }

    private static void validateEvents(JSONArray events) throws JSONException {
        for (int i = 0; i < events.length(); i++) {
            JSONObject json = events.optJSONObject(i);
            BabyLogDomain.BabyLogEvent event = BabyLogDomain.BabyLogEvent.fromJson(json);
            if (event == null || isBlank(event.id) || isBlank(event.eventType) || isBlank(event.occurredAt)) {
                throw new JSONException("Invalid event at index " + i);
            }
        }
    }

    private static void validateProfiles(JSONArray profiles, String label) throws JSONException {
        if (profiles == null) {
            return;
        }
        for (int i = 0; i < profiles.length(); i++) {
            JSONObject json = profiles.optJSONObject(i);
            if (json == null || isBlank(json.optString("id"))) {
                throw new JSONException("Invalid " + label + " at index " + i);
            }
        }
    }

    private static void validateAttachments(JSONArray attachments, JSONArray blobs) throws JSONException {
        if (attachments == null || attachments.length() == 0) {
            return;
        }
        Set<String> blobAttachmentIds = new HashSet<>();
        if (blobs != null) {
            for (int i = 0; i < blobs.length(); i++) {
                JSONObject blob = blobs.optJSONObject(i);
                String attachmentId = blob == null ? "" : blob.optString("attachmentId");
                if (isBlank(attachmentId) || isBlank(blob.optString("dataBase64"))) {
                    throw new JSONException("Invalid attachment blob at index " + i);
                }
                blobAttachmentIds.add(attachmentId);
            }
        }
        for (int i = 0; i < attachments.length(); i++) {
            JSONObject json = attachments.optJSONObject(i);
            BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.AttachmentRecord.fromJson(json);
            if (attachment == null || isBlank(attachment.id) || isBlank(attachment.kind) || isBlank(attachment.createdAt)) {
                throw new JSONException("Invalid attachment at index " + i);
            }
            if (attachment.deletedAt != null) {
                continue;
            }
            if (!blobAttachmentIds.contains(attachment.id)) {
                throw new JSONException("Missing attachment blob for " + attachment.id);
            }
        }
    }

    private static void validateSyncChanges(JSONArray changes) throws JSONException {
        if (changes == null) {
            return;
        }
        for (int i = 0; i < changes.length(); i++) {
            JSONObject json = changes.optJSONObject(i);
            BabyLogDomain.SyncChange change = BabyLogDomain.SyncChange.fromJson(json);
            if (change == null || isBlank(change.id) || isBlank(change.entityType) || isBlank(change.entityId)) {
                throw new JSONException("Invalid sync change at index " + i);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static long parseTime(String value) {
        return BabyLogFormatters.parseIsoMillis(value);
    }

    public static final class UltrasoundInput {
        public final String examDate;
        public final String gestationalAge;
        public final String hospital;
        public final String reportTime;
        public final String diagnosisText;
        public final String bpdMm;
        public final String hcMm;
        public final String acMm;
        public final String flMm;
        public final String efwGram;
        public final String afiCm;
        public final String deepestPocketCm;
        public final String placentaLocation;
        public final String placentaGrade;
        public final String fetalPresentation;
        public final String fetalHeartRateBpm;
        public final String fetalCount;
        public final String fetalMovement;
        public final String umbilicalInsertion;
        public final String cervicalLengthMm;
        public final String crlMm;
        public final String ntMm;
        public final String umbilicalSd;
        public final String umbilicalPi;
        public final String umbilicalRi;
        public final String photoPath;
        public final String photoName;

        public UltrasoundInput(
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
                String fetalHeartRateBpm,
                String fetalCount,
                String fetalMovement,
                String umbilicalInsertion,
                String cervicalLengthMm,
                String crlMm,
                String ntMm,
                String umbilicalSd,
                String umbilicalPi,
                String umbilicalRi,
                String photoPath,
                String photoName
        ) {
            this(
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
                    fetalHeartRateBpm,
                    fetalCount,
                    fetalMovement,
                    umbilicalInsertion,
                    cervicalLengthMm,
                    crlMm,
                    ntMm,
                    umbilicalSd,
                    umbilicalPi,
                    umbilicalRi,
                    photoPath,
                    photoName
            );
        }

        public UltrasoundInput(
                String examDate,
                String gestationalAge,
                String hospital,
                String reportTime,
                String diagnosisText,
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
                String fetalHeartRateBpm,
                String fetalCount,
                String fetalMovement,
                String umbilicalInsertion,
                String cervicalLengthMm,
                String crlMm,
                String ntMm,
                String umbilicalSd,
                String umbilicalPi,
                String umbilicalRi,
                String photoPath,
                String photoName
        ) {
            this.examDate = examDate;
            this.gestationalAge = gestationalAge;
            this.hospital = hospital;
            this.reportTime = reportTime;
            this.diagnosisText = diagnosisText;
            this.bpdMm = bpdMm;
            this.hcMm = hcMm;
            this.acMm = acMm;
            this.flMm = flMm;
            this.efwGram = efwGram;
            this.afiCm = afiCm;
            this.deepestPocketCm = deepestPocketCm;
            this.placentaLocation = placentaLocation;
            this.placentaGrade = placentaGrade;
            this.fetalPresentation = fetalPresentation;
            this.fetalHeartRateBpm = fetalHeartRateBpm;
            this.fetalCount = fetalCount;
            this.fetalMovement = fetalMovement;
            this.umbilicalInsertion = umbilicalInsertion;
            this.cervicalLengthMm = cervicalLengthMm;
            this.crlMm = crlMm;
            this.ntMm = ntMm;
            this.umbilicalSd = umbilicalSd;
            this.umbilicalPi = umbilicalPi;
            this.umbilicalRi = umbilicalRi;
            this.photoPath = photoPath;
            this.photoName = photoName;
        }
    }

    public static final class BabyCareInput {
        public final String eventType;
        public final String primary;
        public final String secondary;
        public final String tertiary;
        public final String note;

        private BabyCareInput(String eventType, String primary, String secondary, String tertiary, String note) {
            this.eventType = eventType;
            this.primary = primary;
            this.secondary = secondary;
            this.tertiary = tertiary;
            this.note = note;
        }

        public static BabyCareInput feed(String feedType, String amountMl, String note) {
            return new BabyCareInput("feed", feedType, amountMl, "", note);
        }

        public static BabyCareInput sleep(String startTime, String endTime, String place, String note) {
            return new BabyCareInput("sleep", startTime, endTime, place, note);
        }

        public static BabyCareInput diaper(String diaperType, String detail, String note) {
            return new BabyCareInput("diaper", diaperType, detail, "", note);
        }

        public static BabyCareInput temperature(String temperatureC, String measureMethod, String note) {
            return new BabyCareInput("temperature", temperatureC, measureMethod, "", note);
        }

        public static BabyCareInput medication(String medicationName, String dosage, String reason) {
            return new BabyCareInput("medication", medicationName, dosage, reason, "");
        }

        public static BabyCareInput quick(String eventType, String detail, String note) {
            return new BabyCareInput(eventType, detail, note, "", "");
        }
    }

    public static final class PregnancyInput {
        public final String eventType;
        public final String primary;
        public final String gestationalAge;
        public final String secondary;
        public final String tertiary;
        public final String note;
        public final String department;
        public final String systolicBp;
        public final String diastolicBp;
        public final String weightKg;
        public final String fundalHeightCm;
        public final String abdominalCircumferenceCm;
        public final String fetalHeartRateBpm;
        public final String fetalPresentation;
        public final String edema;
        public final String urineRoutine;
        public final String urineProtein;
        public final String hemoglobinGL;
        public final String highRiskFactors;
        public final String treatmentAdvice;
        public final String nextVisitDate;
        public final String reportType;
        public final String attachmentNote;
        public final String attachmentPath;
        public final String attachmentName;
        public final Map<String, String> screeningValues;

        private PregnancyInput(String eventType, String primary, String secondary, String tertiary, String note) {
            this(
                    eventType,
                    primary,
                    "",
                    secondary,
                    tertiary,
                    note,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    Collections.emptyMap()
            );
        }

        private PregnancyInput(
                String eventType,
                String primary,
                String gestationalAge,
                String secondary,
                String tertiary,
                String note,
                String department,
                String systolicBp,
                String diastolicBp,
                String weightKg,
                String fundalHeightCm,
                String abdominalCircumferenceCm,
                String fetalHeartRateBpm,
                String fetalPresentation,
                String edema,
                String urineRoutine,
                String urineProtein,
                String hemoglobinGL,
                String highRiskFactors,
                String treatmentAdvice,
                String nextVisitDate,
                String reportType,
                String attachmentNote,
                String attachmentPath,
                String attachmentName,
                Map<String, String> screeningValues
        ) {
            this.eventType = eventType;
            this.primary = primary == null ? "" : primary.trim();
            this.gestationalAge = gestationalAge == null ? "" : gestationalAge.trim();
            this.secondary = secondary == null ? "" : secondary.trim();
            this.tertiary = tertiary == null ? "" : tertiary.trim();
            this.note = note == null ? "" : note.trim();
            this.department = department == null ? "" : department.trim();
            this.systolicBp = systolicBp == null ? "" : systolicBp.trim();
            this.diastolicBp = diastolicBp == null ? "" : diastolicBp.trim();
            this.weightKg = weightKg == null ? "" : weightKg.trim();
            this.fundalHeightCm = fundalHeightCm == null ? "" : fundalHeightCm.trim();
            this.abdominalCircumferenceCm = abdominalCircumferenceCm == null ? "" : abdominalCircumferenceCm.trim();
            this.fetalHeartRateBpm = fetalHeartRateBpm == null ? "" : fetalHeartRateBpm.trim();
            this.fetalPresentation = fetalPresentation == null ? "" : fetalPresentation.trim();
            this.edema = edema == null ? "" : edema.trim();
            this.urineRoutine = urineRoutine == null ? "" : urineRoutine.trim();
            this.urineProtein = urineProtein == null ? "" : urineProtein.trim();
            this.hemoglobinGL = hemoglobinGL == null ? "" : hemoglobinGL.trim();
            this.highRiskFactors = highRiskFactors == null ? "" : highRiskFactors.trim();
            this.treatmentAdvice = treatmentAdvice == null ? "" : treatmentAdvice.trim();
            this.nextVisitDate = nextVisitDate == null ? "" : nextVisitDate.trim();
            this.reportType = reportType == null ? "" : reportType.trim();
            this.attachmentNote = attachmentNote == null ? "" : attachmentNote.trim();
            this.attachmentPath = attachmentPath == null ? "" : attachmentPath.trim();
            this.attachmentName = attachmentName == null ? "" : attachmentName.trim();
            this.screeningValues = trimScreeningValues(screeningValues);
        }

        public static PregnancyInput checkup(String checkupDate, String provider, String finding, String nextVisitNote) {
            return new PregnancyInput("pregnancy_checkup", checkupDate, provider, finding, nextVisitNote);
        }

        public static PregnancyInput checkupStructured(
                String checkupDate,
                String gestationalAge,
                String provider,
                String department,
                String systolicBp,
                String diastolicBp,
                String weightKg,
                String fundalHeightCm,
                String abdominalCircumferenceCm,
                String fetalHeartRateBpm,
                String fetalPresentation,
                String edema,
                String urineRoutine,
                String urineProtein,
                String hemoglobinGL,
                String highRiskFactors,
                String doctorConclusion,
                String treatmentAdvice,
                String nextVisitDate,
                String reportType,
                String attachmentNote,
                String note,
                String attachmentPath,
                String attachmentName
        ) {
            return new PregnancyInput(
                    "pregnancy_checkup",
                    checkupDate,
                    gestationalAge,
                    provider,
                    doctorConclusion,
                    note,
                    department,
                    systolicBp,
                    diastolicBp,
                    weightKg,
                    fundalHeightCm,
                    abdominalCircumferenceCm,
                    fetalHeartRateBpm,
                    fetalPresentation,
                    edema,
                    urineRoutine,
                    urineProtein,
                    hemoglobinGL,
                    highRiskFactors,
                    treatmentAdvice,
                    nextVisitDate,
                    reportType,
                    attachmentNote,
                    attachmentPath,
                    attachmentName,
                    Collections.emptyMap()
            );
        }

        public static PregnancyInput screening(
                String eventType,
                String screeningDate,
                String gestationalAge,
                Map<String, String> values,
                String note,
                String attachmentPath,
                String attachmentName
        ) {
            return new PregnancyInput(
                    eventType,
                    screeningDate,
                    gestationalAge,
                    "",
                    "",
                    note,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    attachmentPath,
                    attachmentName,
                    values
            );
        }

        public static PregnancyInput fetalMovement(String movementWindow, String movementCount, String note) {
            return new PregnancyInput("fetal_movement", movementWindow, movementCount, "", note);
        }

        public static PregnancyInput contraction(String startTime, String intervalMinutes, String durationSeconds, String note) {
            return new PregnancyInput("contraction", startTime, intervalMinutes, durationSeconds, note);
        }

        private static Map<String, String> trimScreeningValues(Map<String, String> values) {
            Map<String, String> result = new LinkedHashMap<>();
            if (values == null) {
                return result;
            }
            for (Map.Entry<String, String> entry : values.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null && !value.trim().isEmpty()) {
                    result.put(key.trim(), value.trim());
                }
            }
            return result;
        }
    }

    public static final class FetalMovementSessionInput {
        public final String startedAtIso;
        public final String endedAtIso;
        public final int count;
        public final int durationMinutes;
        public final int targetCount;
        public final String note;

        private FetalMovementSessionInput(
                String startedAtIso,
                String endedAtIso,
                int count,
                int durationMinutes,
                int targetCount,
                String note
        ) {
            this.startedAtIso = startedAtIso == null ? "" : startedAtIso.trim();
            this.endedAtIso = endedAtIso == null ? "" : endedAtIso.trim();
            this.count = Math.max(0, count);
            this.durationMinutes = Math.max(0, durationMinutes);
            this.targetCount = Math.max(1, targetCount);
            this.note = note == null ? "" : note.trim();
        }

        public static FetalMovementSessionInput create(
                String startedAtIso,
                String endedAtIso,
                int count,
                int durationMinutes,
                int targetCount,
                String note
        ) {
            return new FetalMovementSessionInput(startedAtIso, endedAtIso, count, durationMinutes, targetCount, note);
        }
    }

    public static final class ContractionSessionInput {
        public final String sessionId;
        public final List<ContractionEntryInput> entries;

        private ContractionSessionInput(String sessionId, List<ContractionEntryInput> entries) {
            this.sessionId = isBlank(sessionId) ? UUID.randomUUID().toString() : sessionId.trim();
            this.entries = entries == null ? Collections.emptyList() : new ArrayList<>(entries);
        }

        public static ContractionSessionInput create(String sessionId, List<ContractionEntryInput> entries) {
            return new ContractionSessionInput(sessionId, entries);
        }
    }

    public static final class ContractionEntryInput {
        public final String startIso;
        public final String endIso;
        public final int durationSec;
        public final Integer intervalFromPrevSec;

        private ContractionEntryInput(String startIso, String endIso, int durationSec, Integer intervalFromPrevSec) {
            this.startIso = startIso == null ? "" : startIso.trim();
            this.endIso = endIso == null ? "" : endIso.trim();
            this.durationSec = Math.max(0, durationSec);
            this.intervalFromPrevSec = intervalFromPrevSec == null ? null : Math.max(0, intervalFromPrevSec);
        }

        public static ContractionEntryInput create(String startIso, String endIso, int durationSec, Integer intervalFromPrevSec) {
            return new ContractionEntryInput(startIso, endIso, durationSec, intervalFromPrevSec);
        }
    }

    public static final class MaternalMetricInput {
        public final String weightKg;
        public final String systolicBp;
        public final String diastolicBp;
        public final String glucoseMmolL;
        public final String glucoseContext;
        public final String note;

        private MaternalMetricInput(
                String weightKg,
                String systolicBp,
                String diastolicBp,
                String glucoseMmolL,
                String glucoseContext,
                String note
        ) {
            this.weightKg = weightKg;
            this.systolicBp = systolicBp;
            this.diastolicBp = diastolicBp;
            this.glucoseMmolL = glucoseMmolL;
            this.glucoseContext = glucoseContext;
            this.note = note;
        }

        public static MaternalMetricInput create(
                String weightKg,
                String systolicBp,
                String diastolicBp,
                String glucoseMmolL,
                String glucoseContext,
                String note
        ) {
            return new MaternalMetricInput(weightKg, systolicBp, diastolicBp, glucoseMmolL, glucoseContext, note);
        }
    }

    public static final class QuickAction {
        public final String label;
        public final String hint;
        public final int toneColor;
        public final String eventType;

        public QuickAction(String label, String hint, int toneColor, String eventType) {
            this.label = label;
            this.hint = hint;
            this.toneColor = toneColor;
            this.eventType = eventType;
        }
    }

    public static final class DashboardSnapshot {
        public final List<BabyLogDomain.BabyLogEvent> recentEvents;
        public final List<BabyLogDomain.AttachmentRecord> attachments;
        public final Map<String, Integer> todayCounts;
        public final int pendingSyncCount;
        public final int failedSyncCount;
        public final int syncedSyncCount;
        public final String lastPulledAt;
        public final int remoteUpdateBannerCount;
        public final long localBytes;
        public final long freeBytes;

        DashboardSnapshot(
                List<BabyLogDomain.BabyLogEvent> recentEvents,
                List<BabyLogDomain.AttachmentRecord> attachments,
                Map<String, Integer> todayCounts,
                int pendingSyncCount,
                int failedSyncCount,
                int syncedSyncCount,
                String lastPulledAt,
                int remoteUpdateBannerCount,
                long localBytes,
                long freeBytes
        ) {
            this.recentEvents = recentEvents;
            this.attachments = attachments;
            this.todayCounts = todayCounts;
            this.pendingSyncCount = pendingSyncCount;
            this.failedSyncCount = failedSyncCount;
            this.syncedSyncCount = syncedSyncCount;
            this.lastPulledAt = lastPulledAt == null ? "" : lastPulledAt;
            this.remoteUpdateBannerCount = remoteUpdateBannerCount;
            this.localBytes = localBytes;
            this.freeBytes = freeBytes;
        }
    }

}
