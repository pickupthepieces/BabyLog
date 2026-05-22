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

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("expected false");
        }
    }
}
