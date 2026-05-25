package app.babylog.nativeapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public final class BabyLogSyncPullWorker extends Worker {
    private static final String UNIQUE_PULL_WORK = "babylog_sync_pull";

    public BabyLogSyncPullWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
        BabyLogSyncPullOrchestrator.PullSummary summary = new BabyLogSyncPullOrchestrator().pullOnce(
                repository,
                secretStore,
                backendConfig,
                new BabyLogRemoteSyncClient()
        );
        if ("PULL_FAILED".equals(summary.lastError) || summary.lastError.startsWith("HTTP_")) {
            return Result.retry();
        }
        BabyLogSyncAttachmentDownloadWorker.enqueueIfNeeded(context);
        return Result.success();
    }

    public static void ensurePeriodicWork(Context context) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        WorkManager workManager = WorkManager.getInstance(appContext);
        try {
            BabyLogRepository repository = new BabyLogRepository(appContext);
            BabyLogDomain.BackendConfig config = repository.loadSyncSettings();
            boolean configured = config != null
                    && config.enabled
                    && !config.backendBaseUrl.isEmpty()
                    && new BabyLogSyncSecretStore(appContext).hasFamilyKey();
            if (!configured) {
                workManager.cancelUniqueWork(UNIQUE_PULL_WORK);
                return;
            }
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    BabyLogSyncPullWorker.class,
                    15,
                    TimeUnit.MINUTES
            )
                    .setConstraints(new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .build();
            workManager.enqueueUniquePeriodicWork(
                    UNIQUE_PULL_WORK,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
            );
        } catch (RuntimeException ignored) {
            // Sync scheduling must never break app startup or local saves.
        }
    }
}
