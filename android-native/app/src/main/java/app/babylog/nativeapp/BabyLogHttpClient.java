package app.babylog.nativeapp;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class BabyLogHttpClient {
    public static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private BabyLogHttpClient() {
    }

    public static OkHttpClient create(int connectTimeoutMs, int readTimeoutMs) {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    public static Request.Builder requestBuilder(String url) throws IOException {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException error) {
            throw new IOException("INVALID_URL", error);
        }
        String scheme = uri.getScheme();
        if (scheme == null
                || uri.getHost() == null
                || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IOException("INVALID_URL");
        }
        try {
            return new Request.Builder().url(url);
        } catch (IllegalArgumentException error) {
            throw new IOException("INVALID_URL", error);
        }
    }

    public static HttpResponse executeText(OkHttpClient client, Request request, int maxChars) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            return new HttpResponse(response.code(), readText(body == null ? null : body.byteStream(), maxChars));
        }
    }

    public static byte[] executeBytes(OkHttpClient client, Request request, int maxBytes, int maxErrorChars) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                String errorBody = readText(body == null ? null : body.byteStream(), maxErrorChars);
                throw new IOException(httpError(response.code(), errorBody));
            }
            return readBytes(body == null ? null : body.byteStream(), maxBytes);
        }
    }

    public static void downloadToFile(OkHttpClient client, Request request, File targetFile, int maxErrorChars) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                String errorBody = readText(body == null ? null : body.byteStream(), maxErrorChars);
                throw new IOException(httpError(response.code(), errorBody));
            }
            writeBodyToFile(body, targetFile);
        }
    }

    public static String httpError(int statusCode, String body) {
        String safeBody = body == null ? "" : body;
        return "HTTP_" + statusCode + (safeBody.isEmpty() ? "" : ":" + safeBody);
    }

    private static String readText(InputStream stream, int maxChars) throws IOException {
        if (stream == null) {
            return "";
        }
        int limit = Math.max(0, maxChars);
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[512];
            int read;
            while ((read = reader.read(buffer)) != -1 && builder.length() < limit) {
                int remaining = limit - builder.length();
                builder.append(buffer, 0, Math.min(read, remaining));
            }
        }
        return builder.toString();
    }

    private static byte[] readBytes(InputStream stream, int maxBytes) throws IOException {
        if (stream == null) {
            return new byte[0];
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = stream.read(buffer)) != -1) {
            total += read;
            if (maxBytes > 0 && total > maxBytes) {
                throw new IOException("RESPONSE_TOO_LARGE");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static void writeBodyToFile(ResponseBody body, File targetFile) throws IOException {
        if (body == null) {
            throw new IOException("EMPTY_RESPONSE");
        }
        try (InputStream input = new BufferedInputStream(body.byteStream());
             FileOutputStream output = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
    }

    public static final class HttpResponse {
        public final int statusCode;
        public final String body;

        HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
        }

        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
