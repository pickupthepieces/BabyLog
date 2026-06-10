import app.babylog.nativeapp.BabyLogSmartTextClient;
import app.babylog.nativeapp.BabyLogSmartConfigStore;

import com.sun.net.httpserver.HttpServer;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BabyLogSmartTextClientSmokeTest {
    public static void main(String[] args) throws Exception {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("primary", "检查日期");
        fields.put("gestationalAge", "孕周");
        fields.put("secondary", "医院");
        fields.put("fetalPresentation", "胎位");
        fields.put("edema", "水肿");
        fields.put("urineProtein", "尿蛋白");
        fields.put("hemoglobinGL", "血红蛋白 Hb g/L");
        fields.put("highRiskFactors", "高危因素");
        fields.put("treatmentAdvice", "处理及建议");
        fields.put("note", "备注");

        JSONObject fillRequest = BabyLogSmartTextClient.buildSmartFillRequest(
                        "test-model",
                        "产检",
                        "pregnancy",
                        fields,
                        "体重50 2000克",
                        "2026-05-20");
        assertEquals("test-model", fillRequest.getString("model"));
        String fillPrompt = fillRequest
                .getJSONArray("messages")
                .getJSONObject(1)
                .getString("content");
        assertTrue(fillPrompt.contains("今天日期"));
        assertTrue(fillPrompt.contains("2026-05-20"));
        assertTrue(fillPrompt.contains("yyyy-MM-dd"));
        assertTrue(fillPrompt.contains("语音转写"));
        assertTrue(fillPrompt.contains("错误切分"));
        assertTrue(fillPrompt.contains("斤→kg"));
        assertTrue(fillPrompt.contains("多事件"));
        assertTrue(fillPrompt.contains("rawText 可省略"));

        String response = "{"
                + "\"choices\":[{\"message\":{\"content\":\"```json\\n"
                + "{\\\"values\\\":{"
                + "\\\"primary\\\":\\\"2026-05-18\\\","
                + "\\\"gestationalAge\\\":\\\"22+5\\\","
                + "\\\"secondary\\\":\\\"奉化区妇幼\\\","
                + "\\\"fetalPresentation\\\":\\\"头位\\\","
                + "\\\"edema\\\":\\\"无\\\","
                + "\\\"urineProtein\\\":\\\"阴性\\\","
                + "\\\"hemoglobinGL\\\":\\\"112\\\","
                + "\\\"highRiskFactors\\\":\\\"无特殊\\\","
                + "\\\"treatmentAdvice\\\":\\\"继续常规产检\\\","
                + "\\\"extra\\\":\\\"不允许字段\\\"},"
                + "\\\"warnings\\\":[\\\"日期需核对\\\"],"
                + "\\\"rawText\\\":\\\"今天在奉化区妇幼做产检\\\"}"
                + "\\n```\"}}]}";

        BabyLogSmartTextClient.SmartFillCandidate candidate =
                BabyLogSmartTextClient.parseSmartFillResponse(response, fields.keySet(), "fallback");

        assertEquals("2026-05-18", candidate.values.get("primary"));
        assertEquals("22+5", candidate.values.get("gestationalAge"));
        assertEquals("奉化区妇幼", candidate.values.get("secondary"));
        assertEquals("头位", candidate.values.get("fetalPresentation"));
        assertEquals("无", candidate.values.get("edema"));
        assertEquals("阴性", candidate.values.get("urineProtein"));
        assertEquals("112", candidate.values.get("hemoglobinGL"));
        assertEquals("无特殊", candidate.values.get("highRiskFactors"));
        assertEquals("继续常规产检", candidate.values.get("treatmentAdvice"));
        assertEquals(null, candidate.values.get("extra"));
        assertEquals("日期需核对", candidate.warnings.get(0));
        assertEquals("今天在奉化区妇幼做产检", candidate.rawText);

        String plain = "{\"message\":{\"content\":\"{\\\"values\\\":{\\\"note\\\":\\\"饭后一小时血糖 8.8\\\"}}\"}}";
        BabyLogSmartTextClient.SmartFillCandidate simple =
                BabyLogSmartTextClient.parseSmartFillResponse(plain, fields.keySet(), "fallback");
        assertEquals("饭后一小时血糖 8.8", simple.values.get("note"));

        try {
            candidate.values.put("primary", "probe");
            throw new AssertionError("values should be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected immutable candidate map.
        }

        Map<String, Map<String, String>> forms = new LinkedHashMap<>();
        Map<String, String> maternalMetricFields = new LinkedHashMap<>();
        maternalMetricFields.put("weightKg", "体重 kg");
        maternalMetricFields.put("glucoseMmolL", "血糖 mmol/L");
        maternalMetricFields.put("glucoseContext", "血糖情境");
        forms.put("maternal_metric", maternalMetricFields);
        Map<String, String> ultrasoundFields = new LinkedHashMap<>();
        ultrasoundFields.put("examDate", "检查日期");
        ultrasoundFields.put("bpdMm", "BPD mm");
        forms.put("ultrasound", ultrasoundFields);

        JSONObject entryRequest = BabyLogSmartTextClient.buildSmartEntryRequest(
                        "test-model",
                        "pregnancy",
                        forms,
                        "体重50 2000克",
                        "2026-05-20");
        assertEquals("test-model", entryRequest.getString("model"));
        String entryPrompt = entryRequest
                .getJSONArray("messages")
                .getJSONObject(1)
                .getString("content");
        assertTrue(entryPrompt.contains("今天日期"));
        assertTrue(entryPrompt.contains("yyyy-MM-dd"));
        assertTrue(entryPrompt.contains("语音转写"));
        assertTrue(entryPrompt.contains("错误切分"));
        assertTrue(entryPrompt.contains("maternal_metric"));
        assertTrue(entryPrompt.contains("\"weightKg\":\"52\""));
        assertTrue(entryPrompt.contains("多事件"));

        String entryResponse = "{"
                + "\"choices\":[{\"message\":{\"content\":\"{"
                + "\\\"eventType\\\":\\\"maternal_metric\\\","
                + "\\\"values\\\":{"
                + "\\\"weightKg\\\":\\\"61.2\\\","
                + "\\\"glucoseMmolL\\\":\\\"8.8\\\","
                + "\\\"glucoseContext\\\":\\\"after_1h\\\","
                + "\\\"bpdMm\\\":\\\"45\\\","
                + "\\\"extra\\\":\\\"drop\\\"},"
                + "\\\"warnings\\\":[\\\"血糖需核对测量时间\\\"],"
                + "\\\"rawText\\\":\\\"饭后一小时血糖8.8，体重61.2\\\"}\"}}]}";
        BabyLogSmartTextClient.SmartEntryCandidate entry =
                BabyLogSmartTextClient.parseSmartEntryResponse(entryResponse, forms, "fallback entry");
        assertEquals("maternal_metric", entry.eventType);
        assertEquals("61.2", entry.values.get("weightKg"));
        assertEquals("8.8", entry.values.get("glucoseMmolL"));
        assertEquals("after_1h", entry.values.get("glucoseContext"));
        assertEquals(null, entry.values.get("bpdMm"));
        assertEquals(null, entry.values.get("extra"));
        assertEquals("血糖需核对测量时间", entry.warnings.get(0));

        String unknownType = "{\"message\":{\"content\":\"{\\\"eventType\\\":\\\"unknown\\\",\\\"values\\\":{\\\"weightKg\\\":\\\"62\\\"}}\"}}";
        BabyLogSmartTextClient.SmartEntryCandidate unknown =
                BabyLogSmartTextClient.parseSmartEntryResponse(unknownType, forms, "fallback");
        assertEquals("", unknown.eventType);
        assertEquals(null, unknown.values.get("weightKg"));

        String polishPrompt = BabyLogSmartTextClient.visitSummaryPolishSystemPrompt();
        assertEquals("test-model", BabyLogSmartTextClient.buildVisitSummaryPolishRequest(
                "test-model",
                "# 汇总").getString("model"));
        assertTrue(polishPrompt.contains("禁止添加任何诊断"));
        assertTrue(polishPrompt.contains("禁止修改数值"));
        assertTrue(polishPrompt.contains("不得删除任何字段或条目"));
        assertTrue(polishPrompt.contains("保留顶部免责声明"));
        String polishResponse = "{"
                + "\"choices\":[{\"message\":{\"content\":\"```markdown\\n"
                + "# BabyLog 复诊汇总\\n"
                + "> 仅家庭记录摘要，未经医学判读。本应用非医疗器械。\\n"
                + "- 血压 118/76 mmHg\\n"
                + "```\"}}]}";
        String polished = BabyLogSmartTextClient.parseVisitSummaryPolishResponse(polishResponse);
        assertEquals("# BabyLog 复诊汇总\n> 仅家庭记录摘要，未经医学判读。本应用非医疗器械。\n- 血压 118/76 mmHg", polished);

        assertClassifyEntryPostsJsonWithBearer();
        assertResponseFormatFallbackRetriesOnce();
    }

    private static void assertClassifyEntryPostsJsonWithBearer() throws Exception {
        final String[] capturedMethod = new String[1];
        final String[] capturedAuthorization = new String[1];
        final String[] capturedBody = new String[1];
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            capturedMethod[0] = exchange.getRequestMethod();
            capturedAuthorization[0] = exchange.getRequestHeaders().getFirst("Authorization");
            capturedBody[0] = new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8);
            String response = "{"
                    + "\"choices\":[{\"message\":{\"content\":\"{"
                    + "\\\"eventType\\\":\\\"maternal_metric\\\","
                    + "\\\"values\\\":{\\\"weightKg\\\":\\\"61.2\\\"},"
                    + "\\\"warnings\\\":[],"
                    + "\\\"rawText\\\":\\\"体重 61.2kg\\\"}\"}}]}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        BabyLogSmartTextClient.SmartEntryCandidate candidate;
        try {
            int port = server.getAddress().getPort();
            Map<String, Map<String, String>> forms = new LinkedHashMap<>();
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("weightKg", "体重 kg");
            forms.put("maternal_metric", fields);
            BabyLogSmartTextClient client = new BabyLogSmartTextClient();
            candidate = client.classifyEntry(
                    "pregnancy",
                    forms,
                    "体重 61.2kg",
                    new BabyLogSmartConfigStore.Config(
                            "http://127.0.0.1:" + port,
                            "qwen-vl-max",
                            "qwen-plus",
                            "secret-key",
                            true
                    )
            );
        } finally {
            server.stop(0);
        }

        assertEquals("POST", capturedMethod[0]);
        assertEquals("Bearer secret-key", capturedAuthorization[0]);
        assertTrue(capturedBody[0].contains("\"model\":\"qwen-plus\""));
        assertFalse(capturedBody[0].contains("\"model\":\"qwen-vl-max\""));
        assertTrue(capturedBody[0].contains("体重 61.2kg"));
        assertEquals("maternal_metric", candidate.eventType);
        assertEquals("61.2", candidate.values.get("weightKg"));
    }

    private static void assertResponseFormatFallbackRetriesOnce() throws Exception {
        final int[] requests = new int[1];
        final String[] firstBody = new String[1];
        final String[] secondBody = new String[1];
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requests[0] += 1;
            String body = new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8);
            if (requests[0] == 1) {
                firstBody[0] = body;
                byte[] bytes = "{\"error\":\"unsupported response_format\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(400, bytes.length);
                exchange.getResponseBody().write(bytes);
            } else {
                secondBody[0] = body;
                String response = "{"
                        + "\"choices\":[{\"message\":{\"content\":\"{"
                        + "\\\"eventType\\\":\\\"maternal_metric\\\","
                        + "\\\"values\\\":{\\\"weightKg\\\":\\\"52\\\"},"
                        + "\\\"warnings\\\":[\\\"语音数字已纠正：50 2000克 → 52千克，请人工核对\\\"]}\"}}]}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
        BabyLogSmartTextClient.SmartEntryCandidate candidate;
        try {
            int port = server.getAddress().getPort();
            Map<String, Map<String, String>> forms = new LinkedHashMap<>();
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("weightKg", "体重 kg");
            forms.put("maternal_metric", fields);
            candidate = new BabyLogSmartTextClient().classifyEntry(
                    "pregnancy",
                    forms,
                    "体重50 2000克",
                    new BabyLogSmartConfigStore.Config(
                            "http://127.0.0.1:" + port,
                            "qwen-vl-max",
                            "qwen-plus",
                            "secret-key",
                            true
                    )
            );
        } finally {
            server.stop(0);
        }

        assertEquals(2, requests[0]);
        assertTrue(firstBody[0].contains("response_format"));
        assertFalse(secondBody[0].contains("response_format"));
        assertEquals("52", candidate.values.get("weightKg"));
    }

    private static byte[] readAll(InputStream input) throws java.io.IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("expected false");
        }
    }
}
