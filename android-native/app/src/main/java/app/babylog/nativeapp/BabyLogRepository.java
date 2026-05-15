package app.babylog.nativeapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class BabyLogRepository {
    private static final String PREFS_NAME = "babylog_native_repository_v1";
    private static final String EVENTS_KEY = "events";
    private static final String ATTACHMENTS_KEY = "attachments";
    private static final String SYNC_CHANGES_KEY = "syncChanges";
    private static final String SETTINGS_KEY = "syncSettings";

    private final SharedPreferences preferences;

    public BabyLogRepository(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void putEvent(BabyLogDomain.BabyLogEvent event) throws JSONException {
        putJson(EVENTS_KEY, event.id, event.toJson());
    }

    public List<BabyLogDomain.BabyLogEvent> listEvents() {
        JSONArray array = readArray(EVENTS_KEY);
        List<BabyLogDomain.BabyLogEvent> events = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            BabyLogDomain.BabyLogEvent event = BabyLogDomain.BabyLogEvent.fromJson(array.optJSONObject(i));
            if (event != null && BabyLogDomain.FAMILY_ID.equals(event.familyId) && event.deletedAt == null) {
                events.add(event);
            }
        }
        return events;
    }

    public void putAttachment(BabyLogDomain.AttachmentRecord attachment) throws JSONException {
        putJson(ATTACHMENTS_KEY, attachment.id, attachment.toJson());
    }

    public List<BabyLogDomain.AttachmentRecord> listAttachments() {
        JSONArray array = readArray(ATTACHMENTS_KEY);
        List<BabyLogDomain.AttachmentRecord> attachments = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.AttachmentRecord.fromJson(array.optJSONObject(i));
            if (attachment != null && BabyLogDomain.FAMILY_ID.equals(attachment.familyId) && attachment.deletedAt == null) {
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    public void putSyncChange(BabyLogDomain.SyncChange change) throws JSONException {
        putJson(SYNC_CHANGES_KEY, change.id, change.toJson());
    }

    public List<BabyLogDomain.SyncChange> listSyncChanges() {
        JSONArray array = readArray(SYNC_CHANGES_KEY);
        List<BabyLogDomain.SyncChange> changes = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            BabyLogDomain.SyncChange change = BabyLogDomain.SyncChange.fromJson(array.optJSONObject(i));
            if (change != null && BabyLogDomain.FAMILY_ID.equals(change.familyId)) {
                changes.add(change);
            }
        }
        return changes;
    }

    public BabyLogDomain.BackendConfig loadSyncSettings() {
        return BabyLogDomain.BackendConfig.fromJson(preferences.getString(SETTINGS_KEY, ""));
    }

    public BabyLogDomain.BackendConfig saveSyncSettings(BabyLogDomain.BackendConfig config) throws JSONException {
        preferences.edit().putString(SETTINGS_KEY, config.toJson().toString()).apply();
        return config;
    }

    public JSONArray exportEvents() {
        return readArray(EVENTS_KEY);
    }

    public JSONArray exportAttachments() {
        return readArray(ATTACHMENTS_KEY);
    }

    public JSONArray exportSyncChanges() {
        return readArray(SYNC_CHANGES_KEY);
    }

    public void importData(JSONArray events, JSONArray attachments, JSONArray syncChanges) {
        preferences.edit()
                .putString(EVENTS_KEY, events == null ? "[]" : events.toString())
                .putString(ATTACHMENTS_KEY, attachments == null ? "[]" : attachments.toString())
                .putString(SYNC_CHANGES_KEY, syncChanges == null ? "[]" : syncChanges.toString())
                .apply();
    }

    public void clearLocalData() {
        preferences.edit()
                .putString(EVENTS_KEY, "[]")
                .putString(ATTACHMENTS_KEY, "[]")
                .putString(SYNC_CHANGES_KEY, "[]")
                .apply();
    }

    public long estimateLocalBytes() {
        long bytes = 0;
        bytes += preferences.getString(EVENTS_KEY, "[]").length();
        bytes += preferences.getString(ATTACHMENTS_KEY, "[]").length();
        bytes += preferences.getString(SYNC_CHANGES_KEY, "[]").length();
        return bytes;
    }

    private void putJson(String key, String id, JSONObject next) throws JSONException {
        JSONArray array = readArray(key);
        JSONArray updated = new JSONArray();
        boolean replaced = false;
        for (int i = 0; i < array.length(); i++) {
            JSONObject current = array.optJSONObject(i);
            if (current == null) {
                continue;
            }
            if (id.equals(current.optString("id"))) {
                updated.put(next);
                replaced = true;
            } else {
                updated.put(current);
            }
        }
        if (!replaced) {
            updated.put(next);
        }
        preferences.edit().putString(key, updated.toString()).apply();
    }

    private JSONArray readArray(String key) {
        try {
            return new JSONArray(preferences.getString(key, "[]"));
        } catch (JSONException ignored) {
            return new JSONArray();
        }
    }
}
