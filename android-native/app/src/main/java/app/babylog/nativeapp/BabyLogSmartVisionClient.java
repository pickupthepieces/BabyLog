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
    private static final int MAX_ERROR_BODY_CHARS = 480;
    private final UploadImagePreparer uploadImagePreparer;

    public BabyLogSmartVisionClient() {
        this(BabyLogSmartVisionClient::compressImageToTemporaryJpeg);
    }

    public BabyLogSmartVisionClient(UploadImagePreparer uploadImagePreparer) {
        if (uploadImagePreparer == null) {
            throw new IllegalArgumentException("uploadImagePreparer is required");
        }
        this.uploadImagePreparer = uploadImagePreparer;
    }

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

        File uploadImage = prepareImageForUpload(image);
        try {
            JSONObject request = buildChatCompletionsRequest(config.getModel(), imageToDataUrl(uploadImage));
            String response = postJson(resolveChatCompletionsUrl(config.getBaseUrl()), config.getApiKey(), request);
            try {
                return BabyLogSmartInput.fromOpenAiVisionResponse(response);
            } catch (IllegalArgumentException error) {
                throw new IOException("模型返回内容无法解析：" + error.getMessage(), error);
            }
        } finally {
            if (uploadImage != null && !image.equals(uploadImage) && uploadImage.exists()) {
                // Best-effort cleanup; a leftover temp upload image is safe but wastes storage.
                uploadImage.delete();
            }
        }
    }

    public File prepareImageForUpload(File image) throws IOException {
        return uploadImagePreparer.prepare(image);
    }

    public static JSONObject buildChatCompletionsRequest(String model, String imageDataUrl) throws JSONException {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "你是家庭孕期记录助手，只从 B 超单图片中提取可见字段。不要诊断，不要推测。只返回一个 JSON object。"));

        JSONArray userContent = new JSONArray();
        userContent.put(new JSONObject()
                .put("type", "text")
                .put("text", ultrasoundRecognitionPrompt()));
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

    public static String ultrasoundRecognitionPrompt() {
        return "请识别这张 B 超单，只提取胎儿生长指标并返回 JSON object。字段固定为：examDate, bpdMm, hcMm, acMm, flMm, efwGram, warnings, rawText。"
                + "只在报告文字明确出现时填写字段：BPD/双顶径、HC/头围、AC/腹围、FL/股骨长、EFW/估重/胎儿体重。"
                + "不要识别、返回或推断孕周；孕周由用户根据报告手动填写或由预产期计算。"
                + "efwGram 只在明确出现 EFW、估重、胎儿体重或胎重时填写；不要把胎心率 143bpm 当 EFW。"
                + "不要填写 AFI、羊水最大深度、胎心率、胎盘、胎位、宫颈管长度、侧脑室宽度、脐血流等非生长指标；可放入 warnings 或 rawText 供人工核对。"
                + "数值字段只填数字，单位按字段要求换算为 mm 或 g；不确定就省略或放入 warnings。";
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

    public static String formatApiErrorMessage(int code, String response) {
        String body = response == null ? "" : response.trim();
        if (body.isEmpty()) {
            body = "无响应内容";
        }
        body = body
                .replaceAll("sk-[A-Za-z0-9_\\-]{8,}", "sk-***")
                .replaceAll("Bearer\\s+[A-Za-z0-9._\\-]{8,}", "Bearer ***");
        if (body.length() > MAX_ERROR_BODY_CHARS) {
            body = body.substring(0, MAX_ERROR_BODY_CHARS) + "…";
        }
        return "模型 API 返回 " + code + "：" + body;
    }

    private static String imageToDataUrl(File image) throws IOException {
        return "data:image/jpeg;base64," + Base64.encodeToString(readAllBytes(image), Base64.NO_WRAP);
    }

    private static File compressImageToTemporaryJpeg(File image) throws IOException {
        File parent = image.getParentFile();
        File output = File.createTempFile("babylog-vision-", ".jpg",
                parent != null && parent.exists() ? parent : null);
        try {
            BabyLogImageUtils.compressFileToJpeg(image, output);
            return output;
        } catch (IOException error) {
            if (output.exists()) {
                output.delete();
            }
            throw error;
        }
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
            throw new IOException(formatApiErrorMessage(code, response));
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

    public interface UploadImagePreparer {
        File prepare(File source) throws IOException;
    }
}
