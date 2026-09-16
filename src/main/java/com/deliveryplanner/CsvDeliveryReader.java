package com.deliveryplanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CsvDeliveryReader {

    private static final int EXPECTED_COLUMNS = 4;

    private final double vehicleCapacityKg;
    private final List<String> rejectedRows = new ArrayList<>();

    public CsvDeliveryReader(double vehicleCapacityKg) {
        this.vehicleCapacityKg = vehicleCapacityKg;
    }

    public List<Delivery> read(String filePath) {
        List<Delivery> deliveries = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();
        Path path = Path.of(filePath);

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            boolean firstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (CsvUtils.isBlankRow(line)) {
                    continue;
                }

                // firstLine means "first non-blank line", so a header still gets
                // detected when the file starts with one or more empty lines.
                if (firstLine) {
                    firstLine = false;
                    if (looksLikeHeader(line)) {
                        continue;
                    }
                }

                parseRow(line, lineNumber, seenIds, deliveries);
            }
        } catch (NoSuchFileException e) {
            throw new RuntimeException("Input file not found: " + filePath, e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read input file: " + filePath, e);
        }

        return deliveries;
    }

    private boolean looksLikeHeader(String line) {
        String normalized = line.trim().toLowerCase();
        return normalized.startsWith("id,area,priority,weight");
    }

    private void parseRow(String line, int lineNumber, Set<Integer> seenIds, List<Delivery> deliveries) {
        List<String> fields = CsvUtils.splitRow(line);

        if (!CsvUtils.hasExpectedColumnCount(fields, EXPECTED_COLUMNS)) {
            reject(lineNumber, line, "expected " + EXPECTED_COLUMNS + " columns, found " + fields.size());
            return;
        }

        if (CsvUtils.anyFieldEmpty(fields)) {
            reject(lineNumber, line, "one or more fields are empty");
            return;
        }

        int id;
        int priority;
        double weight;

        try {
            id = Integer.parseInt(fields.get(0));
        } catch (NumberFormatException e) {
            reject(lineNumber, line, "invalid id");
            return;
        }

        String area = fields.get(1);

        try {
            priority = Integer.parseInt(fields.get(2));
        } catch (NumberFormatException e) {
            reject(lineNumber, line, "invalid priority");
            return;
        }

        try {
            weight = Double.parseDouble(fields.get(3));
        } catch (NumberFormatException e) {
            reject(lineNumber, line, "invalid weight");
            return;
        }

        if (priority < 0) {
            reject(lineNumber, line, "priority must not be negative");
            return;
        }

        if (weight <= 0) {
            reject(lineNumber, line, "weight must be positive");
            return;
        }

        if (weight > vehicleCapacityKg) {
            reject(lineNumber, line, "weight " + weight + "kg exceeds vehicle capacity " + vehicleCapacityKg + "kg");
            return;
        }

        if (seenIds.contains(id)) {
            reject(lineNumber, line, "duplicate id " + id);
            return;
        }

        seenIds.add(id);
        deliveries.add(new Delivery(id, area, priority, weight));
    }

    private void reject(int lineNumber, String line, String reason) {
        rejectedRows.add("Line " + lineNumber + ": \"" + line + "\" -> " + reason);
    }

    public List<String> getRejectedRows() {
        return rejectedRows;
    }
}