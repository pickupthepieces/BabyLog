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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        if ("birth".equals(event.eventType)) {
            String birthDate = event.occurredAt == null || event.occurredAt.length() < 10
                    ? BabyLogFormatters.todayDateInput()
                    : event.occurredAt.substring(0, 10);
            repository.saveChildProfile(repository.loadChildProfile().withBirthDate(birthDate));
        }
        repository.putSyncChange(BabyLogDomain.createSyncChange("event", event.id, "upsert"));
        return event;
    }

    public BabyLogDomain.BabyLogEvent recordBabyCareEvent(BabyCareInput input) throws JSONException {
        JSONObject payload = buildBabyCarePayload(input);
        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                input.eventType,
                BabyLogFormatters.nowIso(),
                payload,
                Collections.emptyList(),
                "manual"
        );
        repository.putEvent(event);
        repository.putSyncChange(BabyLogDomain.createSyncChange("event", event.id, "upsert"));
        return event;
    }

    public BabyLogDomain.BabyLogEvent recordPregnancyEvent(PregnancyInput input) throws JSONException {
        JSONObject payload = buildPregnancyPayload(input);
        String occurredAt = "pregnancy_checkup".equals(input.eventType) && BabyLogFormatters.isValidDateInput(input.primary)
                ? BabyLogFormatters.createOccurredAtFromDate(input.primary)
                : BabyLogFormatters.nowIso();
        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                input.eventType,
                occurredAt,
                payload,
                Collections.emptyList(),
                "manual"
        );
        repository.putEvent(event);
        repository.putSyncChange(BabyLogDomain.createSyncChange("event", event.id, "upsert"));
        return event;
    }

    public BabyLogDomain.BabyLogEvent recordMaternalMetric(MaternalMetricInput input) throws JSONException {
        JSONObject payload = buildMaternalMetricPayload(input);
        BabyLogDomain.BabyLogEvent event = BabyLogDomain.createEvent(
                "maternal_metric",
                BabyLogFormatters.nowIso(),
                payload,
                Collections.emptyList(),
                "manual"
        );
        repository.putEvent(event);
        repository.putSyncChange(BabyLogDomain.createSyncChange("event", event.id, "upsert"));
        return event;
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
        }

        payload.put("summary", formatBabyCareSummary(input));
        return payload;
    }

    public static JSONObject buildPregnancyPayload(PregnancyInput input) throws JSONException {
        JSONObject payload = new JSONObject();

        if ("pregnancy_checkup".equals(input.eventType)) {
            putStringIfNotBlank(payload, "checkupDate", input.primary);
            putStringIfNotBlank(payload, "provider", input.secondary);
            putStringIfNotBlank(payload, "finding", input.tertiary);
            putStringIfNotBlank(payload, "nextVisitNote", input.note);
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
        }
        if (summary.toString().equals(BabyLogFormatters.eventLabel(input.eventType))) {
            summary.append(" · 待补充详情");
        }
        return summary.toString();
    }

    public static String formatPregnancySummary(PregnancyInput input) {
        StringBuilder summary = new StringBuilder(BabyLogFormatters.eventLabel(input.eventType));
        if ("pregnancy_checkup".equals(input.eventType)) {
            appendSummary(summary, input.secondary);
            appendSummary(summary, input.tertiary);
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
        List<String> attachmentIds = new ArrayList<>();
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
                repository.putAttachment(attachment);
                repository.putSyncChange(BabyLogDomain.createSyncChange("attachment", attachment.id, "upsert"));
                attachmentIds.add(attachment.id);
            }
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
        return new DashboardSnapshot(
                events,
                attachments,
                summarizeToday(),
                pending,
                failed,
                repository.estimateLocalBytes() + estimateAttachmentBytes(),
                context.getFilesDir().getUsableSpace()
        );
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
                counts.put(event.eventType, counts.getOrDefault(event.eventType, 0) + 1);
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
        data.put("familyProfiles", repository.exportFamilyProfiles());
        data.put("childProfiles", repository.exportChildProfiles());
        data.put("familyMembers", repository.exportFamilyMembers());
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
                data.optJSONArray("familyProfiles"),
                data.optJSONArray("childProfiles"),
                data.optJSONArray("familyMembers"),
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
        repository.clearLocalData();
    }

    private long estimateAttachmentBytes() {
        return directoryBytes(new File(context.getFilesDir(), "attachments"));
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
        summary.append(" · ").append(value.trim());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
    }

    public static final class PregnancyInput {
        public final String eventType;
        public final String primary;
        public final String secondary;
        public final String tertiary;
        public final String note;

        private PregnancyInput(String eventType, String primary, String secondary, String tertiary, String note) {
            this.eventType = eventType;
            this.primary = primary;
            this.secondary = secondary;
            this.tertiary = tertiary;
            this.note = note;
        }

        public static PregnancyInput checkup(String checkupDate, String provider, String finding, String nextVisitNote) {
            return new PregnancyInput("pregnancy_checkup", checkupDate, provider, finding, nextVisitNote);
        }

        public static PregnancyInput fetalMovement(String movementWindow, String movementCount, String note) {
            return new PregnancyInput("fetal_movement", movementWindow, movementCount, "", note);
        }

        public static PregnancyInput contraction(String startTime, String intervalMinutes, String durationSeconds, String note) {
            return new PregnancyInput("contraction", startTime, intervalMinutes, durationSeconds, note);
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
        public final long freeBytes;

        DashboardSnapshot(
                List<BabyLogDomain.BabyLogEvent> recentEvents,
                List<BabyLogDomain.AttachmentRecord> attachments,
                Map<String, Integer> todayCounts,
                int pendingSyncCount,
                int failedSyncCount,
                long localBytes,
                long freeBytes
        ) {
            this.recentEvents = recentEvents;
            this.attachments = attachments;
            this.todayCounts = todayCounts;
            this.pendingSyncCount = pendingSyncCount;
            this.failedSyncCount = failedSyncCount;
            this.localBytes = localBytes;
            this.freeBytes = freeBytes;
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
