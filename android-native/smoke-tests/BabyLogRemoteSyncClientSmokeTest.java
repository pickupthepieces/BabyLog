import app.babylog.nativeapp.BabyLogRemoteSyncClient;
import app.babylog.nativeapp.BabyLogSyncProtocol;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

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

        assertHeaderUsesFamilyKeyHashOnly();

        BabyLogRemoteSyncClient.ConnectionResult ok = BabyLogRemoteSyncClient.ConnectionResult.ok("连接成功");
        assertTrue(ok.ok);
        assertEquals("连接成功", ok.message);

        BabyLogRemoteSyncClient.ConnectionResult failed = BabyLogRemoteSyncClient.ConnectionResult.failed("连接失败");
        assertFalse(failed.ok);
        assertEquals("连接失败", failed.message);
    }

    private static void assertHeaderUsesFamilyKeyHashOnly() throws Exception {
        final String[] capturedHeader = new String[1];
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/health", exchange -> {
            capturedHeader[0] = exchange.getRequestHeaders().getFirst(BabyLogSyncProtocol.HEADER_FAMILY_KEY);
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            BabyLogRemoteSyncClient client = new BabyLogRemoteSyncClient();
            client.checkHealth("http://127.0.0.1:" + port, "family-secret");
        } finally {
            server.stop(0);
        }
        assertTrue(capturedHeader[0] != null);
        assertEquals(64, capturedHeader[0].length());
        assertFalse(capturedHeader[0].contains("family-secret"));
        assertEquals(BabyLogSyncProtocol.hashFamilyKeyForLookup("family-secret"), capturedHeader[0]);
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
