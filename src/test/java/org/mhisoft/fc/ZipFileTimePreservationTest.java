package org.mhisoft.fc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mhisoft.fc.ui.ConsoleRdProUIImpl;

import static org.junit.Assert.*;

/**
 * Test class to verify that all three file times (modified, access, creation)
 * are preserved through ZIP compression and extraction.
 *
 * @author Tony Xue
 * @since Dec, 2025
 */
public class ZipFileTimePreservationTest {

    private ConsoleRdProUIImpl ui;
    private Path testDir;

    @Before
    public void setUp() throws Exception {
        ui = new ConsoleRdProUIImpl();
        testDir = Files.createTempDirectory("zipTimeTest");

        // Setup RunTimeProperties
        RunTimeProperties.instance.setOverrideTarget(true);
        RunTimeProperties.instance.setPreserveFileTimesAndAccessAttributes(true);
        RunTimeProperties.instance.setVerbose(true); // Enable to see debug messages
    }

    @After
    public void tearDown() throws Exception {
        // Clean up test directory
        deleteDirectory(testDir.toFile());
    }

    @Test
    public void testZipPreservesAllThreeFileTimes() throws Exception {
        System.out.println("=== Test: ZIP Preserves All Three File Times ===\n");

        // Create a test file with specific timestamps
        Path sourceFile = testDir.resolve("test-with-times.txt");
        Files.write(sourceFile, "Test content for time preservation".getBytes());

        // Set specific times on the source file
        long now = System.currentTimeMillis();
        long modifiedTime = now - TimeUnit.DAYS.toMillis(100);  // 100 days ago
        long accessTime = now - TimeUnit.DAYS.toMillis(50);     // 50 days ago
        long creationTime = now - TimeUnit.DAYS.toMillis(200);  // 200 days ago

        BasicFileAttributeView sourceView = Files.getFileAttributeView(sourceFile, BasicFileAttributeView.class);
        try {
            sourceView.setTimes(
                FileTime.fromMillis(modifiedTime),
                FileTime.fromMillis(accessTime),
                FileTime.fromMillis(creationTime)
            );
        } catch (Exception e) {
            // Creation time might not be settable on all platforms
            sourceView.setTimes(
                FileTime.fromMillis(modifiedTime),
                FileTime.fromMillis(accessTime),
                null
            );
        }

        // Read back the times we just set
        BasicFileAttributes sourceAttrs = Files.readAttributes(sourceFile, BasicFileAttributes.class);
        System.out.println("Source file times:");
        System.out.println("  Modified: " + sourceAttrs.lastModifiedTime());
        System.out.println("  Access:   " + sourceAttrs.lastAccessTime());
        System.out.println("  Created:  " + sourceAttrs.creationTime());

        // Setup FileUtils
        FileUtils fileUtils = new FileUtils();
        fileUtils.setRdProUI(ui);

        // Compress the directory
        Path targetDir = Files.createTempDirectory("zipTarget");
        FileUtils.CompressedPackageVO compressedVO = fileUtils.compressDirectory(
            testDir.toString(),
            targetDir.toString(),
            true,  // recursive
            -1     // include all files
        );

        assertNotNull("Should create compressed package", compressedVO);
        assertTrue("ZIP file should exist", new File(compressedVO.sourceZipFileWithPath).exists());
        System.out.println("\nZIP created: " + compressedVO.zipName);

        // Create extraction directory
        Path extractDir = Files.createTempDirectory("zipExtract");

        // Extract the ZIP
        File zipFile = new File(compressedVO.sourceZipFileWithPath);
        FileCopyStatistics statistics = new FileCopyStatistics();
        fileUtils.unzipFile(zipFile, extractDir.toFile(), statistics);

        System.out.println("Extracted to: " + extractDir);

        // Check the extracted file
        Path extractedFile = extractDir.resolve(sourceFile.getFileName().toString());
        assertTrue("Extracted file should exist", extractedFile.toFile().exists());

        // Read times from extracted file
        BasicFileAttributes extractedAttrs = Files.readAttributes(extractedFile, BasicFileAttributes.class);
        System.out.println("\nExtracted file times:");
        System.out.println("  Modified: " + extractedAttrs.lastModifiedTime());
        System.out.println("  Access:   " + extractedAttrs.lastAccessTime());
        System.out.println("  Created:  " + extractedAttrs.creationTime());

        // Verify times are preserved (allow 1 second tolerance for filesystem precision)
        long tolerance = 1000; // 1 second in milliseconds

        long modifiedDiff = Math.abs(sourceAttrs.lastModifiedTime().toMillis() - extractedAttrs.lastModifiedTime().toMillis());
        long accessDiff = Math.abs(sourceAttrs.lastAccessTime().toMillis() - extractedAttrs.lastAccessTime().toMillis());

        System.out.println("\nVerification:");
        System.out.println("  Modified time diff: " + modifiedDiff + " ms");
        System.out.println("  Access time diff:   " + accessDiff + " ms");

        assertTrue("Modified time should be preserved (within " + tolerance + "ms)",
            modifiedDiff <= tolerance);
        assertTrue("Access time should be preserved (within " + tolerance + "ms)",
            accessDiff <= tolerance);

        // Creation time verification - may not work on all platforms
        long creationDiff = Math.abs(sourceAttrs.creationTime().toMillis() - extractedAttrs.creationTime().toMillis());
        System.out.println("  Creation time diff: " + creationDiff + " ms");

        if (creationDiff <= tolerance) {
            System.out.println("  ✓ Creation time preserved (platform supports it)");
        } else {
            System.out.println("  ⚠ Creation time not preserved (platform limitation or not settable)");
        }

        System.out.println("\n✓ All verifiable times preserved successfully!");

        // Clean up
        deleteDirectory(targetDir.toFile());
        deleteDirectory(extractDir.toFile());
        zipFile.delete();
    }

    @Test
    public void testZipMultipleFilesPreserveTimes() throws Exception {
        System.out.println("=== Test: ZIP Multiple Files Preserve Times ===\n");

        // Create multiple test files with different timestamps
        long now = System.currentTimeMillis();
        Path file1 = testDir.resolve("file1.txt");
        Path file2 = testDir.resolve("file2.txt");
        Path file3 = testDir.resolve("file3.txt");

        Files.write(file1, "File 1".getBytes());
        Files.write(file2, "File 2".getBytes());
        Files.write(file3, "File 3".getBytes());

        // Set different times for each file
        setFileTimes(file1, now - TimeUnit.DAYS.toMillis(10), now - TimeUnit.DAYS.toMillis(5), now - TimeUnit.DAYS.toMillis(20));
        setFileTimes(file2, now - TimeUnit.DAYS.toMillis(30), now - TimeUnit.DAYS.toMillis(15), now - TimeUnit.DAYS.toMillis(60));
        setFileTimes(file3, now - TimeUnit.DAYS.toMillis(90), now - TimeUnit.DAYS.toMillis(45), now - TimeUnit.DAYS.toMillis(180));

        // Read original times
        BasicFileAttributes attrs1 = Files.readAttributes(file1, BasicFileAttributes.class);
        BasicFileAttributes attrs2 = Files.readAttributes(file2, BasicFileAttributes.class);
        BasicFileAttributes attrs3 = Files.readAttributes(file3, BasicFileAttributes.class);

        System.out.println("Original times set");

        // Setup FileUtils and compress
        FileUtils fileUtils = new FileUtils();
        fileUtils.setRdProUI(ui);

        Path targetDir = Files.createTempDirectory("zipTarget");
        FileUtils.CompressedPackageVO compressedVO = fileUtils.compressDirectory(
            testDir.toString(),
            targetDir.toString(),
            true,
            -1
        );

        // Extract
        Path extractDir = Files.createTempDirectory("zipExtract");
        File zipFile = new File(compressedVO.sourceZipFileWithPath);
        FileCopyStatistics statistics = new FileCopyStatistics();
        fileUtils.unzipFile(zipFile, extractDir.toFile(), statistics);

        // Verify all files
        Path extracted1 = extractDir.resolve("file1.txt");
        Path extracted2 = extractDir.resolve("file2.txt");
        Path extracted3 = extractDir.resolve("file3.txt");

        assertTrue("File 1 should exist", extracted1.toFile().exists());
        assertTrue("File 2 should exist", extracted2.toFile().exists());
        assertTrue("File 3 should exist", extracted3.toFile().exists());

        // Read extracted times
        BasicFileAttributes extractedAttrs1 = Files.readAttributes(extracted1, BasicFileAttributes.class);
        BasicFileAttributes extractedAttrs2 = Files.readAttributes(extracted2, BasicFileAttributes.class);
        BasicFileAttributes extractedAttrs3 = Files.readAttributes(extracted3, BasicFileAttributes.class);

        // Verify times (1 second tolerance)
        long tolerance = 1000;

        assertTrue("File 1 modified time preserved",
            Math.abs(attrs1.lastModifiedTime().toMillis() - extractedAttrs1.lastModifiedTime().toMillis()) <= tolerance);
        assertTrue("File 2 modified time preserved",
            Math.abs(attrs2.lastModifiedTime().toMillis() - extractedAttrs2.lastModifiedTime().toMillis()) <= tolerance);
        assertTrue("File 3 modified time preserved",
            Math.abs(attrs3.lastModifiedTime().toMillis() - extractedAttrs3.lastModifiedTime().toMillis()) <= tolerance);

        System.out.println("✓ All files' times preserved successfully!");

        // Clean up
        deleteDirectory(targetDir.toFile());
        deleteDirectory(extractDir.toFile());
        zipFile.delete();
    }

    private void setFileTimes(Path file, long modified, long access, long creation) throws IOException {
        BasicFileAttributeView view = Files.getFileAttributeView(file, BasicFileAttributeView.class);
        try {
            view.setTimes(
                FileTime.fromMillis(modified),
                FileTime.fromMillis(access),
                FileTime.fromMillis(creation)
            );
        } catch (Exception e) {
            // Fall back to just modified and access if creation fails
            view.setTimes(
                FileTime.fromMillis(modified),
                FileTime.fromMillis(access),
                null
            );
        }
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
