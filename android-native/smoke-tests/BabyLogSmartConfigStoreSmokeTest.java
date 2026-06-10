import app.babylog.nativeapp.BabyLogSmartConfigStore;
import app.babylog.nativeapp.BabyLogSpeechToTextProtocol;

public final class BabyLogSmartConfigStoreSmokeTest {
    public static void main(String[] args) {
        BabyLogSmartConfigStore.Config smart =
                new BabyLogSmartConfigStore.Config(
                        "https://dashscope.aliyuncs.com/compatible-mode/v1",
                        "qwen3-vl-plus",
                        "smart-key",
                        true);
        assertEquals("qwen3-vl-plus", smart.getModel());
        assertEquals("", smart.getTextModel());
        assertEquals("qwen3-vl-plus", smart.resolveTextModel());
        assertTrue(smart.isConfigured());

        BabyLogSmartConfigStore.Config splitModels =
                new BabyLogSmartConfigStore.Config(
                        "https://dashscope.aliyuncs.com/compatible-mode/v1",
                        "qwen-vl-max",
                        "qwen-plus",
                        "smart-key",
                        true);
        assertEquals("qwen-vl-max", splitModels.getModel());
        assertEquals("qwen-plus", splitModels.getTextModel());
        assertEquals("qwen-plus", splitModels.resolveTextModel());
        assertTrue(splitModels.toString().contains("textModel='qwen-plus'"));
        BabyLogSmartConfigStore.Config roundTrip =
                new BabyLogSmartConfigStore.Config(
                        splitModels.getBaseUrl(),
                        splitModels.getModel(),
                        splitModels.getTextModel(),
                        splitModels.getApiKey(),
                        splitModels.isEnabled());
        assertEquals("qwen-plus", roundTrip.getTextModel());
        assertEquals("qwen-plus", roundTrip.resolveTextModel());

        BabyLogSmartConfigStore.SpeechConfig speech =
                new BabyLogSmartConfigStore.SpeechConfig(
                        "speech-key",
                        BabyLogSpeechToTextProtocol.DEFAULT_MODEL,
                        true);
        assertEquals("speech-key", speech.getApiKey());
        assertEquals(BabyLogSpeechToTextProtocol.DEFAULT_MODEL, speech.getModel());
        assertEquals(true, speech.isInverseTextNormalizationEnabled());
        assertTrue(speech.isConfigured());

        BabyLogSmartConfigStore.SpeechConfig disabled =
                new BabyLogSmartConfigStore.SpeechConfig(
                        "speech-key",
                        BabyLogSpeechToTextProtocol.DEFAULT_MODEL,
                        false);
        assertFalse(disabled.isConfigured());

        BabyLogSmartConfigStore.SpeechConfig keepsChineseNumbers =
                new BabyLogSmartConfigStore.SpeechConfig(
                        "speech-key",
                        BabyLogSpeechToTextProtocol.DEFAULT_MODEL,
                        true,
                        false);
        assertEquals(false, keepsChineseNumbers.isInverseTextNormalizationEnabled());
        assertTrue(keepsChineseNumbers.isConfigured());

        BabyLogSmartConfigStore.SpeechConfig blankKey =
                new BabyLogSmartConfigStore.SpeechConfig(
                        "",
                        BabyLogSpeechToTextProtocol.DEFAULT_MODEL,
                        true);
        assertFalse(blankKey.isConfigured());
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
