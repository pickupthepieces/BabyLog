package app.babylog.nativeapp;

public interface BabyLogSyncTrigger {
    void triggerAfterLocalWrite();

    static BabyLogSyncTrigger noop() {
        return new BabyLogSyncTrigger() {
            @Override
            public void triggerAfterLocalWrite() {
                // Intentionally empty for smoke tests and background sync internals.
            }
        };
    }
}
