package app.babylog.nativeapp;

import android.content.Context;
import android.net.Uri;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BabyLogService {
    public static final String BACKUP_FORMAT = "babylog.backup";
    public static final int BACKUP_VERSION = 1;

    private final Context context;
    private final BabyLogRepository repository;

    public BabyLogService(Context context, BabyLogRepository repository) {
        this.context = context.getApplicationContext();
        this.repository = repository;
    }

    public BabyLogDomain.BabyLogEvent recordQuickEvent(QuickAction action) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("summary", action.summary);
        payload.put("quickAction", action.label);
        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                action.eventType,
                BabyLogFormatters.nowIso(),
                payload,
                Collections.emptyList(),
                "manual"
        );
        repository.putEvent(event);
        repository.putSyncChange(BabyLogDomain.createSyncChange("event", event.id, "upsert"));
        return event;
    }

    public BabyLogDomain.BabyLogEvent recordUltrasound(UltrasoundInput input) throws JSONException {
        List<String> attachmentIds = new ArrayList<>();
        if (input.photoPath != null && !input.photoPath.isEmpty()) {
            File image = new File(input.photoPath);
            BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.createAttachment(
                    "ultrasound_image",
                    input.photoName == null || input.photoName.isEmpty() ? image.getName() : input.photoName,
                    "image/jpeg",
                    image.exists() ? image.length() : 0,
                    input.photoPath
            );
            repository.putAttachment(attachment);
            repository.putSyncChange(BabyLogDomain.createSyncChange("attachment", attachment.id, "upsert"));
            attachmentIds.add(attachment.id);
        }

        JSONObject payload = new JSONObject();
        payload.put("examDate", input.examDate);
        putIfNotNull(payload, "gestationalAgeDays", BabyLogFormatters.parseGestationalAgeDays(input.gestationalAge));
        putIfNotNull(payload, "bpdMm", BabyLogFormatters.parseOptionalNumber(input.bpdMm));
        putIfNotNull(payload, "hcMm", BabyLogFormatters.parseOptionalNumber(input.hcMm));
        putIfNotNull(payload, "acMm", BabyLogFormatters.parseOptionalNumber(input.acMm));
        putIfNotNull(payload, "flMm", BabyLogFormatters.parseOptionalNumber(input.flMm));
        putIfNotNull(payload, "efwGram", BabyLogFormatters.parseOptionalNumber(input.efwGram));
        String summary = BabyLogFormatters.formatUltrasoundSummary(payload);
        payload.put("summary", summary);

        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                "ultrasound",
                BabyLogFormatters.createOccurredAtFromDate(input.examDate),
                payload,
                attachmentIds,
                "manual"
        );
        repository.putEvent(event);
        repository.putSyncChange(BabyLogDomain.createSyncChange("event", event.id, "upsert"));
        return event;
    }

    public DashboardSnapshot loadDashboard() {
        List<BabyLogDomain.BabyLogEvent> events = listRecentEvents(20);
        List<BabyLogDomain.AttachmentRecord> attachments = listAttachmentsNewestFirst();
        List<BabyLogDomain.SyncChange> changes = repository.listSyncChanges();
        int pending = 0;
        int failed = 0;
        for (BabyLogDomain.SyncChange change : changes) {
            if ("pending".equals(change.status) || "failed".equals(change.status)) {
                pending += 1;
            }
            if ("failed".equals(change.status)) {
                failed += 1;
            }
        }
        return new DashboardSnapshot(events, attachments, summarizeToday(), pending, failed, repository.estimateLocalBytes());
    }

    public List<BabyLogDomain.BabyLogEvent> listRecentEvents(int limit) {
        List<BabyLogDomain.BabyLogEvent> events = repository.listEvents();
        events.sort((left, right) -> Long.compare(parseTime(right.occurredAt), parseTime(left.occurredAt)));
        return events.size() <= limit ? events : new ArrayList<>(events.subList(0, limit));
    }

    public List<BabyLogDomain.AttachmentRecord> listAttachmentsNewestFirst() {
        List<BabyLogDomain.AttachmentRecord> attachments = repository.listAttachments();
        attachments.sort((left, right) -> Long.compare(parseTime(right.createdAt), parseTime(left.createdAt)));
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
                counts.put(event.eventType, counts.get(event.eventType) + 1);
            }
        }
        return counts;
    }

    public SyncResult runSyncNow() throws JSONException {
        List<BabyLogDomain.SyncChange> retryable = new ArrayList<>();
        for (BabyLogDomain.SyncChange change : repository.listSyncChanges()) {
            if ("pending".equals(change.status) || "failed".equals(change.status)) {
                retryable.add(change);
            }
        }
        if (retryable.isEmpty()) {
            return new SyncResult(true, "OK", 0);
        }

        BabyLogDomain.BackendConfig config = repository.loadSyncSettings();
        String code = config.enabled ? "BACKEND_UNREACHABLE" : "BACKEND_NOT_CONFIGURED";
        for (BabyLogDomain.SyncChange change : retryable) {
            repository.putSyncChange(change.withStatus("failed", code));
        }
        return new SyncResult(false, code, retryable.size());
    }

    public String createBackupJson() throws JSONException, IOException {
        JSONObject data = new JSONObject();
        data.put("familyProfiles", new JSONArray());
        data.put("childProfiles", new JSONArray());
        data.put("events", repository.exportEvents());
        data.put("attachments", repository.exportAttachments());
        data.put("attachmentBlobs", createAttachmentBlobBackup());
        data.put("syncChanges", repository.exportSyncChanges());

        JSONObject backup = new JSONObject();
        backup.put("format", BACKUP_FORMAT);
        backup.put("version", BACKUP_VERSION);
        backup.put("exportedAt", BabyLogFormatters.nowIso());
        backup.put("data", data);
        return backup.toString(2);
    }

    public int importBackupJson(String raw) throws JSONException, IOException {
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
        JSONArray attachments = data.optJSONArray("attachments");
        JSONArray restoredAttachments = restoreAttachmentBlobs(attachments, data.optJSONArray("attachmentBlobs"));
        repository.importData(
                data.optJSONArray("events"),
                restoredAttachments,
                data.optJSONArray("syncChanges")
        );
        return data.optJSONArray("events").length();
    }

    public String copyImageUriToPrivateFile(Uri uri, String nameHint) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new IOException("无法读取图片");
        }
        File output = createAttachmentFile(nameHint == null ? "selected.jpg" : nameHint);
        try (InputStream in = input; FileOutputStream out = new FileOutputStream(output)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        return output.getAbsolutePath();
    }

    public File createAttachmentFile(String nameHint) {
        File dir = new File(context.getFilesDir(), "attachments");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String safeName = nameHint == null ? "scan.jpg" : nameHint.replaceAll("[^A-Za-z0-9._-]", "_");
        return new File(dir, System.currentTimeMillis() + "-" + safeName);
    }

    private JSONArray createAttachmentBlobBackup() throws IOException, JSONException {
        JSONArray blobs = new JSONArray();
        for (BabyLogDomain.AttachmentRecord attachment : repository.listAttachments()) {
            File file = new File(attachment.localPath);
            if (!file.exists()) {
                continue;
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

    private void putIfNotNull(JSONObject payload, String key, Object value) throws JSONException {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private long parseTime(String value) {
        return BabyLogFormatters.parseIsoMillis(value);
    }

    public static final class UltrasoundInput {
        public final String examDate;
        public final String gestationalAge;
        public final String bpdMm;
        public final String hcMm;
        public final String acMm;
        public final String flMm;
        public final String efwGram;
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
                String photoPath,
                String photoName
        ) {
            this.examDate = examDate;
            this.gestationalAge = gestationalAge;
            this.bpdMm = bpdMm;
            this.hcMm = hcMm;
            this.acMm = acMm;
            this.flMm = flMm;
            this.efwGram = efwGram;
            this.photoPath = photoPath;
            this.photoName = photoName;
        }
    }

    public static final class QuickAction {
        public final String label;
        public final String hint;
        public final int assetResId;
        public final int toneColor;
        public final String eventType;
        public final String summary;

        public QuickAction(String label, String hint, int assetResId, int toneColor, String eventType, String summary) {
            this.label = label;
            this.hint = hint;
            this.assetResId = assetResId;
            this.toneColor = toneColor;
            this.eventType = eventType;
            this.summary = summary;
        }
    }

    public static final class DashboardSnapshot {
        public final List<BabyLogDomain.BabyLogEvent> recentEvents;
        public final List<BabyLogDomain.AttachmentRecord> attachments;
        public final Map<String, Integer> todayCounts;
        public final int pendingSyncCount;
        public final int failedSyncCount;
        public final long localBytes;

        DashboardSnapshot(
                List<BabyLogDomain.BabyLogEvent> recentEvents,
                List<BabyLogDomain.AttachmentRecord> attachments,
                Map<String, Integer> todayCounts,
                int pendingSyncCount,
                int failedSyncCount,
                long localBytes
        ) {
            this.recentEvents = recentEvents;
            this.attachments = attachments;
            this.todayCounts = todayCounts;
            this.pendingSyncCount = pendingSyncCount;
            this.failedSyncCount = failedSyncCount;
            this.localBytes = localBytes;
        }
    }

    public static final class SyncResult {
        public final boolean ok;
        public final String code;
        public final int attempted;

        SyncResult(boolean ok, String code, int attempted) {
            this.ok = ok;
            this.code = code;
            this.attempted = attempted;
        }
    }
}
