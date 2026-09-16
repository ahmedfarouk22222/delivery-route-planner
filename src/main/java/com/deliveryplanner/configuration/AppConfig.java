package com.deliveryplanner.configuration;

public record AppConfig(String inputFile, String outputFile, double vehicleCapacityKg) {

    public static AppConfig from(ConfigReader reader) {
        String inputFile = reader.getRequired("input.file");
        String outputFile = reader.getRequired("output.file");

        double capacity;
        try {
            capacity = Double.parseDouble(reader.getRequired("vehicle.capacity"));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("vehicle.capacity must be a valid number", e);
        }

        if (capacity <= 0) {
            throw new IllegalStateException("vehicle.capacity must be positive, got: " + capacity);
        }

        return new AppConfig(inputFile, outputFile, capacity);
    }
}