package app.babylog.nativeapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BabyLogDomain {
    public static final String FAMILY_ID = "family_local";
    public static final String CHILD_ID = "child_singleton";
    public static final String UPDATED_BY_LOCAL = "local";
    public static final int SCHEMA_VERSION = 1;

    public static final String[] EVENT_TYPES = {
            "pregnancy_checkup",
            "ultrasound",
            "fetal_movement",
            "contraction",
            "birth",
            "feed",
            "sleep",
            "diaper",
            "temperature",
            "medication",
            "illness",
            "growth",
            "vaccine",
            "milestone",
            "note"
    };

    private BabyLogDomain() {
    }

    public static BabyLogEvent createEvent(
            String eventType,
            String occurredAt,
            JSONObject payload,
            List<String> attachmentIds,
            String source
    ) {
        String now = BabyLogFormatters.nowIso();
        return new BabyLogEvent(
                "evt_" + UUID.randomUUID(),
                FAMILY_ID,
                CHILD_ID,
                eventType,
                occurredAt,
                payload == null ? new JSONObject() : payload,
                attachmentIds == null ? new ArrayList<>() : new ArrayList<>(attachmentIds),
                source == null ? "manual" : source,
                now,
                now,
                UPDATED_BY_LOCAL,
                SCHEMA_VERSION,
                null
        );
    }

    public static AttachmentRecord createAttachment(
            String kind,
            String originalName,
            String mimeType,
            long byteSize,
            String localPath
    ) {
        String now = BabyLogFormatters.nowIso();
        return new AttachmentRecord(
                "att_" + UUID.randomUUID(),
                FAMILY_ID,
                CHILD_ID,
                kind,
                originalName,
                mimeType,
                byteSize,
                localPath,
                null,
                null,
                null,
                null,
                "not-requested",
                now,
                now,
                UPDATED_BY_LOCAL,
                SCHEMA_VERSION,
                null
        );
    }

    public static SyncChange createSyncChange(String entityType, String entityId, String operation) {
        String now = BabyLogFormatters.nowIso();
        return new SyncChange(
                "chg_" + UUID.randomUUID(),
                FAMILY_ID,
                CHILD_ID,
                entityType,
                entityId,
                operation,
                "pending",
                0,
                null,
                now,
                now,
                SCHEMA_VERSION
        );
    }

    static JSONArray toJsonArray(List<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) {
            array.put(value);
        }
        return array;
    }

    static List<String> stringListFromJson(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (int i = 0; i < array.length(); i++) {
            values.add(array.optString(i));
        }
        return values;
    }

    static String optNullableString(JSONObject json, String key) {
        if (json == null || json.isNull(key)) {
            return null;
        }
        return json.optString(key, null);
    }

    public static final class BabyLogEvent {
        public final String id;
        public final String familyId;
        public final String childId;
        public final String eventType;
        public final String occurredAt;
        public final JSONObject payload;
        public final List<String> attachmentIds;
        public final String source;
        public final String createdAt;
        public final String updatedAt;
        public final String updatedBy;
        public final int schemaVersion;
        public final String deletedAt;

        BabyLogEvent(
                String id,
                String familyId,
                String childId,
                String eventType,
                String occurredAt,
                JSONObject payload,
                List<String> attachmentIds,
                String source,
                String createdAt,
                String updatedAt,
                String updatedBy,
                int schemaVersion,
                String deletedAt
        ) {
            this.id = id;
            this.familyId = familyId;
            this.childId = childId;
            this.eventType = eventType;
            this.occurredAt = occurredAt;
            this.payload = payload;
            this.attachmentIds = attachmentIds;
            this.source = source;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.updatedBy = updatedBy;
            this.schemaVersion = schemaVersion;
            this.deletedAt = deletedAt;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("familyId", familyId);
            json.put("childId", childId);
            json.put("eventType", eventType);
            json.put("occurredAt", occurredAt);
            json.put("payload", payload);
            json.put("attachmentIds", toJsonArray(attachmentIds));
            json.put("source", source);
            json.put("createdAt", createdAt);
            json.put("updatedAt", updatedAt);
            json.put("updatedBy", updatedBy);
            json.put("schemaVersion", schemaVersion);
            json.put("deletedAt", deletedAt == null ? JSONObject.NULL : deletedAt);
            return json;
        }

        public static BabyLogEvent fromJson(JSONObject json) {
            if (json == null) {
                return null;
            }
            return new BabyLogEvent(
                    json.optString("id"),
                    json.optString("familyId", FAMILY_ID),
                    json.optString("childId", CHILD_ID),
                    json.optString("eventType", "note"),
                    json.optString("occurredAt"),
                    json.optJSONObject("payload") == null ? new JSONObject() : json.optJSONObject("payload"),
                    stringListFromJson(json.optJSONArray("attachmentIds")),
                    json.optString("source", "manual"),
                    json.optString("createdAt"),
                    json.optString("updatedAt"),
                    json.optString("updatedBy", UPDATED_BY_LOCAL),
                    json.optInt("schemaVersion", SCHEMA_VERSION),
                    optNullableString(json, "deletedAt")
            );
        }
    }

    public static final class AttachmentRecord {
        public final String id;
        public final String familyId;
        public final String childId;
        public final String kind;
        public final String originalName;
        public final String mimeType;
        public final long byteSize;
        public final String localPath;
        public final Integer widthPx;
        public final Integer heightPx;
        public final String contentHash;
        public final String remoteUrl;
        public final String ocrStatus;
        public final String createdAt;
        public final String updatedAt;
        public final String updatedBy;
        public final int schemaVersion;
        public final String deletedAt;

        AttachmentRecord(
                String id,
                String familyId,
                String childId,
                String kind,
                String originalName,
                String mimeType,
                long byteSize,
                String localPath,
                Integer widthPx,
                Integer heightPx,
                String contentHash,
                String remoteUrl,
                String ocrStatus,
                String createdAt,
                String updatedAt,
                String updatedBy,
                int schemaVersion,
                String deletedAt
        ) {
            this.id = id;
            this.familyId = familyId;
            this.childId = childId;
            this.kind = kind;
            this.originalName = originalName;
            this.mimeType = mimeType;
            this.byteSize = byteSize;
            this.localPath = localPath;
            this.widthPx = widthPx;
            this.heightPx = heightPx;
            this.contentHash = contentHash;
            this.remoteUrl = remoteUrl;
            this.ocrStatus = ocrStatus;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.updatedBy = updatedBy;
            this.schemaVersion = schemaVersion;
            this.deletedAt = deletedAt;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("familyId", familyId);
            json.put("childId", childId);
            json.put("kind", kind);
            json.put("originalName", originalName);
            json.put("mimeType", mimeType);
            json.put("byteSize", byteSize);
            json.put("localBlobKey", localPath);
            json.put("localPath", localPath);
            json.put("widthPx", widthPx == null ? JSONObject.NULL : widthPx);
            json.put("heightPx", heightPx == null ? JSONObject.NULL : heightPx);
            json.put("contentHash", contentHash == null ? JSONObject.NULL : contentHash);
            json.put("remoteUrl", remoteUrl == null ? JSONObject.NULL : remoteUrl);
            json.put("ocrStatus", ocrStatus);
            json.put("createdAt", createdAt);
            json.put("updatedAt", updatedAt);
            json.put("updatedBy", updatedBy);
            json.put("schemaVersion", schemaVersion);
            json.put("deletedAt", deletedAt == null ? JSONObject.NULL : deletedAt);
            return json;
        }

        public static AttachmentRecord fromJson(JSONObject json) {
            if (json == null) {
                return null;
            }
            String path = json.optString("localPath", json.optString("localBlobKey"));
            return new AttachmentRecord(
                    json.optString("id"),
                    json.optString("familyId", FAMILY_ID),
                    json.optString("childId", CHILD_ID),
                    json.optString("kind", "other"),
                    json.optString("originalName", "attachment"),
                    json.optString("mimeType", "application/octet-stream"),
                    json.optLong("byteSize", 0),
                    path,
                    json.isNull("widthPx") ? null : json.optInt("widthPx"),
                    json.isNull("heightPx") ? null : json.optInt("heightPx"),
                    optNullableString(json, "contentHash"),
                    optNullableString(json, "remoteUrl"),
                    json.optString("ocrStatus", "not-requested"),
                    json.optString("createdAt"),
                    json.optString("updatedAt"),
                    json.optString("updatedBy", UPDATED_BY_LOCAL),
                    json.optInt("schemaVersion", SCHEMA_VERSION),
                    optNullableString(json, "deletedAt")
            );
        }
    }

    public static final class SyncChange {
        public final String id;
        public final String familyId;
        public final String childId;
        public final String entityType;
        public final String entityId;
        public final String operation;
        public final String status;
        public final int attemptCount;
        public final String lastError;
        public final String createdAt;
        public final String updatedAt;
        public final int schemaVersion;

        SyncChange(
                String id,
                String familyId,
                String childId,
                String entityType,
                String entityId,
                String operation,
                String status,
                int attemptCount,
                String lastError,
                String createdAt,
                String updatedAt,
                int schemaVersion
        ) {
            this.id = id;
            this.familyId = familyId;
            this.childId = childId;
            this.entityType = entityType;
            this.entityId = entityId;
            this.operation = operation;
            this.status = status;
            this.attemptCount = attemptCount;
            this.lastError = lastError;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.schemaVersion = schemaVersion;
        }

        public SyncChange withStatus(String nextStatus, String errorCode) {
            return new SyncChange(
                    id,
                    familyId,
                    childId,
                    entityType,
                    entityId,
                    operation,
                    nextStatus,
                    "failed".equals(nextStatus) ? attemptCount + 1 : attemptCount,
                    errorCode,
                    createdAt,
                    BabyLogFormatters.nowIso(),
                    schemaVersion
            );
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("familyId", familyId);
            json.put("childId", childId);
            json.put("entityType", entityType);
            json.put("entityId", entityId);
            json.put("operation", operation);
            json.put("status", status);
            json.put("attemptCount", attemptCount);
            json.put("lastError", lastError == null ? JSONObject.NULL : lastError);
            json.put("createdAt", createdAt);
            json.put("updatedAt", updatedAt);
            json.put("schemaVersion", schemaVersion);
            return json;
        }

        public static SyncChange fromJson(JSONObject json) {
            if (json == null) {
                return null;
            }
            return new SyncChange(
                    json.optString("id"),
                    json.optString("familyId", FAMILY_ID),
                    json.optString("childId", CHILD_ID),
                    json.optString("entityType", "event"),
                    json.optString("entityId"),
                    json.optString("operation", "upsert"),
                    json.optString("status", "pending"),
                    json.optInt("attemptCount", 0),
                    optNullableString(json, "lastError"),
                    json.optString("createdAt"),
                    json.optString("updatedAt"),
                    json.optInt("schemaVersion", SCHEMA_VERSION)
            );
        }
    }

    public static final class BackendConfig {
        public final boolean enabled;
        public final String backendBaseUrl;
        public final String region;
        public final String lastHealthCheck;

        public BackendConfig(boolean enabled, String backendBaseUrl, String region, String lastHealthCheck) {
            String normalizedUrl = BabyLogFormatters.normalizeBackendBaseUrl(backendBaseUrl);
            this.enabled = enabled && !normalizedUrl.isEmpty();
            this.backendBaseUrl = normalizedUrl;
            this.region = region == null ? "" : region.trim();
            this.lastHealthCheck = lastHealthCheck;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("enabled", enabled);
            json.put("backendBaseUrl", backendBaseUrl);
            json.put("region", region);
            json.put("lastHealthCheck", lastHealthCheck == null ? JSONObject.NULL : lastHealthCheck);
            return json;
        }

        public static BackendConfig fromJson(String raw) {
            if (raw == null || raw.trim().isEmpty()) {
                return disabled();
            }
            try {
                JSONObject json = new JSONObject(raw);
                return new BackendConfig(
                        json.optBoolean("enabled", false),
                        json.optString("backendBaseUrl", ""),
                        json.optString("region", ""),
                        optNullableString(json, "lastHealthCheck")
                );
            } catch (JSONException ignored) {
                return disabled();
            }
        }

        public static BackendConfig disabled() {
            return new BackendConfig(false, "", "", null);
        }
    }
}
