package org.mhisoft.fc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mhisoft.fc.ui.ConsoleRdProUIImpl;

import static org.junit.Assert.*;

/**
 * Test class to verify that file permissions are preserved through ZIP compression
 * and extraction using compressDirectory and unzipFile methods.
 *
 * @author Tony Xue
 * @since Dec, 2025
 */
public class ZipFilePermissionPreservationTest {

    private ConsoleRdProUIImpl ui;
    private Path testDir;
    private FileUtils fileUtils;
    private boolean isPosixSupported;

    @Before
    public void setUp() throws Exception {
        ui = new ConsoleRdProUIImpl();
        testDir = Files.createTempDirectory("zipPermTest");

        // Setup RunTimeProperties
        RunTimeProperties.instance.setOverrideTarget(true);
        RunTimeProperties.instance.setPreserveFileTimesAndAccessAttributes(true);
        RunTimeProperties.instance.setVerbose(true); // Enable to see debug messages

        // Setup FileUtils
        fileUtils = new FileUtils();
        fileUtils.setRdProUI(ui);

        // Check if POSIX permissions are supported
        try {
            isPosixSupported = Files.getFileStore(testDir).supportsFileAttributeView("posix");
        } catch (Exception e) {
            isPosixSupported = false;
        }
    }

    @After
    public void tearDown() throws Exception {
        // Clean up test directory
        deleteDirectory(testDir.toFile());
    }

    @Test
    public void testZipPreservesExecutablePermissions() throws Exception {
        if (!isPosixSupported) {
            System.out.println("⚠ Skipping test - POSIX permissions not supported on this platform");
            return;
        }

        System.out.println("=== Test: ZIP Preserves Executable Permissions ===\n");

        // Create a test file with executable permissions
        Path sourceFile = testDir.resolve("executable-script.sh");
        Files.write(sourceFile, "#!/bin/bash\necho 'Hello World'\n".getBytes());

        // Set executable permissions (rwxr-xr-x = 755)
        Set<PosixFilePermission> execPerms = PosixFilePermissions.fromString("rwxr-xr-x");
        Files.setPosixFilePermissions(sourceFile, execPerms);

        System.out.println("Source file permissions: " + PosixFilePermissions.toString(execPerms));
        assertEquals("rwxr-xr-x", PosixFilePermissions.toString(Files.getPosixFilePermissions(sourceFile)));

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
        System.out.println("ZIP created: " + compressedVO.zipName);

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

        // Verify permissions are preserved
        Set<PosixFilePermission> extractedPerms = Files.getPosixFilePermissions(extractedFile);
        System.out.println("Extracted file permissions: " + PosixFilePermissions.toString(extractedPerms));

        assertEquals("Executable permissions should be preserved",
            PosixFilePermissions.toString(execPerms),
            PosixFilePermissions.toString(extractedPerms));

        // Verify file is executable
        assertTrue("File should be executable", extractedFile.toFile().canExecute());

        System.out.println("✓ Executable permissions preserved successfully!\n");

        // Clean up
        deleteDirectory(targetDir.toFile());
        deleteDirectory(extractDir.toFile());
    }

    @Test
    public void testZipPreservesReadOnlyPermissions() throws Exception {
        if (!isPosixSupported) {
            System.out.println("⚠ Skipping test - POSIX permissions not supported on this platform");
            return;
        }

        System.out.println("=== Test: ZIP Preserves Read-Only Permissions ===\n");

        // Create a test file with read-only permissions
        Path sourceFile = testDir.resolve("readonly-file.txt");
        Files.write(sourceFile, "Read-only content".getBytes());

        // Set read-only permissions (r--r--r-- = 444)
        Set<PosixFilePermission> readOnlyPerms = PosixFilePermissions.fromString("r--r--r--");
        Files.setPosixFilePermissions(sourceFile, readOnlyPerms);

        System.out.println("Source file permissions: " + PosixFilePermissions.toString(readOnlyPerms));

        // Compress the directory
        Path targetDir = Files.createTempDirectory("zipTarget");
        FileUtils.CompressedPackageVO compressedVO = fileUtils.compressDirectory(
            testDir.toString(),
            targetDir.toString(),
            true,  // recursive
            -1     // include all files
        );

        assertNotNull("Should create compressed package", compressedVO);

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

        // Verify permissions are preserved
        Set<PosixFilePermission> extractedPerms = Files.getPosixFilePermissions(extractedFile);
        System.out.println("Extracted file permissions: " + PosixFilePermissions.toString(extractedPerms));

        assertEquals("Read-only permissions should be preserved",
            PosixFilePermissions.toString(readOnlyPerms),
            PosixFilePermissions.toString(extractedPerms));

        // Verify file is not writable
        assertFalse("File should not be writable", extractedFile.toFile().canWrite());

        System.out.println("✓ Read-only permissions preserved successfully!\n");

        // Clean up
        deleteDirectory(targetDir.toFile());
        deleteDirectory(extractDir.toFile());
    }

    @Test
    public void testZipPreservesVariousPermissions() throws Exception {
        if (!isPosixSupported) {
            System.out.println("⚠ Skipping test - POSIX permissions not supported on this platform");
            return;
        }

        System.out.println("=== Test: ZIP Preserves Various Permission Combinations ===\n");

        // Create multiple files with different permissions
        Path file1 = testDir.resolve("file-755.txt");
        Path file2 = testDir.resolve("file-644.txt");
        Path file3 = testDir.resolve("file-600.txt");
        Path file4 = testDir.resolve("file-777.txt");

        Files.write(file1, "Content 755".getBytes());
        Files.write(file2, "Content 644".getBytes());
        Files.write(file3, "Content 600".getBytes());
        Files.write(file4, "Content 777".getBytes());

        // Set different permissions
        Set<PosixFilePermission> perms755 = PosixFilePermissions.fromString("rwxr-xr-x");
        Set<PosixFilePermission> perms644 = PosixFilePermissions.fromString("rw-r--r--");
        Set<PosixFilePermission> perms600 = PosixFilePermissions.fromString("rw-------");
        Set<PosixFilePermission> perms777 = PosixFilePermissions.fromString("rwxrwxrwx");

        Files.setPosixFilePermissions(file1, perms755);
        Files.setPosixFilePermissions(file2, perms644);
        Files.setPosixFilePermissions(file3, perms600);
        Files.setPosixFilePermissions(file4, perms777);

        System.out.println("Source file permissions:");
        System.out.println("  file-755.txt: " + PosixFilePermissions.toString(perms755));
        System.out.println("  file-644.txt: " + PosixFilePermissions.toString(perms644));
        System.out.println("  file-600.txt: " + PosixFilePermissions.toString(perms600));
        System.out.println("  file-777.txt: " + PosixFilePermissions.toString(perms777));

        // Compress the directory
        Path targetDir = Files.createTempDirectory("zipTarget");
        FileUtils.CompressedPackageVO compressedVO = fileUtils.compressDirectory(
            testDir.toString(),
            targetDir.toString(),
            true,  // recursive
            -1     // include all files
        );

        assertNotNull("Should create compressed package", compressedVO);
        System.out.println("\nZIP created: " + compressedVO.zipName);

        // Create extraction directory
        Path extractDir = Files.createTempDirectory("zipExtract");

        // Extract the ZIP
        File zipFile = new File(compressedVO.sourceZipFileWithPath);
        FileCopyStatistics statistics = new FileCopyStatistics();
        fileUtils.unzipFile(zipFile, extractDir.toFile(), statistics);

        System.out.println("Extracted to: " + extractDir + "\n");

        // Verify all files and their permissions
        Path extracted1 = extractDir.resolve("file-755.txt");
        Path extracted2 = extractDir.resolve("file-644.txt");
        Path extracted3 = extractDir.resolve("file-600.txt");
        Path extracted4 = extractDir.resolve("file-777.txt");

        assertTrue("File 1 should exist", extracted1.toFile().exists());
        assertTrue("File 2 should exist", extracted2.toFile().exists());
        assertTrue("File 3 should exist", extracted3.toFile().exists());
        assertTrue("File 4 should exist", extracted4.toFile().exists());

        Set<PosixFilePermission> extracted1Perms = Files.getPosixFilePermissions(extracted1);
        Set<PosixFilePermission> extracted2Perms = Files.getPosixFilePermissions(extracted2);
        Set<PosixFilePermission> extracted3Perms = Files.getPosixFilePermissions(extracted3);
        Set<PosixFilePermission> extracted4Perms = Files.getPosixFilePermissions(extracted4);

        System.out.println("Extracted file permissions:");
        System.out.println("  file-755.txt: " + PosixFilePermissions.toString(extracted1Perms));
        System.out.println("  file-644.txt: " + PosixFilePermissions.toString(extracted2Perms));
        System.out.println("  file-600.txt: " + PosixFilePermissions.toString(extracted3Perms));
        System.out.println("  file-777.txt: " + PosixFilePermissions.toString(extracted4Perms));

        assertEquals("File 755 permissions should be preserved",
            PosixFilePermissions.toString(perms755),
            PosixFilePermissions.toString(extracted1Perms));
        assertEquals("File 644 permissions should be preserved",
            PosixFilePermissions.toString(perms644),
            PosixFilePermissions.toString(extracted2Perms));
        assertEquals("File 600 permissions should be preserved",
            PosixFilePermissions.toString(perms600),
            PosixFilePermissions.toString(extracted3Perms));
        assertEquals("File 777 permissions should be preserved",
            PosixFilePermissions.toString(perms777),
            PosixFilePermissions.toString(extracted4Perms));

        System.out.println("\n✓ All permission combinations preserved successfully!\n");

        // Clean up
        deleteDirectory(targetDir.toFile());
        deleteDirectory(extractDir.toFile());
    }

    @Test
    public void testZipPreservesPermissionsInSubdirectories() throws Exception {
        if (!isPosixSupported) {
            System.out.println("⚠ Skipping test - POSIX permissions not supported on this platform");
            return;
        }

        System.out.println("=== Test: ZIP Preserves Permissions in Subdirectories ===\n");

        // Create subdirectory structure with files having different permissions
        Path subDir1 = testDir.resolve("subdir1");
        Path subDir2 = testDir.resolve("subdir1/subdir2");
        Files.createDirectories(subDir2);

        Path file1 = subDir1.resolve("script.sh");
        Path file2 = subDir2.resolve("config.conf");

        Files.write(file1, "#!/bin/bash\necho 'test'\n".getBytes());
        Files.write(file2, "config=value\n".getBytes());

        // Set different permissions
        Set<PosixFilePermission> execPerms = PosixFilePermissions.fromString("rwxr-xr-x");
        Set<PosixFilePermission> configPerms = PosixFilePermissions.fromString("rw-r-----");

        Files.setPosixFilePermissions(file1, execPerms);
        Files.setPosixFilePermissions(file2, configPerms);

        System.out.println("Source file permissions:");
        System.out.println("  subdir1/script.sh: " + PosixFilePermissions.toString(execPerms));
        System.out.println("  subdir1/subdir2/config.conf: " + PosixFilePermissions.toString(configPerms));

        // Compress the directory
        Path targetDir = Files.createTempDirectory("zipTarget");
        FileUtils.CompressedPackageVO compressedVO = fileUtils.compressDirectory(
            testDir.toString(),
            targetDir.toString(),
            true,  // recursive
            -1     // include all files
        );

        assertNotNull("Should create compressed package", compressedVO);
        System.out.println("\nZIP created: " + compressedVO.zipName);

        // Create extraction directory
        Path extractDir = Files.createTempDirectory("zipExtract");

        // Extract the ZIP
        File zipFile = new File(compressedVO.sourceZipFileWithPath);
        FileCopyStatistics statistics = new FileCopyStatistics();
        fileUtils.unzipFile(zipFile, extractDir.toFile(), statistics);

        System.out.println("Extracted to: " + extractDir + "\n");

        // Verify files in subdirectories and their permissions
        Path extractedFile1 = extractDir.resolve("subdir1/script.sh");
        Path extractedFile2 = extractDir.resolve("subdir1/subdir2/config.conf");

        assertTrue("Subdirectory file 1 should exist", extractedFile1.toFile().exists());
        assertTrue("Subdirectory file 2 should exist", extractedFile2.toFile().exists());

        Set<PosixFilePermission> extracted1Perms = Files.getPosixFilePermissions(extractedFile1);
        Set<PosixFilePermission> extracted2Perms = Files.getPosixFilePermissions(extractedFile2);

        System.out.println("Extracted file permissions:");
        System.out.println("  subdir1/script.sh: " + PosixFilePermissions.toString(extracted1Perms));
        System.out.println("  subdir1/subdir2/config.conf: " + PosixFilePermissions.toString(extracted2Perms));

        assertEquals("Subdirectory file 1 permissions should be preserved",
            PosixFilePermissions.toString(execPerms),
            PosixFilePermissions.toString(extracted1Perms));
        assertEquals("Subdirectory file 2 permissions should be preserved",
            PosixFilePermissions.toString(configPerms),
            PosixFilePermissions.toString(extracted2Perms));

        System.out.println("\n✓ Permissions in subdirectories preserved successfully!\n");

        // Clean up
        deleteDirectory(targetDir.toFile());
        deleteDirectory(extractDir.toFile());
    }

    @Test
    public void testWindowsFallbackPreservesExecutable() throws Exception {
        System.out.println("=== Test: Windows Fallback Preserves Executable Flag ===\n");

        // Create a test file
        Path sourceFile = testDir.resolve("test-executable.sh");
        Files.write(sourceFile, "#!/bin/bash\necho 'test'\n".getBytes());

        // Set executable flag (works on all platforms)
        sourceFile.toFile().setExecutable(true);
        boolean wasExecutable = sourceFile.toFile().canExecute();

        System.out.println("Source file executable: " + wasExecutable);

        if (!wasExecutable) {
            System.out.println("⚠ Skipping test - Cannot set executable flag on this platform");
            return;
        }

        // Compress the directory
        Path targetDir = Files.createTempDirectory("zipTarget");
        FileUtils.CompressedPackageVO compressedVO = fileUtils.compressDirectory(
            testDir.toString(),
            targetDir.toString(),
            true,  // recursive
            -1     // include all files
        );

        assertNotNull("Should create compressed package", compressedVO);
        System.out.println("ZIP created: " + compressedVO.zipName);

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

        // Verify executable flag is preserved
        boolean isExecutable = extractedFile.toFile().canExecute();
        System.out.println("Extracted file executable: " + isExecutable);

        assertTrue("Executable flag should be preserved", isExecutable);

        System.out.println("✓ Executable flag preserved successfully!\n");

        // Clean up
        deleteDirectory(targetDir.toFile());
        deleteDirectory(extractDir.toFile());
    }

    /**
     * Helper method to recursively delete a directory
     */
    private void deleteDirectory(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}
