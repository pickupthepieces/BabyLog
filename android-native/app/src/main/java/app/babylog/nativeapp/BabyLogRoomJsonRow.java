package app.babylog.nativeapp;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

import org.json.JSONObject;

@Entity(
        tableName = "repository_json_rows",
        primaryKeys = {"bucket", "id"},
        indices = {
                @Index({"bucket", "familyId", "deletedAt"}),
                @Index({"bucket", "familyId", "deletedAt", "occurredAt"}),
                @Index({"bucket", "updatedAt"})
        }
)
public final class BabyLogRoomJsonRow {
    @NonNull
    public String bucket;
    @NonNull
    public String id;
    @NonNull
    public String familyId;
    public String childId;
    public String deletedAt;
    public String occurredAt;
    public String updatedAt;
    @NonNull
    public String json;

    public BabyLogRoomJsonRow(
            @NonNull String bucket,
            @NonNull String id,
            @NonNull String familyId,
            String childId,
            String deletedAt,
            String occurredAt,
            String updatedAt,
            @NonNull String json
    ) {
        this.bucket = bucket;
        this.id = id;
        this.familyId = familyId;
        this.childId = childId;
        this.deletedAt = deletedAt;
        this.occurredAt = occurredAt;
        this.updatedAt = updatedAt;
        this.json = json;
    }

    static BabyLogRoomJsonRow fromJson(String bucket, JSONObject json) {
        String id = json.optString("id", "").trim();
        return new BabyLogRoomJsonRow(
                bucket,
                id,
                json.optString("familyId", BabyLogDomain.FAMILY_ID),
                json.optString("childId", BabyLogDomain.CHILD_ID),
                nullableString(json, "deletedAt"),
                nullableString(json, "occurredAt"),
                json.optString("updatedAt", ""),
                json.toString()
        );
    }

    private static String nullableString(JSONObject json, String key) {
        if (json == null || !json.has(key) || json.isNull(key)) {
            return null;
        }
        String value = json.optString(key, "");
        return value.trim().isEmpty() ? null : value;
    }
}
