import app.babylog.nativeapp.BabyLogDomain;
import app.babylog.nativeapp.BabyLogRemoteSyncClient;
import app.babylog.nativeapp.BabyLogRepository;
import app.babylog.nativeapp.BabyLogSyncProtocol;
import app.babylog.nativeapp.BabyLogSyncPullOrchestrator;
import app.babylog.nativeapp.BabyLogSyncPushOrchestrator;

import com.sun.net.httpserver.HttpServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BabyLogSyncPullOrchestratorSmokeTest {
    public static void main(String[] args) throws Exception {
        String familyKey = "family-secret";
        BabyLogRepository repository = BabyLogRepository.forSmokeTest();
        repository.putEvent(localEvent("evt_4", "2026-05-25T10:00:00.000+0800", "本机版"));

        List<BabyLogRemoteSyncClient.EncryptedRecord> records = new ArrayList<>();
        records.add(encrypted(familyKey, "client_a", remoteEvent("evt_1", "2026-05-25T09:00:00.000+0800", "A 版"), false));
        records.add(encrypted(familyKey, "client_b", remoteEvent("evt_1", "2026-05-25T10:00:00.000+0800", "B 版"), false));
        records.add(encrypted(familyKey, "client_c", remoteEvent("evt_2", "2026-05-25T11:00:00.000+0800", "删除版"), true));
        records.add(encrypted(familyKey, "client_d", remoteEvent("evt_3", "2026-05-25T12:00:00.000+0800", "远端新增"), false));
        records.add(encrypted(familyKey, "client_e", remoteEvent("evt_4", "2026-05-25T08:00:00.000+0800", "远端旧版"), false));

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/collections/encrypted_records/records", exchange -> {
            try {
                String query = URLDecoder.decode(exchange.getRequestURI().getRawQuery(), "UTF-8");
                String cursor = cursorFromQuery(query);
                JSONArray items = new JSONArray();
                for (BabyLogRemoteSyncClient.EncryptedRecord record : records) {
                    if (cursor.isEmpty() || record.updatedAtClient.compareTo(cursor) > 0) {
                        items.put(record.toJson());
                    }
                }
                JSONObject body = new JSONObject()
                        .put("page", 1)
                        .put("perPage", 200)
                        .put("totalItems", items.length())
                        .put("totalPages", items.length() == 0 ? 0 : 1)
                        .put("items", items);
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (JSONException error) {
                throw new IOException(error);
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            BabyLogSyncPullOrchestrator orchestrator = new BabyLogSyncPullOrchestrator();
            BabyLogDomain.BackendConfig backend = new BabyLogDomain.BackendConfig(
                    true,
                    "http://127.0.0.1:" + server.getAddress().getPort(),
                    "",
                    null
            );
            BabyLogRepository wrongKeyRepository = BabyLogRepository.forSmokeTest();
            BabyLogSyncPullOrchestrator.PullSummary wrongKeySummary = orchestrator.pullOnce(
                    wrongKeyRepository,
                    "wrong-family-secret",
                    backend,
                    new BabyLogRemoteSyncClient()
            );
            assertEquals(5, wrongKeySummary.totalFetched);
            assertEquals(0, wrongKeySummary.applied);
            assertEquals(5, wrongKeySummary.skipped);
            assertEquals("", wrongKeyRepository.loadSyncLastPulledAt());

            BabyLogSyncPullOrchestrator.PullSummary summary = orchestrator.pullOnce(
                    repository,
                    familyKey,
                    backend,
                    new BabyLogRemoteSyncClient()
            );
            assertEquals("", summary.lastError);
            assertEquals(5, summary.totalFetched);
            assertEquals(3, summary.applied);
            assertEquals(2, summary.skipped);
            assertEquals("2026-05-25T12:00:00.000+0800", summary.newCursor);
            assertEquals("B 版", repository.findEventById("evt_1").payload.optString("note"));
            assertTrue(repository.findEventById("evt_2").deletedAt != null);
            assertEquals("远端新增", repository.findEventById("evt_3").payload.optString("note"));
            assertEquals("本机版", repository.findEventById("evt_4").payload.optString("note"));
            assertEquals(3, repository.loadRemoteUpdateBannerCount());
            assertEquals("2026-05-25T12:00:00.000+0800", repository.loadSyncLastPulledAt());
            assertEquals(0, repository.listSyncChanges().size());

            BabyLogSyncPullOrchestrator.PullSummary second = orchestrator.pullOnce(
                    repository,
                    familyKey,
                    backend,
                    new BabyLogRemoteSyncClient()
            );
            assertEquals(0, second.totalFetched);
            assertEquals(0, repository.listSyncChanges().size());
        } finally {
            server.stop(0);
        }
    }

    private static BabyLogRemoteSyncClient.EncryptedRecord encrypted(
            String familyKey,
            String clientId,
            BabyLogDomain.BabyLogEvent event,
            boolean deleted
    ) throws Exception {
        return BabyLogSyncPushOrchestrator.encryptEntityForPush(
                familyKey,
                clientId,
                BabyLogSyncProtocol.ENTITY_EVENT,
                event.id,
                event.toJson(),
                deleted
        );
    }

    private static BabyLogDomain.BabyLogEvent localEvent(String id, String updatedAt, String note) {
        return event(id, updatedAt, note, null);
    }

    private static BabyLogDomain.BabyLogEvent remoteEvent(String id, String updatedAt, String note) {
        String deletedAt = id.equals("evt_2") ? updatedAt : null;
        return event(id, updatedAt, note, deletedAt);
    }

    private static BabyLogDomain.BabyLogEvent event(String id, String updatedAt, String note, String deletedAt) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("note", note);
        } catch (Exception ignored) {
            // Static test data only.
        }
        return new BabyLogDomain.BabyLogEvent(
                id,
                BabyLogDomain.FAMILY_ID,
                BabyLogDomain.CHILD_ID,
                "note",
                updatedAt,
                payload,
                Collections.emptyList(),
                "manual",
                updatedAt,
                updatedAt,
                BabyLogDomain.UPDATED_BY_LOCAL,
                BabyLogDomain.SCHEMA_VERSION,
                deletedAt
        );
    }

    private static String cursorFromQuery(String query) {
        if (query == null) {
            return "";
        }
        int marker = query.indexOf("updatedAtClient > \"");
        if (marker < 0) {
            return "";
        }
        int start = marker + "updatedAtClient > \"".length();
        int end = query.indexOf('"', start);
        return end < 0 ? "" : query.substring(start, end);
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
}
