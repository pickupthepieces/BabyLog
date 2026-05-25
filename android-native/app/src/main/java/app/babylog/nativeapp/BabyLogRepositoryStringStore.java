package app.babylog.nativeapp;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BabyLogRepositoryStringStore {
    private BabyLogRepositoryStringStore() {
    }

    static Store fromSharedPreferences(SharedPreferences preferences) {
        return new SharedPreferencesStore(preferences);
    }

    static Store memory() {
        return new MemoryStore();
    }

    interface Store {
        String getString(String key, String defaultValue);

        Editor edit();
    }

    interface Editor {
        Editor putString(String key, String value);

        Editor remove(String key);

        boolean commit();
    }

    private static final class SharedPreferencesStore implements Store {
        private final SharedPreferences preferences;

        SharedPreferencesStore(SharedPreferences preferences) {
            this.preferences = preferences;
        }

        @Override
        public String getString(String key, String defaultValue) {
            return preferences.getString(key, defaultValue);
        }

        @Override
        public Editor edit() {
            return new SharedPreferencesStoreEditor(preferences.edit());
        }
    }

    private static final class SharedPreferencesStoreEditor implements Editor {
        private final SharedPreferences.Editor editor;

        SharedPreferencesStoreEditor(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        @Override
        public Editor putString(String key, String value) {
            editor.putString(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            editor.remove(key);
            return this;
        }

        @Override
        public boolean commit() {
            return editor.commit();
        }
    }

    private static final class MemoryStore implements Store {
        private final Map<String, String> values = new LinkedHashMap<>();

        @Override
        public String getString(String key, String defaultValue) {
            String value = values.get(key);
            return value == null ? defaultValue : value;
        }

        @Override
        public Editor edit() {
            return new MemoryStoreEditor(values);
        }
    }

    private static final class MemoryStoreEditor implements Editor {
        private final Map<String, String> values;
        private final Map<String, String> pending = new LinkedHashMap<>();
        private final List<String> removals = new ArrayList<>();

        MemoryStoreEditor(Map<String, String> values) {
            this.values = values;
        }

        @Override
        public Editor putString(String key, String value) {
            pending.put(key, value);
            removals.remove(key);
            return this;
        }

        @Override
        public Editor remove(String key) {
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
