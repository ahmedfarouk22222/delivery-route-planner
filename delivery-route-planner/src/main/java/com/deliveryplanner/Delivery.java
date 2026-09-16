package com.deliveryplanner;

public record Delivery(int id, String area, int priority, double weightKg) {

    public Delivery {
        if (area == null || area.isBlank()) {
            throw new IllegalArgumentException("Area must not be blank");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("Priority must not be negative");
        }
        if (weightKg <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
    }
}