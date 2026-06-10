import static app.babylog.nativeapp.SmokeAssert.*;

import app.babylog.nativeapp.BabyLogDisclaimerPolicy;

public final class BabyLogDisclaimerPolicySmokeTest {
    public static void main(String[] args) {
        assertFalse(BabyLogDisclaimerPolicy.isAccepted(null));
        assertFalse(BabyLogDisclaimerPolicy.isAccepted(""));
        assertFalse(BabyLogDisclaimerPolicy.isAccepted("older-version"));
        assertTrue(BabyLogDisclaimerPolicy.needsAcceptance("older-version"));

        String current = BabyLogDisclaimerPolicy.CURRENT_VERSION;
        assertTrue(BabyLogDisclaimerPolicy.isAccepted(current));
        assertFalse(BabyLogDisclaimerPolicy.needsAcceptance(current));

        assertEquals("medical_disclaimer_accepted_version", BabyLogDisclaimerPolicy.ACCEPTED_VERSION_KEY);
    }



}
