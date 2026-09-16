package com.deliveryplanner;

/**
 * Entry point for the test suite. Runs every suite in turn and exits non-zero if
 * any assertion failed, so the run can be used as a build gate.
 */
public class TestRunner {

    public static void main(String[] args) {
        run("DeliveryPlannerTest", DeliveryPlannerTest::run);
        run("CsvDeliveryReaderTest", CsvDeliveryReaderTest::run);
        run("TripTest", TripTest::run);
        run("ReportGeneratorTest", ReportGeneratorTest::run);

        int passed = Assert.passedCount();
        int failed = Assert.failedCount();

        System.out.println();
        System.out.println("========================================");
        System.out.println(failed == 0
                ? "ALL TESTS PASSED (" + passed + " assertions)"
                : failed + " FAILED, " + passed + " passed");
        System.out.println("========================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void run(String name, Suite suite) {
        System.out.println();
        System.out.println(name);
        try {
            suite.run();
        } catch (Exception e) {
            // A suite that blows up mid-way must not be mistaken for a clean run.
            System.out.println("  ERROR  suite threw " + e.getClass().getSimpleName() + ": " + e.getMessage());
            Assert.assertTrue(false, name + " completed without throwing");
        }
    }

    @FunctionalInterface
    private interface Suite {
        void run() throws Exception;
    }
}
