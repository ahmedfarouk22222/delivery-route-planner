// src/main/java/com/deliveryplanner/DeliveryPlanner.java
package com.deliveryplanner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DeliveryPlanner {

    private final double vehicleCapacityKg;

    public DeliveryPlanner(double vehicleCapacityKg) {
        if (vehicleCapacityKg <= 0) {
            throw new IllegalArgumentException("Vehicle capacity must be positive");
        }
        this.vehicleCapacityKg = vehicleCapacityKg;
    }

    public List<Trip> plan(List<Delivery> deliveries) {
        List<Delivery> sorted = new ArrayList<>(deliveries);
        sorted.sort(
                Comparator.comparingInt(Delivery::priority)
                        .thenComparing(Delivery::area)
                        .thenComparing(Comparator.comparingDouble(Delivery::weightKg).reversed())
                        .thenComparingInt(Delivery::id)
        );

        List<Trip> trips = new ArrayList<>();

        for (Delivery delivery : sorted) {
            Trip targetTrip = findFirstTripWithRoom(trips, delivery);
            if (targetTrip == null) {
                targetTrip = new Trip(vehicleCapacityKg);
                trips.add(targetTrip);
            }
            targetTrip.add(delivery);
        }

        return trips;
    }

    private Trip findFirstTripWithRoom(List<Trip> trips, Delivery delivery) {
        for (Trip trip : trips) {
            if (trip.canFit(delivery)) {
                return trip;
            }
        }
        return null;
    }
}