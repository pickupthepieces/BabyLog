package app.babylog.nativeapp;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BabyLogPcmVoiceRecorder {
    private static final int SAMPLE_RATE_HZ = 16_000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final long MIN_AUDIO_BYTES = 3200L;

    private final AtomicBoolean recording = new AtomicBoolean(false);
    private AudioRecord audioRecord;
    private Thread writerThread;
    private File outputFile;
    private IOException writerError;

    @SuppressLint("MissingPermission")
    public synchronized void start(File cacheDir) throws IOException {
        if (recording.get()) {
            return;
        }
        if (cacheDir == null) {
            throw new IOException("无法访问缓存目录");
        }
        File dir = new File(cacheDir, "voice-stt");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建录音缓存目录");
        }

        int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (minBuffer <= 0) {
            throw new IOException("当前设备不支持 16k 单声道录音");
        }
        int bufferSize = Math.max(minBuffer, 4096);
        outputFile = File.createTempFile("babylog-voice-", ".pcm", dir);
        writerError = null;
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE_HZ,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            releaseRecorder();
            throw new IOException("麦克风初始化失败");
        }

        audioRecord.startRecording();
        recording.set(true);
        byte[] buffer = new byte[bufferSize];
        File target = outputFile;
        AudioRecord recorder = audioRecord;
        writerThread = new Thread(() -> {
            try (FileOutputStream output = new FileOutputStream(target)) {
                while (recording.get()) {
                    int read = recorder.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        output.write(buffer, 0, read);
                    }
                }
            } catch (IOException error) {
                writerError = error;
            }
        }, "BabyLogVoiceRecorder");
        writerThread.start();
    }

    public synchronized File stop() throws IOException {
        if (!recording.getAndSet(false)) {
            return outputFile;
        }
        try {
            if (audioRecord != null) {
                audioRecord.stop();
            }
        } catch (IllegalStateException ignored) {
            // Recorder may already be stopped by the platform.
        }
        if (writerThread != null) {
            try {
                writerThread.join(1500L);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IOException("停止录音被中断", error);
            }
        }
        releaseRecorder();
        if (writerError != null) {
            throw writerError;
        }
        if (outputFile == null || outputFile.length() < MIN_AUDIO_BYTES) {
            deleteQuietly(outputFile);
            throw new IOException("录音太短，请按住说完整一点");
        }
        return outputFile;
    }

    public synchronized void cancel() {
        recording.set(false);
        releaseRecorder();
        deleteQuietly(outputFile);
        outputFile = null;
    }

    private void releaseRecorder() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        writerThread = null;
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            // Best effort cleanup only.
            file.delete();
        }
    }
}
