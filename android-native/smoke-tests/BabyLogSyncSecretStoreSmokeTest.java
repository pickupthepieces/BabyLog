import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogSyncSecretStore;

public final class BabyLogSyncSecretStoreSmokeTest {
    public static void main(String[] args) {
        assertEquals("family-secret", BabyLogSyncSecretStore.normalizeFamilyKeyForStorage("  family-secret  "));
        assertEquals("", BabyLogSyncSecretStore.normalizeFamilyKeyForStorage(null));
        assertFalse(BabyLogSyncSecretStore.hasUsableFamilyKey(""));
        assertFalse(BabyLogSyncSecretStore.hasUsableFamilyKey("   "));
        assertTrue(BabyLogSyncSecretStore.hasUsableFamilyKey("family-secret"));
        if (BabyLogSyncSecretStore.PREF_FILE_NAME.contains("native_repository")) {
            throw new AssertionError("family key must not share repository prefs");
        }
    }



}
