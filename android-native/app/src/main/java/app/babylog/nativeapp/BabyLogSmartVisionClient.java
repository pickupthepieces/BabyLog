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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        PregnancyDocumentOcrCandidate candidate = recognizePregnancyDocumentImage(image, config);
        if (candidate.ultrasound != null) {
            return candidate.ultrasound;
        }
        if (candidate.checkup != null && !candidate.checkup.values.isEmpty()) {
            throw new IOException("这张图片更像产检检查单，请从产检记录入口核对候选字段。");
        }
        throw new IOException("未识别到可用的 B 超字段，请确认图片是清晰的 B 超单。");
    }

    public BabyLogSmartTextClient.SmartFillCandidate recognizeCheckupImage(
            File image,
            BabyLogSmartConfigStore.Config config
    ) throws IOException, JSONException {
        PregnancyDocumentOcrCandidate candidate = recognizePregnancyDocumentImage(image, config);
        if (candidate.checkup != null) {
            return candidate.checkup;
        }
        if (candidate.ultrasound != null) {
            throw new IOException("这张图片更像 B 超单，请从 B 超记录入口核对候选字段。");
        }
        throw new IOException("未识别到可用的产检字段，请确认图片是清晰的检查单或母子健康手册记录。");
    }

    public PregnancyDocumentOcrCandidate recognizePregnancyDocumentImage(
            File image,
            BabyLogSmartConfigStore.Config config
    ) throws IOException, JSONException {
        if (image == null || !image.exists() || image.length() <= 0) {
            throw new IOException("请先拍照或选择孕期报告图片");
        }
        if (config == null || !config.isConfigured()) {
            throw new IOException("请先配置多模态模型 API");
        }

        File uploadImage = prepareImageForUpload(image);
        try {
            JSONObject request = buildPregnancyDocumentChatCompletionsRequest(config.getModel(), imageToDataUrl(uploadImage));
            String response = postJson(resolveChatCompletionsUrl(config.getBaseUrl()), config.getApiKey(), request);
            try {
                return parsePregnancyDocumentResponse(response);
            } catch (IllegalArgumentException error) {
                throw new IOException("模型返回内容无法解析：" + error.getMessage(), error);
            }
        } finally {
            if (uploadImage != null && !image.equals(uploadImage) && uploadImage.exists()) {
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

    public static JSONObject buildPregnancyDocumentChatCompletionsRequest(String model, String imageDataUrl) throws JSONException {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", "你是 BabyLog 孕期报告 OCR 助手。你只从用户主动上传的 B 超单、产检检查单或母子健康手册照片中提取可见字段。不要诊断，不要推测。必须只返回 JSON object。"));

        JSONArray userContent = new JSONArray();
        userContent.put(new JSONObject()
                .put("type", "text")
                .put("text", pregnancyDocumentRecognitionPrompt()));
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
        return "请识别这张 B 超单，只提取胎儿生长指标和报告公共信息，并返回 JSON object。"
                + "字段固定为：examDate, hospital, reportTime, diagnosisText, bpdMm, hcMm, acMm, flMm, efwGram, afiCm, deepestPocketCm, placentaLocation, placentaGrade, fetalPresentation, fetalHeartRateBpm, fetalCount, fetalMovement, umbilicalInsertion, cervicalLengthMm, crlMm, ntMm, umbilicalSd, umbilicalPi, umbilicalRi, warnings, rawText。"
                + "公共信息只识别报告明确写出的就诊医院/报告医院、报告时间/检查时间、超声诊断/诊断意见/超声提示；不要识别姓名、身份证、门诊号、住院号、床号、医生签名等个人身份信息。"
                + "只在报告文字明确出现时填写字段：BPD/双顶径、HC/头围、AC/腹围、FL/股骨长、EFW/估重/胎儿体重、AFI、羊水最大深度/最大平段/最大羊水池、胎盘位置/分级、胎位、胎心率、胎儿个数、胎动、脐带插入处、宫颈管长度、CRL/顶臀径、NT、脐动脉 S/D/PI/RI。"
                + "不要识别、返回或推断孕周；孕周由用户根据报告手动填写或由预产期计算。"
                + "efwGram 只在明确出现 EFW、估重、胎儿体重或胎重时填写；不要把胎心率 143bpm 当 EFW。"
                + "不要把侧脑室、后角宽、脑室宽、鼻骨、四腔心、肢体可及等结构筛查文字误填为羊水、NT 或生长指标；这类内容只放 rawText 或 warnings。"
                + "数值字段只填数字，单位按字段要求换算：BPD/HC/AC/FL/CRL/NT/宫颈管长度为 mm，EFW 为 g，AFI/羊水最大深度为 cm，胎心率为 bpm；不确定就省略或放入 warnings。";
    }

    public static String pregnancyDocumentRecognitionPrompt() {
        return "请先判断图片类型，然后识别字段。只返回 JSON object："
                + "{\"eventType\":\"ultrasound、pregnancy_checkup、screening_nt、screening_serum、screening_nipt、screening_anomaly、screening_ogtt、screening_gbs、screening_nst 或空字符串\",\"values\":{\"字段key\":\"候选值\"},\"warnings\":[\"需要人工核对的点\"],\"rawText\":\"可见报告文字摘要\"}。"
                + "eventType=ultrasound 时，values 只能包含这些 B 超字段：examDate, hospital, reportTime, diagnosisText, bpdMm, hcMm, acMm, flMm, efwGram, afiCm, deepestPocketCm, placentaLocation, placentaGrade, fetalPresentation, fetalHeartRateBpm, fetalCount, fetalMovement, umbilicalInsertion, cervicalLengthMm, crlMm, ntMm, umbilicalSd, umbilicalPi, umbilicalRi。"
                + "eventType=pregnancy_checkup 时，values 只能包含这些产检字段："
                + checkupRecognitionFields().keySet()
                + "。"
                + "eventType=screening_* 时，values 只能包含这些专项字段："
                + screeningRecognitionFields().keySet()
                + "。"
                + "如果同时有 B 超和产检字段，优先选择图片主体对应的报告类型；无法判断时 eventType 置空。"
                + "不要识别姓名、身份证、门诊号、住院号、床号、手机号、医生签名等个人身份信息。"
                + "B 超不要识别、返回或推断孕周；产检/母子健康手册只有明确写出孕周时才可填写 gestationalAge。"
                + "所有分级、阴阳性、报告标注只按报告原文抄录，不要根据数值推断，不要输出诊断或治疗建议。"
                + "数值字段只填数字并按字段单位换算：B 超长度为 mm，EFW 为 g，羊水为 cm；血压为 mmHg，体重 kg，Hb g/L。"
                + "不确定的内容省略或放入 warnings，不要输出 Markdown。";
    }

    public static Map<String, String> checkupRecognitionFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("primary", "检查日期 yyyy-MM-dd");
        fields.put("gestationalAge", "孕周，例如 22+5");
        fields.put("secondary", "医院 / 机构");
        fields.put("department", "科室 / 医生");
        fields.put("systolicBp", "收缩压 mmHg");
        fields.put("diastolicBp", "舒张压 mmHg");
        fields.put("weightKg", "体重 kg");
        fields.put("fundalHeightCm", "宫高 cm");
        fields.put("abdominalCircumferenceCm", "腹围 cm");
        fields.put("fetalHeartRateBpm", "胎心率 bpm");
        fields.put("fetalPresentation", "胎位");
        fields.put("edema", "水肿");
        fields.put("urineRoutine", "尿常规摘要");
        fields.put("urineProtein", "尿蛋白");
        fields.put("hemoglobinGL", "血红蛋白 Hb g/L");
        fields.put("highRiskFactors", "高危因素 / 特殊情况");
        fields.put("tertiary", "医生结论");
        fields.put("treatmentAdvice", "处理及建议");
        fields.put("nextVisitDate", "下次产检日期 yyyy-MM-dd");
        fields.put("reportType", "报告类型");
        fields.put("attachmentNote", "附件备注");
        fields.put("note", "备注");
        return Collections.unmodifiableMap(fields);
    }

    public static Map<String, String> screeningRecognitionFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("primary", "检查日期 yyyy-MM-dd");
        fields.put("gestationalAge", "孕周，例如 20+3");
        fields.put("ntMm", "NT 值 mm");
        fields.put("riskT21", "21 三体风险值，照报告原文");
        fields.put("riskT18", "18 三体风险值，照报告原文");
        fields.put("riskOntd", "开放性神经管风险，照报告原文");
        fields.put("riskLevel", "分级：低危 / 临界 / 高危 / 见报告");
        fields.put("t21Result", "T21 结果：低风险 / 高风险 / 见报告");
        fields.put("t18Result", "T18 结果：低风险 / 高风险 / 见报告");
        fields.put("t13Result", "T13 结果：低风险 / 高风险 / 见报告");
        fields.put("sexChromosome", "性染色体结果，可空");
        fields.put("structureConclusion", "结构结论 / 大排畸描述");
        fields.put("fastingGlucoseMmolL", "空腹血糖 mmol/L");
        fields.put("oneHourGlucoseMmolL", "1h 血糖 mmol/L");
        fields.put("twoHourGlucoseMmolL", "2h 血糖 mmol/L");
        fields.put("abnormalFlag", "报告标注：报告写正常/需核对/见报告");
        fields.put("gbsResult", "GBS：阴性 / 阳性 / 见报告");
        fields.put("nstResult", "胎心监护：反应型 / 无反应型 / 见报告");
        fields.put("conclusion", "结论文本");
        fields.put("attachmentNote", "附件备注");
        fields.put("note", "备注");
        return Collections.unmodifiableMap(fields);
    }

    public static PregnancyDocumentOcrCandidate parsePregnancyDocumentResponse(String responseJson) {
        String content = BabyLogSmartInput.extractOpenAiMessageContent(responseJson);
        String candidateJson = BabyLogSmartInput.extractJsonFromMessageContent(content);
        return fromPregnancyDocumentJson(candidateJson);
    }

    public static PregnancyDocumentOcrCandidate fromPregnancyDocumentJson(String json) {
        Map<String, Object> object = unwrapCandidateObject(
                BabyLogSmartInput.parseJsonObject(json, "pregnancy document OCR candidate"));
        String eventType = optionalString(object, "eventType");
        Map<String, Object> values = asObject(object.get("values"));
        if (values == null) {
            values = object;
        }
        List<String> warnings = parseWarnings(object.get("warnings"));
        String rawText = optionalString(object, "rawText");

        if ("ultrasound".equals(eventType)) {
            JSONObject ultrasound = new JSONObject(values);
            try {
                ultrasound.put("warnings", new JSONArray(warnings));
                if (rawText != null) {
                    ultrasound.put("rawText", rawText);
                }
            } catch (JSONException error) {
                throw new IllegalArgumentException("failed to build ultrasound candidate", error);
            }
            return new PregnancyDocumentOcrCandidate(
                    "ultrasound",
                    BabyLogSmartInput.fromCandidateJson(ultrasound.toString()),
                    null,
                    warnings,
                    rawText
            );
        }
        if ("pregnancy_checkup".equals(eventType) || BabyLogFormatters.isScreeningEventType(eventType)) {
            Map<String, String> checkupValues = "pregnancy_checkup".equals(eventType)
                    ? filterValues(values, checkupRecognitionFields())
                    : filterValues(values, screeningRecognitionFields());
            List<String> nextWarnings = new ArrayList<>(warnings);
            if (checkupValues.isEmpty()) {
                nextWarnings.add("未识别到可直接填写的字段，请保留原图手动核对");
            }
            return new PregnancyDocumentOcrCandidate(
                    eventType,
                    null,
                    new BabyLogSmartTextClient.SmartFillCandidate(checkupValues, nextWarnings, rawText == null ? "" : rawText),
                    nextWarnings,
                    rawText
            );
        }

        List<String> nextWarnings = new ArrayList<>(warnings);
        nextWarnings.add("未能判断报告类型，请手动选择 B 超或产检表单");
        return new PregnancyDocumentOcrCandidate("", null, null, nextWarnings, rawText);
    }

    private static Map<String, String> filterCheckupValues(Map<String, Object> values) {
        return filterValues(values, checkupRecognitionFields());
    }

    private static Map<String, String> filterValues(Map<String, Object> values, Map<String, String> allowedFields) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : allowedFields.keySet()) {
            String value = optionalString(values, key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
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
        Object value = object.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "/".equals(text) || "-".equals(text) || "—".equals(text)) {
            return null;
        }
        return text;
    }

    private static List<String> parseWarnings(Object warningValue) {
        List<String> warnings = new ArrayList<>();
        if (warningValue instanceof List) {
            List<?> warningArray = (List<?>) warningValue;
            for (Object item : warningArray) {
                addWarning(warnings, item);
            }
        } else {
            addWarning(warnings, warningValue);
        }
        return warnings;
    }

    private static void addWarning(List<String> warnings, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (!text.isEmpty()) {
            warnings.add(text);
        }
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

    public static final class PregnancyDocumentOcrCandidate {
        public final String eventType;
        public final BabyLogSmartInput.UltrasoundOcrCandidate ultrasound;
        public final BabyLogSmartTextClient.SmartFillCandidate checkup;
        public final List<String> warnings;
        public final String rawText;

        public PregnancyDocumentOcrCandidate(
                String eventType,
                BabyLogSmartInput.UltrasoundOcrCandidate ultrasound,
                BabyLogSmartTextClient.SmartFillCandidate checkup,
                List<String> warnings,
                String rawText
        ) {
            this.eventType = eventType == null ? "" : eventType;
            this.ultrasound = ultrasound;
            this.checkup = checkup;
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings == null ? Collections.emptyList() : warnings));
            this.rawText = rawText == null ? "" : rawText;
        }
    }
}
