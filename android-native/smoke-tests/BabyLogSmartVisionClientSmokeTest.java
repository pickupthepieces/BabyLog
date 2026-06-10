import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogSmartVisionClient;
import app.babylog.nativeapp.BabyLogSmartApi;
import app.babylog.nativeapp.BabyLogSmartConfigStore;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class BabyLogSmartVisionClientSmokeTest {
    public static void main(String[] args) throws Exception {
        assertEquals("https://api.example.com/v1/chat/completions",
                BabyLogSmartApi.resolveChatCompletionsUrl("https://api.example.com"));
        assertEquals("https://api.example.com/v1/chat/completions",
                BabyLogSmartApi.resolveChatCompletionsUrl("https://api.example.com/v1"));
        assertEquals("https://api.example.com/custom/chat/completions",
                BabyLogSmartApi.resolveChatCompletionsUrl("https://api.example.com/custom/chat/completions"));
        String prompt = BabyLogSmartVisionClient.ultrasoundRecognitionPrompt();
        assertTrue(prompt.contains("bpdMm"));
        assertTrue(prompt.contains("fetalHeartRateBpm"));
        assertTrue(prompt.contains("deepestPocketCm"));
        assertTrue(prompt.contains("hospital"));
        assertTrue(prompt.contains("diagnosisText"));
        assertTrue(prompt.contains("crlMm"));
        assertTrue(prompt.contains("ntMm"));
        assertTrue(prompt.contains("yyyy-MM-dd"));
        assertTrue(prompt.contains("多胎"));
        assertTrue(prompt.contains("识别质量自检"));
        assertFalse(prompt.contains("gestationalAge"));

        String documentPrompt = BabyLogSmartVisionClient.pregnancyDocumentRecognitionPrompt();
        assertTrue(documentPrompt.contains("eventType"));
        assertTrue(documentPrompt.contains("ultrasound"));
        assertTrue(documentPrompt.contains("pregnancy_checkup"));
        assertTrue(documentPrompt.contains("screening_ogtt"));
        assertTrue(documentPrompt.contains("bpdMm"));
        assertTrue(documentPrompt.contains("hemoglobinGL"));
        assertTrue(documentPrompt.contains("fastingGlucoseMmolL"));
        assertTrue(documentPrompt.contains("不要识别姓名"));
        assertTrue(documentPrompt.contains("yyyy-MM-dd"));
        assertTrue(documentPrompt.contains("多胎"));
        assertTrue(documentPrompt.contains("识别质量自检"));

        String ultrasoundResponse = "{"
                + "\"choices\":[{\"message\":{\"content\":\"{"
                + "\\\"eventType\\\":\\\"ultrasound\\\","
                + "\\\"values\\\":{\\\"examDate\\\":\\\"2026-05-02\\\",\\\"bpdMm\\\":\\\"45\\\",\\\"rawText\\\":\\\"BPD 45mm\\\"},"
                + "\\\"warnings\\\":[\\\"请核对\\\"],"
                + "\\\"rawText\\\":\\\"BPD 45mm\\\"}\"}}]}";
        BabyLogSmartVisionClient.PregnancyDocumentOcrCandidate ultrasound =
                BabyLogSmartVisionClient.parsePregnancyDocumentResponse(ultrasoundResponse);
        assertEquals("ultrasound", ultrasound.eventType);
        assertEquals(45.0, ultrasound.ultrasound.bpdMm.value);
        assertEquals(null, ultrasound.checkup);

        String checkupResponse = "{"
                + "\"choices\":[{\"message\":{\"content\":\"{"
                + "\\\"eventType\\\":\\\"pregnancy_checkup\\\","
                + "\\\"values\\\":{\\\"primary\\\":\\\"2026-05-18\\\",\\\"hemoglobinGL\\\":\\\"112\\\",\\\"extra\\\":\\\"drop\\\"},"
                + "\\\"warnings\\\":[\\\"人工核对\\\"],"
                + "\\\"rawText\\\":\\\"Hb 112\\\"}\"}}]}";
        BabyLogSmartVisionClient.PregnancyDocumentOcrCandidate checkup =
                BabyLogSmartVisionClient.parsePregnancyDocumentResponse(checkupResponse);
        assertEquals("pregnancy_checkup", checkup.eventType);
        assertEquals("112", checkup.checkup.values.get("hemoglobinGL"));
        assertEquals(null, checkup.checkup.values.get("extra"));
        assertEquals(null, checkup.ultrasound);

        String ogttResponse = "{"
                + "\"choices\":[{\"message\":{\"content\":\"{"
                + "\\\"eventType\\\":\\\"screening_ogtt\\\","
                + "\\\"values\\\":{\\\"primary\\\":\\\"2026-05-18\\\",\\\"fastingGlucoseMmolL\\\":\\\"4.8\\\",\\\"extra\\\":\\\"drop\\\"},"
                + "\\\"warnings\\\":[\\\"照报告核对\\\"],"
                + "\\\"rawText\\\":\\\"OGTT 空腹 4.8\\\"}\"}}]}";
        BabyLogSmartVisionClient.PregnancyDocumentOcrCandidate ogtt =
                BabyLogSmartVisionClient.parsePregnancyDocumentResponse(ogttResponse);
        assertEquals("screening_ogtt", ogtt.eventType);
        assertEquals("4.8", ogtt.checkup.values.get("fastingGlucoseMmolL"));
        assertEquals(null, ogtt.checkup.values.get("extra"));
        assertEquals(null, ogtt.ultrasound);

        RecordingPreparer preparer = new RecordingPreparer();
        BabyLogSmartVisionClient client = new BabyLogSmartVisionClient(preparer);
        File source = new File("source-ultrasound.jpg");
        File prepared = client.prepareImageForUpload(source);
        assertEquals(source, preparer.seen);
        assertEquals(new File("prepared-ultrasound.jpg"), prepared);
        assertEquals(1, preparer.calls);

        StringBuilder longError = new StringBuilder("bad key sk-12345678901234567890 ");
        for (int i = 0; i < 700; i++) {
            longError.append('x');
        }
        String formatted = BabyLogSmartApi.formatApiErrorMessage(403, longError.toString());
        assertTrue(formatted.contains("模型 API 返回 403"));
        assertFalse(formatted.contains("sk-12345678901234567890"));
        assertTrue(formatted.length() < 620);

        assertVisionUsesVisualModelAndRetriesWithoutResponseFormat();
    }

    private static void assertVisionUsesVisualModelAndRetriesWithoutResponseFormat() throws Exception {
        final int[] requests = new int[1];
        final String[] firstBody = new String[1];
        final String[] secondBody = new String[1];
        File image = File.createTempFile("babylog-vision-smoke-", ".jpg");
        try (FileOutputStream output = new FileOutputStream(image)) {
            output.write(new byte[]{1, 2, 3, 4});
        }
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requests[0] += 1;
            String body = new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8);
            if (requests[0] == 1) {
                firstBody[0] = body;
                byte[] bytes = "{\"error\":\"response_format is not supported\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(400, bytes.length);
                exchange.getResponseBody().write(bytes);
            } else {
                secondBody[0] = body;
                String response = "{"
                        + "\"choices\":[{\"message\":{\"content\":\"{"
                        + "\\\"eventType\\\":\\\"ultrasound\\\","
                        + "\\\"values\\\":{\\\"examDate\\\":\\\"2026-05-02\\\",\\\"bpdMm\\\":\\\"45\\\"},"
                        + "\\\"warnings\\\":[]}\"}}]}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
        BabyLogSmartVisionClient.PregnancyDocumentOcrCandidate candidate;
        try {
            int port = server.getAddress().getPort();
            BabyLogSmartVisionClient client = new BabyLogSmartVisionClient(source -> source);
            candidate = client.recognizePregnancyDocumentImage(
                    image,
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
            image.delete();
        }

        assertEquals(2, requests[0]);
        assertTrue(firstBody[0].contains("\"model\":\"qwen-vl-max\""));
        assertTrue(firstBody[0].contains("response_format"));
        assertFalse(firstBody[0].contains("\"model\":\"qwen-plus\""));
        assertTrue(secondBody[0].contains("\"model\":\"qwen-vl-max\""));
        assertFalse(secondBody[0].contains("response_format"));
        assertEquals("ultrasound", candidate.eventType);
        assertEquals(45.0, candidate.ultrasound.bpdMm.value);
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




    private static final class RecordingPreparer implements BabyLogSmartVisionClient.UploadImagePreparer {
        int calls;
        File seen;

        @Override
        public File prepare(File source) {
            calls++;
            seen = source;
            return new File("prepared-ultrasound.jpg");
        }
    }
}
