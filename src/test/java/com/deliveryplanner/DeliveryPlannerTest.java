package com.deliveryplanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeliveryPlannerTest {

    private static final double CAPACITY = 10.0;

    public static void run() {
        emptyInputProducesNoTrips();
        briefSampleMatchesDocumentedPlan();
        firstFitRefillsAnEarlierTrip();
        everyDeliveryAppearsExactlyOnce();
        noTripExceedsCapacity();
        planIsIndependentOfInputOrder();
        samePriorityGroupsByArea();
        nonPositiveCapacityIsRejected();
    }

    /** The 5 rows from the assignment brief. */
    private static List<Delivery> briefSample() {
        return new ArrayList<>(List.of(
                new Delivery(1, "Nasr City", 2, 4.5),
                new Delivery(2, "Maadi", 1, 2.0),
                new Delivery(3, "Nasr City", 3, 1.2),
                new Delivery(4, "Zamalek", 1, 7.0),
                new Delivery(5, "Maadi", 2, 3.5)
        ));
    }

    /** The same rows with delivery 4 raised to 10 kg — the case that separates first-fit from next-fit. */
    private static List<Delivery> heavySample() {
        return new ArrayList<>(List.of(
                new Delivery(1, "Nasr City", 2, 4.5),
                new Delivery(2, "Maadi", 1, 2.0),
                new Delivery(3, "Nasr City", 3, 1.2),
                new Delivery(4, "Zamalek", 1, 10.0),
                new Delivery(5, "Maadi", 2, 3.5)
        ));
    }

    private static void emptyInputProducesNoTrips() {
        List<Trip> trips = new DeliveryPlanner(CAPACITY).plan(List.of());
        Assert.assertEquals(0, trips.size(), "empty input produces zero trips");
    }

    /** Pins the exact plan the README documents as the example run. */
    private static void briefSampleMatchesDocumentedPlan() {
        List<Trip> trips = new DeliveryPlanner(CAPACITY).plan(briefSample());

        Assert.assertEquals(2, trips.size(), "brief sample plans into 2 trips");
        Assert.assertEquals("[2, 4]", idsOf(trips.get(0)), "trip 1 carries both priority-1 deliveries");
        Assert.assertEquals("[5, 1, 3]", idsOf(trips.get(1)), "trip 2 carries the rest in priority order");
        Assert.assertEquals(9.0, trips.get(0).getCurrentWeightKg(), 1e-9, "trip 1 weighs 9.0 kg");
        Assert.assertEquals(9.2, trips.get(1).getCurrentWeightKg(), 1e-9, "trip 2 weighs 9.2 kg");
    }

    /**
     * Delivery 3 arrives last and no longer fits trip 1, but first-fit still places deliveries 5
     * and 1 back into trip 1 after the 10 kg delivery forced trip 2 open. Next-fit would have
     * abandoned trip 1 at 2.0 kg.
     */
    private static void firstFitRefillsAnEarlierTrip() {
        List<Trip> trips = new DeliveryPlanner(CAPACITY).plan(heavySample());

        Assert.assertEquals(3, trips.size(), "heavy sample plans into 3 trips");
        Assert.assertEquals("[2, 5, 1]", idsOf(trips.get(0)), "first-fit refills trip 1 after opening trip 2");
        Assert.assertEquals(10.0, trips.get(0).getCurrentWeightKg(), 1e-9, "trip 1 is filled to exactly capacity");
        Assert.assertEquals("[4]", idsOf(trips.get(1)), "the 10 kg delivery gets a trip of its own");
        Assert.assertEquals("[3]", idsOf(trips.get(2)), "the leftover opens a third trip");
    }

    /** The assignment's core invariant: every valid delivery in exactly one trip. */
    private static void everyDeliveryAppearsExactlyOnce() {
        List<Delivery> input = heavySample();
        List<Trip> trips = new DeliveryPlanner(CAPACITY).plan(input);

        List<Integer> placed = new ArrayList<>();
        for (Trip trip : trips) {
            for (Delivery delivery : trip.getDeliveries()) {
                placed.add(delivery.id());
            }
        }

        Set<Integer> distinct = new HashSet<>(placed);
        Assert.assertEquals(input.size(), placed.size(), "no delivery is dropped or duplicated");
        Assert.assertEquals(input.size(), distinct.size(), "every placed delivery id is distinct");

        for (Delivery delivery : input) {
            Assert.assertTrue(distinct.contains(delivery.id()), "delivery " + delivery.id() + " was placed");
        }
    }

    private static void noTripExceedsCapacity() {
        List<Trip> trips = new DeliveryPlanner(CAPACITY).plan(heavySample());
        for (int i = 0; i < trips.size(); i++) {
            Assert.assertTrue(trips.get(i).getCurrentWeightKg() <= CAPACITY,
                    "trip " + (i + 1) + " stays within capacity");
        }
    }

    /**
     * The comparator falls through to the unique id, so no two deliveries ever compare equal and
     * the same set of deliveries always plans identically regardless of the order it arrives in.
     */
    private static void planIsIndependentOfInputOrder() {
        List<Trip> expected = new DeliveryPlanner(CAPACITY).plan(briefSample());

        List<Delivery> shuffled = briefSample();
        Collections.reverse(shuffled);
        List<Trip> actual = new DeliveryPlanner(CAPACITY).plan(shuffled);

        Assert.assertEquals(expected.size(), actual.size(), "reversed input produces the same trip count");
        for (int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(idsOf(expected.get(i)), idsOf(actual.get(i)),
                    "reversed input produces identical trip " + (i + 1));
        }
    }

    /** Within one priority tier, same-area deliveries are adjacent in the plan. */
    private static void samePriorityGroupsByArea() {
        List<Delivery> deliveries = List.of(
                new Delivery(1, "Zamalek", 1, 2.0),
                new Delivery(2, "Maadi", 1, 2.0),
                new Delivery(3, "Zamalek", 1, 2.0),
                new Delivery(4, "Maadi", 1, 2.0)
        );

        List<Trip> trips = new DeliveryPlanner(CAPACITY).plan(deliveries);
        Assert.assertEquals(1, trips.size(), "8 kg of deliveries fit one trip");
        Assert.assertEquals("[2, 4, 1, 3]", idsOf(trips.get(0)), "same-area deliveries are grouped together");
    }

    private static void nonPositiveCapacityIsRejected() {
        Assert.assertThrows(IllegalArgumentException.class, () -> new DeliveryPlanner(0),
                "zero capacity is rejected");
        Assert.assertThrows(IllegalArgumentException.class, () -> new DeliveryPlanner(-1),
                "negative capacity is rejected");
    }

    private static String idsOf(Trip trip) {
        List<Integer> ids = new ArrayList<>();
        for (Delivery delivery : trip.getDeliveries()) {
            ids.add(delivery.id());
        }
        return ids.toString();
    }
}
