package app.babylog.nativeapp;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class BabyLogSmartVisionClient {
    private static final int CONNECT_TIMEOUT_MS = 20_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    public BabyLogSmartInput.UltrasoundOcrCandidate recognizeUltrasoundImage(
            File image,
            BabyLogSmartConfigStore.Config config
    ) throws IOException, JSONException {
        if (image == null || !image.exists() || image.length() <= 0) {
            throw new IOException("请先拍照或选择 B 超单图片");
        }
        if (config == null || !config.isConfigured()) {
            throw new IOException("请先配置多模态模型 API");
        }

        JSONObject request = buildChatCompletionsRequest(config.getModel(), imageToDataUrl(image));
        String response = postJson(resolveChatCompletionsUrl(config.getBaseUrl()), config.getApiKey(), request);
        try {
            return BabyLogSmartInput.fromOpenAiVisionResponse(response);
        } catch (IllegalArgumentException error) {
            throw new IOException("模型返回内容无法解析：" + error.getMessage(), error);
        }
    }

    public static JSONObject buildChatCompletionsRequest(String model, String imageDataUrl) throws JSONException {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "你是家庭孕期记录助手，只从 B 超单图片中提取可见字段。不要诊断，不要推测。只返回一个 JSON object。"));

        JSONArray userContent = new JSONArray();
        userContent.put(new JSONObject()
                .put("type", "text")
                .put("text", "请识别这张 B 超单，返回 JSON，字段固定为：examDate, gestationalAge, bpdMm, hcMm, acMm, flMm, efwGram, afiCm, deepestPocketCm, placentaLocation, placentaGrade, fetalPresentation, umbilicalSd, umbilicalPi, umbilicalRi, warnings, rawText。数值字段只填数字，不确定就省略或放入 warnings。"));
        userContent.put(new JSONObject()
                .put("type", "image_url")
                .put("image_url", new JSONObject().put("url", imageDataUrl)));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", userContent));

        return new JSONObject()
                .put("model", model)
                .put("temperature", 0)
                .put("messages", messages)
                .put("response_format", new JSONObject().put("type", "json_object"));
    }

    public static String resolveChatCompletionsUrl(String baseUrl) {
        String normalized = BabyLogFormatters.normalizeBackendBaseUrl(baseUrl);
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/chat/completions";
        }
        return normalized + "/v1/chat/completions";
    }

    private static String imageToDataUrl(File image) throws IOException {
        return "data:image/jpeg;base64," + Base64.encodeToString(readAllBytes(image), Base64.NO_WRAP);
    }

    private static byte[] readAllBytes(File image) throws IOException {
        try (InputStream in = new FileInputStream(image);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static String postJson(String url, String apiKey, JSONObject body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode((long) payload.length);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(payload);
        }

        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String response = readResponse(stream);
        if (code < 200 || code >= 300) {
            throw new IOException("模型 API 返回 " + code + "：" + response);
        }
        return response;
    }

    private static String readResponse(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}
