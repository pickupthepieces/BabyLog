package app.babylog.nativeapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class BabyLogRemoteSyncClient {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;
    private static final int MAX_BODY_CHARS = 4096;

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

    private HttpResponse get(String url, String familyKey) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty(BabyLogSyncProtocol.HEADER_CLIENT_SCHEMA, String.valueOf(BabyLogDomain.SCHEMA_VERSION));
        if (BabyLogSyncProtocol.hasFamilyKey(familyKey)) {
            connection.setRequestProperty(BabyLogSyncProtocol.HEADER_FAMILY_KEY, BabyLogSyncProtocol.hashFamilyKeyForLookup(familyKey));
        }
        int statusCode = connection.getResponseCode();
        String body = readLimited(statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream());
        connection.disconnect();
        return new HttpResponse(statusCode, body);
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
}
