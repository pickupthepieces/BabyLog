package app.babylog.nativeapp;

import java.util.Objects;

public final class SmokeAssert {
    private SmokeAssert() {
    }

    public static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    public static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("Expected true");
        }
    }

    public static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("Expected false");
        }
    }

    public static void assertNotNull(Object value) {
        if (value == null) {
            throw new AssertionError("Expected non-null value");
        }
    }

    public static void assertContains(String value, String expectedPart) {
        if (value == null || !value.contains(expectedPart)) {
            throw new AssertionError("Expected text to contain " + expectedPart + " but was " + value);
        }
    }

    public static void assertNotContains(String value, String forbiddenPart) {
        if (value != null && value.contains(forbiddenPart)) {
            throw new AssertionError("Expected text not to contain " + forbiddenPart + " but was " + value);
        }
    }

    public static void assertNear(double expected, double actual, double tolerance) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError("Expected " + expected + " +/- " + tolerance + " but was " + actual);
        }
    }

    public static void assertBetween(double min, double max, double actual) {
        if (actual < min || actual > max) {
            throw new AssertionError("Expected " + actual + " between " + min + " and " + max);
        }
    }

    public static void assertThrows(ThrowingRunnable action) throws Exception {
        boolean thrown = false;
        try {
            action.run();
        } catch (Exception expected) {
            thrown = true;
        }
        if (!thrown) {
            throw new AssertionError("Expected exception");
        }
    }

    public static void assertThrowsType(Class<?> expectedType, ThrowingRunnable action) throws Exception {
        try {
            action.run();
        } catch (Exception expected) {
            if (!expectedType.isInstance(expected)) {
                throw new AssertionError(
                        "Expected " + expectedType.getName() + " but was " + expected.getClass().getName()
                );
            }
            return;
        }
        throw new AssertionError("Expected " + expectedType.getName());
    }

    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
