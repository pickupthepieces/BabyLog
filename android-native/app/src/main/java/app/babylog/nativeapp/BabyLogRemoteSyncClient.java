package app.babylog.nativeapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

public final class BabyLogRemoteSyncClient {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int MAX_BODY_CHARS = 1024 * 1024;
    private static final int MAX_FILE_BODY_BYTES = 6 * 1024 * 1024;

    public ConnectionResult checkConnection(String backendBaseUrl, String familyKey) throws IOException {
        String normalizedUrl = BabyLogFormatters.normalizeBackendBaseUrl(backendBaseUrl);
        if (normalizedUrl.isEmpty()) {
            return ConnectionResult.failed("请先填写服务器地址");
        }
        if (!BabyLogSyncProtocol.hasFamilyKey(familyKey)) {
            return ConnectionResult.failed("请先填写家庭密钥");
        }

        ConnectionResult health = checkHealth(normalizedUrl, familyKey);
        if (!health.ok) {
            return health;
        }
        return checkFamily(normalizedUrl, familyKey);
    }

    public ConnectionResult checkHealth(String backendBaseUrl) throws IOException {
        return checkHealth(backendBaseUrl, "");
    }

    public ConnectionResult checkHealth(String backendBaseUrl, String familyKey) throws IOException {
        HttpResponse response = get(healthUrl(backendBaseUrl), familyKey);
        if (response.statusCode >= 200 && response.statusCode < 300) {
            return ConnectionResult.ok("服务器可达");
        }
        return ConnectionResult.failed("服务器健康检查失败：" + response.statusCode);
    }

    public ConnectionResult checkFamily(String backendBaseUrl, String familyKey) throws IOException {
        String normalizedKey = BabyLogSyncProtocol.normalizeFamilyKeyForTransport(familyKey);
        HttpResponse response = get(familyLookupUrl(backendBaseUrl, normalizedKey), normalizedKey);
        if (response.statusCode < 200 || response.statusCode >= 300) {
            return ConnectionResult.failed("家庭查询失败：" + response.statusCode);
        }
        try {
            JSONObject json = new JSONObject(response.body);
            JSONArray items = json.optJSONArray("items");
            if (items != null && items.length() > 0) {
                return ConnectionResult.ok("连接成功，已找到家庭");
            }
            return ConnectionResult.failed("服务器可达，但未找到匹配家庭");
        } catch (JSONException error) {
            return ConnectionResult.failed("服务器返回格式不符合 PocketBase records API");
        }
    }

    public PushResult pushPendingChanges(
            String backendBaseUrl,
            String familyKey,
            List<EncryptedRecord> encrypted
    ) throws IOException {
        String normalizedUrl = BabyLogFormatters.normalizeBackendBaseUrl(backendBaseUrl);
        if (normalizedUrl.isEmpty()) {
            return PushResult.failedAll(encrypted, "BACKEND_NOT_CONFIGURED");
        }
        if (!BabyLogSyncProtocol.hasFamilyKey(familyKey)) {
            return PushResult.failedAll(encrypted, "FAMILY_KEY_MISSING");
        }
        List<RecordPushResult> results = new ArrayList<>();
        if (encrypted == null) {
            return new PushResult(results);
        }
        for (EncryptedRecord record : encrypted) {
            if (record == null) {
                continue;
            }
            RecordPushResult result = pushOne(normalizedUrl, familyKey, record);
            results.add(result);
        }
        return new PushResult(results);
    }

    public PullResult pullEncryptedRecords(
            String backendBaseUrl,
            String familyKey,
            String sinceCursor,
            int page,
            int perPage
    ) throws IOException {
        String normalizedUrl = BabyLogFormatters.normalizeBackendBaseUrl(backendBaseUrl);
        if (normalizedUrl.isEmpty()) {
            return PullResult.failed("BACKEND_NOT_CONFIGURED");
        }
        if (!BabyLogSyncProtocol.hasFamilyKey(familyKey)) {
            return PullResult.failed("FAMILY_KEY_MISSING");
        }
        HttpResponse response = get(encryptedRecordsPullUrl(normalizedUrl, familyKey, sinceCursor, page, perPage), familyKey);
        if (response.statusCode < 200 || response.statusCode >= 300) {
            return PullResult.failed("HTTP_" + response.statusCode);
        }
        try {
            JSONObject json = new JSONObject(response.body);
            JSONArray items = json.optJSONArray("items");
            List<EncryptedRecord> records = new ArrayList<>();
            if (items != null) {
                for (int i = 0; i < items.length(); i += 1) {
                    JSONObject item = items.optJSONObject(i);
                    if (item != null) {
                        records.add(EncryptedRecord.fromJson(item));
                    }
                }
            }
            return new PullResult(
                    records,
                    json.optInt("page", page),
                    json.optInt("totalPages", 0),
                    json.optInt("totalItems", records.size()),
                    ""
            );
        } catch (JSONException error) {
            return PullResult.failed("PARSE_FAILED");
        }
    }

    public RecordPushResult uploadAttachmentFile(
            String backendBaseUrl,
            String familyKey,
            String recordId,
            byte[] sealedBytes,
            String contentHashVersion
    ) throws IOException {
        String normalizedUrl = BabyLogFormatters.normalizeBackendBaseUrl(backendBaseUrl);
        if (normalizedUrl.isEmpty()) {
            return RecordPushResult.failed(recordId, "BACKEND_NOT_CONFIGURED");
        }
        if (!BabyLogSyncProtocol.hasFamilyKey(familyKey)) {
            return RecordPushResult.failed(recordId, "FAMILY_KEY_MISSING");
        }
        String safeRecordId = recordId == null ? "" : recordId.trim();
        if (safeRecordId.isEmpty()) {
            return RecordPushResult.failed(recordId, "RECORD_ID_MISSING");
        }
        String boundary = "BabyLogBoundary" + Long.toHexString(System.nanoTime());
        byte[] body = multipartAttachmentBody(boundary, safeRecordId, sealedBytes, contentHashVersion);
        String url = encryptedRecordsUrl(normalizedUrl)
                + "/"
                + URLEncoder.encode(safeRecordId, "UTF-8");
        HttpResponse response = rawPatch(url, familyKey, "multipart/form-data; boundary=" + boundary, body);
        if (response.statusCode >= 200 && response.statusCode < 300) {
            return RecordPushResult.ok(safeRecordId);
        }
        return RecordPushResult.failed(safeRecordId, "HTTP_" + response.statusCode);
    }

    public byte[] downloadAttachmentFile(
            String backendBaseUrl,
            String familyKey,
            String recordId,
            String filename
    ) throws IOException {
        String normalizedUrl = BabyLogFormatters.normalizeBackendBaseUrl(backendBaseUrl);
        if (normalizedUrl.isEmpty()) {
            throw new IOException("BACKEND_NOT_CONFIGURED");
        }
        if (!BabyLogSyncProtocol.hasFamilyKey(familyKey)) {
            throw new IOException("FAMILY_KEY_MISSING");
        }
        String safeRecordId = recordId == null ? "" : recordId.trim();
        String safeFilename = filename == null ? "" : filename.trim();
        if (safeRecordId.isEmpty() || safeFilename.isEmpty()) {
            throw new IOException("ATTACHMENT_FILE_MISSING");
        }
        return getBytes(
                normalizedUrl
                        + "/api/files/"
                        + BabyLogSyncProtocol.COLLECTION_ENCRYPTED_RECORDS
                        + "/"
                        + URLEncoder.encode(safeRecordId, "UTF-8")
                        + "/"
                        + URLEncoder.encode(safeFilename, "UTF-8"),
                familyKey
        );
    }

    public static String healthUrl(String backendBaseUrl) {
        return BabyLogFormatters.normalizeBackendBaseUrl(backendBaseUrl) + "/api/health";
    }

    public static String familyLookupUrl(String backendBaseUrl, String familyKey) throws IOException {
        String familyKeyHash = BabyLogSyncProtocol.hashFamilyKeyForLookup(familyKey);
        String filter = "familyKeyHash=\"" + familyKeyHash + "\"";
        return BabyLogFormatters.normalizeBackendBaseUrl(backendBaseUrl)
                + "/api/collections/"
                + BabyLogSyncProtocol.COLLECTION_FAMILIES
                + "/records?filter="
                + URLEncoder.encode(filter, "UTF-8");
    }

    public static String encryptedRecordsUrl(String backendBaseUrl) {
        return BabyLogFormatters.normalizeBackendBaseUrl(backendBaseUrl)
                + "/api/collections/"
                + BabyLogSyncProtocol.COLLECTION_ENCRYPTED_RECORDS
                + "/records";
    }

    public static String encryptedRecordsPullUrl(
            String backendBaseUrl,
            String familyKey,
            String sinceCursor,
            int page,
            int perPage
    ) throws IOException {
        String familyKeyHash = BabyLogSyncProtocol.hashFamilyKeyForLookup(familyKey);
        StringBuilder filter = new StringBuilder();
        filter.append("familyKeyHash=\"").append(familyKeyHash).append("\"");
        if (sinceCursor != null && !sinceCursor.trim().isEmpty()) {
            filter.append(" && updatedAtClient > \"").append(sinceCursor.trim()).append("\"");
        }
        return encryptedRecordsUrl(backendBaseUrl)
                + "?filter="
                + URLEncoder.encode(filter.toString(), "UTF-8")
                + "&sort=updatedAtClient"
                + "&page="
                + Math.max(1, page)
                + "&perPage="
                + Math.max(1, perPage);
    }

    private RecordPushResult pushOne(String backendBaseUrl, String familyKey, EncryptedRecord record) throws IOException {
        HttpResponse response = request("POST", encryptedRecordsUrl(backendBaseUrl), familyKey, record.toJson().toString());
        if (response.statusCode >= 200 && response.statusCode < 300) {
            return RecordPushResult.ok(record.clientId, remoteIdFromBody(response.body));
        }
        if (response.statusCode == 409) {
            String remoteId = findRemoteRecordId(backendBaseUrl, familyKey, record.clientId);
            if (!remoteId.isEmpty()) {
                HttpResponse patched = request(
                        "PATCH",
                        encryptedRecordsUrl(backendBaseUrl) + "/" + URLEncoder.encode(remoteId, "UTF-8"),
                        familyKey,
                        record.toJson().toString()
                );
                if (patched.statusCode >= 200 && patched.statusCode < 300) {
                    return RecordPushResult.ok(record.clientId, remoteId);
                }
                return RecordPushResult.failed(record.clientId, "HTTP_" + patched.statusCode);
            }
        }
        return RecordPushResult.failed(record.clientId, "HTTP_" + response.statusCode);
    }

    private String findRemoteRecordId(String backendBaseUrl, String familyKey, String clientId) throws IOException {
        String filter = "clientId=\"" + (clientId == null ? "" : clientId) + "\"";
        String url = encryptedRecordsUrl(backendBaseUrl)
                + "?filter="
                + URLEncoder.encode(filter, "UTF-8");
        HttpResponse response = get(url, familyKey);
        if (response.statusCode < 200 || response.statusCode >= 300) {
            return "";
        }
        try {
            JSONObject json = new JSONObject(response.body);
            JSONArray items = json.optJSONArray("items");
            JSONObject first = items == null || items.length() == 0 ? null : items.optJSONObject(0);
            return first == null ? "" : first.optString("id", "");
        } catch (JSONException ignored) {
            return "";
        }
    }

    private HttpResponse get(String url, String familyKey) throws IOException {
        return request("GET", url, familyKey, null);
    }

    private byte[] getBytes(String url, String familyKey) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty(BabyLogSyncProtocol.HEADER_CLIENT_SCHEMA, String.valueOf(BabyLogDomain.SCHEMA_VERSION));
        if (BabyLogSyncProtocol.hasFamilyKey(familyKey)) {
            connection.setRequestProperty(BabyLogSyncProtocol.HEADER_FAMILY_KEY, BabyLogSyncProtocol.hashFamilyKeyForLookup(familyKey));
        }
        int statusCode = connection.getResponseCode();
        if (statusCode < 200 || statusCode >= 300) {
            String errorBody = readLimited(connection.getErrorStream());
            connection.disconnect();
            throw new IOException("HTTP_" + statusCode + (errorBody.isEmpty() ? "" : ":" + errorBody));
        }
        byte[] bytes = readBytesLimited(connection.getInputStream());
        connection.disconnect();
        return bytes;
    }

    private HttpResponse rawPatch(String url, String familyKey, String contentType, byte[] body) throws IOException {
        URI uri = URI.create(url);
        boolean https = "https".equalsIgnoreCase(uri.getScheme());
        int port = uri.getPort() > 0 ? uri.getPort() : (https ? 443 : 80);
        String path = uri.getRawPath();
        if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
            path += "?" + uri.getRawQuery();
        }
        Socket socket = https
                ? SSLSocketFactory.getDefault().createSocket(uri.getHost(), port)
                : new Socket(uri.getHost(), port);
        socket.setSoTimeout(READ_TIMEOUT_MS);
        try (Socket closeable = socket;
             OutputStream output = closeable.getOutputStream();
             InputStream input = closeable.getInputStream()) {
            byte[] safeBody = body == null ? new byte[0] : body;
            StringBuilder headers = new StringBuilder();
            headers.append("PATCH ").append(path).append(" HTTP/1.1\r\n");
            headers.append("Host: ").append(uri.getHost());
            if (uri.getPort() > 0) {
                headers.append(":").append(uri.getPort());
            }
            headers.append("\r\n");
            headers.append(BabyLogSyncProtocol.HEADER_CLIENT_SCHEMA)
                    .append(": ")
                    .append(BabyLogDomain.SCHEMA_VERSION)
                    .append("\r\n");
            headers.append(BabyLogSyncProtocol.HEADER_FAMILY_KEY)
                    .append(": ")
                    .append(BabyLogSyncProtocol.hashFamilyKeyForLookup(familyKey))
                    .append("\r\n");
            headers.append("Content-Type: ").append(contentType).append("\r\n");
            headers.append("Content-Length: ").append(safeBody.length).append("\r\n");
            headers.append("Connection: close\r\n\r\n");
            output.write(headers.toString().getBytes(StandardCharsets.UTF_8));
            output.write(safeBody);
            output.flush();
            return readRawHttpResponse(input);
        }
    }

    private String remoteIdFromBody(String body) {
        try {
            return new JSONObject(body == null ? "{}" : body).optString("id", "");
        } catch (JSONException ignored) {
            return "";
        }
    }

    private static HttpResponse readRawHttpResponse(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String statusLine = reader.readLine();
        int statusCode = 0;
        if (statusLine != null) {
            String[] parts = statusLine.split(" ");
            if (parts.length >= 2) {
                try {
                    statusCode = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    statusCode = 0;
                }
            }
        }
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            // Skip headers.
        }
        StringBuilder body = new StringBuilder();
        char[] buffer = new char[512];
        int read;
        while ((read = reader.read(buffer)) != -1 && body.length() < MAX_BODY_CHARS) {
            int remaining = MAX_BODY_CHARS - body.length();
            body.append(buffer, 0, Math.min(read, remaining));
        }
        return new HttpResponse(statusCode, body.toString());
    }

    private static byte[] multipartAttachmentBody(
            String boundary,
            String recordId,
            byte[] sealedBytes,
            String contentHashVersion
    ) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeUtf8(body, "--" + boundary + "\r\n");
        writeUtf8(body, "Content-Disposition: form-data; name=\"attachmentFile\"; filename=\"attachment-" + recordId + ".bin\"\r\n");
        writeUtf8(body, "Content-Type: application/octet-stream\r\n\r\n");
        body.write(sealedBytes == null ? new byte[0] : sealedBytes);
        writeUtf8(body, "\r\n--" + boundary + "\r\n");
        writeUtf8(body, "Content-Disposition: form-data; name=\"attachmentFileVersion\"\r\n\r\n");
        writeUtf8(body, contentHashVersion == null ? "" : contentHashVersion);
        writeUtf8(body, "\r\n--" + boundary + "--\r\n");
        return body.toByteArray();
    }

    private static void writeUtf8(OutputStream output, String value) throws IOException {
        output.write((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private HttpResponse request(String method, String url, String familyKey, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty(BabyLogSyncProtocol.HEADER_CLIENT_SCHEMA, String.valueOf(BabyLogDomain.SCHEMA_VERSION));
        if (BabyLogSyncProtocol.hasFamilyKey(familyKey)) {
            connection.setRequestProperty(BabyLogSyncProtocol.HEADER_FAMILY_KEY, BabyLogSyncProtocol.hashFamilyKeyForLookup(familyKey));
        }
        if (body != null) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }
        }
        int statusCode = connection.getResponseCode();
        String responseBody = readLimited(statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream());
        connection.disconnect();
        return new HttpResponse(statusCode, responseBody);
    }

    private static String readLimited(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[512];
            int read;
            while ((read = reader.read(buffer)) != -1 && builder.length() < MAX_BODY_CHARS) {
                int remaining = MAX_BODY_CHARS - builder.length();
                builder.append(buffer, 0, Math.min(read, remaining));
            }
        }
        return builder.toString();
    }

    private static byte[] readBytesLimited(InputStream stream) throws IOException {
        if (stream == null) {
            return new byte[0];
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = stream.read(buffer)) != -1) {
            total += read;
            if (total > MAX_FILE_BODY_BYTES) {
                throw new IOException("ATTACHMENT_FILE_TOO_LARGE");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static final class HttpResponse {
        final int statusCode;
        final String body;

        HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
        }
    }

    public static final class ConnectionResult {
        public final boolean ok;
        public final String message;

        private ConnectionResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message == null ? "" : message;
        }

        public static ConnectionResult ok(String message) {
            return new ConnectionResult(true, message);
        }

        public static ConnectionResult failed(String message) {
            return new ConnectionResult(false, message);
        }
    }

    public static final class EncryptedRecord {
        public final String clientId;
        public final String familyKeyHash;
        public final int schemaVersion;
        public final int cipherVersion;
        public final String nonce;
        public final String ciphertext;
        public final String updatedAtClient;
        public final int deletedFlag;
        public final String remoteId;
        public final String attachmentFile;
        public final String attachmentFileVersion;

        public EncryptedRecord(
                String clientId,
                String familyKeyHash,
                int schemaVersion,
                int cipherVersion,
                String nonce,
                String ciphertext,
                String updatedAtClient,
                int deletedFlag
        ) {
            this(clientId, familyKeyHash, schemaVersion, cipherVersion, nonce, ciphertext, updatedAtClient, deletedFlag, "", "", "");
        }

        public EncryptedRecord(
                String clientId,
                String familyKeyHash,
                int schemaVersion,
                int cipherVersion,
                String nonce,
                String ciphertext,
                String updatedAtClient,
                int deletedFlag,
                String remoteId,
                String attachmentFile,
                String attachmentFileVersion
        ) {
            this.clientId = clientId == null ? "" : clientId;
            this.familyKeyHash = familyKeyHash == null ? "" : familyKeyHash;
            this.schemaVersion = schemaVersion;
            this.cipherVersion = cipherVersion;
            this.nonce = nonce == null ? "" : nonce;
            this.ciphertext = ciphertext == null ? "" : ciphertext;
            this.updatedAtClient = updatedAtClient == null ? "" : updatedAtClient;
            this.deletedFlag = deletedFlag == 0 ? 0 : 1;
            this.remoteId = remoteId == null ? "" : remoteId;
            this.attachmentFile = attachmentFile == null ? "" : attachmentFile;
            this.attachmentFileVersion = attachmentFileVersion == null ? "" : attachmentFileVersion;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("clientId", clientId);
                json.put("familyKeyHash", familyKeyHash);
                json.put("schemaVersion", schemaVersion);
                json.put("cipherVersion", cipherVersion);
                json.put("nonce", nonce);
                json.put("ciphertext", ciphertext);
                json.put("updatedAtClient", updatedAtClient);
                json.put("deletedFlag", deletedFlag);
            } catch (JSONException error) {
                throw new IllegalStateException("Failed to encode encrypted record", error);
            }
            return json;
        }

        public static EncryptedRecord fromJson(JSONObject json) {
            if (json == null) {
                return new EncryptedRecord("", "", 0, 0, "", "", "", 0);
            }
            return new EncryptedRecord(
                    json.optString("clientId", ""),
                    json.optString("familyKeyHash", ""),
                    json.optInt("schemaVersion", 0),
                    json.optInt("cipherVersion", 0),
                    json.optString("nonce", ""),
                    json.optString("ciphertext", ""),
                    json.optString("updatedAtClient", ""),
                    json.optInt("deletedFlag", 0),
                    json.optString("id", ""),
                    json.optString("attachmentFile", ""),
                    json.optString("attachmentFileVersion", "")
            );
        }
    }

    public static final class RecordPushResult {
        public final String clientId;
        public final String remoteRecordId;
        public final boolean ok;
        public final String errorCode;

        private RecordPushResult(String clientId, String remoteRecordId, boolean ok, String errorCode) {
            this.clientId = clientId == null ? "" : clientId;
            this.remoteRecordId = remoteRecordId == null ? "" : remoteRecordId;
            this.ok = ok;
            this.errorCode = errorCode == null ? "" : errorCode;
        }

        public static RecordPushResult ok(String clientId) {
            return ok(clientId, "");
        }

        public static RecordPushResult ok(String clientId, String remoteRecordId) {
            return new RecordPushResult(clientId, remoteRecordId, true, "");
        }

        public static RecordPushResult failed(String clientId, String errorCode) {
            return new RecordPushResult(clientId, "", false, errorCode);
        }
    }

    public static final class PushResult {
        public final List<RecordPushResult> records;
        public final int total;
        public final int pushed;
        public final int failed;

        private PushResult(List<RecordPushResult> records) {
            this.records = records == null ? new ArrayList<>() : new ArrayList<>(records);
            this.total = this.records.size();
            int ok = 0;
            for (RecordPushResult result : this.records) {
                if (result.ok) {
                    ok += 1;
                }
            }
            this.pushed = ok;
            this.failed = total - ok;
        }

        static PushResult failedAll(List<EncryptedRecord> encrypted, String errorCode) {
            List<RecordPushResult> results = new ArrayList<>();
            if (encrypted != null) {
                for (EncryptedRecord record : encrypted) {
                    if (record != null) {
                        results.add(RecordPushResult.failed(record.clientId, errorCode));
                    }
                }
            }
            return new PushResult(results);
        }
    }

    public static final class PullResult {
        public final List<EncryptedRecord> records;
        public final int page;
        public final int totalPages;
        public final int totalItems;
        public final String lastError;

        public PullResult(List<EncryptedRecord> records, int page, int totalPages, int totalItems, String lastError) {
            this.records = records == null ? new ArrayList<>() : new ArrayList<>(records);
            this.page = page;
            this.totalPages = totalPages;
            this.totalItems = totalItems;
            this.lastError = lastError == null ? "" : lastError;
        }

        static PullResult failed(String errorCode) {
            return new PullResult(new ArrayList<EncryptedRecord>(), 0, 0, 0, errorCode);
        }
    }
}
