package app.babylog.nativeapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BabyLogSyncPushOrchestrator {
    public static final int CIPHER_VERSION = 1;
    private static final int MAX_PUSH_PER_RUN = 200;
    private static final int MAX_FILE_UPLOADS_PER_RUN = 3;
    private static final long MAX_FILE_BYTES_PER_RUN = 10L * 1024L * 1024L;

    public PushSummary pushOnce(
            BabyLogService service,
            BabyLogRepository repository,
            BabyLogSyncSecretStore secretStore,
            BabyLogDomain.BackendConfig backendConfig,
            BabyLogRemoteSyncClient remoteClient
    ) {
        String familyKey;
        try {
            familyKey = secretStore == null ? "" : secretStore.loadFamilyKey();
        } catch (Exception error) {
            if (repository == null) {
                return PushSummary.failed(0, "REPOSITORY_MISSING");
            }
            return markAllFailed(repository, retryableChanges(repository.listSyncChanges()), "FAMILY_KEY_LOAD_FAILED");
        }
        return pushOnceWithFamilyKey(repository, backendConfig, remoteClient, familyKey);
    }

    public PushSummary pushOnceForSmokeTest(
            BabyLogRepository repository,
            BabyLogDomain.BackendConfig backendConfig,
            BabyLogRemoteSyncClient remoteClient,
            String familyKey
    ) {
        return pushOnceWithFamilyKey(repository, backendConfig, remoteClient, familyKey);
    }

    private PushSummary pushOnceWithFamilyKey(
            BabyLogRepository repository,
            BabyLogDomain.BackendConfig backendConfig,
            BabyLogRemoteSyncClient remoteClient,
            String familyKey
    ) {
        if (repository == null) {
            return PushSummary.failed(0, "REPOSITORY_MISSING");
        }
        List<BabyLogDomain.SyncChange> retryable = retryableChanges(repository.listSyncChanges());
        if (retryable.isEmpty()) {
            return new PushSummary(0, 0, 0, "");
        }
        if (backendConfig == null || !backendConfig.enabled || backendConfig.backendBaseUrl.isEmpty()) {
            return markAllFailed(repository, retryable, "BACKEND_NOT_CONFIGURED");
        }
        if (!BabyLogSyncProtocol.hasFamilyKey(familyKey)) {
            return markAllFailed(repository, retryable, "FAMILY_KEY_MISSING");
        }

        List<PendingEncryptedRecord> pending = new ArrayList<>();
        int failedBeforeUpload = 0;
        String lastError = "";
        for (BabyLogDomain.SyncChange change : retryable) {
            if (pending.size() >= MAX_PUSH_PER_RUN) {
                break;
            }
            try {
                JSONObject entityJson = entityJsonForChange(repository, change);
                if (entityJson == null) {
                    repository.putSyncChange(change.withStatus("failed", "ENTITY_NOT_FOUND"));
                    failedBeforeUpload += 1;
                    lastError = "ENTITY_NOT_FOUND";
                    continue;
                }
                pending.add(new PendingEncryptedRecord(
                        change,
                        encryptEntityForPush(
                                familyKey,
                                change.id,
                                change.entityType,
                                change.entityId,
                                entityJson,
                                "delete".equals(change.operation)
                        )
                ));
            } catch (Exception error) {
                String code = "ENCRYPT_FAILED";
                try {
                    repository.putSyncChange(change.withStatus("failed", code));
                } catch (JSONException ignored) {
                    code = "ENCRYPT_AND_STATUS_FAILED";
                }
                failedBeforeUpload += 1;
                lastError = code;
            }
        }
        if (pending.isEmpty()) {
            return new PushSummary(Math.min(retryable.size(), MAX_PUSH_PER_RUN), 0, failedBeforeUpload, lastError);
        }

        BabyLogRemoteSyncClient client = remoteClient == null ? new BabyLogRemoteSyncClient() : remoteClient;
        BabyLogRemoteSyncClient.PushResult result;
        try {
            result = client.pushPendingChanges(backendConfig.backendBaseUrl, familyKey, recordsOnly(pending));
        } catch (IOException error) {
            return markPendingFailed(repository, pending, failedBeforeUpload, "PUSH_FAILED");
        }

        Map<String, BabyLogDomain.SyncChange> changesByClientId = new LinkedHashMap<>();
        for (PendingEncryptedRecord item : pending) {
            changesByClientId.put(item.record.clientId, item.change);
        }
        int pushed = 0;
        int failed = failedBeforeUpload;
        int filesUploaded = 0;
        int filesPending = 0;
        long bytesUploaded = 0;
        String familyKeyHash = BabyLogFamilyKeyDeriver.lookupHashHex(familyKey);
        for (BabyLogRemoteSyncClient.RecordPushResult recordResult : result.records) {
            BabyLogDomain.SyncChange change = changesByClientId.get(recordResult.clientId);
            if (change == null) {
                continue;
            }
            try {
                if (recordResult.ok) {
                    AttachmentUploadOutcome attachmentUpload = uploadAttachmentIfNeeded(
                            repository,
                            client,
                            backendConfig.backendBaseUrl,
                            familyKey,
                            familyKeyHash,
                            change,
                            recordResult,
                            filesUploaded,
                            bytesUploaded
                    );
                    filesUploaded += attachmentUpload.filesUploaded;
                    filesPending += attachmentUpload.filesPending;
                    bytesUploaded += attachmentUpload.bytesUploaded;
                    repository.putSyncChange(change.withStatus(attachmentUpload.status, attachmentUpload.errorCode));
                    pushed += 1;
                } else {
                    String code = recordResult.errorCode.isEmpty() ? "PUSH_FAILED" : recordResult.errorCode;
                    repository.putSyncChange(change.withStatus("failed", code));
                    failed += 1;
                    lastError = code;
                }
            } catch (JSONException error) {
                failed += 1;
                lastError = "STATUS_UPDATE_FAILED";
            }
        }
        return new PushSummary(Math.min(retryable.size(), MAX_PUSH_PER_RUN), pushed, failed, lastError, filesUploaded, filesPending, bytesUploaded);
    }

    public static BabyLogRemoteSyncClient.EncryptedRecord encryptEntityForPush(
            String familyKey,
            String clientId,
            String entityType,
            String entityId,
            JSONObject entityJson
    ) throws GeneralSecurityException, JSONException {
        return encryptEntityForPush(familyKey, clientId, entityType, entityId, entityJson, false);
    }

    public static BabyLogRemoteSyncClient.EncryptedRecord encryptEntityForPush(
            String familyKey,
            String clientId,
            String entityType,
            String entityId,
            JSONObject entityJson,
            boolean deleted
    ) throws GeneralSecurityException, JSONException {
        String familyKeyHash = BabyLogFamilyKeyDeriver.lookupHashHex(familyKey);
        JSONObject plaintext = createPlaintext(entityType, entityId, entityJson);
        BabyLogPayloadCipher.SealResult sealed = BabyLogPayloadCipher.seal(
                BabyLogFamilyKeyDeriver.deriveDataKey(familyKey),
                aadBytes(clientId, CIPHER_VERSION, familyKeyHash),
                plaintext.toString().getBytes(StandardCharsets.UTF_8)
        );
        return new BabyLogRemoteSyncClient.EncryptedRecord(
                clientId,
                familyKeyHash,
                BabyLogDomain.SCHEMA_VERSION,
                CIPHER_VERSION,
                Base64.getEncoder().encodeToString(sealed.nonce),
                Base64.getEncoder().encodeToString(sealed.ciphertext),
                updatedAtFor(entityJson),
                deleted || hasDeletedAt(entityJson) ? 1 : 0
        );
    }

    public static byte[] aadBytes(String clientId, int cipherVersion, String familyKeyHash) {
        return ((clientId == null ? "" : clientId)
                + "|"
                + cipherVersion
                + "|"
                + (familyKeyHash == null ? "" : familyKeyHash)).getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] attachmentAadBytes(String attachmentId, int cipherVersion, String familyKeyHash) {
        return ((attachmentId == null ? "" : attachmentId)
                + "|"
                + cipherVersion
                + "|"
                + (familyKeyHash == null ? "" : familyKeyHash)).getBytes(StandardCharsets.UTF_8);
    }

    private static JSONObject createPlaintext(String entityType, String entityId, JSONObject entityJson) throws JSONException {
        JSONObject plaintext = new JSONObject()
                .put("entityType", entityType == null ? "" : entityType)
                .put("entityId", entityId == null ? "" : entityId)
                .put("payload", entityJson == null ? new JSONObject() : entityJson);
        if (BabyLogSyncProtocol.ENTITY_EVENT.equals(entityType) && entityJson != null) {
            plaintext.put("occurredAt", entityJson.optString("occurredAt"));
            JSONArray attachmentIds = entityJson.optJSONArray("attachmentIds");
            plaintext.put("attachmentIds", attachmentIds == null ? new JSONArray() : attachmentIds);
        }
        return plaintext;
    }

    private static JSONObject entityJsonForChange(BabyLogRepository repository, BabyLogDomain.SyncChange change) throws JSONException {
        if (change == null || !BabyLogSyncProtocol.isSyncableEntityType(change.entityType)) {
            return null;
        }
        switch (change.entityType) {
            case BabyLogSyncProtocol.ENTITY_EVENT:
                BabyLogDomain.BabyLogEvent event = repository.findEventById(change.entityId);
                return event == null ? null : event.toJson();
            case BabyLogSyncProtocol.ENTITY_ATTACHMENT:
                BabyLogDomain.AttachmentRecord attachment = repository.findAttachmentById(change.entityId);
                return attachment == null ? null : attachment.toJson();
            case BabyLogSyncProtocol.ENTITY_CHILD_PROFILE:
                return repository.loadChildProfile().toJson();
            case BabyLogSyncProtocol.ENTITY_FAMILY_PROFILE:
                BabyLogDomain.FamilyProfile family = repository.loadFamilyProfile();
                return family == null ? BabyLogDomain.FamilyProfile.localDefault().toJson() : family.toJson();
            case BabyLogSyncProtocol.ENTITY_FAMILY_MEMBER:
                return repository.loadCurrentMember().toJson();
            default:
                return null;
        }
    }

    private static List<BabyLogDomain.SyncChange> retryableChanges(List<BabyLogDomain.SyncChange> source) {
        List<BabyLogDomain.SyncChange> changes = new ArrayList<>();
        if (source != null) {
            for (BabyLogDomain.SyncChange change : source) {
                if (change != null && !"synced".equals(change.status)) {
                    changes.add(change);
                }
            }
        }
        Collections.sort(changes, new Comparator<BabyLogDomain.SyncChange>() {
            @Override
            public int compare(BabyLogDomain.SyncChange left, BabyLogDomain.SyncChange right) {
                String leftUpdatedAt = left.updatedAt == null ? "" : left.updatedAt;
                String rightUpdatedAt = right.updatedAt == null ? "" : right.updatedAt;
                return leftUpdatedAt.compareTo(rightUpdatedAt);
            }
        });
        return changes;
    }

    private static List<BabyLogRemoteSyncClient.EncryptedRecord> recordsOnly(List<PendingEncryptedRecord> pending) {
        List<BabyLogRemoteSyncClient.EncryptedRecord> records = new ArrayList<>();
        for (PendingEncryptedRecord item : pending) {
            records.add(item.record);
        }
        return records;
    }

    private static AttachmentUploadOutcome uploadAttachmentIfNeeded(
            BabyLogRepository repository,
            BabyLogRemoteSyncClient client,
            String backendBaseUrl,
            String familyKey,
            String familyKeyHash,
            BabyLogDomain.SyncChange change,
            BabyLogRemoteSyncClient.RecordPushResult recordResult,
            int filesUploadedSoFar,
            long bytesUploadedSoFar
    ) {
        if (!BabyLogSyncProtocol.ENTITY_ATTACHMENT.equals(change.entityType) || "delete".equals(change.operation)) {
            return AttachmentUploadOutcome.synced();
        }
        BabyLogDomain.AttachmentRecord attachment = repository.findAttachmentById(change.entityId);
        if (attachment == null || attachment.deletedAt != null) {
            return AttachmentUploadOutcome.synced();
        }
        byte[] blob = repository.findAttachmentBlobBytes(attachment.id);
        if (blob == null || blob.length == 0 || recordResult.remoteRecordId.isEmpty()) {
            return AttachmentUploadOutcome.pending("ATTACHMENT_FILE_PENDING");
        }
        if (filesUploadedSoFar >= MAX_FILE_UPLOADS_PER_RUN || bytesUploadedSoFar + blob.length > MAX_FILE_BYTES_PER_RUN) {
            return AttachmentUploadOutcome.pending("ATTACHMENT_FILE_DEFERRED");
        }
        try {
            byte[] sealed = BabyLogAttachmentCipher.sealFile(
                    BabyLogFamilyKeyDeriver.deriveAttachmentKey(familyKey),
                    attachmentAadBytes(attachment.id, CIPHER_VERSION, familyKeyHash),
                    blob
            );
            BabyLogRemoteSyncClient.RecordPushResult upload = client.uploadAttachmentFile(
                    backendBaseUrl,
                    familyKey,
                    recordResult.remoteRecordId,
                    sealed,
                    attachmentVersion(attachment, blob)
            );
            if (upload.ok) {
                return AttachmentUploadOutcome.uploaded(blob.length);
            }
            return AttachmentUploadOutcome.pending(upload.errorCode.isEmpty() ? "ATTACHMENT_UPLOAD_FAILED" : upload.errorCode);
        } catch (Exception error) {
            return AttachmentUploadOutcome.pending("ATTACHMENT_UPLOAD_FAILED");
        }
    }

    private static String attachmentVersion(BabyLogDomain.AttachmentRecord attachment, byte[] blob) throws GeneralSecurityException {
        if (attachment != null && attachment.contentHash != null && !attachment.contentHash.trim().isEmpty()) {
            return attachment.contentHash;
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return toHex(digest.digest(blob == null ? new byte[0] : blob));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value & 0xff));
        }
        return builder.toString();
    }

    private static PushSummary markAllFailed(BabyLogRepository repository, List<BabyLogDomain.SyncChange> changes, String code) {
        int failed = 0;
        for (BabyLogDomain.SyncChange change : changes) {
            try {
                repository.putSyncChange(change.withStatus("failed", code));
                failed += 1;
            } catch (JSONException ignored) {
                failed += 1;
            }
        }
        return new PushSummary(changes.size(), 0, failed, code);
    }

    private static PushSummary markPendingFailed(BabyLogRepository repository, List<PendingEncryptedRecord> pending, int alreadyFailed, String code) {
        int failed = alreadyFailed;
        for (PendingEncryptedRecord item : pending) {
            try {
                repository.putSyncChange(item.change.withStatus("failed", code));
            } catch (JSONException ignored) {
                // Still count this item as failed for the user's summary.
            }
            failed += 1;
        }
        return new PushSummary(pending.size() + alreadyFailed, 0, failed, code);
    }

    private static String updatedAtFor(JSONObject entityJson) {
        String updatedAt = entityJson == null ? "" : entityJson.optString("updatedAt", "");
        return updatedAt == null || updatedAt.trim().isEmpty() ? BabyLogFormatters.nowIso() : updatedAt;
    }

    private static boolean hasDeletedAt(JSONObject entityJson) {
        return entityJson != null && entityJson.has("deletedAt") && !entityJson.isNull("deletedAt") && !entityJson.optString("deletedAt", "").isEmpty();
    }

    private static final class PendingEncryptedRecord {
        final BabyLogDomain.SyncChange change;
        final BabyLogRemoteSyncClient.EncryptedRecord record;

        PendingEncryptedRecord(BabyLogDomain.SyncChange change, BabyLogRemoteSyncClient.EncryptedRecord record) {
            this.change = change;
            this.record = record;
        }
    }

    private static final class AttachmentUploadOutcome {
        final String status;
        final String errorCode;
        final int filesUploaded;
        final int filesPending;
        final long bytesUploaded;

        private AttachmentUploadOutcome(String status, String errorCode, int filesUploaded, int filesPending, long bytesUploaded) {
            this.status = status;
            this.errorCode = errorCode;
            this.filesUploaded = filesUploaded;
            this.filesPending = filesPending;
            this.bytesUploaded = bytesUploaded;
        }

        static AttachmentUploadOutcome synced() {
            return new AttachmentUploadOutcome("synced", null, 0, 0, 0);
        }

        static AttachmentUploadOutcome uploaded(long bytesUploaded) {
            return new AttachmentUploadOutcome("synced", null, 1, 0, bytesUploaded);
        }

        static AttachmentUploadOutcome pending(String errorCode) {
            return new AttachmentUploadOutcome("metadata_synced_file_pending", errorCode, 0, 1, 0);
        }
    }

    public static final class PushSummary {
        public final int total;
        public final int pushed;
        public final int failed;
        public final String lastError;
        public final int filesUploaded;
        public final int filesPending;
        public final long bytesUploaded;

        public PushSummary(int total, int pushed, int failed, String lastError) {
            this(total, pushed, failed, lastError, 0, 0, 0);
        }

        public PushSummary(int total, int pushed, int failed, String lastError, int filesUploaded, int filesPending, long bytesUploaded) {
            this.total = total;
            this.pushed = pushed;
            this.failed = failed;
            this.lastError = lastError == null ? "" : lastError;
            this.filesUploaded = filesUploaded;
            this.filesPending = filesPending;
            this.bytesUploaded = bytesUploaded;
        }

        public static PushSummary failed(int total, String errorCode) {
            return new PushSummary(total, 0, total, errorCode);
        }
    }
}
