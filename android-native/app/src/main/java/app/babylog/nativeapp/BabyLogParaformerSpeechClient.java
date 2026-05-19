package app.babylog.nativeapp;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class BabyLogParaformerSpeechClient {
    private static final String PARAFORMER_WS_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference/";
    private static final int CONNECT_TIMEOUT_SECONDS = 20;
    private static final int START_TIMEOUT_SECONDS = 20;
    private static final int FINISH_TIMEOUT_SECONDS = 90;
    private static final int AUDIO_CHUNK_BYTES = 3200;
    private static final long AUDIO_CHUNK_SLEEP_MS = 90L;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build();

    public SpeechResult transcribePcm(File pcmFile, BabyLogSmartConfigStore.Config config) throws IOException {
        if (pcmFile == null || !pcmFile.exists() || pcmFile.length() == 0L) {
            throw new IOException("没有可识别的录音");
        }
        if (config == null || !config.isConfigured()) {
            throw new IOException("请先配置智能识别 API Key；语音识别首版使用 DashScope Paraformer");
        }

        String taskId = UUID.randomUUID().toString();
        String model = resolveParaformerModel(config.getModel());
        ParaformerListener listener = new ParaformerListener();
        Request request = new Request.Builder()
                .url(PARAFORMER_WS_URL)
                .header("Authorization", "Bearer " + config.getApiKey())
                .build();
        WebSocket webSocket = client.newWebSocket(request, listener);

        if (!webSocket.send(BabyLogSpeechToTextProtocol.buildRunTaskJson(
                taskId,
                model,
                BabyLogSpeechToTextProtocol.DEFAULT_AUDIO_FORMAT,
                BabyLogSpeechToTextProtocol.DEFAULT_SAMPLE_RATE
        ))) {
            throw new IOException("无法启动语音识别任务");
        }

        try {
            if (!listener.awaitStarted(START_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IOException(listener.errorOr("语音识别服务未启动，请稍后重试"));
            }
            sendPcmAudio(webSocket, pcmFile);
            webSocket.send(BabyLogSpeechToTextProtocol.buildFinishTaskJson(taskId));
            if (!listener.awaitFinished(FINISH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IOException(listener.errorOr("语音识别超时，请稍后重试"));
            }
            if (!listener.errorOr("").isEmpty()) {
                throw new IOException(listener.errorOr("语音识别失败"));
            }
            String text = listener.resultText();
            if (text.isEmpty()) {
                throw new IOException("语音识别未返回文字，请重试或手动输入");
            }
            return new SpeechResult(text, model);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException("语音识别被中断", error);
        } finally {
            webSocket.close(1000, null);
        }
    }

    private static String resolveParaformerModel(String configuredModel) {
        if (configuredModel != null && configuredModel.trim().toLowerCase().contains("paraformer")) {
            return configuredModel.trim();
        }
        return BabyLogSpeechToTextProtocol.DEFAULT_MODEL;
    }

    private static void sendPcmAudio(WebSocket webSocket, File pcmFile) throws IOException, InterruptedException {
        byte[] buffer = new byte[AUDIO_CHUNK_BYTES];
        try (FileInputStream input = new FileInputStream(pcmFile)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (!webSocket.send(ByteString.of(buffer, 0, read))) {
                    throw new IOException("发送录音数据失败");
                }
                Thread.sleep(AUDIO_CHUNK_SLEEP_MS);
            }
        }
    }

    private static final class ParaformerListener extends WebSocketListener {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch finished = new CountDownLatch(1);
        private final StringBuilder confirmedText = new StringBuilder();
        private final AtomicReference<String> latestText = new AtomicReference<>("");
        private final AtomicReference<String> errorMessage = new AtomicReference<>("");

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            BabyLogSpeechToTextProtocol.ServerEvent event;
            try {
                event = BabyLogSpeechToTextProtocol.parseServerEvent(text);
            } catch (RuntimeException error) {
                errorMessage.set("语音识别返回无法解析：" + error.getMessage());
                finished.countDown();
                return;
            }

            if ("task-started".equals(event.event)) {
                started.countDown();
                return;
            }
            if (event.taskFailed) {
                errorMessage.set(event.errorMessage.isEmpty() ? "语音识别任务失败" : event.errorMessage);
                started.countDown();
                finished.countDown();
                return;
            }
            if (!event.text.isEmpty()) {
                latestText.set(event.text);
                if (event.sentenceEnd && !confirmedText.toString().contains(event.text)) {
                    if (confirmedText.length() > 0) {
                        confirmedText.append(' ');
                    }
                    confirmedText.append(event.text);
                }
            }
            if (event.taskFinished) {
                if (confirmedText.length() == 0 && !event.text.isEmpty()) {
                    confirmedText.append(event.text);
                }
                finished.countDown();
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            String message = t == null ? "" : t.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = response == null ? "语音识别网络失败" : "语音识别网络失败：" + response.code();
            }
            errorMessage.set(message);
            started.countDown();
            finished.countDown();
        }

        boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
            return started.await(timeout, unit);
        }

        boolean awaitFinished(long timeout, TimeUnit unit) throws InterruptedException {
            return finished.await(timeout, unit);
        }

        String errorOr(String fallback) {
            String error = errorMessage.get();
            return error == null || error.trim().isEmpty() ? fallback : error;
        }

        String resultText() {
            String text = confirmedText.toString().trim();
            if (text.isEmpty()) {
                text = latestText.get();
            }
            return text == null ? "" : text.trim();
        }
    }

    public static final class SpeechResult {
        public final String text;
        public final String model;

        public SpeechResult(String text, String model) {
            this.text = text == null ? "" : text;
            this.model = model == null ? "" : model;
        }
    }
}
