package com.deliveryplanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Trip {
    private final List<Delivery> deliveries = new ArrayList<>();
    private final double capacityKg;
    private double currentWeightKg = 0.0;

    public Trip(double capacityKg) {
        this.capacityKg = capacityKg;
    }
    public boolean canFit(Delivery delivery) {
        return currentWeightKg + delivery.weightKg() <= capacityKg;
    }
    public void add(Delivery delivery) {
        if (!canFit(delivery)) {
            throw new IllegalStateException("Delivery " + delivery.id() + " does not fit in this trip");
        }
        deliveries.add(delivery);
        currentWeightKg += delivery.weightKg();
    }
    public List<Delivery> getDeliveries() {
        return Collections.unmodifiableList(deliveries);
    }
    public double getCurrentWeightKg() {
        return currentWeightKg;
    }
    public double getRemainingCapacityKg() {
        return capacityKg - currentWeightKg;
    }
    public double getCapacityKg() {
        return capacityKg;
    }
    public double getUtilizationPercent() {
        return capacityKg == 0 ? 0 : (currentWeightKg / capacityKg) * 100.0;
    }
}