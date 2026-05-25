package app.babylog.nativeapp;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BabyLogSyncPullOrchestrator {
    private static final int PER_PAGE = 200;
    private static final int MAX_PAGES_PER_RUN = 10;

    public PullSummary pullOnce(
            BabyLogRepository repository,
            BabyLogSyncSecretStore secretStore,
            BabyLogDomain.BackendConfig backendConfig,
            BabyLogRemoteSyncClient remoteClient
    ) {
        String familyKey;
        try {
            familyKey = secretStore == null ? "" : secretStore.loadFamilyKey();
        } catch (Exception error) {
            return PullSummary.failed("FAMILY_KEY_LOAD_FAILED");
        }
        return pullOnce(repository, familyKey, backendConfig, remoteClient);
    }

    public PullSummary pullOnce(
            BabyLogRepository repository,
            String familyKey,
            BabyLogDomain.BackendConfig backendConfig,
            BabyLogRemoteSyncClient remoteClient
    ) {
        if (repository == null) {
            return PullSummary.failed("REPOSITORY_MISSING");
        }
        if (backendConfig == null || !backendConfig.enabled || backendConfig.backendBaseUrl.isEmpty()) {
            return PullSummary.failed("BACKEND_NOT_CONFIGURED");
        }
        if (!BabyLogSyncProtocol.hasFamilyKey(familyKey)) {
            return PullSummary.failed("FAMILY_KEY_MISSING");
        }
        BabyLogRemoteSyncClient client = remoteClient == null ? new BabyLogRemoteSyncClient() : remoteClient;
        String cursor = repository.loadSyncLastPulledAt();
        List<BabyLogRemoteSyncClient.EncryptedRecord> fetched = new ArrayList<>();
        String lastError = "";
        for (int page = 1; page <= MAX_PAGES_PER_RUN; page += 1) {
            BabyLogRemoteSyncClient.PullResult result;
            try {
                result = client.pullEncryptedRecords(backendConfig.backendBaseUrl, familyKey, cursor, page, PER_PAGE);
            } catch (Exception error) {
                return PullSummary.failed("PULL_FAILED");
            }
            if (!result.lastError.isEmpty()) {
                return PullSummary.failed(result.lastError);
            }
            fetched.addAll(result.records);
            if (result.records.size() < PER_PAGE || (result.totalPages > 0 && page >= result.totalPages)) {
                break;
            }
        }
        if (fetched.isEmpty()) {
            return new PullSummary(0, 0, 0, "", cursor);
        }
        return applyFetchedRecords(repository, familyKey, fetched);
    }

    private PullSummary applyFetchedRecords(
            BabyLogRepository repository,
            String familyKey,
            List<BabyLogRemoteSyncClient.EncryptedRecord> fetched
    ) {
        Map<String, RemoteVersion> latestByEntity = new LinkedHashMap<>();
        int skipped = 0;
        String maxCursor = "";
        for (BabyLogRemoteSyncClient.EncryptedRecord record : fetched) {
            if (record == null) {
                continue;
            }
            RemoteVersion version;
            try {
                version = decryptRecord(familyKey, record);
            } catch (Exception error) {
                skipped += 1;
                continue;
            }
            if (record.updatedAtClient.compareTo(maxCursor) > 0) {
                maxCursor = record.updatedAtClient;
            }
            String key = version.entityType + "/" + version.entityId;
            RemoteVersion existing = latestByEntity.get(key);
            if (existing == null) {
                latestByEntity.put(key, version);
            } else if (version.updatedAtClient.compareTo(existing.updatedAtClient) > 0) {
                latestByEntity.put(key, version);
                skipped += 1;
            } else {
                skipped += 1;
            }
        }

        int applied = 0;
        for (RemoteVersion version : latestByEntity.values()) {
            String localUpdatedAt = localUpdatedAt(repository, version.entityType, version.entityId);
            if (!localUpdatedAt.isEmpty() && version.updatedAtClient.compareTo(localUpdatedAt) <= 0) {
                skipped += 1;
                continue;
            }
            try {
                if (repository.putEntityFromRemote(version.entityType, version.payload, version.deletedFlag)) {
                    applied += 1;
                } else {
                    skipped += 1;
                }
            } catch (JSONException error) {
                skipped += 1;
            }
        }
        if (!maxCursor.isEmpty()) {
            repository.saveSyncLastPulledAt(maxCursor);
        }
        repository.addRemoteUpdateBannerCount(applied);
        return new PullSummary(fetched.size(), applied, skipped, "", maxCursor);
    }

    private static RemoteVersion decryptRecord(String familyKey, BabyLogRemoteSyncClient.EncryptedRecord record)
            throws GeneralSecurityException, JSONException {
        String familyKeyHash = BabyLogFamilyKeyDeriver.lookupHashHex(familyKey);
        byte[] plaintext = BabyLogPayloadCipher.open(
                BabyLogFamilyKeyDeriver.deriveDataKey(familyKey),
                BabyLogSyncPushOrchestrator.aadBytes(record.clientId, record.cipherVersion, familyKeyHash),
                Base64.getDecoder().decode(record.nonce),
                Base64.getDecoder().decode(record.ciphertext)
        );
        JSONObject json = new JSONObject(new String(plaintext, StandardCharsets.UTF_8));
        JSONObject payload = json.optJSONObject("payload");
        return new RemoteVersion(
                json.optString("entityType", ""),
                json.optString("entityId", ""),
                payload == null ? new JSONObject() : payload,
                record.updatedAtClient,
                record.deletedFlag == 1
        );
    }

    private static String localUpdatedAt(BabyLogRepository repository, String entityType, String entityId) {
        if (BabyLogSyncProtocol.ENTITY_EVENT.equals(entityType)) {
            BabyLogDomain.BabyLogEvent event = repository.findEventById(entityId);
            return event == null ? "" : safe(event.updatedAt);
        }
        if (BabyLogSyncProtocol.ENTITY_ATTACHMENT.equals(entityType)) {
            BabyLogDomain.AttachmentRecord attachment = repository.findAttachmentById(entityId);
            return attachment == null ? "" : safe(attachment.updatedAt);
        }
        if (BabyLogSyncProtocol.ENTITY_CHILD_PROFILE.equals(entityType)) {
            return safe(repository.loadChildProfile().updatedAt);
        }
        if (BabyLogSyncProtocol.ENTITY_FAMILY_PROFILE.equals(entityType)) {
            BabyLogDomain.FamilyProfile profile = repository.loadFamilyProfile();
            return profile == null ? "" : safe(profile.updatedAt);
        }
        if (BabyLogSyncProtocol.ENTITY_FAMILY_MEMBER.equals(entityType)) {
            return safe(repository.loadCurrentMember().updatedAt);
        }
        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class RemoteVersion {
        final String entityType;
        final String entityId;
        final JSONObject payload;
        final String updatedAtClient;
        final boolean deletedFlag;

        RemoteVersion(String entityType, String entityId, JSONObject payload, String updatedAtClient, boolean deletedFlag) {
            this.entityType = entityType == null ? "" : entityType;
            this.entityId = entityId == null ? "" : entityId;
            this.payload = payload == null ? new JSONObject() : payload;
            this.updatedAtClient = updatedAtClient == null ? "" : updatedAtClient;
            this.deletedFlag = deletedFlag;
        }
    }

    public static final class PullSummary {
        public final int totalFetched;
        public final int applied;
        public final int skipped;
        public final String lastError;
        public final String newCursor;

        public PullSummary(int totalFetched, int applied, int skipped, String lastError, String newCursor) {
            this.totalFetched = totalFetched;
            this.applied = applied;
            this.skipped = skipped;
            this.lastError = lastError == null ? "" : lastError;
            this.newCursor = newCursor == null ? "" : newCursor;
        }

        public static PullSummary failed(String errorCode) {
            return new PullSummary(0, 0, 0, errorCode, "");
        }
    }
}
