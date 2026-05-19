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

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("expected false");
        }
    }
}
