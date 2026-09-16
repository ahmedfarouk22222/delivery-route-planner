package com.deliveryplanner;

public class TripTest {

    public static void run() {
        canFitAcceptsAnExactFill();
        canFitRejectsAnOverfill();
        addBeyondCapacityThrows();
        weightAndRemainingCapacityTrackAdds();
        deliveryListIsUnmodifiable();
        zeroCapacityUtilizationDoesNotDivideByZero();
        deliveryRecordValidatesItsFields();
    }

    /** The boundary that decides whether a trip can be filled to exactly 10.0 kg. */
    private static void canFitAcceptsAnExactFill() {
        Trip trip = new Trip(10.0);
        trip.add(new Delivery(1, "Maadi", 1, 6.0));

        Assert.assertTrue(trip.canFit(new Delivery(2, "Maadi", 1, 4.0)),
                "a delivery filling the trip exactly fits");
    }

    private static void canFitRejectsAnOverfill() {
        Trip trip = new Trip(10.0);
        trip.add(new Delivery(1, "Maadi", 1, 6.0));

        Assert.assertFalse(trip.canFit(new Delivery(2, "Maadi", 1, 4.1)),
                "a delivery exceeding remaining capacity does not fit");
    }

    private static void addBeyondCapacityThrows() {
        Trip trip = new Trip(10.0);
        trip.add(new Delivery(1, "Maadi", 1, 9.0));

        Assert.assertThrows(IllegalStateException.class,
                () -> trip.add(new Delivery(2, "Maadi", 1, 2.0)),
                "adding beyond capacity throws instead of silently overloading");
    }

    private static void weightAndRemainingCapacityTrackAdds() {
        Trip trip = new Trip(10.0);
        Assert.assertEquals(0.0, trip.getCurrentWeightKg(), 1e-9, "a new trip is empty");
        Assert.assertEquals(10.0, trip.getRemainingCapacityKg(), 1e-9, "a new trip has full capacity");

        trip.add(new Delivery(1, "Maadi", 1, 2.5));
        trip.add(new Delivery(2, "Zamalek", 1, 5.0));

        Assert.assertEquals(2, trip.getDeliveries().size(), "both deliveries are stored");
        Assert.assertEquals(7.5, trip.getCurrentWeightKg(), 1e-9, "weight is the sum of the deliveries");
        Assert.assertEquals(2.5, trip.getRemainingCapacityKg(), 1e-9, "remaining capacity shrinks");
        Assert.assertEquals(75.0, trip.getUtilizationPercent(), 1e-9, "utilization is weight over capacity");
    }

    private static void deliveryListIsUnmodifiable() {
        Trip trip = new Trip(10.0);
        trip.add(new Delivery(1, "Maadi", 1, 2.0));

        Assert.assertThrows(UnsupportedOperationException.class,
                () -> trip.getDeliveries().add(new Delivery(2, "Maadi", 1, 1.0)),
                "callers cannot bypass the capacity check by mutating the returned list");
    }

    private static void zeroCapacityUtilizationDoesNotDivideByZero() {
        Assert.assertEquals(0.0, new Trip(0.0).getUtilizationPercent(), 1e-9,
                "zero capacity reports 0% rather than NaN");
    }

    /** The record's own invariants, which protect callers other than the CSV reader. */
    private static void deliveryRecordValidatesItsFields() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> new Delivery(1, " ", 1, 2.0), "a blank area is rejected");
        Assert.assertThrows(IllegalArgumentException.class,
                () -> new Delivery(1, "Maadi", -1, 2.0), "a negative priority is rejected");
        Assert.assertThrows(IllegalArgumentException.class,
                () -> new Delivery(1, "Maadi", 1, 0.0), "a non-positive weight is rejected");
    }
}
