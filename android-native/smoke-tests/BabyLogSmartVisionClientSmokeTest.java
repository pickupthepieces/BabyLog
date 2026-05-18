import app.babylog.nativeapp.BabyLogSmartVisionClient;

import java.io.File;

public final class BabyLogSmartVisionClientSmokeTest {
    public static void main(String[] args) throws Exception {
        assertEquals("https://api.example.com/v1/chat/completions",
                BabyLogSmartVisionClient.resolveChatCompletionsUrl("https://api.example.com"));
        assertEquals("https://api.example.com/v1/chat/completions",
                BabyLogSmartVisionClient.resolveChatCompletionsUrl("https://api.example.com/v1"));
        assertEquals("https://api.example.com/custom/chat/completions",
                BabyLogSmartVisionClient.resolveChatCompletionsUrl("https://api.example.com/custom/chat/completions"));
        String prompt = BabyLogSmartVisionClient.ultrasoundRecognitionPrompt();
        assertTrue(prompt.contains("bpdMm"));
        assertFalse(prompt.contains("gestationalAge"));

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
        String formatted = BabyLogSmartVisionClient.formatApiErrorMessage(403, longError.toString());
        assertTrue(formatted.contains("模型 API 返回 403"));
        assertFalse(formatted.contains("sk-12345678901234567890"));
        assertTrue(formatted.length() < 620);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean actual) {
        if (!actual) {
            throw new AssertionError("expected true but got false");
        }
    }

    private static void assertFalse(boolean actual) {
        if (actual) {
            throw new AssertionError("expected false but got true");
        }
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
