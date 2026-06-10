package app.babylog.nativeapp;

import android.content.Context;
import android.util.Log;

public final class BabyLogWorkManagerSyncTrigger implements BabyLogSyncTrigger {
    private static final String TAG = "BabyLog";

    private final Context appContext;

    public BabyLogWorkManagerSyncTrigger(Context context) {
        appContext = context == null ? null : context.getApplicationContext();
    }

    @Override
    public void triggerAfterLocalWrite() {
        if (appContext == null) {
            return;
        }
        try {
            BabyLogSyncPushWorker.enqueueIfConfigured(appContext);
        } catch (RuntimeException error) {
            Log.w(TAG, "Failed to enqueue sync push after local write", error);
        }
    }
}
