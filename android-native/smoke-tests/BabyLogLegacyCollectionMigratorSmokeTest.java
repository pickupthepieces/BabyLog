package app.babylog.nativeapp;

import static app.babylog.nativeapp.SmokeAssert.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BabyLogLegacyCollectionMigratorSmokeTest {
    public static void main(String[] args) throws Exception {
        assertDamagedJsonKeepsRecoverableSnapshot();
        assertIncompleteMigrationKeepsLegacyKey();
        assertSuccessfulMigrationRemovesLegacyKey();
    }

    private static void assertDamagedJsonKeepsRecoverableSnapshot() {
        BabyLogRepositoryStringStore.Store legacyStore = BabyLogRepositoryStringStore.memory();
        legacyStore.edit()
                .putString(BabyLogRepository.EVENTS_KEY, "[{\"id\":\"broken\"")
                .commit();
        FakeTargetStore targetStore = new FakeTargetStore();

        BabyLogLegacyCollectionMigrator.migrate(
                legacyStore,
                Arrays.asList(BabyLogRepository.EVENTS_KEY),
                targetStore
        );

        assertEquals(null, legacyStore.getString(BabyLogRepository.EVENTS_KEY, null));
        assertEquals(
                "[{\"id\":\"broken\"",
                legacyStore.getString(
                        BabyLogLegacyCollectionMigrator.migratedBackupKey(BabyLogRepository.EVENTS_KEY),
                        null
                )
        );
        assertEquals(0, targetStore.countRows(BabyLogRepository.EVENTS_KEY));
    }

    private static void assertIncompleteMigrationKeepsLegacyKey() throws Exception {
        BabyLogRepositoryStringStore.Store legacyStore = BabyLogRepositoryStringStore.memory();
        String raw = new JSONArray()
                .put(new JSONObject().put("id", "event_1"))
                .put(new JSONObject().put("note", "missing id"))
                .toString();
        legacyStore.edit().putString(BabyLogRepository.EVENTS_KEY, raw).commit();
        FakeTargetStore targetStore = new FakeTargetStore();

        BabyLogLegacyCollectionMigrator.migrate(
                legacyStore,
                Arrays.asList(BabyLogRepository.EVENTS_KEY),
                targetStore
        );

        assertEquals(raw, legacyStore.getString(BabyLogRepository.EVENTS_KEY, null));
        assertEquals(
                raw,
                legacyStore.getString(
                        BabyLogLegacyCollectionMigrator.migratedBackupKey(BabyLogRepository.EVENTS_KEY),
                        null
                )
        );
        assertEquals(1, targetStore.countRows(BabyLogRepository.EVENTS_KEY));
    }

    private static void assertSuccessfulMigrationRemovesLegacyKey() throws Exception {
        BabyLogRepositoryStringStore.Store legacyStore = BabyLogRepositoryStringStore.memory();
        String raw = new JSONArray()
                .put(new JSONObject().put("id", "event_1"))
                .put(new JSONObject().put("id", "event_2"))
                .toString();
        legacyStore.edit().putString(BabyLogRepository.EVENTS_KEY, raw).commit();
        FakeTargetStore targetStore = new FakeTargetStore();

        BabyLogLegacyCollectionMigrator.migrate(
                legacyStore,
                Arrays.asList(BabyLogRepository.EVENTS_KEY),
                targetStore
        );

        assertEquals(null, legacyStore.getString(BabyLogRepository.EVENTS_KEY, null));
        assertEquals(
                raw,
                legacyStore.getString(
                        BabyLogLegacyCollectionMigrator.migratedBackupKey(BabyLogRepository.EVENTS_KEY),
                        null
                )
        );
        assertEquals(2, targetStore.countRows(BabyLogRepository.EVENTS_KEY));
    }

    private static final class FakeTargetStore implements BabyLogLegacyCollectionMigrator.TargetStore {
        private final Map<String, Integer> rows = new LinkedHashMap<>();

        @Override
        public int countRows(String key) {
            Integer count = rows.get(key);
            return count == null ? 0 : count;
        }

        @Override
        public void upsertRows(String key, JSONArray array) {
            int count = countRows(key);
            for (int i = 0; i < array.length(); i += 1) {
                JSONObject json = array.optJSONObject(i);
                if (json != null && !json.optString("id", "").trim().isEmpty()) {
                    count += 1;
                }
            }
            rows.put(key, count);
        }
    }

}
