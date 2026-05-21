package app.babylog.nativeapp;

import org.json.JSONException;
import org.json.JSONObject;

public final class BabyLogSyncProtocol {
    public static final String HEADER_FAMILY_KEY = "X-BabyLog-Family-Key";
    public static final String HEADER_CLIENT_SCHEMA = "X-BabyLog-Schema-Version";

    public static final String ENTITY_FAMILY_PROFILE = "familyProfile";
    public static final String ENTITY_CHILD_PROFILE = "childProfile";
    public static final String ENTITY_FAMILY_MEMBER = "familyMember";
    public static final String ENTITY_EVENT = "event";
    public static final String ENTITY_ATTACHMENT = "attachment";

    public static final String COLLECTION_FAMILY_PROFILES = "family_profiles";
    public static final String COLLECTION_CHILD_PROFILES = "child_profiles";
    public static final String COLLECTION_FAMILY_MEMBERS = "family_members";
    public static final String COLLECTION_EVENTS = "events";
    public static final String COLLECTION_ATTACHMENTS = "attachments";

    public static final String LOCAL_ONLY_SMART_CONFIG = "smartConfig";
    public static final String LOCAL_ONLY_REMINDER = "reminder";
    public static final String LOCAL_ONLY_PRE_VISIT_QUESTION = "preVisitQuestion";
    public static final String LOCAL_ONLY_DISCLAIMER_CONSENT = "disclaimerConsent";

    private BabyLogSyncProtocol() {
    }

    public static boolean isSyncableEntityType(String entityType) {
        if (entityType == null) {
            return false;
        }
        switch (entityType) {
            case ENTITY_FAMILY_PROFILE:
            case ENTITY_CHILD_PROFILE:
            case ENTITY_FAMILY_MEMBER:
            case ENTITY_EVENT:
            case ENTITY_ATTACHMENT:
                return true;
            default:
                return false;
        }
    }

    public static String collectionForEntityType(String entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException("entityType is required");
        }
        switch (entityType) {
            case ENTITY_FAMILY_PROFILE:
                return COLLECTION_FAMILY_PROFILES;
            case ENTITY_CHILD_PROFILE:
                return COLLECTION_CHILD_PROFILES;
            case ENTITY_FAMILY_MEMBER:
                return COLLECTION_FAMILY_MEMBERS;
            case ENTITY_EVENT:
                return COLLECTION_EVENTS;
            case ENTITY_ATTACHMENT:
                return COLLECTION_ATTACHMENTS;
            default:
                throw new IllegalArgumentException("Unsupported sync entityType: " + entityType);
        }
    }

    public static JSONObject wrapRecord(String entityType, String entityId, JSONObject payload) throws JSONException {
        if (!isSyncableEntityType(entityType)) {
            throw new IllegalArgumentException("Unsupported sync entityType: " + entityType);
        }
        if (entityId == null || entityId.trim().isEmpty()) {
            throw new IllegalArgumentException("entityId is required");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload is required");
        }
        return new JSONObject()
                .put("collection", collectionForEntityType(entityType))
                .put("entityType", entityType)
                .put("entityId", entityId.trim())
                .put("familyId", payload.optString("familyId", BabyLogDomain.FAMILY_ID))
                .put("childId", payload.optString("childId", BabyLogDomain.CHILD_ID))
                .put("updatedAt", payload.optString("updatedAt", BabyLogFormatters.nowIso()))
                .put("schemaVersion", payload.optInt("schemaVersion", BabyLogDomain.SCHEMA_VERSION))
                .put("payload", payload);
    }

    public static boolean hasFamilyKey(String familyKey) {
        return !normalizeFamilyKeyForTransport(familyKey).isEmpty();
    }

    public static String normalizeFamilyKeyForTransport(String familyKey) {
        return familyKey == null ? "" : familyKey.trim();
    }
}
