package app.babylog.nativeapp;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class BabyLogBackupManager {
    private static final String LAST_IMPORT_UNDO_FILE = "last-import-undo.json";

    private final Context context;
    private final BabyLogRepository repository;
    private final BabyLogAttachmentInputBuilder attachmentBuilder;

    BabyLogBackupManager(
            Context context,
            BabyLogRepository repository,
            BabyLogAttachmentInputBuilder attachmentBuilder
    ) {
        this.context = context;
        this.repository = repository;
        this.attachmentBuilder = attachmentBuilder;
    }

    String createBackupJson() throws BabyLogException {
        try {
            JSONObject data = new JSONObject();
            data.put("familyProfiles", repository.exportFamilyProfiles());
            data.put("childProfiles", repository.exportChildProfiles());
            data.put("familyMembers", repository.exportFamilyMembers());
            data.put("events", repository.exportEvents());
            JSONArray attachments = sanitizeAttachmentsForBackup(repository.exportAttachments(), BabyLogFormatters.nowIso());
            data.put("attachments", attachments);
            data.put("attachmentBlobs", attachmentBuilder.createAttachmentBlobBackup(attachments));
            data.put("syncChanges", repository.exportSyncChanges());
            validateBackupDataForImport(data);

            JSONObject backup = new JSONObject();
            backup.put("format", BabyLogService.BACKUP_FORMAT);
            backup.put("version", BabyLogService.BACKUP_VERSION);
            backup.put("exportedAt", BabyLogFormatters.nowIso());
            backup.put("data", data);
            return backup.toString(2);
        } catch (IOException | JSONException error) {
            throw new BabyLogException.StorageException("导出备份失败", error);
        }
    }

    int importBackupJson(String raw) throws BabyLogException {
        return importBackupJson(raw, true);
    }

    boolean hasImportUndoSnapshot() {
        File file = importUndoSnapshotFile();
        return file != null && file.isFile() && file.length() > 0;
    }

    int undoLastImport() throws BabyLogException {
        File file = importUndoSnapshotFile();
        if (file == null || !file.isFile()) {
            throw new BabyLogException.NotFoundException("没有可撤销的导入快照");
        }
        try {
            String raw = new String(BabyLogAttachmentInputBuilder.readFileBytes(file), StandardCharsets.UTF_8);
            int count = importBackupJson(raw, false);
            file.delete();
            return count;
        } catch (IOException error) {
            throw new BabyLogException.StorageException("读取导入快照失败", error);
        }
    }

    private int importBackupJson(String raw, boolean createUndoSnapshot) throws BabyLogException {
        try {
            JSONObject backup = new JSONObject(raw);
            if (!BabyLogService.BACKUP_FORMAT.equals(backup.optString("format"))) {
                throw new BabyLogException.ValidationException("无效的栗记备份格式");
            }
            if (backup.optInt("version") != BabyLogService.BACKUP_VERSION) {
                throw new BabyLogException.ValidationException("Unsupported backup version");
            }
            JSONObject data = backup.optJSONObject("data");
            if (data == null || data.optJSONArray("events") == null) {
                throw new BabyLogException.ValidationException("无效的栗记备份数据");
            }
            validateBackupDataForImport(data);
            if (createUndoSnapshot) {
                writeImportUndoSnapshot(createBackupJson());
            }
            JSONArray attachments = data.optJSONArray("attachments");
            JSONArray restoredAttachments = attachmentBuilder.restoreAttachmentBlobs(attachments, data.optJSONArray("attachmentBlobs"));
            boolean imported = repository.importData(
                    data.optJSONArray("familyProfiles"),
                    data.optJSONArray("childProfiles"),
                    data.optJSONArray("familyMembers"),
                    data.optJSONArray("events"),
                    restoredAttachments,
                    data.optJSONArray("syncChanges")
            );
            if (!imported) {
                throw new BabyLogException.StorageException("导入写入失败，原数据未确认替换");
            }
            return data.optJSONArray("events").length();
        } catch (BabyLogException error) {
            throw error;
        } catch (JSONException error) {
            throw new BabyLogException.ValidationException(
                    error.getMessage() == null ? "无效的栗记备份数据" : error.getMessage(),
                    error
            );
        } catch (IOException error) {
            throw new BabyLogException.StorageException("导入备份失败", error);
        }
    }

    static void validateBackupDataForImport(JSONObject data) throws BabyLogException.ValidationException {
        if (data == null) {
            throw new BabyLogException.ValidationException("无效的栗记备份数据");
        }
        JSONArray events = data.optJSONArray("events");
        if (events == null) {
            throw new BabyLogException.ValidationException("无效的栗记备份记录");
        }
        validateEvents(events);
        validateProfiles(data.optJSONArray("familyProfiles"), "familyProfiles");
        validateProfiles(data.optJSONArray("childProfiles"), "childProfiles");
        validateProfiles(data.optJSONArray("familyMembers"), "familyMembers");
        try {
            BabyLogAttachmentInputBuilder.validateAttachments(data.optJSONArray("attachments"), data.optJSONArray("attachmentBlobs"));
        } catch (JSONException error) {
            throw new BabyLogException.ValidationException(error.getMessage() == null ? "Invalid attachment data" : error.getMessage(), error);
        }
        validateSyncChanges(data.optJSONArray("syncChanges"));
    }

    static JSONArray sanitizeAttachmentsForBackup(JSONArray attachments, String missingAt) throws JSONException {
        return BabyLogAttachmentInputBuilder.sanitizeAttachmentsForBackup(attachments, missingAt);
    }

    private File importUndoSnapshotFile() {
        return context == null ? null : new File(context.getFilesDir(), LAST_IMPORT_UNDO_FILE);
    }

    private void writeImportUndoSnapshot(String raw) throws IOException {
        File file = importUndoSnapshotFile();
        if (file == null) {
            throw new IOException("没有可写入的导入快照位置");
        }
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(raw.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void validateEvents(JSONArray events) throws BabyLogException.ValidationException {
        for (int i = 0; i < events.length(); i++) {
            JSONObject json = events.optJSONObject(i);
            BabyLogDomain.BabyLogEvent event = BabyLogDomain.BabyLogEvent.fromJson(json);
            if (event == null || isBlank(event.id) || isBlank(event.eventType) || isBlank(event.occurredAt)) {
                throw new BabyLogException.ValidationException("Invalid event at index " + i);
            }
        }
    }

    private static void validateProfiles(JSONArray profiles, String label) throws BabyLogException.ValidationException {
        if (profiles == null) {
            return;
        }
        for (int i = 0; i < profiles.length(); i++) {
            JSONObject json = profiles.optJSONObject(i);
            if (json == null || isBlank(json.optString("id"))) {
                throw new BabyLogException.ValidationException("Invalid " + label + " at index " + i);
            }
        }
    }

    private static void validateSyncChanges(JSONArray changes) throws BabyLogException.ValidationException {
        if (changes == null) {
            return;
        }
        for (int i = 0; i < changes.length(); i++) {
            JSONObject json = changes.optJSONObject(i);
            BabyLogDomain.SyncChange change = BabyLogDomain.SyncChange.fromJson(json);
            if (change == null || isBlank(change.id) || isBlank(change.entityType) || isBlank(change.entityId)) {
                throw new BabyLogException.ValidationException("Invalid sync change at index " + i);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
