package app.babylog.nativeapp;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class BabyLogSyncPushWorker extends Worker {
    private static final String TAG = "BabyLog";
    private static final String UNIQUE_PUSH_WORK = "babylog_sync_push";

    public BabyLogSyncPushWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
        BabyLogService service = new BabyLogService(context, repository);
        BabyLogSyncPushOrchestrator.PushSummary summary = new BabyLogSyncPushOrchestrator().pushOnce(
                service,
                repository,
                secretStore,
                backendConfig,
                new BabyLogRemoteSyncClient()
        );
        if (summary.failed > 0 && "PUSH_FAILED".equals(summary.lastError)) {
            return Result.retry();
        }
        return Result.success();
    }

    public static void enqueueIfConfigured(Context context) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        try {
            BabyLogRepository repository = new BabyLogRepository(appContext);
            BabyLogDomain.BackendConfig config = repository.loadSyncSettings();
            if (config == null || !config.enabled || config.backendBaseUrl.isEmpty()) {
                return;
            }
            if (!new BabyLogSyncSecretStore(appContext).hasFamilyKey()) {
                return;
            }
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BabyLogSyncPushWorker.class)
                    .setConstraints(new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .build();
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                    UNIQUE_PUSH_WORK,
                    ExistingWorkPolicy.REPLACE,
                    request
            );
        } catch (RuntimeException error) {
            Log.w(TAG, "Failed to enqueue sync push work", error);
        }
    }
}
