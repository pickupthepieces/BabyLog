package app.babylog.nativeapp;

import org.json.JSONArray;

import java.util.List;

final class BabyLogLegacyCollectionMigrator {
    private static final String MIGRATED_PREFIX = "legacy_migrated_";

    private BabyLogLegacyCollectionMigrator() {
    }

    static void migrate(
            BabyLogRepositoryStringStore.Store legacyStore,
            List<String> collectionKeys,
            TargetStore targetStore
    ) {
        if (legacyStore == null || collectionKeys == null || targetStore == null) {
            return;
        }

        BabyLogRepositoryStringStore.Editor snapshotEditor = legacyStore.edit();
        for (String key : collectionKeys) {
            String raw = legacyStore.getString(key, null);
            if (raw != null) {
                snapshotEditor.putString(migratedBackupKey(key), raw);
            }
        }
        boolean snapshotsCommitted = snapshotEditor.commit();

        BabyLogRepositoryStringStore.Editor cleanupEditor = legacyStore.edit();
        for (String key : collectionKeys) {
            String raw = legacyStore.getString(key, null);
            if (raw == null) {
                continue;
            }
            JSONArray legacyArray = BabyLogRepositoryCollectionStore.readArrayFromString(raw);
            if (legacyArray.length() > 0 && targetStore.countRows(key) == 0) {
                targetStore.upsertRows(key, legacyArray);
            }
            if (snapshotsCommitted && targetStore.countRows(key) >= legacyArray.length()) {
                cleanupEditor.remove(key);
            }
        }
        cleanupEditor.commit();
    }

    static String migratedBackupKey(String key) {
        return MIGRATED_PREFIX + key;
    }

    interface TargetStore {
        int countRows(String key);

        void upsertRows(String key, JSONArray rows);
    }
}
