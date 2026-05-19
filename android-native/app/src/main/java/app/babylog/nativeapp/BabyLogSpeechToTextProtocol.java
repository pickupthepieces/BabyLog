package app.babylog.nativeapp;

import java.util.Map;

public final class BabyLogSpeechToTextProtocol {
    public static final String DEFAULT_MODEL = "paraformer-realtime-v2";
    public static final String DEFAULT_AUDIO_FORMAT = "pcm";
    public static final int DEFAULT_SAMPLE_RATE = 16_000;

    private BabyLogSpeechToTextProtocol() {
    }

    public static String buildRunTaskJson(String taskId, String model, String audioFormat, int sampleRate) {
        String normalizedModel = isBlank(model) ? DEFAULT_MODEL : model.trim();
        String normalizedFormat = isBlank(audioFormat) ? DEFAULT_AUDIO_FORMAT : audioFormat.trim();
        int normalizedSampleRate = sampleRate > 0 ? sampleRate : DEFAULT_SAMPLE_RATE;
        return "{"
                + "\"header\":{"
                + "\"action\":\"run-task\","
                + "\"task_id\":\"" + escape(taskId) + "\","
                + "\"streaming\":\"duplex\""
                + "},"
                + "\"payload\":{"
                + "\"task_group\":\"audio\","
                + "\"task\":\"asr\","
                + "\"function\":\"recognition\","
                + "\"model\":\"" + escape(normalizedModel) + "\","
                + "\"parameters\":{"
                + "\"format\":\"" + escape(normalizedFormat) + "\","
                + "\"sample_rate\":" + normalizedSampleRate + ","
                + "\"language_hints\":[\"zh\"],"
                + "\"enable_punctuation_prediction\":true,"
                + "\"enable_inverse_text_normalization\":true"
                + "},"
                + "\"input\":{}"
                + "}"
                + "}";
    }

    public static String buildFinishTaskJson(String taskId) {
        return "{"
                + "\"header\":{"
                + "\"action\":\"finish-task\","
                + "\"task_id\":\"" + escape(taskId) + "\","
                + "\"streaming\":\"duplex\""
                + "},"
                + "\"payload\":{\"input\":{}}"
                + "}";
    }

    public static ServerEvent parseServerEvent(String json) {
        Map<String, Object> object = BabyLogSmartInput.parseJsonObject(json, "paraformer server event");
        Map<String, Object> header = asObject(object.get("header"));
        Map<String, Object> payload = asObject(object.get("payload"));
        String event = cleanText(readNested(header, "event"));
        String errorMessage = cleanText(readNested(header, "error_message"));
        if (errorMessage == null) {
            errorMessage = cleanText(readNested(payload, "message"));
        }
        if (errorMessage == null) {
            errorMessage = cleanText(readNested(payload, "error_message"));
        }
        String text = cleanText(
                firstNonNull(
                        readNested(payload, "output", "sentence", "text"),
                        readNested(payload, "output", "text"),
                        readNested(payload, "sentence", "text"),
                        readNested(payload, "text")
                )
        );
        Boolean sentenceEnd = boolValue(
                firstNonNull(
                        readNested(payload, "output", "sentence", "sentence_end"),
                        readNested(payload, "sentence", "sentence_end")
                )
        );
        boolean finished = "task-finished".equals(event);
        boolean failed = "task-failed".equals(event);
        return new ServerEvent(event, text, sentenceEnd != null && sentenceEnd, finished, failed, errorMessage);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private static Object readNested(Map<String, Object> object, String... path) {
        Object current = object;
        for (String key : path) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(key);
        }
        return current;
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Boolean boolValue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        return null;
    }

    private static String cleanText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(c);
            }
        }
        return builder.toString();
    }

    public static final class ServerEvent {
        public final String event;
        public final String text;
        public final boolean sentenceEnd;
        public final boolean taskFinished;
        public final boolean taskFailed;
        public final String errorMessage;

        public ServerEvent(
                String event,
                String text,
                boolean sentenceEnd,
                boolean taskFinished,
                boolean taskFailed,
                String errorMessage
        ) {
            this.event = event == null ? "" : event;
            this.text = text == null ? "" : text;
            this.sentenceEnd = sentenceEnd;
            this.taskFinished = taskFinished;
            this.taskFailed = taskFailed;
            this.errorMessage = errorMessage == null ? "" : errorMessage;
        }
    }
}
