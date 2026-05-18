import app.babylog.nativeapp.BabyLogSmartVisionClient;

public final class BabyLogSmartVisionClientSmokeTest {
    public static void main(String[] args) throws Exception {
        assertEquals("https://api.example.com/v1/chat/completions",
                BabyLogSmartVisionClient.resolveChatCompletionsUrl("https://api.example.com"));
        assertEquals("https://api.example.com/v1/chat/completions",
                BabyLogSmartVisionClient.resolveChatCompletionsUrl("https://api.example.com/v1"));
        assertEquals("https://api.example.com/custom/chat/completions",
                BabyLogSmartVisionClient.resolveChatCompletionsUrl("https://api.example.com/custom/chat/completions"));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }
}
