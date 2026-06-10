package app.babylog.nativeapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BabyLogRepositoryCollectionStore {
    private BabyLogRepositoryCollectionStore() {
    }

    static Store fromStringStore(BabyLogRepositoryStringStore.Store store) {
        return new StringBackedStore(store);
    }

    interface Store {
        JSONArray readArray(String key);

        JSONArray readActiveArray(String key);

        JSONArray readActiveArrayPage(String key, int limit, int offset);

        JSONArray readDeletedArray(String key);

        JSONObject findJsonById(String key, String id);

        boolean putJson(String key, String id, JSONObject next) throws JSONException;

        boolean putJsons(List<Write> writes) throws JSONException;

        boolean replaceArrays(JSONArray events, JSONArray attachments, JSONArray syncChanges);

        void hardDeleteJson(String key, String id);

        void clearCollections();

        long estimateBytes();
    }

    static final class Write {
        final String key;
        final String id;
        final JSONObject json;

        Write(String key, String id, JSONObject json) {
            this.key = key;
            this.id = id;
            this.json = json;
        }
    }

    private static final class StringBackedStore implements Store {
        private final BabyLogRepositoryStringStore.Store store;

        StringBackedStore(BabyLogRepositoryStringStore.Store store) {
            this.store = store;
        }

        @Override
        public JSONArray readArray(String key) {
            return readArrayFromString(store.getString(key, "[]"));
        }

        @Override
        public JSONArray readActiveArray(String key) {
            return filterByDeletedState(readArray(key), false);
        }

        @Override
        public JSONArray readActiveArrayPage(String key, int limit, int offset) {
            if (limit <= 0) {
                return new JSONArray();
            }
            return sliceArray(sortRowsNewestFirst(readActiveArray(key)), limit, Math.max(0, offset));
        }

        @Override
        public JSONArray readDeletedArray(String key) {
            return filterByDeletedState(readArray(key), true);
        }

        @Override
        public JSONObject findJsonById(String key, String id) {
            if (id == null || id.trim().isEmpty()) {
                return null;
            }
            JSONArray array = readArray(key);
            for (int i = 0; i < array.length(); i += 1) {
                JSONObject current = array.optJSONObject(i);
                if (current != null && id.equals(current.optString("id"))) {
                    return current;
                }
            }
            return null;
        }

        @Override
        public boolean putJson(String key, String id, JSONObject next) throws JSONException {
            JSONArray updated = upsertJson(readArray(key), id, next);
            return store.edit().putString(key, updated.toString()).commit();
        }

        @Override
        public boolean putJsons(List<Write> writes) throws JSONException {
            Map<String, JSONArray> arrays = new LinkedHashMap<>();
            if (writes != null) {
                for (Write write : writes) {
                    if (write != null && write.json != null && write.id != null && !write.id.trim().isEmpty()) {
                        JSONArray current = arrays.containsKey(write.key) ? arrays.get(write.key) : readArray(write.key);
                        arrays.put(write.key, upsertJson(current, write.id, write.json));
                    }
                }
            }
            BabyLogRepositoryStringStore.Editor editor = store.edit();
            for (Map.Entry<String, JSONArray> entry : arrays.entrySet()) {
                editor.putString(entry.getKey(), entry.getValue().toString());
            }
            return editor.commit();
        }

        @Override
        public boolean replaceArrays(JSONArray events, JSONArray attachments, JSONArray syncChanges) {
            return store.edit()
                    .putString(BabyLogRepository.EVENTS_KEY, events == null ? "[]" : events.toString())
                    .putString(BabyLogRepository.ATTACHMENTS_KEY, attachments == null ? "[]" : attachments.toString())
                    .putString(BabyLogRepository.SYNC_CHANGES_KEY, syncChanges == null ? "[]" : syncChanges.toString())
                    .commit();
        }

        @Override
        public void hardDeleteJson(String key, String id) {
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

        @Override
        public void clearCollections() {
            store.edit()
                    .putString(BabyLogRepository.EVENTS_KEY, "[]")
                    .putString(BabyLogRepository.ATTACHMENTS_KEY, "[]")
                    .putString(BabyLogRepository.SYNC_CHANGES_KEY, "[]")
                    .commit();
        }

        @Override
        public long estimateBytes() {
            long bytes = 0L;
            bytes += store.getString(BabyLogRepository.EVENTS_KEY, "[]").length();
            bytes += store.getString(BabyLogRepository.ATTACHMENTS_KEY, "[]").length();
            bytes += store.getString(BabyLogRepository.SYNC_CHANGES_KEY, "[]").length();
            return bytes;
        }
    }

    static JSONArray readArrayFromString(String raw) {
        try {
            return new JSONArray(raw == null || raw.trim().isEmpty() ? "[]" : raw);
        } catch (JSONException ignored) {
            return new JSONArray();
        }
    }

    static JSONArray upsertJson(JSONArray array, String id, JSONObject next) throws JSONException {
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

    static JSONArray filterByDeletedState(JSONArray array, boolean deleted) {
        JSONArray filtered = new JSONArray();
        for (int i = 0; i < array.length(); i += 1) {
            JSONObject current = array.optJSONObject(i);
            if (current == null || !BabyLogDomain.FAMILY_ID.equals(current.optString("familyId", BabyLogDomain.FAMILY_ID))) {
                continue;
            }
            boolean hasDeletedAt = current.has("deletedAt") && !current.isNull("deletedAt")
                    && !current.optString("deletedAt", "").trim().isEmpty();
            if (deleted == hasDeletedAt) {
                filtered.put(current);
            }
        }
        return filtered;
    }

    static JSONArray sliceArray(JSONArray array, int limit, int offset) {
        JSONArray sliced = new JSONArray();
        if (array == null || limit <= 0) {
            return sliced;
        }
        int start = Math.max(0, offset);
        int end = Math.min(array.length(), start + limit);
        for (int i = start; i < end; i += 1) {
            Object current = array.opt(i);
            if (current != null) {
                sliced.put(current);
            }
        }
        return sliced;
    }

    static JSONArray sortRowsNewestFirst(JSONArray array) {
        List<JSONObject> rows = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i += 1) {
                JSONObject current = array.optJSONObject(i);
                if (current != null) {
                    rows.add(current);
                }
            }
        }
        Collections.sort(rows, (left, right) -> {
            int byTime = Long.compare(sortTime(right), sortTime(left));
            return byTime != 0 ? byTime : left.optString("id").compareTo(right.optString("id"));
        });
        JSONArray sorted = new JSONArray();
        for (JSONObject row : rows) {
            sorted.put(row);
        }
        return sorted;
    }

    private static long sortTime(JSONObject row) {
        long occurredAt = BabyLogFormatters.parseIsoMillis(row.optString("occurredAt"));
        return occurredAt > 0L ? occurredAt : BabyLogFormatters.parseIsoMillis(row.optString("updatedAt"));
    }

    static List<Write> writesFor(String key, JSONArray array) {
        List<Write> writes = new ArrayList<>();
        if (array == null) {
            return writes;
        }
        for (int i = 0; i < array.length(); i += 1) {
            JSONObject json = array.optJSONObject(i);
            if (json != null && !json.optString("id", "").trim().isEmpty()) {
                writes.add(new Write(key, json.optString("id"), json));
            }
        }
        return writes;
    }
}
