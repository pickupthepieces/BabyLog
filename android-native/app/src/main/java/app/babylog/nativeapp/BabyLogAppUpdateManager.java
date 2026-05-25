package app.babylog.nativeapp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class BabyLogAppUpdateManager {
    public static final String DEFAULT_MANIFEST_URL =
            "https://github.com/pickupthepieces/BabyLog/releases/latest/download/babylog-update.json";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final int BUFFER_SIZE = 32 * 1024;

    private BabyLogAppUpdateManager() {
    }

    public static UpdateInfo fetchLatest(String manifestUrl, int currentVersionCode) throws IOException {
        String body = getText(manifestUrl);
        try {
            return parseManifest(body, currentVersionCode);
        } catch (IllegalArgumentException error) {
            throw new IOException(error.getMessage(), error);
        }
    }

    public static UpdateInfo parseManifest(String json, int currentVersionCode) {
        try {
            JSONObject object = new JSONObject(json);
            int versionCode = object.getInt("versionCode");
            if (versionCode <= currentVersionCode) {
                return null;
            }
            String versionName = object.optString("versionName", String.valueOf(versionCode)).trim();
            String apkUrl = object.getString("apkUrl").trim();
            String sha256 = normalizeSha256(object.getString("sha256"));
            String notes = object.optString("notes", "").trim();
            if (!apkUrl.startsWith("https://")) {
                throw new IllegalArgumentException("更新 APK 必须使用 HTTPS 地址");
            }
            if (versionName.isEmpty()) {
                throw new IllegalArgumentException("更新版本名不能为空");
            }
            return new UpdateInfo(versionCode, versionName, apkUrl, sha256, notes);
        } catch (JSONException error) {
            throw new IllegalArgumentException("更新清单格式无效", error);
        }
    }

    public static File downloadApk(UpdateInfo update, File targetFile) throws IOException {
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建更新下载目录");
        }
        File tempFile = new File(targetFile.getPath() + ".download");
        HttpURLConnection connection = openConnection(update.apkUrl);
        try (InputStream in = new BufferedInputStream(connection.getInputStream());
             FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            connection.disconnect();
        }
        if (!verifySha256(tempFile, update.sha256)) {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            throw new IOException("更新包 SHA-256 校验失败");
        }
        if (targetFile.exists() && !targetFile.delete()) {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            throw new IOException("无法替换旧更新包");
        }
        if (!tempFile.renameTo(targetFile)) {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
            throw new IOException("无法保存更新包");
        }
        return targetFile;
    }

    public static boolean verifySha256(File file, String expectedSha256) throws IOException {
        return sha256Hex(file).equals(normalizeSha256(expectedSha256));
    }

    public static String sha256Hex(File file) throws IOException {
        MessageDigest digest = sha256Digest();
        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return toHex(digest.digest());
    }

    private static String getText(String url) throws IOException {
        HttpURLConnection connection = openConnection(url);
        try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
            byte[] buffer = new byte[BUFFER_SIZE];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString("UTF-8");
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection openConnection(String url) throws IOException {
        if (!url.startsWith("https://")) {
            throw new IOException("更新地址必须使用 HTTPS");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/json, application/octet-stream");
        return connection;
    }

    private static String normalizeSha256(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.US);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("更新包 SHA-256 必须是 64 位 hex");
        }
        return normalized;
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format(Locale.US, "%02x", value & 0xff));
        }
        return builder.toString();
    }

    public static final class UpdateInfo {
        public final int versionCode;
        public final String versionName;
        public final String apkUrl;
        public final String sha256;
        public final String notes;

        UpdateInfo(int versionCode, String versionName, String apkUrl, String sha256, String notes) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.apkUrl = apkUrl;
            this.sha256 = sha256;
            this.notes = notes;
        }
    }
}
