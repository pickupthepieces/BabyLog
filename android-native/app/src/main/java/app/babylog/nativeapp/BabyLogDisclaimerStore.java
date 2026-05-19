package app.babylog.nativeapp;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

public final class BabyLogDisclaimerStore {
    private static final String PREF_FILE_NAME = "babylog_disclaimer_state";

    private final Context appContext;

    public BabyLogDisclaimerStore(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        this.appContext = context.getApplicationContext();
    }

    public String getAcceptedVersion() {
        return getPreferences().getString(BabyLogDisclaimerPolicy.ACCEPTED_VERSION_KEY, "");
    }

    public boolean hasAcceptedCurrentVersion() {
        return BabyLogDisclaimerPolicy.isAccepted(getAcceptedVersion());
    }

    public void markCurrentVersionAccepted() throws IOException {
        SharedPreferences.Editor editor = getPreferences()
                .edit()
                .putString(
                        BabyLogDisclaimerPolicy.ACCEPTED_VERSION_KEY,
                        BabyLogDisclaimerPolicy.CURRENT_VERSION);
        if (!editor.commit()) {
            throw new IOException("Failed to save disclaimer acceptance");
        }
    }

    private SharedPreferences getPreferences() {
        return appContext.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }
}
