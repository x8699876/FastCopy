package org.mhisoft.fc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mhisoft.fc.ui.ConsoleRdProUIImpl;

/**
 * Performance test for different file time setting implementations.
 * Compares current implementation vs recommended approaches.
 *
 * @author Tony Xue
 * @since Dec, 2025
 */
public class FileTimePerformanceTest {

    private ConsoleRdProUIImpl ui;
    private List<Path> testFiles;
    private static final int NUM_FILES = 1000;

    @Before
    public void setUp() throws Exception {
        ui = new ConsoleRdProUIImpl();
        testFiles = new ArrayList<>();

        // Create test files
        System.out.println("Creating " + NUM_FILES + " test files...");
        for (int i = 0; i < NUM_FILES; i++) {
            Path file = Files.createTempFile("perf_test_", ".txt");
            Files.write(file, ("Test content " + i).getBytes());
            testFiles.add(file);
        }
        System.out.println("Test files created.\n");
    }

    @After
    public void tearDown() throws Exception {
        // Clean up test files
        for (Path file : testFiles) {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void testCurrentImplementation_setTimesWithAccessTime() throws IOException {
        System.out.println("=== Test 1: Current Implementation (sets modified + access time) ===");
        long startTime = System.currentTimeMillis();

        for (Path file : testFiles) {
            // Current implementation: setTimes(time, time, null)
            Path tPath = Paths.get(file.toString());
            BasicFileAttributeView attributes = Files.getFileAttributeView(tPath, BasicFileAttributeView.class);
            FileTime time = FileTime.fromMillis(System.currentTimeMillis() - 86400000); // 1 day ago
            attributes.setTimes(time, time, null);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Files processed: " + NUM_FILES);
        System.out.println("Total time: " + duration + " ms");
        System.out.println("Average per file: " + String.format("%.3f", duration / (double) NUM_FILES) + " ms");
        System.out.println("Operations: 2 time attributes set (modified + access)");
        System.out.println();
    }

    @Test
    public void testRecommendedImplementation_setTimesModifiedOnly() throws IOException {
        System.out.println("=== Test 2: Recommended Implementation (sets modified time only) ===");
        long startTime = System.currentTimeMillis();

        for (Path file : testFiles) {
            // Recommended: setTimes(time, null, null)
            Path tPath = Paths.get(file.toString());
            BasicFileAttributeView attributes = Files.getFileAttributeView(tPath, BasicFileAttributeView.class);
            FileTime time = FileTime.fromMillis(System.currentTimeMillis() - 86400000); // 1 day ago
            attributes.setTimes(time, null, null);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Files processed: " + NUM_FILES);
        System.out.println("Total time: " + duration + " ms");
        System.out.println("Average per file: " + String.format("%.3f", duration / (double) NUM_FILES) + " ms");
        System.out.println("Operations: 1 time attribute set (modified only)");
        System.out.println();
    }

    @Test
    public void testSetFileAllTimes_ReadAndSetThreeTimes() throws IOException {
        System.out.println("=== Test 3: setFileAllTimes (reads source + sets 3 times) ===");

        // Create source files with specific times
        List<Path> sourceFiles = new ArrayList<>();
        for (int i = 0; i < NUM_FILES; i++) {
            Path source = Files.createTempFile("source_", ".txt");
            Files.write(source, ("Source " + i).getBytes());
            sourceFiles.add(source);
        }

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUM_FILES; i++) {
            Path sourcePath = sourceFiles.get(i);
            Path targetPath = testFiles.get(i);

            // Read all times from source
            BasicFileAttributes sourceAttrs = Files.readAttributes(sourcePath, BasicFileAttributes.class);

            // Set all times on target
            BasicFileAttributeView targetAttrs = Files.getFileAttributeView(targetPath, BasicFileAttributeView.class);
            targetAttrs.setTimes(
                    sourceAttrs.lastModifiedTime(),
                    sourceAttrs.lastAccessTime(),
                    sourceAttrs.creationTime()
            );
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Files processed: " + NUM_FILES);
        System.out.println("Total time: " + duration + " ms");
        System.out.println("Average per file: " + String.format("%.3f", duration / (double) NUM_FILES) + " ms");
        System.out.println("Operations: Read source attributes + set 3 time attributes");
        System.out.println();

        // Clean up source files
        for (Path source : sourceFiles) {
            Files.deleteIfExists(source);
        }
    }

    @Test
    public void testSetFileAllTimes_WithFileObject() throws IOException {
        System.out.println("=== Test 4: Current pattern (File.lastModified() + set 2 times) ===");

        long startTime = System.currentTimeMillis();

        for (Path file : testFiles) {
            // Current pattern: get modified time from File object, set modified + access
            long millis = file.toFile().lastModified();

            Path tPath = Paths.get(file.toString());
            BasicFileAttributeView attributes = Files.getFileAttributeView(tPath, BasicFileAttributeView.class);
            FileTime time = FileTime.fromMillis(millis);
            attributes.setTimes(time, time, null);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Files processed: " + NUM_FILES);
        System.out.println("Total time: " + duration + " ms");
        System.out.println("Average per file: " + String.format("%.3f", duration / (double) NUM_FILES) + " ms");
        System.out.println("Operations: File.lastModified() + set 2 time attributes");
        System.out.println();
    }

    @Test
    public void testComparisonAllMethods() throws IOException {
        System.out.println("=== Performance Comparison Summary ===\n");

        // Method 1: Current implementation (modified + access)
        long start1 = System.nanoTime();
        for (Path file : testFiles) {
            BasicFileAttributeView attributes = Files.getFileAttributeView(file, BasicFileAttributeView.class);
            FileTime time = FileTime.fromMillis(System.currentTimeMillis() - 86400000);
            attributes.setTimes(time, time, null);
        }
        long duration1 = System.nanoTime() - start1;

        // Method 2: Recommended (modified only)
        long start2 = System.nanoTime();
        for (Path file : testFiles) {
            BasicFileAttributeView attributes = Files.getFileAttributeView(file, BasicFileAttributeView.class);
            FileTime time = FileTime.fromMillis(System.currentTimeMillis() - 86400000);
            attributes.setTimes(time, null, null);
        }
        long duration2 = System.nanoTime() - start2;

        // Method 3: setFileAllTimes (read source + set 3 times)
        List<Path> sourceFiles = new ArrayList<>();
        for (int i = 0; i < NUM_FILES; i++) {
            Path source = Files.createTempFile("source_", ".txt");
            Files.write(source, "Test".getBytes());
            sourceFiles.add(source);
        }

        long start3 = System.nanoTime();
        for (int i = 0; i < NUM_FILES; i++) {
            BasicFileAttributes sourceAttrs = Files.readAttributes(sourceFiles.get(i), BasicFileAttributes.class);
            BasicFileAttributeView targetAttrs = Files.getFileAttributeView(testFiles.get(i), BasicFileAttributeView.class);
            targetAttrs.setTimes(
                    sourceAttrs.lastModifiedTime(),
                    sourceAttrs.lastAccessTime(),
                    sourceAttrs.creationTime()
            );
        }
        long duration3 = System.nanoTime() - start3;

        // Print results
        System.out.println("Method 1: Current (modified + access time)");
        System.out.println("  Total: " + duration1 / 1_000_000 + " ms");
        System.out.println("  Avg:   " + String.format("%.3f", duration1 / 1_000_000.0 / NUM_FILES) + " ms/file");
        System.out.println();

        System.out.println("Method 2: Recommended (modified only)");
        System.out.println("  Total: " + duration2 / 1_000_000 + " ms");
        System.out.println("  Avg:   " + String.format("%.3f", duration2 / 1_000_000.0 / NUM_FILES) + " ms/file");
        System.out.println("  Speedup: " + String.format("%.2f", duration1 / (double) duration2) + "x");
        System.out.println();

        System.out.println("Method 3: setFileAllTimes (read + set 3 times)");
        System.out.println("  Total: " + duration3 / 1_000_000 + " ms");
        System.out.println("  Avg:   " + String.format("%.3f", duration3 / 1_000_000.0 / NUM_FILES) + " ms/file");
        System.out.println("  Overhead: " + String.format("%.2f", duration3 / (double) duration1) + "x vs current");
        System.out.println();

        // Performance characteristics
        System.out.println("Performance Characteristics:");
        System.out.println("• Method 1 (current): Sets 2 attributes, ~baseline performance");
        System.out.println("• Method 2 (recommended): Sets 1 attribute, " +
                          String.format("%.0f%%", (1 - duration2 / (double) duration1) * 100) + " faster");
        System.out.println("• Method 3 (setFileAllTimes): Reads 3 + sets 3 attributes, " +
                          String.format("%.0f%%", (duration3 / (double) duration1 - 1) * 100) + " slower");
        System.out.println();

        System.out.println("Recommendation:");
        if (duration2 < duration1) {
            System.out.println("✓ Method 2 is fastest and most accurate - RECOMMENDED");
        }
        if (duration3 > duration1 * 1.5) {
            System.out.println("⚠ Method 3 has significant overhead - use only if all times needed");
        } else {
            System.out.println("✓ Method 3 overhead acceptable if full time preservation required");
        }

        // Clean up source files
        for (Path source : sourceFiles) {
            Files.deleteIfExists(source);
        }
    }

    @Test
    public void testMemoryFootprint() {
        System.out.println("=== Memory Footprint Analysis ===\n");

        Runtime runtime = Runtime.getRuntime();

        // Method 1: Current implementation
        runtime.gc();
        long memBefore1 = runtime.totalMemory() - runtime.freeMemory();
        try {
            for (Path file : testFiles) {
                BasicFileAttributeView attributes = Files.getFileAttributeView(file, BasicFileAttributeView.class);
                FileTime time = FileTime.fromMillis(System.currentTimeMillis());
                attributes.setTimes(time, time, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        runtime.gc();
        long memAfter1 = runtime.totalMemory() - runtime.freeMemory();

        // Method 2: setFileAllTimes
        runtime.gc();
        long memBefore2 = runtime.totalMemory() - runtime.freeMemory();
        try {
            for (Path file : testFiles) {
                BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                BasicFileAttributeView view = Files.getFileAttributeView(file, BasicFileAttributeView.class);
                view.setTimes(attrs.lastModifiedTime(), attrs.lastAccessTime(), attrs.creationTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        runtime.gc();
        long memAfter2 = runtime.totalMemory() - runtime.freeMemory();

        System.out.println("Current implementation memory delta: " +
                          String.format("%.2f", (memAfter1 - memBefore1) / 1024.0) + " KB");
        System.out.println("setFileAllTimes memory delta: " +
                          String.format("%.2f", (memAfter2 - memBefore2) / 1024.0) + " KB");
        System.out.println();
        System.out.println("Note: Memory measurements are approximate due to GC behavior");
    }
}
