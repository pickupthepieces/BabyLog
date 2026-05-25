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

public final class BabyLogAttachmentBlobStore {
    public static final String PREF_FILE_NAME = "babylog_attachment_blobs";

    private static final String TAG = "BabyLog";
    private static final String ATTACHMENT_BLOBS_KEY = "attachmentBlobs";
    private static final String ATTACHMENT_DOWNLOAD_QUEUE_KEY = "attachmentDownloadQueue";

    private final StringStore store;
    private final File attachmentBlobDir;

    public BabyLogAttachmentBlobStore(Context context, SharedPreferences legacyRepositoryPreferences) {
        this(
                new SharedPreferencesStringStore(context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)),
                new File(context.getFilesDir(), "attachments")
        );
        migrateLegacyStore(new SharedPreferencesLegacyStore(legacyRepositoryPreferences));
    }

    private BabyLogAttachmentBlobStore(StringStore store, File attachmentBlobDir) {
        this.store = store;
        this.attachmentBlobDir = attachmentBlobDir;
    }

    public static BabyLogAttachmentBlobStore forSmokeTest() {
        return new BabyLogAttachmentBlobStore(
                new MemoryStringStore(),
                new File(System.getProperty("java.io.tmpdir"), "babylog-attachment-blobs-" + System.nanoTime())
        );
    }

    public static BabyLogAttachmentBlobStore forSmokeTestMigrating(Map<String, String> legacyValues) {
        BabyLogAttachmentBlobStore store = forSmokeTest();
        store.migrateLegacyStore(new MapLegacyStore(legacyValues));
        return store;
    }

    public boolean putAttachmentBlobFromRemote(String attachmentId, byte[] bytes, String contentHash) {
        if (attachmentId == null || attachmentId.trim().isEmpty() || bytes == null) {
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
        try {
            JSONObject json = new JSONObject()
                    .put("id", attachmentId)
                    .put("attachmentId", attachmentId)
                    .put("localPath", output.getAbsolutePath())
                    .put("localBlobKey", output.getAbsolutePath())
                    .put("byteSize", bytes.length)
                    .put("contentHash", contentHash == null || contentHash.trim().isEmpty() ? JSONObject.NULL : contentHash);
            JSONArray updated = upsertJson(readArray(ATTACHMENT_BLOBS_KEY), attachmentId, json);
            return store.edit().putString(ATTACHMENT_BLOBS_KEY, updated.toString()).commit();
        } catch (JSONException error) {
            Log.w(TAG, "Failed to index remote attachment blob", error);
            return false;
        }
    }

    public boolean hasAttachmentBlob(String attachmentId) {
        byte[] bytes = findAttachmentBlobBytes(attachmentId);
        return bytes != null && bytes.length > 0;
    }

    public String attachmentBlobContentHash(String attachmentId) {
        JSONObject json = findBlobJson(attachmentId);
        return json == null ? "" : json.optString("contentHash", "");
    }

    public byte[] findAttachmentBlobBytes(String attachmentId) {
        JSONObject json = findBlobJson(attachmentId);
        String localPath = json == null ? "" : json.optString("localPath", "");
        return readFile(localPath);
    }

    byte[] findAttachmentBlobBytes(String attachmentId, String fallbackLocalPath) {
        byte[] bytes = findAttachmentBlobBytes(attachmentId);
        return bytes != null ? bytes : readFile(fallbackLocalPath);
    }

    String localPathForAttachment(String attachmentId) {
        JSONObject json = findBlobJson(attachmentId);
        return json == null ? "" : json.optString("localPath", "");
    }

    long byteSizeForAttachment(String attachmentId) {
        JSONObject json = findBlobJson(attachmentId);
        return json == null ? 0L : json.optLong("byteSize", 0L);
    }

    public void enqueueAttachmentDownload(
            String attachmentId,
            String remoteRecordId,
            String filename,
            String fileVersion
    ) throws JSONException {
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

    public List<BabyLogRepository.AttachmentDownloadRequest> listAttachmentDownloadQueue() {
        JSONArray array = readArray(ATTACHMENT_DOWNLOAD_QUEUE_KEY);
        List<BabyLogRepository.AttachmentDownloadRequest> requests = new ArrayList<>();
        for (int i = 0; i < array.length(); i += 1) {
            JSONObject json = array.optJSONObject(i);
            if (json != null) {
                requests.add(new BabyLogRepository.AttachmentDownloadRequest(
                        json.optString("attachmentId", ""),
                        json.optString("remoteRecordId", ""),
                        json.optString("filename", ""),
                        json.optString("fileVersion", "")
                ));
            }
        }
        return requests;
    }

    public void removeFromAttachmentDownloadQueue(String attachmentId) {
        hardDeleteJson(ATTACHMENT_DOWNLOAD_QUEUE_KEY, attachmentId);
    }

    private void migrateLegacyStore(LegacyStore legacy) {
        if (legacy == null) {
            return;
        }
        String blobs = legacy.getString(ATTACHMENT_BLOBS_KEY, "");
        String queue = legacy.getString(ATTACHMENT_DOWNLOAD_QUEUE_KEY, "");
        StringStoreEditor editor = store.edit();
        boolean changed = false;
        if (blobs != null && !blobs.trim().isEmpty()
                && store.getString(ATTACHMENT_BLOBS_KEY, "").trim().isEmpty()) {
            editor.putString(ATTACHMENT_BLOBS_KEY, blobs);
            changed = true;
        }
        if (queue != null && !queue.trim().isEmpty()
                && store.getString(ATTACHMENT_DOWNLOAD_QUEUE_KEY, "").trim().isEmpty()) {
            editor.putString(ATTACHMENT_DOWNLOAD_QUEUE_KEY, queue);
            changed = true;
        }
        if (changed) {
            editor.commit();
        }
        legacy.edit()
                .remove(ATTACHMENT_BLOBS_KEY)
                .remove(ATTACHMENT_DOWNLOAD_QUEUE_KEY)
                .commit();
    }

    private JSONObject findBlobJson(String attachmentId) {
        if (attachmentId == null || attachmentId.trim().isEmpty()) {
            return null;
        }
        JSONArray array = readArray(ATTACHMENT_BLOBS_KEY);
        for (int i = 0; i < array.length(); i += 1) {
            JSONObject json = array.optJSONObject(i);
            if (json != null && attachmentId.equals(json.optString("attachmentId", json.optString("id", "")))) {
                return json;
            }
        }
        return null;
    }

    private byte[] readFile(String localPath) {
        if (localPath == null || localPath.trim().isEmpty()) {
            return null;
        }
        File file = new File(localPath);
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

    private void hardDeleteJson(String key, String id) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        JSONArray array = readArray(key);
        JSONArray updated = new JSONArray();
        for (int i = 0; i < array.length(); i += 1) {
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
            Log.w(TAG, "Failed to parse attachment blob JSON array for key " + key, ignored);
            return new JSONArray();
        }
    }

    private JSONArray upsertJson(JSONArray array, String id, JSONObject next) throws JSONException {
        JSONArray updated = new JSONArray();
        boolean replaced = false;
        for (int i = 0; i < array.length(); i += 1) {
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

    private interface StringStore {
        String getString(String key, String defaultValue);

        StringStoreEditor edit();
    }

    private interface StringStoreEditor {
        StringStoreEditor putString(String key, String value);

        boolean commit();
    }

    private interface LegacyStore {
        String getString(String key, String defaultValue);

        LegacyEditor edit();
    }

    private interface LegacyEditor {
        LegacyEditor remove(String key);

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
        public boolean commit() {
            return editor.commit();
        }
    }

    private static final class SharedPreferencesLegacyStore implements LegacyStore {
        private final SharedPreferences preferences;

        SharedPreferencesLegacyStore(SharedPreferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public String getString(String key, String defaultValue) {
            return preferences == null ? defaultValue : preferences.getString(key, defaultValue);
        }

        @Override
        public LegacyEditor edit() {
            return new SharedPreferencesLegacyEditor(preferences == null ? null : preferences.edit());
        }
    }

    private static final class SharedPreferencesLegacyEditor implements LegacyEditor {
        private final SharedPreferences.Editor editor;

        SharedPreferencesLegacyEditor(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        @Override
        public LegacyEditor remove(String key) {
            if (editor != null) {
                editor.remove(key);
            }
            return this;
        }

        @Override
        public boolean commit() {
            return editor == null || editor.commit();
        }
    }

    private static final class MapLegacyStore implements LegacyStore {
        private final Map<String, String> values;

        MapLegacyStore(Map<String, String> values) {
            this.values = values == null ? new LinkedHashMap<>() : values;
        }

        @Override
        public String getString(String key, String defaultValue) {
            String value = values.get(key);
            return value == null ? defaultValue : value;
        }

        @Override
        public LegacyEditor edit() {
            return new MapLegacyEditor(values);
        }
    }

    private static final class MapLegacyEditor implements LegacyEditor {
        private final Map<String, String> values;
        private final List<String> removals = new ArrayList<>();

        MapLegacyEditor(Map<String, String> values) {
            this.values = values;
        }

        @Override
        public LegacyEditor remove(String key) {
            removals.add(key);
            return this;
        }

        @Override
        public boolean commit() {
            for (String key : removals) {
                values.remove(key);
            }
            return true;
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

        MemoryStringStoreEditor(Map<String, String> values) {
            this.values = values;
        }

        @Override
        public StringStoreEditor putString(String key, String value) {
            pending.put(key, value);
            return this;
        }

        @Override
        public boolean commit() {
            values.putAll(pending);
            return true;
        }
    }
}
