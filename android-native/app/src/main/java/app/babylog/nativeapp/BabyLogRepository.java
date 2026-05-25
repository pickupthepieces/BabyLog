package app.babylog.nativeapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BabyLogRepository {
    private static final String TAG = "BabyLog";
    private static final String PREFS_NAME = "babylog_native_repository_v1";
    private static final String EVENTS_KEY = "events";
    private static final String ATTACHMENTS_KEY = "attachments";
    private static final String SYNC_CHANGES_KEY = "syncChanges";
    private static final String SETTINGS_KEY = "syncSettings";
    private static final String FAMILY_PROFILE_KEY = "familyProfile";
    private static final String CHILD_PROFILE_KEY = "childProfile";
    private static final String CURRENT_MEMBER_KEY = "currentMember";
    private static final String SYNC_LAST_PULLED_AT_KEY = "syncLastPulledAt";
    private static final String REMOTE_UPDATE_BANNER_COUNT_KEY = "remoteUpdateBannerCount";
    private static final String ATTACHMENT_DOWNLOAD_QUEUE_KEY = "attachmentDownloadQueue";

    private final StringStore store;
    private final File attachmentBlobDir;

    public BabyLogRepository(Context context) {
        store = new SharedPreferencesStringStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
        attachmentBlobDir = new File(context.getFilesDir(), "attachments");
    }

    private BabyLogRepository(StringStore store) {
        this.store = store;
        attachmentBlobDir = new File(System.getProperty("java.io.tmpdir"), "babylog-repository-smoke-" + System.nanoTime());
    }

    public static BabyLogRepository forSmokeTest() {
        return new BabyLogRepository(new MemoryStringStore());
    }

    public void putEvent(BabyLogDomain.BabyLogEvent event) throws JSONException {
        putJson(EVENTS_KEY, event.id, event.toJson());
    }

    public BabyLogDomain.BabyLogEvent findEventById(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return null;
        }
        JSONArray array = readArray(EVENTS_KEY);
        for (int i = 0; i < array.length(); i++) {
            BabyLogDomain.BabyLogEvent event = BabyLogDomain.BabyLogEvent.fromJson(array.optJSONObject(i));
            if (event != null && BabyLogDomain.FAMILY_ID.equals(event.familyId) && eventId.equals(event.id)) {
                return event;
            }
        }
        return null;
    }

    public BabyLogDomain.FamilyProfile loadFamilyProfile() {
        String raw = store.getString(FAMILY_PROFILE_KEY, "");
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return BabyLogDomain.FamilyProfile.fromJson(new JSONObject(raw));
        } catch (JSONException ignored) {
            Log.w(TAG, "Failed to parse family profile JSON", ignored);
            return null;
        }
    }

    public BabyLogDomain.ChildProfile loadChildProfile() {
        String raw = store.getString(CHILD_PROFILE_KEY, "");
        if (raw == null || raw.trim().isEmpty()) {
            return BabyLogDomain.ChildProfile.empty();
        }
        try {
            BabyLogDomain.ChildProfile profile = BabyLogDomain.ChildProfile.fromJson(new JSONObject(raw));
            return profile == null ? BabyLogDomain.ChildProfile.empty() : profile;
        } catch (JSONException ignored) {
            Log.w(TAG, "Failed to parse child profile JSON", ignored);
            return BabyLogDomain.ChildProfile.empty();
        }
    }

    public BabyLogDomain.FamilyMember loadCurrentMember() {
        String raw = store.getString(CURRENT_MEMBER_KEY, "");
        if (raw == null || raw.trim().isEmpty()) {
            return BabyLogDomain.FamilyMember.localManager();
        }
        try {
            BabyLogDomain.FamilyMember member = BabyLogDomain.FamilyMember.fromJson(new JSONObject(raw));
            return member == null ? BabyLogDomain.FamilyMember.localManager() : member;
        } catch (JSONException ignored) {
            Log.w(TAG, "Failed to parse family member JSON", ignored);
            return BabyLogDomain.FamilyMember.localManager();
        }
    }

    public boolean hasCompletedSetup() {
        return loadChildProfile().setupCompleted;
    }

    public void saveProfileBundle(
            BabyLogDomain.FamilyProfile family,
            BabyLogDomain.ChildProfile child,
            BabyLogDomain.FamilyMember member
    ) throws JSONException {
        store.edit()
                .putString(FAMILY_PROFILE_KEY, (family == null ? BabyLogDomain.FamilyProfile.localDefault() : family).toJson().toString())
                .putString(CHILD_PROFILE_KEY, (child == null ? BabyLogDomain.ChildProfile.empty() : child).toJson().toString())
                .putString(CURRENT_MEMBER_KEY, (member == null ? BabyLogDomain.FamilyMember.localManager() : member).toJson().toString())
                .commit();
    }

    public void saveChildProfile(BabyLogDomain.ChildProfile child) throws JSONException {
        store.edit()
                .putString(CHILD_PROFILE_KEY, (child == null ? BabyLogDomain.ChildProfile.empty() : child).toJson().toString())
                .commit();
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

    public List<BabyLogDomain.BabyLogEvent> listDeletedEvents() {
        JSONArray array = readArray(EVENTS_KEY);
        List<BabyLogDomain.BabyLogEvent> events = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            BabyLogDomain.BabyLogEvent event = BabyLogDomain.BabyLogEvent.fromJson(array.optJSONObject(i));
            if (event != null && BabyLogDomain.FAMILY_ID.equals(event.familyId) && event.deletedAt != null) {
                events.add(event);
            }
        }
        return events;
    }

    public void putAttachment(BabyLogDomain.AttachmentRecord attachment) throws JSONException {
        putJson(ATTACHMENTS_KEY, attachment.id, attachment.toJson());
    }

    public BabyLogDomain.AttachmentRecord findAttachmentById(String attachmentId) {
        if (attachmentId == null || attachmentId.trim().isEmpty()) {
            return null;
        }
        JSONArray array = readArray(ATTACHMENTS_KEY);
        for (int i = 0; i < array.length(); i++) {
            BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.AttachmentRecord.fromJson(array.optJSONObject(i));
            if (attachment != null && BabyLogDomain.FAMILY_ID.equals(attachment.familyId) && attachmentId.equals(attachment.id)) {
                return attachment;
            }
        }
        return null;
    }

    public byte[] findAttachmentBlobBytes(String attachmentId) {
        BabyLogDomain.AttachmentRecord attachment = findAttachmentById(attachmentId);
        if (attachment == null || attachment.localPath == null || attachment.localPath.trim().isEmpty()) {
            return null;
        }
        File file = new File(attachment.localPath);
        if (!file.isFile()) {
            return null;
        }
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException error) {
            Log.w(TAG, "Failed to read attachment blob", error);
            return null;
        }
    }
    public boolean hasAttachmentBlob(String attachmentId) {
        byte[] bytes = findAttachmentBlobBytes(attachmentId);
        return bytes != null && bytes.length > 0;
    }
    public String attachmentBlobContentHash(String attachmentId) {
        BabyLogDomain.AttachmentRecord attachment = findAttachmentById(attachmentId);
        return attachment == null || attachment.contentHash == null ? "" : attachment.contentHash;
    }
    public boolean putAttachmentBlobFromRemote(String attachmentId, byte[] bytes, String contentHash) throws JSONException {
        if (attachmentId == null || attachmentId.trim().isEmpty() || bytes == null) {
            return false;
        }
        BabyLogDomain.AttachmentRecord attachment = findAttachmentById(attachmentId);
        if (attachment == null) {
            return false;
        }
        if (!attachmentBlobDir.exists() && !attachmentBlobDir.mkdirs()) {
            return false;
        }
        File output = new File(attachmentBlobDir, safeFileName(attachmentId) + ".bin");
        try (FileOutputStream stream = new FileOutputStream(output)) {
            stream.write(bytes);
        } catch (IOException error) {
            Log.w(TAG, "Failed to write remote attachment blob", error);
            return false;
        }
        JSONObject json = attachment.toJson()
                .put("localPath", output.getAbsolutePath())
                .put("localBlobKey", output.getAbsolutePath())
                .put("byteSize", bytes.length)
                .put("contentHash", contentHash == null || contentHash.trim().isEmpty() ? JSONObject.NULL : contentHash);
        JSONArray updated = upsertJson(readArray(ATTACHMENTS_KEY), attachmentId, json);
        return store.edit().putString(ATTACHMENTS_KEY, updated.toString()).commit();
    }
    public void enqueueAttachmentDownload(String attachmentId, String remoteRecordId, String filename, String fileVersion) throws JSONException {
        if (attachmentId == null || attachmentId.trim().isEmpty()
                || remoteRecordId == null || remoteRecordId.trim().isEmpty()
                || filename == null || filename.trim().isEmpty()) {
            return;
        }
        JSONObject json = new JSONObject()
                .put("id", attachmentId)
                .put("attachmentId", attachmentId)
                .put("remoteRecordId", remoteRecordId)
                .put("filename", filename)
                .put("fileVersion", fileVersion == null ? "" : fileVersion);
        JSONArray updated = upsertJson(readArray(ATTACHMENT_DOWNLOAD_QUEUE_KEY), attachmentId, json);
        store.edit().putString(ATTACHMENT_DOWNLOAD_QUEUE_KEY, updated.toString()).commit();
    }
    public List<AttachmentDownloadRequest> listAttachmentDownloadQueue() {
        JSONArray array = readArray(ATTACHMENT_DOWNLOAD_QUEUE_KEY);
        List<AttachmentDownloadRequest> requests = new ArrayList<>();
        for (int i = 0; i < array.length(); i += 1) {
            JSONObject json = array.optJSONObject(i);
            if (json != null) {
                requests.add(new AttachmentDownloadRequest(
                        json.optString("attachmentId", ""),
                        json.optString("remoteRecordId", ""),
                        json.optString("filename", ""),
                        json.optString("fileVersion", "")
                ));
            }
        }
        return requests;
    }
    public void removeAttachmentDownload(String attachmentId) {
        hardDeleteJson(ATTACHMENT_DOWNLOAD_QUEUE_KEY, attachmentId);
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

    public void hardDeleteEvent(String eventId) {
        hardDeleteJson(EVENTS_KEY, eventId);
    }

    public void hardDeleteAttachment(String attachmentId) {
        hardDeleteJson(ATTACHMENTS_KEY, attachmentId);
    }

    public void putSyncChange(BabyLogDomain.SyncChange change) throws JSONException {
        putJson(SYNC_CHANGES_KEY, change.id, change.toJson());
    }

    public boolean putEventsWithSyncChanges(
            List<BabyLogDomain.BabyLogEvent> events,
            List<BabyLogDomain.SyncChange> changes
    ) throws JSONException {
        JSONArray updatedEvents = readArray(EVENTS_KEY);
        JSONArray updatedChanges = readArray(SYNC_CHANGES_KEY);
        if (events != null) {
            for (BabyLogDomain.BabyLogEvent event : events) {
                if (event != null) {
                    updatedEvents = upsertJson(updatedEvents, event.id, event.toJson());
                }
            }
        }
        if (changes != null) {
            for (BabyLogDomain.SyncChange change : changes) {
                if (change != null) {
                    updatedChanges = upsertJson(updatedChanges, change.id, change.toJson());
                }
            }
        }
        return store.edit()
                .putString(EVENTS_KEY, updatedEvents.toString())
                .putString(SYNC_CHANGES_KEY, updatedChanges.toString())
                .commit();
    }

    public boolean putEventWithAttachmentsAndSyncChanges(
            BabyLogDomain.BabyLogEvent event,
            List<BabyLogDomain.AttachmentRecord> attachments,
            List<BabyLogDomain.SyncChange> changes
    ) throws JSONException {
        return putEventProfileAttachmentsAndSyncChanges(event, null, attachments, changes);
    }

    public boolean putEventProfileAttachmentsAndSyncChanges(
            BabyLogDomain.BabyLogEvent event,
            BabyLogDomain.ChildProfile childProfile,
            List<BabyLogDomain.AttachmentRecord> attachments,
            List<BabyLogDomain.SyncChange> changes
    ) throws JSONException {
        JSONArray updatedEvents = readArray(EVENTS_KEY);
        JSONArray updatedAttachments = readArray(ATTACHMENTS_KEY);
        JSONArray updatedChanges = readArray(SYNC_CHANGES_KEY);
        if (event != null) {
            updatedEvents = upsertJson(updatedEvents, event.id, event.toJson());
        }
        if (attachments != null) {
            for (BabyLogDomain.AttachmentRecord attachment : attachments) {
                if (attachment != null) {
                    updatedAttachments = upsertJson(updatedAttachments, attachment.id, attachment.toJson());
                }
            }
        }
        if (changes != null) {
            for (BabyLogDomain.SyncChange change : changes) {
                if (change != null) {
                    updatedChanges = upsertJson(updatedChanges, change.id, change.toJson());
                }
            }
        }
        StringStoreEditor editor = store.edit()
                .putString(EVENTS_KEY, updatedEvents.toString())
                .putString(ATTACHMENTS_KEY, updatedAttachments.toString())
                .putString(SYNC_CHANGES_KEY, updatedChanges.toString());
        if (childProfile != null) {
            editor.putString(CHILD_PROFILE_KEY, childProfile.toJson().toString());
        }
        return editor.commit();
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

    public boolean putEntityFromRemote(String entityType, JSONObject entityJson, boolean softDelete) throws JSONException {
        if (entityType == null || entityJson == null || entityJson.optString("id", "").trim().isEmpty()) {
            return false;
        }
        if (softDelete && !entityJson.has("deletedAt")) {
            entityJson.put("deletedAt", BabyLogFormatters.nowIso());
        }
        StringStoreEditor editor = store.edit();
        if (BabyLogSyncProtocol.ENTITY_EVENT.equals(entityType)) {
            JSONArray events = upsertJson(readArray(EVENTS_KEY), entityJson.optString("id"), entityJson);
            return editor.putString(EVENTS_KEY, events.toString()).commit();
        }
        if (BabyLogSyncProtocol.ENTITY_ATTACHMENT.equals(entityType)) {
            JSONArray attachments = upsertJson(readArray(ATTACHMENTS_KEY), entityJson.optString("id"), entityJson);
            return editor.putString(ATTACHMENTS_KEY, attachments.toString()).commit();
        }
        if (BabyLogSyncProtocol.ENTITY_CHILD_PROFILE.equals(entityType)) {
            return editor.putString(CHILD_PROFILE_KEY, entityJson.toString()).commit();
        }
        if (BabyLogSyncProtocol.ENTITY_FAMILY_PROFILE.equals(entityType)) {
            return editor.putString(FAMILY_PROFILE_KEY, entityJson.toString()).commit();
        }
        if (BabyLogSyncProtocol.ENTITY_FAMILY_MEMBER.equals(entityType)) {
            return editor.putString(CURRENT_MEMBER_KEY, entityJson.toString()).commit();
        }
        return false;
    }

    public String loadSyncLastPulledAt() {
        return store.getString(SYNC_LAST_PULLED_AT_KEY, "");
    }

    public void saveSyncLastPulledAt(String cursor) {
        store.edit().putString(SYNC_LAST_PULLED_AT_KEY, cursor == null ? "" : cursor).commit();
    }

    public int loadRemoteUpdateBannerCount() {
        try {
            return Integer.parseInt(store.getString(REMOTE_UPDATE_BANNER_COUNT_KEY, "0"));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public void addRemoteUpdateBannerCount(int count) {
        if (count <= 0) {
            return;
        }
        store.edit()
                .putString(REMOTE_UPDATE_BANNER_COUNT_KEY, String.valueOf(loadRemoteUpdateBannerCount() + count))
                .commit();
    }

    public void dismissRemoteUpdateBanner() {
        store.edit().putString(REMOTE_UPDATE_BANNER_COUNT_KEY, "0").commit();
    }

    public BabyLogDomain.BackendConfig loadSyncSettings() {
        return BabyLogDomain.BackendConfig.fromJson(store.getString(SETTINGS_KEY, ""));
    }

    public BabyLogDomain.BackendConfig saveSyncSettings(BabyLogDomain.BackendConfig config) throws JSONException {
        store.edit().putString(SETTINGS_KEY, config.toJson().toString()).commit();
        return config;
    }

    public JSONArray exportFamilyProfiles() throws JSONException {
        JSONArray array = new JSONArray();
        BabyLogDomain.FamilyProfile profile = loadFamilyProfile();
        if (profile != null) {
            array.put(profile.toJson());
        }
        return array;
    }

    public JSONArray exportChildProfiles() throws JSONException {
        JSONArray array = new JSONArray();
        BabyLogDomain.ChildProfile profile = loadChildProfile();
        if (profile.setupCompleted) {
            array.put(profile.toJson());
        }
        return array;
    }

    public JSONArray exportFamilyMembers() throws JSONException {
        JSONArray array = new JSONArray();
        if (loadChildProfile().setupCompleted) {
            array.put(loadCurrentMember().toJson());
        }
        return array;
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

    public boolean importData(
            JSONArray familyProfiles,
            JSONArray childProfiles,
            JSONArray familyMembers,
            JSONArray events,
            JSONArray attachments,
            JSONArray syncChanges
    ) {
        StringStoreEditor editor = store.edit()
                .putString(EVENTS_KEY, events == null ? "[]" : events.toString())
                .putString(ATTACHMENTS_KEY, attachments == null ? "[]" : attachments.toString())
                .putString(SYNC_CHANGES_KEY, syncChanges == null ? "[]" : syncChanges.toString());
        putFirstObjectOrRemove(editor, FAMILY_PROFILE_KEY, familyProfiles);
        putFirstObjectOrRemove(editor, CHILD_PROFILE_KEY, childProfiles);
        putFirstObjectOrRemove(editor, CURRENT_MEMBER_KEY, familyMembers);
        return editor.commit();
    }

    public void clearLocalData() {
        store.edit()
                .putString(EVENTS_KEY, "[]")
                .putString(ATTACHMENTS_KEY, "[]")
                .putString(SYNC_CHANGES_KEY, "[]")
                .remove(FAMILY_PROFILE_KEY)
                .remove(CHILD_PROFILE_KEY)
                .remove(CURRENT_MEMBER_KEY)
                .commit();
    }

    public long estimateLocalBytes() {
        long bytes = 0;
        bytes += store.getString(EVENTS_KEY, "[]").length();
        bytes += store.getString(ATTACHMENTS_KEY, "[]").length();
        bytes += store.getString(SYNC_CHANGES_KEY, "[]").length();
        bytes += store.getString(FAMILY_PROFILE_KEY, "").length();
        bytes += store.getString(CHILD_PROFILE_KEY, "").length();
        bytes += store.getString(CURRENT_MEMBER_KEY, "").length();
        return bytes;
    }

    private void putJson(String key, String id, JSONObject next) throws JSONException {
        JSONArray updated = upsertJson(readArray(key), id, next);
        store.edit().putString(key, updated.toString()).commit();
    }

    private JSONArray upsertJson(JSONArray array, String id, JSONObject next) throws JSONException {
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
        return updated;
    }

    private static String safeFileName(String value) {
        String safe = value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isEmpty() ? "attachment" : safe;
    }

    public static final class AttachmentDownloadRequest {
        public final String attachmentId, remoteRecordId, filename, fileVersion;
        AttachmentDownloadRequest(String attachmentId, String remoteRecordId, String filename, String fileVersion) {
            this.attachmentId = attachmentId == null ? "" : attachmentId;
            this.remoteRecordId = remoteRecordId == null ? "" : remoteRecordId;
            this.filename = filename == null ? "" : filename;
            this.fileVersion = fileVersion == null ? "" : fileVersion;
        }
    }

    private void hardDeleteJson(String key, String id) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        JSONArray array = readArray(key);
        JSONArray updated = new JSONArray();
        for (int i = 0; i < array.length(); i++) {
            JSONObject current = array.optJSONObject(i);
            if (current == null || id.equals(current.optString("id"))) {
                continue;
            }
            updated.put(current);
        }
        store.edit().putString(key, updated.toString()).commit();
    }

    private JSONArray readArray(String key) {
        try {
            return new JSONArray(store.getString(key, "[]"));
        } catch (JSONException ignored) {
            Log.w(TAG, "Failed to parse repository JSON array for key " + key, ignored);
            return new JSONArray();
        }
    }

    private void putFirstObjectOrRemove(StringStoreEditor editor, String key, JSONArray array) {
        JSONObject first = array == null ? null : array.optJSONObject(0);
        if (first == null) {
            editor.remove(key);
        } else {
            editor.putString(key, first.toString());
        }
    }

    private interface StringStore {
        String getString(String key, String defaultValue);

        StringStoreEditor edit();
    }

    private interface StringStoreEditor {
        StringStoreEditor putString(String key, String value);

        StringStoreEditor remove(String key);

        boolean commit();
    }

    private static final class SharedPreferencesStringStore implements StringStore {
        private final SharedPreferences preferences;

        SharedPreferencesStringStore(SharedPreferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public String getString(String key, String defaultValue) {
            return preferences.getString(key, defaultValue);
        }

        @Override
        public StringStoreEditor edit() {
            return new SharedPreferencesStringStoreEditor(preferences.edit());
        }
    }

    private static final class SharedPreferencesStringStoreEditor implements StringStoreEditor {
        private final SharedPreferences.Editor editor;

        SharedPreferencesStringStoreEditor(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        @Override
        public StringStoreEditor putString(String key, String value) {
            editor.putString(key, value);
            return this;
        }

        @Override
        public StringStoreEditor remove(String key) {
            editor.remove(key);
            return this;
        }

        @Override
        public boolean commit() {
            return editor.commit();
        }
    }

    private static final class MemoryStringStore implements StringStore {
        private final Map<String, String> values = new LinkedHashMap<>();

        @Override
        public String getString(String key, String defaultValue) {
            String value = values.get(key);
            return value == null ? defaultValue : value;
        }

        @Override
        public StringStoreEditor edit() {
            return new MemoryStringStoreEditor(values);
        }
    }

    private static final class MemoryStringStoreEditor implements StringStoreEditor {
        private final Map<String, String> values;
        private final Map<String, String> pending = new LinkedHashMap<>();
        private final List<String> removals = new ArrayList<>();

        MemoryStringStoreEditor(Map<String, String> values) {
            this.values = values;
        }

        @Override
        public StringStoreEditor putString(String key, String value) {
            pending.put(key, value);
            removals.remove(key);
            return this;
        }

        @Override
        public StringStoreEditor remove(String key) {
            removals.add(key);
            pending.remove(key);
            return this;
        }

        @Override
        public boolean commit() {
            for (String key : removals) {
                values.remove(key);
            }
            values.putAll(pending);
            return true;
        }
    }
}

