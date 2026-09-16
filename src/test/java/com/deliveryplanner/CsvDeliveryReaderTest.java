package com.deliveryplanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvDeliveryReaderTest {

    private static final double CAPACITY = 10.0;

    private static Path tempDir;

    public static void run() throws IOException {
        // Fixtures are written fresh rather than read from src/main/resources, so changing the
        // committed sample files can never silently change what these tests assert.
        tempDir = Files.createTempDirectory("delivery-reader-test");
        try {
            headerIsOptional();
            blankLinesAreIgnored();
            headerAfterBlankLineIsStillDetected();
            fieldsAreTrimmed();
            wrongColumnCountIsRejected();
            emptyFieldIsRejected();
            nonNumericFieldsAreRejected();
            negativePriorityIsRejected();
            nonPositiveWeightIsRejected();
            overCapacityWeightIsRejected();
            duplicateIdIsRejected();
            rejectionKeepsTheRestOfTheFile();
            rejectionNamesThePhysicalLineNumber();
            missingFileIsReported();
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private static void headerIsOptional() throws IOException {
        List<Delivery> withHeader = readLines("id,area,priority,weight", "1,Maadi,1,2.0");
        Assert.assertEquals(1, withHeader.size(), "header row is skipped");

        List<Delivery> withoutHeader = readLines("1,Maadi,1,2.0");
        Assert.assertEquals(1, withoutHeader.size(), "a file without a header still parses");

        List<Delivery> mixedCase = readLines("ID,Area,Priority,Weight", "1,Maadi,1,2.0");
        Assert.assertEquals(1, mixedCase.size(), "header detection is case-insensitive");
    }

    private static void blankLinesAreIgnored() throws IOException {
        List<Delivery> deliveries = readLines("1,Maadi,1,2.0", "", "   ", "2,Zamalek,1,3.0", "");
        Assert.assertEquals(2, deliveries.size(), "blank and whitespace-only lines are ignored");
    }

    /**
     * Regression: the header used to be matched against the first physical line, so a leading
     * blank line consumed the check and the real header was rejected as an invalid id.
     */
    private static void headerAfterBlankLineIsStillDetected() throws IOException {
        CsvDeliveryReader reader = new CsvDeliveryReader(CAPACITY);
        List<Delivery> deliveries = reader.read(write("", "id,area,priority,weight", "1,Maadi,1,2.0"));

        Assert.assertEquals(1, deliveries.size(), "header after a blank line is skipped, not parsed");
        Assert.assertEquals(0, reader.getRejectedRows().size(), "header after a blank line is not rejected");
    }

    private static void fieldsAreTrimmed() throws IOException {
        List<Delivery> deliveries = readLines("  1 ,  Maadi  , 1 , 2.0 ");
        Assert.assertEquals(1, deliveries.size(), "whitespace-padded row is accepted");
        Assert.assertEquals("Maadi", deliveries.get(0).area(), "area is trimmed");
    }

    private static void wrongColumnCountIsRejected() throws IOException {
        assertRejected("expected 4 columns, found 3", "column count is validated", "1,Maadi,1");
        assertRejected("expected 4 columns, found 5", "extra columns are rejected", "1,Maadi,1,2.0,extra");
    }

    private static void emptyFieldIsRejected() throws IOException {
        assertRejected("one or more fields are empty", "an empty area is rejected", "1,,1,2.0");
    }

    private static void nonNumericFieldsAreRejected() throws IOException {
        assertRejected("invalid id", "a non-numeric id is rejected", "abc,Maadi,1,2.0");
        assertRejected("invalid priority", "a non-numeric priority is rejected", "1,Maadi,high,2.0");
        assertRejected("invalid weight", "a non-numeric weight is rejected", "1,Maadi,1,heavy");
    }

    private static void negativePriorityIsRejected() throws IOException {
        assertRejected("priority must not be negative", "a negative priority is rejected", "1,Maadi,-1,2.0");
    }

    private static void nonPositiveWeightIsRejected() throws IOException {
        assertRejected("weight must be positive", "a zero weight is rejected", "1,Maadi,1,0");
        assertRejected("weight must be positive", "a negative weight is rejected", "1,Maadi,1,-2.0");
    }

    /** Over-capacity packages are rejected at read time rather than split, so the planner never stalls. */
    private static void overCapacityWeightIsRejected() throws IOException {
        assertRejected("exceeds vehicle capacity", "a package heavier than the vehicle is rejected",
                "1,Giza,1,11.5");

        List<Delivery> atCapacity = readLines("1,Giza,1,10.0");
        Assert.assertEquals(1, atCapacity.size(), "a package weighing exactly capacity is accepted");
    }

    private static void duplicateIdIsRejected() throws IOException {
        CsvDeliveryReader reader = new CsvDeliveryReader(CAPACITY);
        List<Delivery> deliveries = reader.read(write("1,Maadi,1,2.0", "1,Zamalek,2,3.0"));

        Assert.assertEquals(1, deliveries.size(), "only the first row with a given id is kept");
        Assert.assertEquals(1, reader.getRejectedRows().size(), "the duplicate is rejected");
        Assert.assertContains(reader.getRejectedRows().get(0), "duplicate id 1", "duplicate id names the id");
    }

    /** The audit-trail feature: a bad row must not abort the run. */
    private static void rejectionKeepsTheRestOfTheFile() throws IOException {
        CsvDeliveryReader reader = new CsvDeliveryReader(CAPACITY);
        List<Delivery> deliveries = reader.read(write(
                "1,Maadi,1,2.0",
                "2,,1,3.0",
                "3,Zamalek,1,4.0"
        ));

        Assert.assertEquals(2, deliveries.size(), "rows after a bad row are still read");
        Assert.assertEquals(1, reader.getRejectedRows().size(), "exactly one row is rejected");
    }

    private static void rejectionNamesThePhysicalLineNumber() throws IOException {
        CsvDeliveryReader reader = new CsvDeliveryReader(CAPACITY);
        reader.read(write("id,area,priority,weight", "1,Maadi,1,2.0", "", "bad,row,here,now"));

        Assert.assertEquals(1, reader.getRejectedRows().size(), "one row is rejected");
        Assert.assertContains(reader.getRejectedRows().get(0), "Line 4:",
                "the rejection points at the physical line, counting the header and blank line");
        Assert.assertContains(reader.getRejectedRows().get(0), "bad,row,here,now",
                "the rejection quotes the raw row");
    }

    private static void missingFileIsReported() {
        Path missing = tempDir.resolve("does-not-exist.csv");
        Assert.assertThrows(RuntimeException.class,
                () -> new CsvDeliveryReader(CAPACITY).read(missing.toString()),
                "a missing input file throws");
    }

    // --- helpers -------------------------------------------------------------

    private static List<Delivery> readLines(String... lines) throws IOException {
        return new CsvDeliveryReader(CAPACITY).read(write(lines));
    }

    /** Reads a single-row file and asserts it was rejected for the given reason. */
    private static void assertRejected(String expectedReason, String label, String row) throws IOException {
        CsvDeliveryReader reader = new CsvDeliveryReader(CAPACITY);
        List<Delivery> deliveries = reader.read(write(row));

        Assert.assertEquals(0, deliveries.size(), label + " (no delivery produced)");
        if (reader.getRejectedRows().size() == 1) {
            Assert.assertContains(reader.getRejectedRows().get(0), expectedReason, label);
        } else {
            Assert.assertEquals(1, reader.getRejectedRows().size(), label + " (one rejection recorded)");
        }
    }

    private static String write(String... lines) throws IOException {
        Path file = Files.createTempFile(tempDir, "deliveries", ".csv");
        Files.writeString(file, String.join("\n", lines) + "\n");
        return file.toString();
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (var paths = Files.walk(dir)) {
            for (Path path : paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
