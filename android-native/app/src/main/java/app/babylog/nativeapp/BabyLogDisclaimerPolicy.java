package app.babylog.nativeapp;

public final class BabyLogDisclaimerPolicy {
    public static final String CURRENT_VERSION = "medical-disclaimer-2026-05-v1";
    public static final String ACCEPTED_VERSION_KEY = "medical_disclaimer_accepted_version";

    private BabyLogDisclaimerPolicy() {
    }

    public static boolean isAccepted(String acceptedVersion) {
        return CURRENT_VERSION.equals(acceptedVersion);
    }

    public static boolean needsAcceptance(String acceptedVersion) {
        return !isAccepted(acceptedVersion);
    }
}
