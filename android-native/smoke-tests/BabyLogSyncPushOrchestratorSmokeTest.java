import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogDomain;
import app.babylog.nativeapp.BabyLogAttachmentCipher;
import app.babylog.nativeapp.BabyLogFamilyKeyDeriver;
import app.babylog.nativeapp.BabyLogPayloadCipher;
import app.babylog.nativeapp.BabyLogRepository;
import app.babylog.nativeapp.BabyLogRemoteSyncClient;
import app.babylog.nativeapp.BabyLogSyncProtocol;
import app.babylog.nativeapp.BabyLogSyncPushOrchestrator;

import com.sun.net.httpserver.HttpServer;

import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

public final class BabyLogSyncPushOrchestratorSmokeTest {
    public static void main(String[] args) throws Exception {
        assertPushOnceUploadsAttachmentAsEncryptedMultipart();
        assertEncryptEntityBodyHasNoPlaintext();
    }

    private static void assertEncryptEntityBodyHasNoPlaintext() throws Exception {
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
                    Base64.getDecoder().decode(serverJson.getString("nonce")),
                    Base64.getDecoder().decode(serverJson.getString("ciphertext"))
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

    private static void assertPushOnceUploadsAttachmentAsEncryptedMultipart() throws Exception {
        final String familyKey = "family-secret";
        final String familyHash = BabyLogFamilyKeyDeriver.lookupHashHex(familyKey);
        final byte[] plaintextFile = "ultrasound-file-plain-bytes".getBytes(StandardCharsets.UTF_8);
        final int[] metadataRequests = new int[1];
        final int[] fileRequests = new int[1];
        final byte[][] capturedMultipart = new byte[1][];
        final String[] capturedContentType = new String[1];

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/collections/encrypted_records/records", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                metadataRequests[0] += 1;
                byte[] body = "{\"id\":\"remote_att_1\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
                return;
            }
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.createContext("/api/collections/encrypted_records/records/remote_att_1", exchange -> {
            fileRequests[0] += 1;
            capturedContentType[0] = exchange.getRequestHeaders().getFirst("Content-Type");
            capturedMultipart[0] = exchange.getRequestBody().readAllBytes();
            byte[] body = "{\"id\":\"remote_att_1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        File tempFile = File.createTempFile("babylog-s5d-", ".bin");
        try {
            Files.write(tempFile.toPath(), plaintextFile);
            BabyLogRepository repository = BabyLogRepository.forSmokeTest();
            BabyLogDomain.AttachmentRecord attachment = BabyLogDomain.createAttachment(
                    "ultrasound",
                    "scan.jpg",
                    "image/jpeg",
                    plaintextFile.length,
                    tempFile.getAbsolutePath()
            );
            BabyLogDomain.SyncChange change = BabyLogDomain.createSyncChange(
                    BabyLogSyncProtocol.ENTITY_ATTACHMENT,
                    attachment.id,
                    "upsert"
            );
            repository.putEventWithAttachmentsAndSyncChanges(null, Collections.singletonList(attachment), Collections.singletonList(change));

            int port = server.getAddress().getPort();
            BabyLogSyncPushOrchestrator.PushSummary summary = new BabyLogSyncPushOrchestrator().pushOnceForSmokeTest(
                    repository,
                    new BabyLogDomain.BackendConfig(true, "http://127.0.0.1:" + port, "cn", null),
                    new BabyLogRemoteSyncClient(),
                    familyKey
            );

            assertEquals(1, metadataRequests[0]);
            assertEquals(1, fileRequests[0]);
            assertEquals(1, summary.pushed);
            assertEquals(0, summary.failed);
            assertEquals(1, summary.filesUploaded);
            assertEquals(0, summary.filesPending);
            assertEquals((long) plaintextFile.length, summary.bytesUploaded);

            String boundary = boundaryFrom(capturedContentType[0]);
            byte[] sealedFile = extractMultipartFile(capturedMultipart[0], boundary);
            assertTrue(sealedFile.length > plaintextFile.length + 12);
            byte[] restored = BabyLogAttachmentCipher.openFile(
                    BabyLogFamilyKeyDeriver.deriveAttachmentKey(familyKey),
                    BabyLogSyncPushOrchestrator.attachmentAadBytes(attachment.id, BabyLogSyncPushOrchestrator.CIPHER_VERSION, familyHash),
                    sealedFile
            );
            assertTrue(Arrays.equals(plaintextFile, restored));

            List<BabyLogDomain.SyncChange> changes = repository.listSyncChanges();
            assertEquals(1, changes.size());
            assertEquals("synced", changes.get(0).status);
        } finally {
            server.stop(0);
            tempFile.delete();
        }
    }

    private static String boundaryFrom(String contentType) {
        String marker = "boundary=";
        int index = contentType == null ? -1 : contentType.indexOf(marker);
        if (index < 0) {
            throw new AssertionError("missing multipart boundary");
        }
        return contentType.substring(index + marker.length());
    }

    private static byte[] extractMultipartFile(byte[] body, String boundary) {
        byte[] headerEnd = "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);
        byte[] nextBoundary = ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        int start = indexOf(body, headerEnd, 0);
        if (start < 0) {
            throw new AssertionError("missing file header end");
        }
        start += headerEnd.length;
        int end = indexOf(body, nextBoundary, start);
        if (end <= start) {
            throw new AssertionError("missing file boundary");
        }
        return Arrays.copyOfRange(body, start, end);
    }

    private static int indexOf(byte[] source, byte[] needle, int from) {
        for (int offset = Math.max(0, from); offset <= source.length - needle.length; offset += 1) {
            boolean match = true;
            for (int i = 0; i < needle.length; i += 1) {
                if (source[offset + i] != needle[i]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return offset;
            }
        }
        return -1;
    }

    private static void assertServerBodyHasNoPlaintext(String body) {
        assertFalse(body.contains("ultrasound"));
        assertFalse(body.contains("sensitive content X"));
        assertFalse(body.contains("evt_xxx"));
        assertFalse(body.contains("\"event\""));
        assertFalse(body.contains("familyProfile"));
    }



}
