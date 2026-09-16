package com.deliveryplanner;

import com.deliveryplanner.configuration.AppConfig;
import com.deliveryplanner.configuration.ConfigReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    private static final String DEFAULT_CONFIG_PATH = "src/main/resources/config.properties";
    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG_PATH;
        AppConfig config;
        try {
            config = AppConfig.from(new ConfigReader(configPath));
        } catch (RuntimeException e) {
            System.err.println("Configuration error: " + e.getMessage());
            return;
        }
        CsvDeliveryReader reader = new CsvDeliveryReader(config.vehicleCapacityKg());
        List<Delivery> deliveries;
        try {
            deliveries = reader.read(config.inputFile());
        } catch (RuntimeException e) {
            System.err.println("Failed to read deliveries: " + e.getMessage());
            return;
        }
        if (deliveries.isEmpty() && reader.getRejectedRows().isEmpty()) {
            System.out.println("No deliveries found.");
            return;
        }
        if (deliveries.isEmpty()) {
            System.out.println("No valid deliveries found.");
        }

        DeliveryPlanner planner = new DeliveryPlanner(config.vehicleCapacityKg());
        List<Trip> trips = planner.plan(deliveries);
        int totalDeliveries = deliveries.size() + reader.getRejectedRows().size();
        ReportGenerator reportGenerator = new ReportGenerator();
        String report = reportGenerator.generate(trips, totalDeliveries, reader.getRejectedRows().size());
        System.out.println(report);

        if (!reader.getRejectedRows().isEmpty()) {
            System.out.println("========== Rejected Rows ==========");
            reader.getRejectedRows().forEach(System.out::println);
            System.out.println("====================================\n");
        }

        writeOutput(config.outputFile(), report);
    }
    private static void writeOutput(String outputPath, String content) {
        try {
            Path path = Path.of(outputPath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content);
        } catch (IOException e) {
            System.err.println("Failed to write output file: " + e.getMessage());
        }
    }
}