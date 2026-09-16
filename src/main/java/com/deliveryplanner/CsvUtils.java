package com.deliveryplanner;

import java.util.ArrayList;
import java.util.List;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static List<String> splitRow(String line) {
        List<String> fields = new ArrayList<>();
        for (String raw : line.split(",", -1)) {
            fields.add(raw.trim());
        }
        return fields;
    }

    public static boolean isBlankRow(String line) {
        return line == null || line.trim().isEmpty();
    }

    public static boolean hasExpectedColumnCount(List<String> fields, int expectedCount) {
        return fields.size() == expectedCount;
    }

    public static boolean anyFieldEmpty(List<String> fields) {
        for (String field : fields) {
            if (field.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}