package app.babylog.nativeapp;

import android.app.Application;

public final class BabyLogApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BabyLogSyncPullWorker.ensurePeriodicWork(this);
    }
}
