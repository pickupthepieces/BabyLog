package app.babylog.nativeapp;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class BabyLogRemoteSyncClient {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int MAX_BODY_CHARS = 1024 * 1024;
    private static final int MAX_FILE_BODY_BYTES = 6 * 1024 * 1024;

    private final OkHttpClient httpClient = BabyLogHttpClient.create(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);

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
        BabyLogHttpClient.HttpResponse response = get(healthUrl(backendBaseUrl), familyKey);
        if (response.statusCode >= 200 && response.statusCode < 300) {
            return ConnectionResult.ok("服务器可达");
        }
        return ConnectionResult.failed("服务器健康检查失败：" + response.statusCode);
    }

    public ConnectionResult checkFamily(String backendBaseUrl, String familyKey) throws IOException {
        String normalizedKey = BabyLogSyncProtocol.normalizeFamilyKeyForTransport(familyKey);
        BabyLogHttpClient.HttpResponse response = get(familyLookupUrl(backendBaseUrl, normalizedKey), normalizedKey);
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
        BabyLogHttpClient.HttpResponse response = get(encryptedRecordsPullUrl(normalizedUrl, familyKey, sinceCursor, page, perPage), familyKey);
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
        BabyLogHttpClient.HttpResponse response = request("PATCH", url, familyKey, MediaType.get("multipart/form-data; boundary=" + boundary), body);
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
        BabyLogHttpClient.HttpResponse response = request("POST", encryptedRecordsUrl(backendBaseUrl), familyKey, record.toJson().toString());
        if (response.statusCode >= 200 && response.statusCode < 300) {
            return RecordPushResult.ok(record.clientId, remoteIdFromBody(response.body));
        }
        if (response.statusCode == 409) {
            String remoteId = findRemoteRecordId(backendBaseUrl, familyKey, record.clientId);
            if (!remoteId.isEmpty()) {
                BabyLogHttpClient.HttpResponse patched = request(
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
        BabyLogHttpClient.HttpResponse response = get(url, familyKey);
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

    private BabyLogHttpClient.HttpResponse get(String url, String familyKey) throws IOException {
        return request("GET", url, familyKey, null);
    }

    private byte[] getBytes(String url, String familyKey) throws IOException {
        Request request = requestBuilder(url, familyKey).get().build();
        try {
            return BabyLogHttpClient.executeBytes(httpClient, request, MAX_FILE_BODY_BYTES, MAX_BODY_CHARS);
        } catch (IOException error) {
            if ("RESPONSE_TOO_LARGE".equals(error.getMessage())) {
                throw new IOException("ATTACHMENT_FILE_TOO_LARGE", error);
            }
            throw error;
        }
    }

    private BabyLogHttpClient.HttpResponse request(String method, String url, String familyKey, String body) throws IOException {
        return request(
                method,
                url,
                familyKey,
                BabyLogHttpClient.JSON_MEDIA_TYPE,
                body == null ? null : body.getBytes(StandardCharsets.UTF_8)
        );
    }

    private BabyLogHttpClient.HttpResponse request(String method, String url, String familyKey, MediaType contentType, byte[] body) throws IOException {
        RequestBody requestBody = body == null
                ? null
                : RequestBody.create(body, contentType);
        Request request = requestBuilder(url, familyKey)
                .method(method, requestBody)
                .build();
        return BabyLogHttpClient.executeText(httpClient, request, MAX_BODY_CHARS);
    }

    private Request.Builder requestBuilder(String url, String familyKey) throws IOException {
        Request.Builder builder = BabyLogHttpClient.requestBuilder(url)
                .header(BabyLogSyncProtocol.HEADER_CLIENT_SCHEMA, String.valueOf(BabyLogDomain.SCHEMA_VERSION));
        if (BabyLogSyncProtocol.hasFamilyKey(familyKey)) {
            builder.header(BabyLogSyncProtocol.HEADER_FAMILY_KEY, BabyLogSyncProtocol.hashFamilyKeyForLookup(familyKey));
        }
        return builder;
    }

    private String remoteIdFromBody(String body) {
        try {
            return new JSONObject(body == null ? "{}" : body).optString("id", "");
        } catch (JSONException ignored) {
            return "";
        }
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
