package app.babylog.nativeapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class BabyLogRemoteSyncClient {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int MAX_BODY_CHARS = 1024 * 1024;

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
            return RecordPushResult.ok(record.clientId);
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
                    return RecordPushResult.ok(record.clientId);
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
            this.clientId = clientId == null ? "" : clientId;
            this.familyKeyHash = familyKeyHash == null ? "" : familyKeyHash;
            this.schemaVersion = schemaVersion;
            this.cipherVersion = cipherVersion;
            this.nonce = nonce == null ? "" : nonce;
            this.ciphertext = ciphertext == null ? "" : ciphertext;
            this.updatedAtClient = updatedAtClient == null ? "" : updatedAtClient;
            this.deletedFlag = deletedFlag == 0 ? 0 : 1;
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
                    json.optInt("deletedFlag", 0)
            );
        }
    }

    public static final class RecordPushResult {
        public final String clientId;
        public final boolean ok;
        public final String errorCode;

        private RecordPushResult(String clientId, boolean ok, String errorCode) {
            this.clientId = clientId == null ? "" : clientId;
            this.ok = ok;
            this.errorCode = errorCode == null ? "" : errorCode;
        }

        public static RecordPushResult ok(String clientId) {
            return new RecordPushResult(clientId, true, "");
        }

        public static RecordPushResult failed(String clientId, String errorCode) {
            return new RecordPushResult(clientId, false, errorCode);
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
