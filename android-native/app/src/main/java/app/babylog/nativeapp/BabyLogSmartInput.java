package app.babylog.nativeapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BabyLogSmartInput {
    private static final Pattern JSON_CODE_BLOCK = Pattern.compile(
            "```(?:json)?\\s*([\\s\\S]*?)\\s*```",
            Pattern.CASE_INSENSITIVE
    );

    private BabyLogSmartInput() {
    }

    public static UltrasoundOcrCandidate fromOpenAiVisionResponse(String responseJson) {
        Map<String, Object> response = requireObject(JsonParser.parse(responseJson), "OpenAI-compatible response");
        Object content = null;

        Object choicesValue = response.get("choices");
        if (choicesValue instanceof List && !((List<?>) choicesValue).isEmpty()) {
            Map<String, Object> choice = asObject(((List<?>) choicesValue).get(0));
            Map<String, Object> message = choice == null ? null : asObject(choice.get("message"));
            if (message != null && message.containsKey("content")) {
                content = message.get("content");
            }
        }

        if (content == null) {
            Map<String, Object> message = asObject(response.get("message"));
            if (message != null && message.containsKey("content")) {
                content = message.get("content");
            }
        }

        if (content == null && response.containsKey("content")) {
            content = response.get("content");
        }

        if (content == null) {
            throw new IllegalArgumentException("OpenAI-compatible response missing message.content");
        }
        return fromMessageContent(contentToString(content));
    }

    public static UltrasoundOcrCandidate fromMessageContent(String content) {
        return fromCandidateJson(extractJsonFromMessageContent(content));
    }

    public static String extractJsonFromMessageContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException("message.content is null");
        }
        String trimmed = content.trim();
        Matcher matcher = JSON_CODE_BLOCK.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return trimmed;
    }

    public static UltrasoundOcrCandidate fromCandidateJson(String json) {
        Map<String, Object> object = requireObject(JsonParser.parse(json), "ultrasound OCR candidate");
        return new UltrasoundOcrCandidate(
                stringField(object, "examDate"),
                stringField(object, "gestationalAge"),
                numberField(object, "bpdMm"),
                numberField(object, "hcMm"),
                numberField(object, "acMm"),
                numberField(object, "flMm"),
                numberField(object, "efwGram"),
                numberField(object, "afiCm"),
                numberField(object, "deepestPocketCm"),
                stringField(object, "placentaLocation"),
                stringField(object, "placentaGrade"),
                stringField(object, "fetalPresentation"),
                numberField(object, "umbilicalSd"),
                numberField(object, "umbilicalPi"),
                numberField(object, "umbilicalRi"),
                warnings(object),
                optionalString(object, "rawText")
        );
    }

    private static FieldCandidate<String> stringField(Map<String, Object> object, String name) {
        String value = optionalString(object, name);
        return new FieldCandidate<>(value, value);
    }

    private static FieldCandidate<Double> numberField(Map<String, Object> object, String name) {
        String rawValue = optionalString(object, name);
        return new FieldCandidate<>(BabyLogFormatters.parseOptionalNumber(rawValue), rawValue);
    }

    private static String optionalString(Map<String, Object> object, String name) {
        if (!object.containsKey(name)) {
            return null;
        }
        Object value = object.get(name);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static List<String> warnings(Map<String, Object> object) {
        if (!object.containsKey("warnings")) {
            return Collections.emptyList();
        }
        Object value = object.get("warnings");
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            List<?> array = (List<?>) value;
            for (Object item : array) {
                addWarning(result, item);
            }
        } else {
            addWarning(result, value);
        }
        return Collections.unmodifiableList(result);
    }

    private static void addWarning(List<String> result, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (!text.isEmpty()) {
            result.add(text);
        }
    }

    private static String contentToString(Object content) {
        if (content instanceof List) {
            List<?> array = (List<?>) content;
            StringBuilder builder = new StringBuilder();
            for (Object item : array) {
                if (item instanceof Map) {
                    Map<String, Object> object = asObject(item);
                    String text = optionalString(object, "text");
                    if (text != null && !text.isEmpty()) {
                        builder.append(text);
                    }
                } else if (item != null) {
                    builder.append(String.valueOf(item));
                }
            }
            return builder.toString();
        }
        return String.valueOf(content);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private static Map<String, Object> requireObject(Object value, String label) {
        Map<String, Object> object = asObject(value);
        if (object == null) {
            throw new IllegalArgumentException(label + " must be a JSON object");
        }
        return object;
    }

    public static final class FieldCandidate<T> {
        public final T value;
        public final String rawValue;

        public FieldCandidate(T value, String rawValue) {
            this.value = value;
            this.rawValue = rawValue;
        }
    }

    public static final class UltrasoundOcrCandidate {
        public final FieldCandidate<String> examDate;
        public final FieldCandidate<String> gestationalAge;
        public final FieldCandidate<Double> bpdMm;
        public final FieldCandidate<Double> hcMm;
        public final FieldCandidate<Double> acMm;
        public final FieldCandidate<Double> flMm;
        public final FieldCandidate<Double> efwGram;
        public final FieldCandidate<Double> afiCm;
        public final FieldCandidate<Double> deepestPocketCm;
        public final FieldCandidate<String> placentaLocation;
        public final FieldCandidate<String> placentaGrade;
        public final FieldCandidate<String> fetalPresentation;
        public final FieldCandidate<Double> umbilicalSd;
        public final FieldCandidate<Double> umbilicalPi;
        public final FieldCandidate<Double> umbilicalRi;
        public final List<String> warnings;
        public final String rawText;

        public UltrasoundOcrCandidate(
                FieldCandidate<String> examDate,
                FieldCandidate<String> gestationalAge,
                FieldCandidate<Double> bpdMm,
                FieldCandidate<Double> hcMm,
                FieldCandidate<Double> acMm,
                FieldCandidate<Double> flMm,
                FieldCandidate<Double> efwGram,
                FieldCandidate<Double> afiCm,
                FieldCandidate<Double> deepestPocketCm,
                FieldCandidate<String> placentaLocation,
                FieldCandidate<String> placentaGrade,
                FieldCandidate<String> fetalPresentation,
                FieldCandidate<Double> umbilicalSd,
                FieldCandidate<Double> umbilicalPi,
                FieldCandidate<Double> umbilicalRi,
                List<String> warnings,
                String rawText
        ) {
            this.examDate = examDate;
            this.gestationalAge = gestationalAge;
            this.bpdMm = bpdMm;
            this.hcMm = hcMm;
            this.acMm = acMm;
            this.flMm = flMm;
            this.efwGram = efwGram;
            this.afiCm = afiCm;
            this.deepestPocketCm = deepestPocketCm;
            this.placentaLocation = placentaLocation;
            this.placentaGrade = placentaGrade;
            this.fetalPresentation = fetalPresentation;
            this.umbilicalSd = umbilicalSd;
            this.umbilicalPi = umbilicalPi;
            this.umbilicalRi = umbilicalRi;
            this.warnings = warnings == null ? Collections.emptyList() : Collections.unmodifiableList(warnings);
            this.rawText = rawText;
        }

    }

    private static final class JsonParser {
        private final String text;
        private int index;

        private JsonParser(String text) {
            this.text = text == null ? "" : text;
        }

        static Object parse(String text) {
            JsonParser parser = new JsonParser(text);
            Object value = parser.readValue();
            parser.skipWhitespace();
            if (!parser.isAtEnd()) {
                throw parser.error("Unexpected trailing JSON");
            }
            return value;
        }

        private Object readValue() {
            skipWhitespace();
            if (isAtEnd()) {
                throw error("Unexpected end of JSON");
            }
            char next = text.charAt(index);
            if (next == '{') return readObject();
            if (next == '[') return readArray();
            if (next == '"') return readString();
            if (next == '-' || Character.isDigit(next)) return readNumberText();
            if (matches("true")) return Boolean.TRUE;
            if (matches("false")) return Boolean.FALSE;
            if (matches("null")) return null;
            throw error("Unexpected JSON value");
        }

        private Map<String, Object> readObject() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return object;
            }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                object.put(key, readValue());
                skipWhitespace();
                if (consume('}')) {
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> readArray() {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                return array;
            }
            while (true) {
                array.add(readValue());
                skipWhitespace();
                if (consume(']')) {
                    return array;
                }
                expect(',');
            }
        }

        private String readString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (!isAtEnd()) {
                char current = text.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current != '\\') {
                    builder.append(current);
                    continue;
                }
                if (isAtEnd()) {
                    throw error("Unterminated escape sequence");
                }
                char escape = text.charAt(index++);
                if (escape == '"' || escape == '\\' || escape == '/') builder.append(escape);
                else if (escape == 'b') builder.append('\b');
                else if (escape == 'f') builder.append('\f');
                else if (escape == 'n') builder.append('\n');
                else if (escape == 'r') builder.append('\r');
                else if (escape == 't') builder.append('\t');
                else if (escape == 'u') builder.append(readUnicodeEscape());
                else throw error("Invalid escape sequence");
            }
            throw error("Unterminated string");
        }

        private char readUnicodeEscape() {
            if (index + 4 > text.length()) {
                throw error("Invalid unicode escape");
            }
            String hex = text.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException ignored) {
                throw error("Invalid unicode escape");
            }
        }

        private String readNumberText() {
            int start = index;
            if (consume('-')) {
                if (isAtEnd()) throw error("Invalid number");
            }
            if (consume('0')) {
                // Leading zero is only valid as the entire integer part.
            } else {
                readDigits();
            }
            if (consume('.')) {
                readDigits();
            }
            if (!isAtEnd() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
                index++;
                if (!isAtEnd() && (text.charAt(index) == '+' || text.charAt(index) == '-')) {
                    index++;
                }
                readDigits();
            }
            return text.substring(start, index);
        }

        private void readDigits() {
            int start = index;
            while (!isAtEnd() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw error("Invalid number");
            }
        }

        private boolean matches(String token) {
            if (!text.startsWith(token, index)) {
                return false;
            }
            index += token.length();
            return true;
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private boolean consume(char expected) {
            if (!isAtEnd() && text.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'");
            }
        }

        private boolean isAtEnd() {
            return index >= text.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at offset " + index);
        }
    }
}
