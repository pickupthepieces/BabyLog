package app.babylog.nativeapp;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BabyLogSmartTextClient {
    private static final int CONNECT_TIMEOUT_MS = 20_000;
    private static final int READ_TIMEOUT_MS = 45_000;
    private static final int MAX_RESPONSE_CHARS = 1024 * 1024;
    private static final String SPEECH_NUMBER_CORRECTION_INSTRUCTION =
            "用户原文可能来自语音转写，常见错误是中文数字+单位被错误切分，"
                    + "例如“50 2000克”实为“52千克”、“1 5000毫升”实为“150毫升”、“3 8度5”实为“38.5度”。"
                    + "请结合孕期/育儿常识恢复原意后再填入 values，"
                    + "并在 warnings 中注明“语音数字已纠正：原文 → 纠正值，请人工核对”。"
                    + "无法判断时保留原文并在 warnings 说明。";
    private static final String UNIT_CONVERSION_INSTRUCTION =
            "单位换算：数值字段只填数字，按字段名标注的单位换算后填写；"
                    + "例如斤→kg、千克→kg、毫升→mL、克→g。发生单位换算时在 warnings 说明“单位已换算：原文 → 候选值，请人工核对”。"
                    + "无法确认单位或量纲时不要猜测，保留原文并在 warnings 说明。";
    private static final String MULTI_EVENT_INSTRUCTION =
            "多事件处理：如果一段原文包含多个事件，只选择信息最完整、最适合当前表单或记录类型的一个事件生成 values，"
                    + "并在 warnings 提示其余事件需要另建记录。";
    private static final String FORM_VALUE_NORMALIZATION_INSTRUCTION =
            "字段值规范：宝宝记录的 occurredDate 使用 yyyy-MM-dd，occurredTime 使用 HH:mm；喂养 primary 优先填 母乳 / 奶瓶 / 辅食，"
                    + "母乳侧别 tertiary 优先填 L / R / BOTH，辅食 tertiary 填食材或内容；"
                    + "尿布 primary 优先填 尿 / 便 / 混合，颜色性状 tertiary 可填 黄色软便、绿色偏稀等表单常用值；"
                    + "体温测量方式优先填 腋温 / 耳温 / 额温 / 口温。"
                    + "无法映射到表单常用值时保留原文并在 warnings 提醒人工核对。";

    private final OkHttpClient httpClient = BabyLogHttpClient.create(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);

    public SmartEntryCandidate classifyEntry(
            String stage,
            Map<String, Map<String, String>> forms,
            String rawText,
            BabyLogSmartConfigStore.Config config
    ) throws IOException, JSONException {
        if (config == null || !config.isConfigured()) {
            throw new IOException("请先配置大模型 API");
        }
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IOException("请先输入要解析的内容");
        }
        if (forms == null || forms.isEmpty()) {
            throw new IOException("当前阶段没有可智能录入的表单");
        }

        JSONObject request = buildSmartEntryRequest(
                config.resolveTextModel(),
                stage,
                forms,
                rawText,
                BabyLogFormatters.todayDateInput());
        String response = postJson(BabyLogSmartApi.resolveChatCompletionsUrl(config.getBaseUrl()), config.getApiKey(), request);
        try {
            return parseSmartEntryResponse(response, forms, rawText);
        } catch (IllegalArgumentException error) {
            throw new IOException("模型返回内容无法解析：" + error.getMessage(), error);
        }
    }

    public SmartFillCandidate fillForm(
            String formName,
            String stage,
            Map<String, String> fields,
            String rawText,
            BabyLogSmartConfigStore.Config config
    ) throws IOException, JSONException {
        if (config == null || !config.isConfigured()) {
            throw new IOException("请先配置大模型 API");
        }
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IOException("请先输入要解析的内容");
        }
        if (fields == null || fields.isEmpty()) {
            throw new IOException("当前表单没有可填写字段");
        }

        JSONObject request = buildSmartFillRequest(
                config.resolveTextModel(),
                formName,
                stage,
                fields,
                rawText,
                BabyLogFormatters.todayDateInput());
        String response = postJson(BabyLogSmartApi.resolveChatCompletionsUrl(config.getBaseUrl()), config.getApiKey(), request);
        try {
            return parseSmartFillResponse(response, fields.keySet(), rawText);
        } catch (IllegalArgumentException error) {
            throw new IOException("模型返回内容无法解析：" + error.getMessage(), error);
        }
    }

    public String polishVisitSummary(
            String markdown,
            BabyLogSmartConfigStore.Config config
    ) throws IOException, JSONException {
        if (config == null || !config.isConfigured()) {
            throw new IOException("请先配置大模型 API");
        }
        if (markdown == null || markdown.trim().isEmpty()) {
            throw new IOException("没有可润色的汇总文本");
        }

        JSONObject request = buildVisitSummaryPolishRequest(config.resolveTextModel(), markdown);
        String response = postJson(BabyLogSmartApi.resolveChatCompletionsUrl(config.getBaseUrl()), config.getApiKey(), request);
        try {
            return parseVisitSummaryPolishResponse(response);
        } catch (IllegalArgumentException error) {
            throw new IOException("模型返回内容无法解析：" + error.getMessage(), error);
        }
    }

    public static JSONObject buildSmartFillRequest(
            String model,
            String formName,
            String stage,
            Map<String, String> fields,
            String rawText
    ) throws JSONException {
        return buildSmartFillRequest(model, formName, stage, fields, rawText, BabyLogFormatters.todayDateInput());
    }

    public static JSONObject buildSmartFillRequest(
            String model,
            String formName,
            String stage,
            Map<String, String> fields,
            String rawText,
            String todayDate
    ) throws JSONException {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "你是栗记家庭记录表单助手。你只把用户主动提交的文字整理成候选字段，不诊断、不建议治疗、不自动保存。必须只返回 JSON object。"));

        JSONObject fieldObject = new JSONObject();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            fieldObject.put(entry.getKey(), entry.getValue());
        }

        String prompt = "当前阶段：" + nullToEmpty(stage)
                + "\n今天日期：" + normalizeTodayDate(todayDate)
                + "\n表单：" + nullToEmpty(formName)
                + "\n可填写字段 JSON：" + fieldObject
                + "\n用户原文：\n" + rawText
                + "\n\n" + smartTextSharedInstructions(todayDate)
                + "\n\n请返回 JSON object："
                + "{\"values\":{\"字段key\":\"候选值\"},\"warnings\":[\"需要人工核对的点\"],\"rawText\":\"原文，可省略\"}。"
                + "只能填写可填写字段中的 key；没有把握就不要填。"
                + "日期规范为 yyyy-MM-dd；时间规范为 HH:mm。"
                + "不要添加不存在的字段，不要输出 Markdown。";

        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", prompt));

        return new JSONObject()
                .put("model", model)
                .put("temperature", 0)
                .put("messages", messages)
                .put("response_format", new JSONObject().put("type", "json_object"));
    }

    public static JSONObject buildSmartEntryRequest(
            String model,
            String stage,
            Map<String, Map<String, String>> forms,
            String rawText
    ) throws JSONException {
        return buildSmartEntryRequest(model, stage, forms, rawText, BabyLogFormatters.todayDateInput());
    }

    public static JSONObject buildSmartEntryRequest(
            String model,
            String stage,
            Map<String, Map<String, String>> forms,
            String rawText,
            String todayDate
    ) throws JSONException {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "你是栗记家庭记录智能录入助手。你只把用户主动提交的文字分类成一个记录类型并整理候选字段，不诊断、不建议治疗、不自动保存。必须只返回 JSON object。"));

        JSONObject formObject = new JSONObject();
        for (Map.Entry<String, Map<String, String>> formEntry : forms.entrySet()) {
            JSONObject fieldObject = new JSONObject();
            Map<String, String> fields = formEntry.getValue();
            if (fields != null) {
                for (Map.Entry<String, String> fieldEntry : fields.entrySet()) {
                    fieldObject.put(fieldEntry.getKey(), fieldEntry.getValue());
                }
            }
            formObject.put(formEntry.getKey(), fieldObject);
        }

        String prompt = "当前阶段：" + nullToEmpty(stage)
                + "\n今天日期：" + normalizeTodayDate(todayDate)
                + "\n可选择记录类型与字段 JSON：" + formObject
                + "\n用户原文：\n" + rawText
                + "\n\n" + smartTextSharedInstructions(todayDate)
                + "\n\nfew-shot 示例：用户原文“体重50 2000克”应返回 "
                + "{\"eventType\":\"maternal_metric\",\"values\":{\"weightKg\":\"52\"},"
                + "\"warnings\":[\"语音数字已纠正：50 2000克 → 52千克，请人工核对\"],\"rawText\":\"体重50 2000克\"}。"
                + "\n\n请只返回 JSON object："
                + "{\"eventType\":\"记录类型key\",\"values\":{\"字段key\":\"候选值\"},\"warnings\":[\"需要人工核对的点\"],\"rawText\":\"原文，可省略\"}。"
                + "eventType 必须从可选择记录类型中选一个；如果无法判断，eventType 置空并在 warnings 说明。"
                + "values 只能填写所选 eventType 下允许的字段；没有把握就不要填。"
                + "日期规范为 yyyy-MM-dd；时间规范为 HH:mm；血糖情境 glucoseContext 只能是 fasting、after_1h、after_2h、random。"
                + "B 超孕周不要从图像或文字猜测，除非用户原文明确写出孕周；不要添加不存在的字段，不要输出 Markdown。";

        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", prompt));

        return new JSONObject()
                .put("model", model)
                .put("temperature", 0)
                .put("messages", messages)
                .put("response_format", new JSONObject().put("type", "json_object"));
    }

    public static JSONObject buildVisitSummaryPolishRequest(
            String model,
            String markdown
    ) throws JSONException {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", visitSummaryPolishSystemPrompt()));
        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", "请润色下面这份栗记复诊汇总，严格遵守系统规则：\n\n" + nullToEmpty(markdown)));

        return new JSONObject()
                .put("model", model)
                .put("temperature", 0)
                .put("messages", messages);
    }

    public static String visitSummaryPolishSystemPrompt() {
        return "你是一个把家庭孕期记录整理成医生面摘要的助手。严格规则：\n"
                + "1. 仅基于下方给定文本重新组织语序与衔接，使其简洁易读；\n"
                + "2. 禁止添加任何诊断、解释、医学建议、风险判定或新信息；\n"
                + "3. 禁止修改数值、日期、字段值；不得删除任何字段或条目；\n"
                + "4. 保留原文中的“报告原文”标注；\n"
                + "5. 保留顶部免责声明；\n"
                + "6. 输出纯中文 Markdown，不解释你的工作。";
    }

    private static String smartTextSharedInstructions(String todayDate) {
        return "今天日期：" + normalizeTodayDate(todayDate) + "。"
                + "原文中的相对日期（昨天/今早/前天/上周三）按今天日期换算成 yyyy-MM-dd。"
                + SPEECH_NUMBER_CORRECTION_INSTRUCTION
                + UNIT_CONVERSION_INSTRUCTION
                + MULTI_EVENT_INSTRUCTION
                + FORM_VALUE_NORMALIZATION_INSTRUCTION
                + "纠错和换算只生成候选 values 与 warnings，保存仍需用户人工确认；不要输出诊断、治疗建议或风险判定。"
                + "rawText 可省略，省略时应用会使用用户原文兜底。";
    }

    public static String parseVisitSummaryPolishResponse(String responseJson) {
        String content = BabyLogSmartInput.extractOpenAiMessageContent(responseJson);
        String markdown = stripMarkdownFence(content).trim();
        if (markdown.isEmpty()) {
            throw new IllegalArgumentException("润色文本为空");
        }
        return markdown;
    }

    public static SmartFillCandidate parseSmartFillResponse(
            String responseJson,
            Iterable<String> allowedKeys,
            String fallbackRawText
    ) {
        String content = BabyLogSmartInput.extractOpenAiMessageContent(responseJson);
        String candidateJson = BabyLogSmartInput.extractJsonFromMessageContent(content);
        Map<String, Object> object = unwrapCandidateObject(
                BabyLogSmartInput.parseJsonObject(candidateJson, "smart fill candidate"));
        Map<String, Object> valuesObject = asObject(object.get("values"));
        if (valuesObject == null) {
            valuesObject = object;
        }

        List<String> allowed = new ArrayList<>();
        for (String key : allowedKeys) {
            if (key != null && !key.trim().isEmpty()) {
                allowed.add(key);
            }
        }

        Map<String, String> values = new LinkedHashMap<>();
        for (String key : allowed) {
            String value = optionalString(valuesObject, key);
            if (value != null) {
                values.put(key, value);
            }
        }

        List<String> warnings = parseWarnings(object.get("warnings"));
        if (values.isEmpty()) {
            warnings.add("未识别到可直接填写的字段，请保留原文手动核对");
        }

        String rawText = optionalString(object, "rawText");
        return new SmartFillCandidate(values, warnings, rawText == null ? nullToEmpty(fallbackRawText) : rawText);
    }

    public static SmartEntryCandidate parseSmartEntryResponse(
            String responseJson,
            Map<String, Map<String, String>> forms,
            String fallbackRawText
    ) {
        String content = BabyLogSmartInput.extractOpenAiMessageContent(responseJson);
        String candidateJson = BabyLogSmartInput.extractJsonFromMessageContent(content);
        Map<String, Object> object = unwrapCandidateObject(
                BabyLogSmartInput.parseJsonObject(candidateJson, "smart entry candidate"));

        String eventType = optionalString(object, "eventType");
        Map<String, String> allowedFields = eventType == null ? null : forms.get(eventType);
        List<String> warnings = parseWarnings(object.get("warnings"));
        Map<String, String> values = new LinkedHashMap<>();

        if (eventType == null || allowedFields == null) {
            eventType = "";
            warnings.add("未能判断记录类型，请从快捷入口手动选择表单");
        } else {
            Map<String, Object> valuesObject = asObject(object.get("values"));
            if (valuesObject == null) {
                valuesObject = object;
            }
            for (String key : allowedFields.keySet()) {
                String value = optionalString(valuesObject, key);
                if (value != null) {
                    values.put(key, value);
                }
            }
            if (values.isEmpty()) {
                warnings.add("未识别到可直接填写的字段，请保留原文手动核对");
            }
        }

        String rawText = optionalString(object, "rawText");
        return new SmartEntryCandidate(
                eventType,
                values,
                warnings,
                rawText == null ? nullToEmpty(fallbackRawText) : rawText);
    }

    private static Map<String, Object> unwrapCandidateObject(Map<String, Object> object) {
        String[] wrapperKeys = {"candidate", "result", "data"};
        for (String key : wrapperKeys) {
            Map<String, Object> nested = asObject(object.get(key));
            if (nested != null) {
                return nested;
            }
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private static String optionalString(Map<String, Object> object, String key) {
        if (object == null || !object.containsKey(key)) {
            return null;
        }
        return cleanText(object.get(key));
    }

    private static List<String> parseWarnings(Object warningValue) {
        List<String> warnings = new ArrayList<>();
        if (warningValue instanceof List) {
            List<?> warningArray = (List<?>) warningValue;
            for (Object item : warningArray) {
                String warning = cleanText(item);
                if (warning != null) {
                    warnings.add(warning);
                }
            }
        } else {
            String warning = cleanText(warningValue);
            if (warning != null) {
                warnings.add(warning);
            }
        }
        return warnings;
    }

    private static String cleanText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "/".equals(text) || "-".equals(text) || "—".equals(text)) {
            return null;
        }
        return text;
    }

    private String postJson(String url, String apiKey, JSONObject body) throws IOException {
        BabyLogHttpClient.HttpResponse response = executeJson(url, apiKey, body);
        if (!response.isSuccessful() && shouldRetryWithoutResponseFormat(response, body)) {
            body.remove("response_format");
            response = executeJson(url, apiKey, body);
        }
        if (!response.isSuccessful()) {
            throw new IOException(BabyLogSmartApi.formatApiErrorMessage(response.statusCode, response.body));
        }
        return response.body;
    }

    private BabyLogHttpClient.HttpResponse executeJson(String url, String apiKey, JSONObject body) throws IOException {
        RequestBody requestBody = RequestBody.create(
                body.toString().getBytes(StandardCharsets.UTF_8),
                BabyLogHttpClient.JSON_MEDIA_TYPE
        );
        Request request = BabyLogHttpClient.requestBuilder(url)
                .header("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();
        return BabyLogHttpClient.executeText(httpClient, request, MAX_RESPONSE_CHARS);
    }

    private static boolean shouldRetryWithoutResponseFormat(BabyLogHttpClient.HttpResponse response, JSONObject body) {
        return body.has("response_format")
                && response.body != null
                && response.body.toLowerCase(java.util.Locale.ROOT).contains("response_format");
    }

    private static String stripMarkdownFence(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int fenceEnd = trimmed.lastIndexOf("```");
            if (firstLineEnd >= 0 && fenceEnd > firstLineEnd) {
                return trimmed.substring(firstLineEnd + 1, fenceEnd).trim();
            }
        }
        return trimmed;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeTodayDate(String todayDate) {
        if (todayDate == null || todayDate.trim().isEmpty()) {
            return BabyLogFormatters.todayDateInput();
        }
        return todayDate.trim();
    }

    public static final class SmartFillCandidate {
        public final Map<String, String> values;
        public final List<String> warnings;
        public final String rawText;

        public SmartFillCandidate(Map<String, String> values, List<String> warnings, String rawText) {
            this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
            this.rawText = rawText == null ? "" : rawText;
        }
    }

    public static final class SmartEntryCandidate {
        public final String eventType;
        public final Map<String, String> values;
        public final List<String> warnings;
        public final String rawText;

        public SmartEntryCandidate(String eventType, Map<String, String> values, List<String> warnings, String rawText) {
            this.eventType = eventType == null ? "" : eventType;
            this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
            this.rawText = rawText == null ? "" : rawText;
        }
    }
}
