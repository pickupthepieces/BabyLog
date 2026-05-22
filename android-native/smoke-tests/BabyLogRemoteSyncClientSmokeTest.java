import app.babylog.nativeapp.BabyLogRemoteSyncClient;
import app.babylog.nativeapp.BabyLogSyncProtocol;

public final class BabyLogRemoteSyncClientSmokeTest {
    public static void main(String[] args) throws Exception {
        assertEquals("https://sync.example.com/api/health", BabyLogRemoteSyncClient.healthUrl("https://sync.example.com/"));
        String familyHash = BabyLogSyncProtocol.hashFamilyKeyForLookup(" family-secret ");
        assertEquals(64, familyHash.length());
        assertEquals(familyHash, BabyLogSyncProtocol.hashFamilyKeyForLookup("family-secret"));
        assertFalse(familyHash.contains("family-secret"));

        String familyUrl = BabyLogRemoteSyncClient.familyLookupUrl("https://sync.example.com", "family-secret");
        assertTrue(familyUrl.startsWith("https://sync.example.com/api/collections/families/records?"));
        assertTrue(familyUrl.contains("familyKeyHash"));
        assertFalse(familyUrl.contains("family-secret"));

        BabyLogRemoteSyncClient.ConnectionResult ok = BabyLogRemoteSyncClient.ConnectionResult.ok("连接成功");
        assertTrue(ok.ok);
        assertEquals("连接成功", ok.message);

        BabyLogRemoteSyncClient.ConnectionResult failed = BabyLogRemoteSyncClient.ConnectionResult.failed("连接失败");
        assertFalse(failed.ok);
        assertEquals("连接失败", failed.message);
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
