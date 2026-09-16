package com.deliveryplanner;

/**
 * Minimal assertion helper. No framework, no reflection, no annotations — each test class
 * calls these directly and {@link TestRunner} reads the counters at the end.
 */
public final class Assert {

    private static int passed = 0;
    private static int failed = 0;

    private Assert() {
    }

    public static void assertTrue(boolean condition, String label) {
        if (condition) {
            pass(label);
        } else {
            fail(label, "true", "false");
        }
    }

    public static void assertFalse(boolean condition, String label) {
        assertTrue(!condition, label);
    }

    public static void assertEquals(Object expected, Object actual, String label) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass(label);
        } else {
            fail(label, String.valueOf(expected), String.valueOf(actual));
        }
    }

    public static void assertEquals(int expected, int actual, String label) {
        assertEquals(Integer.valueOf(expected), Integer.valueOf(actual), label);
    }

    /** Doubles are compared with a tolerance because trip weights are accumulated sums. */
    public static void assertEquals(double expected, double actual, double tolerance, String label) {
        if (Math.abs(expected - actual) <= tolerance) {
            pass(label);
        } else {
            fail(label, String.valueOf(expected), String.valueOf(actual));
        }
    }

    public static void assertContains(String haystack, String needle, String label) {
        if (haystack != null && haystack.contains(needle)) {
            pass(label);
        } else {
            fail(label, "text containing \"" + needle + "\"", String.valueOf(haystack));
        }
    }

    /** Asserts that {@code action} throws {@code expectedType} (or a subtype). */
    public static void assertThrows(Class<? extends Throwable> expectedType, Runnable action, String label) {
        try {
            action.run();
        } catch (Throwable thrown) {
            if (expectedType.isInstance(thrown)) {
                pass(label);
            } else {
                fail(label, expectedType.getSimpleName(), thrown.getClass().getSimpleName());
            }
            return;
        }
        fail(label, expectedType.getSimpleName(), "nothing thrown");
    }

    private static void pass(String label) {
        passed++;
        System.out.println("  PASS  " + label);
    }

    private static void fail(String label, String expected, String actual) {
        failed++;
        System.out.println("  FAIL  " + label);
        System.out.println("          expected: " + expected);
        System.out.println("          actual:   " + actual);
    }

    public static int passedCount() {
        return passed;
    }

    public static int failedCount() {
        return failed;
    }
}
