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
    public static final String LOCAL_MEMBER_ID = "member_local_manager";
    public static final String UPDATED_BY_LOCAL = "local";
    public static final int SCHEMA_VERSION = 1;
    public static final String STAGE_AUTO = "auto";
    public static final String STAGE_PREGNANCY = "pregnancy";
    public static final String STAGE_BABY = "baby";
    public static final String STAGE_UNKNOWN = "unknown";

    public static final String[] EVENT_TYPES = {
            "pregnancy_checkup",
            "ultrasound",
            "fetal_movement",
            "contraction",
            "birth",
            "feed",
            "breastfeed",
            "bottle",
            "sleep",
            "wake",
            "diaper",
            "pee",
            "poop",
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

    static String optStringOrEmpty(JSONObject json, String key) {
        if (json == null || json.isNull(key)) {
            return "";
        }
        return json.optString(key, "");
    }

    public static final class FamilyProfile {
        public final String id;
        public final String name;
        public final String status;
        public final String createdAt;
        public final String updatedAt;
        public final int schemaVersion;

        FamilyProfile(
                String id,
                String name,
                String status,
                String createdAt,
                String updatedAt,
                int schemaVersion
        ) {
            this.id = id == null || id.isEmpty() ? FAMILY_ID : id;
            this.name = name == null || name.trim().isEmpty() ? "我的家庭" : name.trim();
            this.status = status == null || status.isEmpty() ? "active" : status;
            this.createdAt = createdAt == null ? "" : createdAt;
            this.updatedAt = updatedAt == null ? "" : updatedAt;
            this.schemaVersion = schemaVersion;
        }

        public static FamilyProfile localDefault() {
            String now = BabyLogFormatters.nowIso();
            return new FamilyProfile(FAMILY_ID, "我的家庭", "active", now, now, SCHEMA_VERSION);
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("name", name);
            json.put("status", status);
            json.put("createdAt", createdAt);
            json.put("updatedAt", updatedAt);
            json.put("schemaVersion", schemaVersion);
            return json;
        }

        public static FamilyProfile fromJson(JSONObject json) {
            if (json == null) {
                return null;
            }
            return new FamilyProfile(
                    json.optString("id", FAMILY_ID),
                    json.optString("name", "我的家庭"),
                    json.optString("status", "active"),
                    json.optString("createdAt"),
                    json.optString("updatedAt"),
                    json.optInt("schemaVersion", SCHEMA_VERSION)
            );
        }
    }

    public static final class ChildProfile {
        public final String id;
        public final String familyId;
        public final String nickname;
        public final String sex;
        public final String expectedDueDate;
        public final String birthDate;
        public final String stageOverride;
        public final boolean setupCompleted;
        public final String createdAt;
        public final String updatedAt;
        public final int schemaVersion;

        ChildProfile(
                String id,
                String familyId,
                String nickname,
                String sex,
                String expectedDueDate,
                String birthDate,
                String stageOverride,
                boolean setupCompleted,
                String createdAt,
                String updatedAt,
                int schemaVersion
        ) {
            this.id = id == null || id.isEmpty() ? CHILD_ID : id;
            this.familyId = familyId == null || familyId.isEmpty() ? FAMILY_ID : familyId;
            this.nickname = nickname == null ? "" : nickname.trim();
            this.sex = normalizeSex(sex);
            this.expectedDueDate = expectedDueDate == null ? "" : expectedDueDate.trim();
            this.birthDate = birthDate == null ? "" : birthDate.trim();
            this.stageOverride = normalizeStageOverride(stageOverride);
            this.setupCompleted = setupCompleted;
            this.createdAt = createdAt == null ? "" : createdAt;
            this.updatedAt = updatedAt == null ? "" : updatedAt;
            this.schemaVersion = schemaVersion;
        }

        public static ChildProfile empty() {
            String now = BabyLogFormatters.nowIso();
            return new ChildProfile(CHILD_ID, FAMILY_ID, "", "unknown", "", "", STAGE_AUTO, false, now, now, SCHEMA_VERSION);
        }

        public static ChildProfile createForNewFamily(
                String nickname,
                String sex,
                String expectedDueDate,
                String birthDate,
                String stageOverride,
                boolean setupCompleted
        ) {
            String now = BabyLogFormatters.nowIso();
            return new ChildProfile(
                    CHILD_ID,
                    FAMILY_ID,
                    nickname,
                    sex,
                    expectedDueDate,
                    birthDate,
                    stageOverride,
                    setupCompleted,
                    now,
                    now,
                    SCHEMA_VERSION
            );
        }

        public ChildProfile withBirthDate(String nextBirthDate) {
            return new ChildProfile(
                    id,
                    familyId,
                    nickname,
                    sex,
                    expectedDueDate,
                    nextBirthDate,
                    stageOverride,
                    setupCompleted,
                    createdAt,
                    BabyLogFormatters.nowIso(),
                    schemaVersion
            );
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("familyId", familyId);
            json.put("nickname", nickname);
            json.put("sex", sex);
            json.put("expectedDueDate", expectedDueDate);
            json.put("birthDate", birthDate);
            json.put("stageOverride", stageOverride);
            json.put("setupCompleted", setupCompleted);
            json.put("createdAt", createdAt);
            json.put("updatedAt", updatedAt);
            json.put("schemaVersion", schemaVersion);
            return json;
        }

        public static ChildProfile fromJson(JSONObject json) {
            if (json == null) {
                return null;
            }
            return new ChildProfile(
                    json.optString("id", CHILD_ID),
                    json.optString("familyId", FAMILY_ID),
                    optStringOrEmpty(json, "nickname"),
                    json.optString("sex", "unknown"),
                    optStringOrEmpty(json, "expectedDueDate"),
                    optStringOrEmpty(json, "birthDate"),
                    json.optString("stageOverride", STAGE_AUTO),
                    json.optBoolean("setupCompleted", true),
                    json.optString("createdAt"),
                    json.optString("updatedAt"),
                    json.optInt("schemaVersion", SCHEMA_VERSION)
            );
        }
    }

    public static final class FamilyMember {
        public final String id;
        public final String familyId;
        public final String displayName;
        public final String role;
        public final String status;
        public final String createdAt;
        public final String updatedAt;
        public final int schemaVersion;

        FamilyMember(
                String id,
                String familyId,
                String displayName,
                String role,
                String status,
                String createdAt,
                String updatedAt,
                int schemaVersion
        ) {
            this.id = id == null || id.isEmpty() ? LOCAL_MEMBER_ID : id;
            this.familyId = familyId == null || familyId.isEmpty() ? FAMILY_ID : familyId;
            this.displayName = displayName == null || displayName.trim().isEmpty() ? "本机主人" : displayName.trim();
            this.role = normalizeRole(role);
            this.status = normalizeMemberStatus(status);
            this.createdAt = createdAt == null ? "" : createdAt;
            this.updatedAt = updatedAt == null ? "" : updatedAt;
            this.schemaVersion = schemaVersion;
        }

        public static FamilyMember localManager() {
            String now = BabyLogFormatters.nowIso();
            return new FamilyMember(LOCAL_MEMBER_ID, FAMILY_ID, "本机主人", "manager", "active", now, now, SCHEMA_VERSION);
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("familyId", familyId);
            json.put("displayName", displayName);
            json.put("role", role);
            json.put("status", status);
            json.put("createdAt", createdAt);
            json.put("updatedAt", updatedAt);
            json.put("schemaVersion", schemaVersion);
            return json;
        }

        public static FamilyMember fromJson(JSONObject json) {
            if (json == null) {
                return null;
            }
            return new FamilyMember(
                    json.optString("id", LOCAL_MEMBER_ID),
                    json.optString("familyId", FAMILY_ID),
                    json.optString("displayName", "本机主人"),
                    json.optString("role", "manager"),
                    json.optString("status", "active"),
                    json.optString("createdAt"),
                    json.optString("updatedAt"),
                    json.optInt("schemaVersion", SCHEMA_VERSION)
            );
        }
    }

    private static String normalizeSex(String sex) {
        if ("male".equals(sex) || "female".equals(sex)) {
            return sex;
        }
        return "unknown";
    }

    private static String normalizeStageOverride(String value) {
        if (STAGE_PREGNANCY.equals(value) || STAGE_BABY.equals(value) || STAGE_UNKNOWN.equals(value)) {
            return value;
        }
        return STAGE_AUTO;
    }

    private static String normalizeRole(String role) {
        if ("manager".equals(role) || "family".equals(role) || "caregiver".equals(role)) {
            return role;
        }
        if ("owner".equals(role) || "parent".equals(role)) {
            return "manager";
        }
        return "manager";
    }

    private static String normalizeMemberStatus(String status) {
        if ("stopped".equals(status) || "revoked".equals(status) || "expired".equals(status)) {
            return "stopped";
        }
        return "active";
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
