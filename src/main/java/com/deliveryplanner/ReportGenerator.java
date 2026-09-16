package com.deliveryplanner;

import java.util.List;

public class ReportGenerator {

    public String generate(List<Trip> trips, int totalDeliveries, List<String> rejectedRows) {
        StringBuilder sb = new StringBuilder();

        int rejectedCount = rejectedRows.size();
        int validDeliveries = trips.stream().mapToInt(t -> t.getDeliveries().size()).sum();
        double totalWeight = trips.stream().mapToDouble(Trip::getCurrentWeightKg).sum();
        double totalCapacity = trips.stream().mapToDouble(Trip::getCapacityKg).sum();
        double unusedCapacity = totalCapacity - totalWeight;
        double avgTripWeight = trips.isEmpty() ? 0 : totalWeight / trips.size();
        double utilization = totalCapacity == 0 ? 0 : (totalWeight / totalCapacity) * 100.0;

        // Newlines are the literal \n rather than %n, so the written file has the same bytes
        // on every platform and two runs can be diffed against each other.
        sb.append("========== Delivery Summary ==========\n");
        sb.append(String.format("Total deliveries:       %d\n", totalDeliveries));
        sb.append(String.format("Valid deliveries:       %d\n", validDeliveries));
        sb.append(String.format("Rejected deliveries:    %d\n\n", rejectedCount));
        sb.append(String.format("Total trips:            %d\n", trips.size()));
        sb.append(String.format("Total weight:           %.1f kg\n", totalWeight));
        sb.append(String.format("Average trip weight:    %.1f kg\n\n", avgTripWeight));
        sb.append(String.format("Unused capacity:        %.1f kg\n", unusedCapacity));
        sb.append(String.format("Vehicle utilization:    %.1f%%\n", utilization));
        sb.append("=======================================\n\n");

        for (int i = 0; i < trips.size(); i++) {
            Trip trip = trips.get(i);
            sb.append(String.format("Trip %d\n", i + 1));
            sb.append("--------------------------------\n");
            for (Delivery d : trip.getDeliveries()) {
                sb.append(String.format("ID %-3d | %-10s | Priority %d | %.1f kg\n",
                        d.id(), d.area(), d.priority(), d.weightKg()));
            }
            sb.append(String.format("\nTotal weight:       %.1f kg\n", trip.getCurrentWeightKg()));
            sb.append(String.format("Remaining capacity: %.1f kg\n", trip.getRemainingCapacityKg()));
            sb.append(String.format("Utilization:        %.1f%%\n\n", trip.getUtilizationPercent()));
        }

        // The summary only says how many rows were rejected, so the reasons belong in the same
        // text — otherwise the written file shows a count nobody can act on.
        if (rejectedCount > 0) {
            sb.append("========== Rejected Rows ==========\n");
            for (String rejectedRow : rejectedRows) {
                sb.append(rejectedRow).append('\n');
            }
            sb.append("====================================\n\n");
        }

        return sb.toString();
    }
}
