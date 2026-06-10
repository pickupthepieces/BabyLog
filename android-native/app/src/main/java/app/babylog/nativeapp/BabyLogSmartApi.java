package app.babylog.nativeapp;

public final class BabyLogSmartApi {
    private static final int MAX_ERROR_BODY_CHARS = 480;

    private BabyLogSmartApi() {
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
}
