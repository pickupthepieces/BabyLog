import app.babylog.nativeapp.BabyLogRemoteSyncClient;
import app.babylog.nativeapp.BabyLogSyncProtocol;

import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
        assertPullEncryptedRecordsUsesHashHeaderAndCursor();
        assertUploadAttachmentFileUsesMultipartPatch();
        assertDownloadAttachmentFileUsesHashHeader();

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

    private static void assertPullEncryptedRecordsUsesHashHeaderAndCursor() throws Exception {
        final String expectedHash = BabyLogSyncProtocol.hashFamilyKeyForLookup("family-secret");
        final String[] capturedHeader = new String[1];
        final String[] capturedQuery = new String[1];
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/collections/encrypted_records/records", exchange -> {
            capturedHeader[0] = exchange.getRequestHeaders().getFirst(BabyLogSyncProtocol.HEADER_FAMILY_KEY);
            capturedQuery[0] = URLDecoder.decode(exchange.getRequestURI().getRawQuery(), "UTF-8");
            String body = "{"
                    + "\"page\":2,"
                    + "\"perPage\":2,"
                    + "\"totalItems\":3,"
                    + "\"totalPages\":2,"
                    + "\"items\":["
                    + "{\"clientId\":\"client_a\",\"familyKeyHash\":\"" + expectedHash + "\",\"schemaVersion\":1,\"cipherVersion\":1,\"nonce\":\"nonce_a\",\"ciphertext\":\"cipher_a\",\"updatedAtClient\":\"2026-05-25T09:00:00Z\",\"deletedFlag\":0},"
                    + "{\"clientId\":\"client_b\",\"familyKeyHash\":\"" + expectedHash + "\",\"schemaVersion\":1,\"cipherVersion\":1,\"nonce\":\"nonce_b\",\"ciphertext\":\"cipher_b\",\"updatedAtClient\":\"2026-05-25T10:00:00Z\",\"deletedFlag\":1}"
                    + "]"
                    + "}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        BabyLogRemoteSyncClient.PullResult result;
        try {
            int port = server.getAddress().getPort();
            BabyLogRemoteSyncClient client = new BabyLogRemoteSyncClient();
            result = client.pullEncryptedRecords(
                    "http://127.0.0.1:" + port,
                    "family-secret",
                    "2026-05-25T08:00:00Z",
                    2,
                    2
            );
        } finally {
            server.stop(0);
        }
        assertEquals(expectedHash, capturedHeader[0]);
        assertTrue(capturedQuery[0].contains("familyKeyHash=\"" + expectedHash + "\""));
        assertTrue(capturedQuery[0].contains("updatedAtClient > \"2026-05-25T08:00:00Z\""));
        assertTrue(capturedQuery[0].contains("page=2"));
        assertTrue(capturedQuery[0].contains("perPage=2"));
        assertEquals(2, result.records.size());
        assertEquals(2, result.page);
        assertEquals(2, result.totalPages);
        assertEquals(3, result.totalItems);
        assertEquals("client_a", result.records.get(0).clientId);
        assertEquals("cipher_b", result.records.get(1).ciphertext);
        assertEquals(1, result.records.get(1).deletedFlag);
        assertEquals("", result.lastError);
    }

    private static void assertUploadAttachmentFileUsesMultipartPatch() throws Exception {
        final String expectedHash = BabyLogSyncProtocol.hashFamilyKeyForLookup("family-secret");
        final byte[] sealedBytes = new byte[]{12, 8, 4, 2, 1, 0, -1, -2, 44, 55, 66, 77, 88, 99};
        final String[] capturedMethod = new String[1];
        final String[] capturedHeader = new String[1];
        final String[] capturedContentType = new String[1];
        final byte[][] capturedBody = new byte[1][];
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/collections/encrypted_records/records/remote_1", exchange -> {
            capturedMethod[0] = exchange.getRequestMethod();
            capturedHeader[0] = exchange.getRequestHeaders().getFirst(BabyLogSyncProtocol.HEADER_FAMILY_KEY);
            capturedContentType[0] = exchange.getRequestHeaders().getFirst("Content-Type");
            capturedBody[0] = readAll(exchange.getRequestBody());
            byte[] body = "{\"id\":\"remote_1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        BabyLogRemoteSyncClient.RecordPushResult result;
        try {
            int port = server.getAddress().getPort();
            BabyLogRemoteSyncClient client = new BabyLogRemoteSyncClient();
            result = client.uploadAttachmentFile(
                    "http://127.0.0.1:" + port,
                    "family-secret",
                    "remote_1",
                    sealedBytes,
                    "hash_v1"
            );
        } finally {
            server.stop(0);
        }
        assertTrue(result.ok);
        assertEquals("remote_1", result.clientId);
        assertEquals("PATCH", capturedMethod[0]);
        assertEquals(expectedHash, capturedHeader[0]);
        assertTrue(capturedContentType[0].startsWith("multipart/form-data; boundary="));
        String bodyText = new String(capturedBody[0], StandardCharsets.ISO_8859_1);
        assertTrue(bodyText.contains("name=\"attachmentFile\"; filename=\"attachment-remote_1.bin\""));
        assertTrue(bodyText.contains("Content-Type: application/octet-stream"));
        assertTrue(bodyText.contains("name=\"attachmentFileVersion\""));
        assertTrue(bodyText.contains("hash_v1"));
        assertTrue(indexOf(capturedBody[0], sealedBytes) >= 0);
    }

    private static void assertDownloadAttachmentFileUsesHashHeader() throws Exception {
        final String expectedHash = BabyLogSyncProtocol.hashFamilyKeyForLookup("family-secret");
        final byte[] sealedBytes = new byte[]{9, 8, 7, 6, 5, 4, 3, 2, 1};
        final String[] capturedHeader = new String[1];
        final String[] capturedMethod = new String[1];
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/files/encrypted_records/remote_2/attachment.bin", exchange -> {
            capturedMethod[0] = exchange.getRequestMethod();
            capturedHeader[0] = exchange.getRequestHeaders().getFirst(BabyLogSyncProtocol.HEADER_FAMILY_KEY);
            exchange.sendResponseHeaders(200, sealedBytes.length);
            exchange.getResponseBody().write(sealedBytes);
            exchange.close();
        });
        server.start();
        byte[] downloaded;
        try {
            int port = server.getAddress().getPort();
            BabyLogRemoteSyncClient client = new BabyLogRemoteSyncClient();
            downloaded = client.downloadAttachmentFile(
                    "http://127.0.0.1:" + port,
                    "family-secret",
                    "remote_2",
                    "attachment.bin"
            );
        } finally {
            server.stop(0);
        }
        assertEquals("GET", capturedMethod[0]);
        assertEquals(expectedHash, capturedHeader[0]);
        assertTrue(Arrays.equals(sealedBytes, downloaded));
    }

    private static byte[] readAll(InputStream input) throws java.io.IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static int indexOf(byte[] source, byte[] needle) {
        if (needle.length == 0 || source.length < needle.length) {
            return -1;
        }
        for (int offset = 0; offset <= source.length - needle.length; offset += 1) {
            if (Arrays.equals(Arrays.copyOfRange(source, offset, offset + needle.length), needle)) {
                return offset;
            }
        }
        return -1;
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
