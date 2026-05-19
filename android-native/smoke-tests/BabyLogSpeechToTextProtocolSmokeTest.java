import app.babylog.nativeapp.BabyLogSpeechToTextProtocol;

public final class BabyLogSpeechToTextProtocolSmokeTest {
    public static void main(String[] args) {
        String runTask = BabyLogSpeechToTextProtocol.buildRunTaskJson("task-1", "paraformer-realtime-v2", "pcm", 16000);
        assertContains(runTask, "\"task\":\"asr\"");
        assertContains(runTask, "\"action\":\"run-task\"");
        assertContains(runTask, "\"model\":\"paraformer-realtime-v2\"");
        assertContains(runTask, "\"language_hints\":[\"zh\"]");
        assertContains(runTask, "\"sample_rate\":16000");

        String finishTask = BabyLogSpeechToTextProtocol.buildFinishTaskJson("task-1");
        assertContains(finishTask, "\"action\":\"finish-task\"");
        assertContains(finishTask, "\"task_id\":\"task-1\"");

        BabyLogSpeechToTextProtocol.ServerEvent sentence =
                BabyLogSpeechToTextProtocol.parseServerEvent("{\"header\":{\"event\":\"result-generated\"},\"payload\":{\"output\":{\"sentence\":{\"text\":\"今天产检血糖八点八\",\"sentence_end\":true}}}}");
        assertEquals("result-generated", sentence.event);
        assertEquals("今天产检血糖八点八", sentence.text);
        assertEquals(true, sentence.sentenceEnd);

        BabyLogSpeechToTextProtocol.ServerEvent finished =
                BabyLogSpeechToTextProtocol.parseServerEvent("{\"header\":{\"event\":\"task-finished\"},\"payload\":{\"output\":{\"sentence\":{\"text\":\"最终文本\"}}}}");
        assertEquals(true, finished.taskFinished);
        assertEquals("最终文本", finished.text);

        BabyLogSpeechToTextProtocol.ServerEvent failed =
                BabyLogSpeechToTextProtocol.parseServerEvent("{\"header\":{\"event\":\"task-failed\",\"error_message\":\"bad audio\"}}");
        assertEquals(true, failed.taskFailed);
        assertEquals("bad audio", failed.errorMessage);
    }

    private static void assertContains(String text, String expected) {
        if (!text.contains(expected)) {
            throw new AssertionError("expected to contain " + expected + " but got " + text);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }
}
