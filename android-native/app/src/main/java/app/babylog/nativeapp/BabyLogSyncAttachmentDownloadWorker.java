package app.babylog.nativeapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class BabyLogSyncAttachmentDownloadWorker extends Worker {
    private static final String UNIQUE_DOWNLOAD_WORK = "babylog_sync_attachment_download";

    public BabyLogSyncAttachmentDownloadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        BabyLogRepository repository = new BabyLogRepository(context);
        BabyLogDomain.BackendConfig backendConfig = repository.loadSyncSettings();
        if (backendConfig == null || !backendConfig.enabled || backendConfig.backendBaseUrl.isEmpty()) {
            return Result.success();
        }
        BabyLogSyncSecretStore secretStore = new BabyLogSyncSecretStore(context);
        if (!secretStore.hasFamilyKey()) {
            return Result.success();
        }
        try {
            BabyLogSyncAttachmentDownloader.DownloadSummary summary = BabyLogSyncAttachmentDownloader.downloadQueuedOnce(
                    repository,
                    backendConfig,
                    secretStore.loadFamilyKey(),
                    new BabyLogRemoteSyncClient()
            );
            return summary.failed > 0 ? Result.retry() : Result.success();
        } catch (Exception error) {
            return Result.retry();
        }
    }

    public static DownloadSummary downloadQueuedOnce(
            BabyLogRepository repository,
            BabyLogDomain.BackendConfig backendConfig,
            String familyKey,
            BabyLogRemoteSyncClient remoteClient
    ) {
        BabyLogSyncAttachmentDownloader.DownloadSummary summary = BabyLogSyncAttachmentDownloader.downloadQueuedOnce(
                repository,
                backendConfig,
                familyKey,
                remoteClient
        );
        return new DownloadSummary(summary.downloaded, summary.failed, summary.bytesDownloaded, summary.lastError);
    }

    public static void enqueueIfNeeded(Context context) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        try {
            BabyLogRepository repository = new BabyLogRepository(appContext);
            if (repository.listAttachmentDownloadQueue().isEmpty()) {
                return;
            }
            BabyLogDomain.BackendConfig config = repository.loadSyncSettings();
            if (config == null || !config.enabled || config.backendBaseUrl.isEmpty()) {
                return;
            }
            if (!new BabyLogSyncSecretStore(appContext).hasFamilyKey()) {
                return;
            }
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BabyLogSyncAttachmentDownloadWorker.class)
                    .setConstraints(new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .build();
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                    UNIQUE_DOWNLOAD_WORK,
                    ExistingWorkPolicy.REPLACE,
                    request
            );
        } catch (RuntimeException ignored) {
            // Attachment sync must never break app startup or local saves.
        }
    }

    public static final class DownloadSummary {
        public final int downloaded;
        public final int failed;
        public final long bytesDownloaded;
        public final String lastError;

        DownloadSummary(int downloaded, int failed, long bytesDownloaded, String lastError) {
            this.downloaded = downloaded;
            this.failed = failed;
            this.bytesDownloaded = bytesDownloaded;
            this.lastError = lastError == null ? "" : lastError;
        }
    }
}
