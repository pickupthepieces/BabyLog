package app.babylog.nativeapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

public final class BabyLogService {
    public static final String BACKUP_FORMAT = "babylog.backup";
    public static final int BACKUP_VERSION = 1;
    public static final int TRASH_RETENTION_DAYS = 7;
    private static final String TAG = "BabyLog";
    private static final long TRASH_RETENTION_MS = TRASH_RETENTION_DAYS * 24L * 60L * 60L * 1000L;

    private final Context context;
    private final BabyLogRepository repository;
    private final BabyLogAttachmentInputBuilder attachmentBuilder;
    private final BabyLogBackupManager backupManager;
    private final BabyLogSyncTrigger syncTrigger;

    public BabyLogService(Context context, BabyLogRepository repository) { this(context, repository, BabyLogSyncTrigger.noop()); }

    public BabyLogService(Context context, BabyLogRepository repository, BabyLogSyncTrigger syncTrigger) {
        this.context = context.getApplicationContext();
        this.repository = repository;
        this.attachmentBuilder = new BabyLogAttachmentInputBuilder(this.context);
        this.backupManager = new BabyLogBackupManager(this.context, repository, attachmentBuilder);
        this.syncTrigger = syncTrigger == null ? BabyLogSyncTrigger.noop() : syncTrigger;
    }

    private BabyLogService(BabyLogRepository repository) { this(repository, BabyLogSyncTrigger.noop()); }

    public static BabyLogService forSmokeTest(BabyLogRepository repository) { return new BabyLogService(repository); }
    public static BabyLogService forSmokeTest(BabyLogRepository repository, BabyLogSyncTrigger syncTrigger) { return new BabyLogService(repository, syncTrigger); }
    private BabyLogService(BabyLogRepository repository, BabyLogSyncTrigger syncTrigger) {
        File filesDir = new File(System.getProperty("java.io.tmpdir"), "babylog-service-smoke-" + System.nanoTime());
        this.context = null; this.repository = repository; this.attachmentBuilder = BabyLogAttachmentInputBuilder.forSmokeTest(filesDir);
        this.backupManager = new BabyLogBackupManager(filesDir, repository, attachmentBuilder);
        this.syncTrigger = syncTrigger == null ? BabyLogSyncTrigger.noop() : syncTrigger;
    }

    public BabyLogDomain.BabyLogEvent recordQuickEvent(QuickAction action) throws BabyLogException {
        if (action == null) {
            throw new BabyLogException.ValidationException("快捷记录不能为空");
        }
        try {
            BabyLogDomain.BabyLogEvent event = BabyLogQuickEventRecorder.record(repository, action);
            onSuccessfulWrite();
            return event;
        } catch (JSONException error) {
            throw storageFailure("保存快捷记录失败", error);
        }
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

    public BabyLogDomain.BabyLogEvent recordBabyCareEvent(BabyCareInput input) throws BabyLogException {
        return recordBabyCareEvent(input, "");
    }

    public BabyLogDomain.BabyLogEvent recordBabyCareEvent(BabyCareInput input, String selectedDate) throws BabyLogException {
        if (!hasBabyCareMinimumContent(input)) {
            throw new BabyLogException.ValidationException("请至少填写一项记录内容");
        }
        try {
            JSONObject payload = buildBabyCarePayload(input);
            BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                    input.eventType,
                    babyCareOccurredAt(input, selectedDate, ""),
                    payload,
                    Collections.emptyList(),
                    "manual"
            );
            saveEventWithSyncChange(event);
            return event;
        } catch (JSONException error) {
            throw storageFailure("保存记录失败", error);
        }
    }

    public BabyLogDomain.BabyLogEvent updateBabyCareEvent(String eventId, BabyCareInput input) throws BabyLogException {
        if (!hasBabyCareMinimumContent(input)) {
            throw new BabyLogException.ValidationException("请至少填写一项记录内容");
        }
        BabyLogDomain.BabyLogEvent existing = requireEditableEvent(eventId, input.eventType);
        try {
            BabyLogDomain.BabyLogEvent event = createEditedEvent(
                    existing,
                    input.eventType,
                    buildBabyCarePayload(input),
                    existing.attachmentIds,
                    babyCareOccurredAt(input, BabyLogFormatters.recordDay(existing.occurredAt), existing.occurredAt)
            );
            saveEventWithSyncChange(event);
            return event;
        } catch (JSONException error) {
            throw storageFailure("更新记录失败", error);
        }
    }

    public BabyLogDomain.BabyLogEvent recordPregnancyEvent(PregnancyInput input) throws BabyLogException {
        if (!hasPregnancyMinimumContent(input)) {
            throw new BabyLogException.ValidationException("请至少填写一项记录内容");
        }
        try {
            JSONObject payload = buildPregnancyPayload(input);
            String occurredAt = isPregnancyDocumentEvent(input.eventType) && BabyLogFormatters.isValidDateInput(input.primary)
                    ? BabyLogFormatters.createOccurredAtFromDate(input.primary)
                    : BabyLogFormatters.nowIso();
            List<BabyLogDomain.AttachmentRecord> attachments = attachmentBuilder.createPregnancyAttachments(input);
            BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                    input.eventType,
                    occurredAt,
                    payload,
                    BabyLogAttachmentInputBuilder.attachmentIdsFromRecords(attachments),
                    "manual"
            );
            saveEventWithAttachmentsAndSyncChanges(event, attachments);
            return event;
        } catch (JSONException error) {
            throw storageFailure("保存记录失败", error);
        }
    }

    public BabyLogDomain.BabyLogEvent updatePregnancyEvent(String eventId, PregnancyInput input) throws BabyLogException {
        if (!hasPregnancyMinimumContent(input)) {
            throw new BabyLogException.ValidationException("请至少填写一项记录内容");
        }
        BabyLogDomain.BabyLogEvent existing = requireEditableEvent(eventId, input.eventType);
        try {
            List<String> attachmentIds = new ArrayList<>(existing.attachmentIds);
            List<BabyLogDomain.AttachmentRecord> attachments = attachmentBuilder.createPregnancyAttachments(input);
            attachmentIds.addAll(BabyLogAttachmentInputBuilder.attachmentIdsFromRecords(attachments));
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
        } catch (JSONException error) {
            throw storageFailure("更新记录失败", error);
        }
    }

    public BabyLogDomain.BabyLogEvent recordFetalMovementSession(FetalMovementSessionInput input) throws BabyLogException {
        try {
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
        } catch (JSONException error) {
            throw storageFailure("保存胎动计数失败", error);
        }
    }

    public List<BabyLogDomain.BabyLogEvent> recordContractionSession(ContractionSessionInput input) throws BabyLogException {
        if (input == null || input.entries.isEmpty()) {
            throw new BabyLogException.ValidationException("请至少记录一次宫缩");
        }
        try {
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
                throw new BabyLogException.StorageException("保存宫缩会话失败");
            }
            onSuccessfulWrite();
            return events;
        } catch (JSONException error) {
            throw storageFailure("保存宫缩会话失败", error);
        }
    }

    public BabyLogDomain.BabyLogEvent recordMaternalMetric(MaternalMetricInput input) throws BabyLogException {
        if (!hasMaternalMetricMinimumContent(input)) {
            throw new BabyLogException.ValidationException("请至少填写一项孕妈指标或备注");
        }
        try {
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
        } catch (JSONException error) {
            throw storageFailure("保存孕妈指标失败", error);
        }
    }

    public BabyLogDomain.BabyLogEvent updateMaternalMetric(String eventId, MaternalMetricInput input) throws BabyLogException {
        if (!hasMaternalMetricMinimumContent(input)) {
            throw new BabyLogException.ValidationException("请至少填写一项孕妈指标或备注");
        }
        BabyLogDomain.BabyLogEvent existing = requireEditableEvent(eventId, "maternal_metric");
        try {
            BabyLogDomain.BabyLogEvent event = createEditedEvent(
                    existing,
                    "maternal_metric",
                    buildMaternalMetricPayload(input),
                    existing.attachmentIds
            );
            saveEventWithSyncChange(event);
            return event;
        } catch (JSONException error) {
            throw storageFailure("更新孕妈指标失败", error);
        }
    }

    public BabyLogDomain.BabyLogEvent deleteEvent(String eventId) throws BabyLogException {
        BabyLogDomain.BabyLogEvent event = repository.findEventById(eventId);
        if (event == null || event.deletedAt != null) {
            throw new BabyLogException.NotFoundException("记录不存在或已删除");
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
        try {
            if (!repository.putEventProfileAttachmentsAndSyncChanges(deleted, profileUpdate, attachments, changes)) {
                throw new BabyLogException.StorageException("删除记录失败");
            }
        } catch (JSONException error) {
            throw storageFailure("删除记录失败", error);
        }
        onSuccessfulWrite();
        return deleted;
    }

    public BabyLogDomain.BabyLogEvent restoreEvent(String eventId) throws BabyLogException {
        BabyLogDomain.BabyLogEvent event = repository.findEventById(eventId);
        if (event == null || event.deletedAt == null) {
            throw new BabyLogException.NotFoundException("记录不存在或不在回收站");
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
        try {
            if (!repository.putEventProfileAttachmentsAndSyncChanges(restored, profileUpdate, attachments, changes)) {
                throw new BabyLogException.StorageException("恢复记录失败");
            }
        } catch (JSONException error) {
            throw storageFailure("恢复记录失败", error);
        }
        onSuccessfulWrite();
        return restored;
    }

    private void saveChildProfileWithSync(BabyLogDomain.ChildProfile profile) throws BabyLogException {
        BabyLogDomain.ChildProfile next = profile == null ? BabyLogDomain.ChildProfile.empty() : profile;
        try {
            repository.saveChildProfile(next);
            repository.putSyncChange(BabyLogDomain.createSyncChange("childProfile", next.id, "upsert"));
        } catch (JSONException error) {
            throw storageFailure("保存档案失败", error);
        }
        onSuccessfulWrite();
    }

    public boolean saveEventWithSyncChange(BabyLogDomain.BabyLogEvent event) throws BabyLogException {
        return saveEventWithAttachmentsAndSyncChanges(event, Collections.emptyList());
    }

    private boolean saveEventWithAttachmentsAndSyncChanges(
            BabyLogDomain.BabyLogEvent event,
            List<BabyLogDomain.AttachmentRecord> attachments
    ) throws BabyLogException {
        return saveEventWithAttachmentsAndOptionalChildProfile(event, attachments, null);
    }

    private boolean saveEventWithAttachmentsAndOptionalChildProfile(
            BabyLogDomain.BabyLogEvent event,
            List<BabyLogDomain.AttachmentRecord> attachments,
            BabyLogDomain.ChildProfile childProfile
    ) throws BabyLogException {
        if (event == null) {
            throw new BabyLogException.ValidationException("记录不能为空");
        }
        List<BabyLogDomain.SyncChange> changes = createSyncChangesForEventUpsert(event, attachments, childProfile);
        try {
            boolean ok = repository.putEventProfileAttachmentsAndSyncChanges(event, childProfile, attachments, changes);
            if (!ok) {
                throw new BabyLogException.StorageException("保存记录失败");
            }
        } catch (JSONException error) {
            throw storageFailure("保存记录失败", error);
        }
        onSuccessfulWrite();
        return true;
    }

    private void onSuccessfulWrite() {
        try { syncTrigger.triggerAfterLocalWrite(); } catch (RuntimeException error) { Log.w(TAG, "Sync trigger failed after local write", error); }
    }

    private static BabyLogException.StorageException storageFailure(String message, JSONException error) {
        return new BabyLogException.StorageException(message, error);
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

    public static OptionalInt sleepDurationMinutes(BabyLogDomain.BabyLogEvent event) {
        if (event == null || !"sleep".equals(event.eventType)) {
            return OptionalInt.empty();
        }
        String start = event.payload.optString("sleepStart");
        String end = event.payload.optString("sleepEnd");
        if (isBlank(start) || isBlank(end)) {
            return OptionalInt.empty();
        }
        long startMillis = BabyLogFormatters.parseIsoMillis(start);
        long endMillis = BabyLogFormatters.parseIsoMillis(end);
        if (startMillis > 0L && endMillis > 0L) {
            long diffMillis = endMillis - startMillis;
            if (diffMillis < 0L) {
                diffMillis += 24L * 60L * 60L * 1000L;
            }
            return OptionalInt.of((int) Math.max(0L, diffMillis / 60_000L));
        }
        Integer startMinute = parseClockMinute(start);
        Integer endMinute = parseClockMinute(end);
        if (startMinute == null || endMinute == null) {
            return OptionalInt.empty();
        }
        int diff = endMinute - startMinute;
        if (diff < 0) {
            diff += 24 * 60;
        }
        return OptionalInt.of(diff);
    }

    public static JSONObject buildBabyCarePayload(BabyCareInput input) throws JSONException {
        return BabyLogBabyCareRecords.buildPayload(input);
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
        return payload;
    }

    public static String formatBabyCareSummary(BabyCareInput input) {
        return BabyLogBabyCareRecords.formatSummary(input);
    }

    public static Map<String, String> babyCareDraftFields(String eventType, JSONObject payload) {
        return BabyLogBabyCareRecords.draftFields(eventType, payload);
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
        return BabyLogAttachmentInputBuilder.hasUsableUltrasoundPhoto(input.photoPath)
                || BabyLogFormatters.parseOptionalNumber(input.bpdMm) != null
                || BabyLogFormatters.parseOptionalNumber(input.hcMm) != null
                || BabyLogFormatters.parseOptionalNumber(input.acMm) != null
                || BabyLogFormatters.parseOptionalNumber(input.flMm) != null
                || BabyLogFormatters.parseOptionalNumber(input.efwGram) != null;
    }

    public static boolean hasBabyCareMinimumContent(BabyCareInput input) {
        return BabyLogBabyCareRecords.hasMinimumContent(input);
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

    public BabyLogDomain.BabyLogEvent recordUltrasound(UltrasoundInput input) throws BabyLogException {
        if (!hasUltrasoundMinimumContent(input)) {
            throw new BabyLogException.ValidationException("请先选择 B 超单图片，或填写至少一个生长指标");
        }
        try {
            List<BabyLogDomain.AttachmentRecord> attachments = attachmentBuilder.createUltrasoundAttachments(input);
            BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                    "ultrasound",
                    BabyLogFormatters.createOccurredAtFromDate(input.examDate),
                    buildUltrasoundPayload(input),
                    BabyLogAttachmentInputBuilder.attachmentIdsFromRecords(attachments),
                    "manual"
            );
            saveEventWithAttachmentsAndSyncChanges(event, attachments);
            return event;
        } catch (JSONException error) {
            throw storageFailure("保存 B 超记录失败", error);
        }
    }

    public BabyLogDomain.BabyLogEvent updateUltrasound(String eventId, UltrasoundInput input) throws BabyLogException {
        BabyLogDomain.BabyLogEvent existing = requireEditableEvent(eventId, "ultrasound");
        if (!hasUltrasoundMinimumContent(input) && existing.attachmentIds.isEmpty()) {
            throw new BabyLogException.ValidationException("请先选择 B 超单图片，或填写至少一个生长指标");
        }
        try {
            List<String> attachmentIds = new ArrayList<>(existing.attachmentIds);
            List<BabyLogDomain.AttachmentRecord> attachments = attachmentBuilder.createUltrasoundAttachments(input);
            attachmentIds.addAll(BabyLogAttachmentInputBuilder.attachmentIdsFromRecords(attachments));
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
        } catch (JSONException error) {
            throw storageFailure("更新 B 超记录失败", error);
        }
    }

    public static BabyLogDomain.BabyLogEvent createEditedEvent(
            BabyLogDomain.BabyLogEvent existing,
            String expectedEventType,
            JSONObject payload,
            List<String> attachmentIds
    ) throws BabyLogException {
        return createEditedEvent(existing, expectedEventType, payload, attachmentIds, existing == null ? null : existing.occurredAt);
    }

    public static BabyLogDomain.BabyLogEvent createEditedEvent(
            BabyLogDomain.BabyLogEvent existing,
            String expectedEventType,
            JSONObject payload,
            List<String> attachmentIds,
            String occurredAt
    ) throws BabyLogException {
        if (existing == null || existing.deletedAt != null) {
            throw new BabyLogException.NotFoundException("记录不存在或已删除");
        }
        if (!existing.eventType.equals(expectedEventType)) {
            throw new BabyLogException.ValidationException("记录类型不匹配");
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

    private BabyLogDomain.BabyLogEvent requireEditableEvent(String eventId, String expectedEventType) throws BabyLogException {
        BabyLogDomain.BabyLogEvent existing = repository.findEventById(eventId);
        if (existing == null || existing.deletedAt != null) {
            throw new BabyLogException.NotFoundException("记录不存在或已删除");
        }
        if (!expectedEventType.equals(existing.eventType)) {
            throw new BabyLogException.ValidationException("记录类型不匹配");
        }
        return existing;
    }

    private static String babyCareOccurredAt(BabyCareInput input, String dateInput, String fallbackOccurredAt) {
        if (input != null
                && BabyLogFormatters.isValidDateInput(dateInput)
                && BabyLogFormatters.isValidTimeInput(input.occurredTime)) {
            return BabyLogFormatters.createOccurredAtFromDateTime(dateInput, input.occurredTime);
        }
        return isBlank(fallbackOccurredAt) ? BabyLogFormatters.nowIso() : fallbackOccurredAt;
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
        int pendingAttachmentUploads = 0;
        long pendingAttachmentBytes = 0;
        for (BabyLogDomain.SyncChange change : changes) {
            if ("pending".equals(change.status) || "failed".equals(change.status) || "metadata_synced_file_pending".equals(change.status)) {
                pending += 1;
            }
            if ("failed".equals(change.status)) {
                failed += 1;
            }
            if ("synced".equals(change.status)) {
                synced += 1;
            }
            if ("attachment".equals(change.entityType) && "upsert".equals(change.operation)
                    && ("pending".equals(change.status) || "failed".equals(change.status) || "metadata_synced_file_pending".equals(change.status))) {
                BabyLogDomain.AttachmentRecord attachment = repository.findAttachmentById(change.entityId);
                if (attachment != null && attachment.deletedAt == null) {
                    pendingAttachmentUploads += 1;
                    pendingAttachmentBytes += Math.max(0L, attachment.byteSize);
                }
            }
        }
        return new DashboardSnapshot(
                events,
                attachments,
                summarizeToday(),
                pending,
                failed,
                synced,
                pendingAttachmentUploads,
                pendingAttachmentBytes,
                repository.listAttachmentDownloadQueue().size(),
                repository.loadSyncLastPulledAt(),
                repository.loadRemoteUpdateBannerCount(),
                repository.estimateLocalBytes() + estimateAttachmentBytes(),
                context.getFilesDir().getUsableSpace()
        );
    }

    public DashboardSnapshot refreshDashboardOnly() {
        return loadDashboard();
    }

    public void dismissRemoteUpdateBanner() {
        repository.dismissRemoteUpdateBanner();
    }

    public List<BabyLogDomain.BabyLogEvent> listRecentEvents(int limit) {
        return repository.listEvents(limit, 0);
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

    public BabyLogDailyBabySummary dailyBabySummary(String dateInput) {
        return BabyLogDailySummaryCalculator.calculate(repository.listEvents(), dateInput);
    }

    public String createBackupJson() throws BabyLogException {
        return backupManager.createBackupJson();
    }

    public int importBackupJson(String raw) throws BabyLogException {
        return backupManager.importBackupJson(raw);
    }

    public boolean hasImportUndoSnapshot() {
        return backupManager.hasImportUndoSnapshot();
    }

    public int undoLastImport() throws BabyLogException {
        return backupManager.undoLastImport();
    }

    public static void validateBackupDataForImport(JSONObject data) throws BabyLogException.ValidationException {
        BabyLogBackupManager.validateBackupDataForImport(data);
    }

    public String copyImageUriToPrivateFile(Uri uri, String nameHint) throws IOException {
        return attachmentBuilder.copyImageUriToPrivateFile(uri, nameHint);
    }

    public String compressImageFileToPrivateFile(File source, String nameHint) throws IOException {
        return attachmentBuilder.compressImageFileToPrivateFile(source, nameHint);
    }

    public File createCameraCaptureFile(String nameHint) throws IOException {
        return attachmentBuilder.createCameraCaptureFile(nameHint);
    }

    public File createAttachmentFile(String nameHint) {
        return attachmentBuilder.createAttachmentFile(nameHint);
    }

    public void clearLocalData() {
        attachmentBuilder.clearLocalAttachmentFiles();
        repository.clearLocalData();
    }

    private long estimateAttachmentBytes() {
        return attachmentBuilder.estimateAttachmentBytes();
    }

    private void deleteLocalFile(String path) {
        attachmentBuilder.deleteLocalFile(path);
    }

    public static JSONArray sanitizeAttachmentsForBackup(JSONArray attachments, String missingAt) throws JSONException {
        return BabyLogBackupManager.sanitizeAttachmentsForBackup(attachments, missingAt);
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

    private static void appendGrowthSummary(StringBuilder summary, Double value, String label, String unit) {
        if (value == null) {
            return;
        }
        appendSummary(summary, label + " " + BabyLogFormatters.formatNumber(value) + " " + unit);
    }

    private static void putDraftField(Map<String, String> values, String key, String value) {
        if (!isBlank(value)) {
            values.put(key, value.trim());
        }
    }

    private static String payloadNumberText(JSONObject payload, String key) {
        if (payload == null || !payload.has(key)) {
            return "";
        }
        double value = payload.optDouble(key, Double.NaN);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "";
        }
        return BabyLogFormatters.formatNumber(value);
    }

    private static boolean isBreastMilkFeed(String feedType) {
        return !isBlank(feedType) && feedType.contains("母乳");
    }

    private static boolean isSolidFoodFeed(String feedType) {
        return !isBlank(feedType) && feedType.contains("辅");
    }

    private static String normalizeBreastSide(String raw) {
        if (isBlank(raw)) {
            return "";
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if ("左".equals(raw.trim()) || "LEFT".equals(value) || "L".equals(value)) {
            return "L";
        }
        if ("右".equals(raw.trim()) || "RIGHT".equals(value) || "R".equals(value)) {
            return "R";
        }
        if ("双侧".equals(raw.trim()) || "两侧".equals(raw.trim()) || "BOTH".equals(value)) {
            return "BOTH";
        }
        return raw.trim();
    }

    private static String normalizeDiaperColor(String raw) {
        if (isBlank(raw)) {
            return "";
        }
        String normalized = raw.trim();
        if (normalized.startsWith("黄")) return "黄";
        if (normalized.startsWith("绿")) return "绿";
        if (normalized.startsWith("黑")) return "黑";
        if (normalized.startsWith("红")) return "红";
        if (normalized.startsWith("白")) return "白";
        if (normalized.startsWith("其它") || normalized.startsWith("其他")) return "其它";
        return "";
    }

    private static String normalizeDiaperConsistency(String raw) {
        if (isBlank(raw)) {
            return "";
        }
        String normalized = raw.trim();
        if (normalized.contains("水样")) return "水样";
        if (normalized.contains("稀")) return "稀";
        if (normalized.contains("软")) return "软便";
        if (normalized.contains("成型")) return "成型";
        return "";
    }

    private static String joinNonBlank(String first, String second) {
        boolean hasFirst = !isBlank(first);
        boolean hasSecond = !isBlank(second);
        if (hasFirst && hasSecond) {
            return first.trim() + " / " + second.trim();
        }
        if (hasFirst) {
            return first.trim();
        }
        if (hasSecond) {
            return second.trim();
        }
        return "";
    }

    private static Integer parseClockMinute(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        int timeStart = Math.max(normalized.lastIndexOf('T'), normalized.lastIndexOf(' '));
        if (timeStart >= 0 && normalized.length() >= timeStart + 6) {
            normalized = normalized.substring(timeStart + 1, timeStart + 6);
        }
        if (normalized.length() < 5 || normalized.charAt(2) != ':') {
            return null;
        }
        try {
            int hour = Integer.parseInt(normalized.substring(0, 2));
            int minute = Integer.parseInt(normalized.substring(3, 5));
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return hour * 60 + minute;
        } catch (NumberFormatException ignored) {
            return null;
        }
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
        public final String occurredTime;
        public final String checkupInstitution;
        public final String checkupConclusion;
        public final String nextCheckupDate;

        private BabyCareInput(String eventType, String primary, String secondary, String tertiary, String note) {
            this(eventType, primary, secondary, tertiary, note, "", "", "", "");
        }

        private BabyCareInput(
                String eventType,
                String primary,
                String secondary,
                String tertiary,
                String note,
                String occurredTime
        ) {
            this(eventType, primary, secondary, tertiary, note, occurredTime, "", "", "");
        }

        private BabyCareInput(
                String eventType,
                String primary,
                String secondary,
                String tertiary,
                String note,
                String occurredTime,
                String checkupInstitution,
                String checkupConclusion,
                String nextCheckupDate
        ) {
            this.eventType = eventType;
            this.primary = primary;
            this.secondary = secondary;
            this.tertiary = tertiary;
            this.note = note;
            this.occurredTime = occurredTime;
            this.checkupInstitution = checkupInstitution;
            this.checkupConclusion = checkupConclusion;
            this.nextCheckupDate = nextCheckupDate;
        }

        public BabyCareInput withOccurredTime(String occurredTime) {
            return new BabyCareInput(
                    eventType,
                    primary,
                    secondary,
                    tertiary,
                    note,
                    occurredTime == null ? "" : occurredTime,
                    checkupInstitution,
                    checkupConclusion,
                    nextCheckupDate
            );
        }

        public static BabyCareInput feed(String feedType, String amountMl, String note) {
            return feed(feedType, amountMl, "", note);
        }

        public static BabyCareInput feed(String feedType, String amountMl, String sideOrSolidFood, String note) {
            return new BabyCareInput("feed", feedType, amountMl, sideOrSolidFood, note);
        }

        public static BabyCareInput sleep(String startTime, String endTime, String place, String note) {
            return new BabyCareInput("sleep", startTime, endTime, place, note);
        }

        public static BabyCareInput diaper(String diaperType, String detail, String note) {
            return new BabyCareInput("diaper", diaperType, detail, "", note);
        }

        public static BabyCareInput diaper(String diaperType, String detail, String observation, String note) {
            return new BabyCareInput("diaper", diaperType, detail, observation, note);
        }

        public static BabyCareInput breastfeed(String leftMinutes, String rightMinutes, String note) {
            return new BabyCareInput("breastfeed", leftMinutes, rightMinutes, "", note);
        }

        public static BabyCareInput bottle(String amountMl, String brand, String note) {
            return new BabyCareInput("bottle", amountMl, brand, "", note);
        }

        public static BabyCareInput temperature(String temperatureC, String measureMethod, String note) {
            return new BabyCareInput("temperature", temperatureC, measureMethod, "", note);
        }

        public static BabyCareInput medication(String medicationName, String dosage, String reason) {
            return new BabyCareInput("medication", medicationName, dosage, reason, "");
        }

        public static BabyCareInput growth(String weightKg, String heightCm, String headCircumferenceCm, String note) {
            return new BabyCareInput("growth", weightKg, heightCm, headCircumferenceCm, note);
        }

        public static BabyCareInput childCheckup(
                String weightKg,
                String heightCm,
                String headCircumferenceCm,
                String checkupInstitution,
                String checkupConclusion,
                String nextCheckupDate,
                String note
        ) {
            return new BabyCareInput(
                    "child_checkup",
                    weightKg,
                    heightCm,
                    headCircumferenceCm,
                    note,
                    "",
                    checkupInstitution,
                    checkupConclusion,
                    nextCheckupDate
            );
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
        public final int pendingAttachmentUploadCount;
        public final long pendingAttachmentUploadBytes;
        public final int pendingAttachmentDownloadCount;
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
                int pendingAttachmentUploadCount,
                long pendingAttachmentUploadBytes,
                int pendingAttachmentDownloadCount,
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
            this.pendingAttachmentUploadCount = pendingAttachmentUploadCount;
            this.pendingAttachmentUploadBytes = pendingAttachmentUploadBytes;
            this.pendingAttachmentDownloadCount = pendingAttachmentDownloadCount;
            this.lastPulledAt = lastPulledAt == null ? "" : lastPulledAt;
            this.remoteUpdateBannerCount = remoteUpdateBannerCount;
            this.localBytes = localBytes;
            this.freeBytes = freeBytes;
        }
    }

}
