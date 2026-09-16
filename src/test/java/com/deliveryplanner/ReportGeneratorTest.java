package com.deliveryplanner;

import java.util.List;

public class ReportGeneratorTest {

    public static void run() {
        summaryCountsMatchTheTrips();
        rejectedRowsAppearInTheReport();
        cleanRunHasNoRejectedSection();
        emptyPlanReportsZeroesWithoutDividingByZero();
        reportUsesPlatformIndependentNewlines();
    }

    private static Trip tripOf(Delivery... deliveries) {
        Trip trip = new Trip(10.0);
        for (Delivery delivery : deliveries) {
            trip.add(delivery);
        }
        return trip;
    }

    private static void summaryCountsMatchTheTrips() {
        List<Trip> trips = List.of(
                tripOf(new Delivery(1, "Maadi", 1, 4.0), new Delivery(2, "Maadi", 1, 5.0)),
                tripOf(new Delivery(3, "Zamalek", 2, 3.0))
        );

        String report = new ReportGenerator().generate(trips, 3, List.of());

        Assert.assertContains(report, "Total deliveries:       3", "total count is reported");
        Assert.assertContains(report, "Valid deliveries:       3", "valid count is derived from the trips");
        Assert.assertContains(report, "Rejected deliveries:    0", "rejected count is reported");
        Assert.assertContains(report, "Total trips:            2", "trip count is reported");
        Assert.assertContains(report, "Total weight:           12.0 kg", "total weight is reported");
        Assert.assertContains(report, "Vehicle utilization:    60.0%", "fleet utilization is reported");
        Assert.assertContains(report, "ID 1   | Maadi      | Priority 1 | 4.0 kg", "each delivery is listed");
    }

    /**
     * The audit trail has to live in the report string itself, otherwise the written file shows a
     * rejection count with no way to find out which rows it refers to.
     */
    private static void rejectedRowsAppearInTheReport() {
        List<String> rejected = List.of(
                "Line 7: \"6,Giza,4,11.5\" -> weight 11.5kg exceeds vehicle capacity 10.0kg",
                "Line 8: \"7,,2,3\" -> one or more fields are empty"
        );

        String report = new ReportGenerator().generate(List.of(tripOf(new Delivery(1, "Maadi", 1, 4.0))), 3, rejected);

        Assert.assertContains(report, "Rejected deliveries:    2", "rejected count comes from the list");
        Assert.assertContains(report, "========== Rejected Rows ==========", "the audit block is present");
        Assert.assertContains(report, rejected.get(0), "the first rejection reason is included");
        Assert.assertContains(report, rejected.get(1), "the second rejection reason is included");
    }

    private static void cleanRunHasNoRejectedSection() {
        String report = new ReportGenerator().generate(List.of(tripOf(new Delivery(1, "Maadi", 1, 4.0))), 1, List.of());
        Assert.assertFalse(report.contains("Rejected Rows"), "a clean run omits the audit block entirely");
    }

    private static void emptyPlanReportsZeroesWithoutDividingByZero() {
        String report = new ReportGenerator().generate(List.of(), 0, List.of());

        Assert.assertContains(report, "Total trips:            0", "an empty plan reports zero trips");
        Assert.assertContains(report, "Average trip weight:    0.0 kg", "average weight is 0, not NaN");
        Assert.assertContains(report, "Vehicle utilization:    0.0%", "utilization is 0, not NaN");
        Assert.assertFalse(report.contains("NaN"), "no NaN leaks into the report");
    }

    private static void reportUsesPlatformIndependentNewlines() {
        String report = new ReportGenerator().generate(List.of(tripOf(new Delivery(1, "Maadi", 1, 4.0))), 1, List.of());
        Assert.assertFalse(report.contains("\r"), "the report contains no carriage returns");
    }
}
