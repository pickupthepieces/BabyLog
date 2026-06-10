package app.babylog.nativeapp;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BabyLogRoomCollectionStore implements BabyLogRepositoryCollectionStore.Store {
    private static final List<String> COLLECTION_KEYS = Arrays.asList(
            BabyLogRepository.EVENTS_KEY,
            BabyLogRepository.ATTACHMENTS_KEY,
            BabyLogRepository.SYNC_CHANGES_KEY
    );

    private final BabyLogRoomDatabase database;
    private final BabyLogRoomJsonDao dao;

    private BabyLogRoomCollectionStore(BabyLogRoomDatabase database) {
        this.database = database;
        dao = database.jsonDao();
    }

    public static BabyLogRepositoryCollectionStore.Store create(
            Context context,
            BabyLogRepositoryStringStore.Store legacyStore
    ) {
        BabyLogRoomCollectionStore store = new BabyLogRoomCollectionStore(BabyLogRoomDatabase.getInstance(context));
        store.migrateLegacyCollections(legacyStore);
        return store;
    }

    @Override
    public JSONArray readArray(String key) {
        return jsonArrayFromRows(dao.listJson(key));
    }

    @Override
    public JSONArray readActiveArray(String key) {
        return jsonArrayFromRows(dao.listActiveJson(key, BabyLogDomain.FAMILY_ID));
    }

    @Override
    public JSONArray readActiveArrayPage(String key, int limit, int offset) {
        if (limit <= 0) {
            return new JSONArray();
        }
        return jsonArrayFromRows(dao.listActiveJsonPage(key, BabyLogDomain.FAMILY_ID, limit, Math.max(0, offset)));
    }

    @Override
    public JSONArray readDeletedArray(String key) {
        return jsonArrayFromRows(dao.listDeletedJson(key, BabyLogDomain.FAMILY_ID));
    }

    @Override
    public JSONObject findJsonById(String key, String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        String raw = dao.findJsonById(key, id);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(raw);
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Override
    public boolean putJson(String key, String id, JSONObject next) {
        if (next == null || id == null || id.trim().isEmpty()) {
            return false;
        }
        dao.upsert(BabyLogRoomJsonRow.fromJson(key, next));
        return true;
    }

    @Override
    public boolean putJsons(List<BabyLogRepositoryCollectionStore.Write> writes) {
        List<BabyLogRoomJsonRow> rows = rowsFor(writes);
        if (rows.isEmpty()) {
            return true;
        }
        database.runInTransaction(new Runnable() {
            @Override
            public void run() {
                dao.upsertAll(rows);
            }
        });
        return true;
    }

    @Override
    public boolean replaceArrays(JSONArray events, JSONArray attachments, JSONArray syncChanges) {
        database.runInTransaction(new Runnable() {
            @Override
            public void run() {
                dao.clearBucket(BabyLogRepository.EVENTS_KEY);
                dao.clearBucket(BabyLogRepository.ATTACHMENTS_KEY);
                dao.clearBucket(BabyLogRepository.SYNC_CHANGES_KEY);
                upsertRows(rowsFor(BabyLogRepository.EVENTS_KEY, events));
                upsertRows(rowsFor(BabyLogRepository.ATTACHMENTS_KEY, attachments));
                upsertRows(rowsFor(BabyLogRepository.SYNC_CHANGES_KEY, syncChanges));
            }
        });
        return true;
    }

    @Override
    public void hardDeleteJson(String key, String id) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        dao.hardDelete(key, id);
    }

    @Override
    public void clearCollections() {
        database.runInTransaction(new Runnable() {
            @Override
            public void run() {
                for (String key : COLLECTION_KEYS) {
                    dao.clearBucket(key);
                }
            }
        });
    }

    @Override
    public long estimateBytes() {
        return dao.estimateBytes();
    }

    private void migrateLegacyCollections(BabyLogRepositoryStringStore.Store legacyStore) {
        if (legacyStore == null) {
            return;
        }
        database.runInTransaction(new Runnable() {
            @Override
            public void run() {
                BabyLogRepositoryStringStore.Editor editor = legacyStore.edit();
                for (String key : COLLECTION_KEYS) {
                    JSONArray legacyArray = BabyLogRepositoryCollectionStore.readArrayFromString(
                            legacyStore.getString(key, "[]")
                    );
                    if (legacyArray.length() > 0 && dao.countRows(key) == 0) {
                        upsertRows(rowsFor(key, legacyArray));
                    }
                    editor.remove(key);
                }
                editor.commit();
            }
        });
    }

    private static List<BabyLogRoomJsonRow> rowsFor(List<BabyLogRepositoryCollectionStore.Write> writes) {
        List<BabyLogRoomJsonRow> rows = new ArrayList<>();
        if (writes == null) {
            return rows;
        }
        for (BabyLogRepositoryCollectionStore.Write write : writes) {
            if (write != null && write.json != null && write.id != null && !write.id.trim().isEmpty()) {
                rows.add(BabyLogRoomJsonRow.fromJson(write.key, write.json));
            }
        }
        return rows;
    }

    private static List<BabyLogRoomJsonRow> rowsFor(String key, JSONArray array) {
        List<BabyLogRoomJsonRow> rows = new ArrayList<>();
        if (array == null) {
            return rows;
        }
        for (int i = 0; i < array.length(); i += 1) {
            JSONObject json = array.optJSONObject(i);
            if (json != null && !json.optString("id", "").trim().isEmpty()) {
                rows.add(BabyLogRoomJsonRow.fromJson(key, json));
            }
        }
        return rows;
    }

    private void upsertRows(List<BabyLogRoomJsonRow> rows) {
        if (rows != null && !rows.isEmpty()) {
            dao.upsertAll(rows);
        }
    }

    private static JSONArray jsonArrayFromRows(List<String> rows) {
        JSONArray array = new JSONArray();
        for (String raw : rows) {
            try {
                array.put(new JSONObject(raw));
            } catch (JSONException ignored) {
                // Invalid rows are skipped; the original JSON store behaved the same way for bad objects.
            }
        }
        return array;
    }
}
