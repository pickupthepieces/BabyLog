package app.babylog.nativeapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
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

    public PushSummary pushOnce(
            BabyLogService service,
            BabyLogRepository repository,
            BabyLogSyncSecretStore secretStore,
            BabyLogDomain.BackendConfig backendConfig,
            BabyLogRemoteSyncClient remoteClient
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
        String familyKey;
        try {
            familyKey = secretStore == null ? "" : secretStore.loadFamilyKey();
        } catch (Exception error) {
            return markAllFailed(repository, retryable, "FAMILY_KEY_LOAD_FAILED");
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
        for (BabyLogRemoteSyncClient.RecordPushResult recordResult : result.records) {
            BabyLogDomain.SyncChange change = changesByClientId.get(recordResult.clientId);
            if (change == null) {
                continue;
            }
            try {
                if (recordResult.ok) {
                    repository.putSyncChange(change.withStatus("synced", null));
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
        return new PushSummary(Math.min(retryable.size(), MAX_PUSH_PER_RUN), pushed, failed, lastError);
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

    public static final class PushSummary {
        public final int total;
        public final int pushed;
        public final int failed;
        public final String lastError;

        public PushSummary(int total, int pushed, int failed, String lastError) {
            this.total = total;
            this.pushed = pushed;
            this.failed = failed;
            this.lastError = lastError == null ? "" : lastError;
        }

        public static PushSummary failed(int total, String errorCode) {
            return new PushSummary(total, 0, total, errorCode);
        }
    }
}
