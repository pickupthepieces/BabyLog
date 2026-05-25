import app.babylog.nativeapp.BabyLogDomain;
import app.babylog.nativeapp.BabyLogFamilyKeyDeriver;
import app.babylog.nativeapp.BabyLogPayloadCipher;
import app.babylog.nativeapp.BabyLogRemoteSyncClient;
import app.babylog.nativeapp.BabyLogSyncBase64;
import app.babylog.nativeapp.BabyLogSyncProtocol;
import app.babylog.nativeapp.BabyLogSyncPushOrchestrator;

import com.sun.net.httpserver.HttpServer;

import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

public final class BabyLogSyncPushOrchestratorSmokeTest {
    public static void main(String[] args) throws Exception {
        final String[] capturedBody = new String[1];
        final String[] capturedHeader = new String[1];
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/collections/encrypted_records/records", exchange -> {
            capturedHeader[0] = exchange.getRequestHeaders().getFirst("X-BabyLog-Family-Key");
            capturedBody[0] = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = "{\"id\":\"remote_1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String familyKey = "family-secret";
            JSONObject payload = new JSONObject()
                    .put("eventType", "ultrasound")
                    .put("note", "sensitive content X");
            BabyLogDomain.BabyLogEvent event = new BabyLogDomain.BabyLogEvent(
                    "evt_xxx",
                    BabyLogDomain.FAMILY_ID,
                    BabyLogDomain.CHILD_ID,
                    "ultrasound",
                    "2026-05-25T09:00:00.000+0800",
                    payload,
                    Collections.emptyList(),
                    "manual",
                    "2026-05-25T09:00:00.000+0800",
                    "2026-05-25T09:00:00.000+0800",
                    BabyLogDomain.UPDATED_BY_LOCAL,
                    BabyLogDomain.SCHEMA_VERSION,
                    null
            );
            BabyLogRemoteSyncClient.EncryptedRecord encrypted = BabyLogSyncPushOrchestrator.encryptEntityForPush(
                    familyKey,
                    "client-test-1",
                    BabyLogSyncProtocol.ENTITY_EVENT,
                    event.id,
                    event.toJson()
            );
            BabyLogRemoteSyncClient client = new BabyLogRemoteSyncClient();
            int port = server.getAddress().getPort();
            BabyLogRemoteSyncClient.PushResult result = client.pushPendingChanges(
                    "http://127.0.0.1:" + port,
                    familyKey,
                    Arrays.asList(encrypted)
            );
            assertEquals(1, result.pushed);
            assertEquals(0, result.failed);
            assertEquals(BabyLogFamilyKeyDeriver.lookupHashHex(familyKey), capturedHeader[0]);
            assertServerBodyHasNoPlaintext(capturedBody[0]);
            assertTrue(capturedBody[0].contains("\"ciphertext\""));
            assertTrue(capturedBody[0].contains("\"nonce\""));
            assertTrue(capturedBody[0].contains("\"familyKeyHash\""));

            JSONObject serverJson = new JSONObject(capturedBody[0]);
            byte[] aad = BabyLogSyncPushOrchestrator.aadBytes(
                    serverJson.getString("clientId"),
                    serverJson.getInt("cipherVersion"),
                    serverJson.getString("familyKeyHash")
            );
            byte[] plaintext = BabyLogPayloadCipher.open(
                    BabyLogFamilyKeyDeriver.deriveDataKey(familyKey),
                    aad,
                    BabyLogSyncBase64.decode(serverJson.getString("nonce")),
                    BabyLogSyncBase64.decode(serverJson.getString("ciphertext"))
            );
            JSONObject restored = new JSONObject(new String(plaintext, StandardCharsets.UTF_8));
            assertEquals(BabyLogSyncProtocol.ENTITY_EVENT, restored.getString("entityType"));
            assertEquals("evt_xxx", restored.getString("entityId"));
            assertEquals("ultrasound", restored.getJSONObject("payload").getString("eventType"));
            assertEquals("sensitive content X", restored.getJSONObject("payload").getJSONObject("payload").getString("note"));
        } finally {
            server.stop(0);
        }
    }

    private static void assertServerBodyHasNoPlaintext(String body) {
        assertFalse(body.contains("ultrasound"));
        assertFalse(body.contains("sensitive content X"));
        assertFalse(body.contains("evt_xxx"));
        assertFalse(body.contains("\"event\""));
        assertFalse(body.contains("familyProfile"));
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
