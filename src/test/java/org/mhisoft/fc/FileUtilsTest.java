/*
 *
 *  * Copyright (c) 2014- MHISoft LLC and/or its affiliates. All rights reserved.
 *  * Licensed to MHISoft LLC under one or more contributor
 *  * license agreements. See the NOTICE file distributed with
 *  * this work for additional information regarding copyright
 *  * ownership. MHISoft LLC licenses this file to you under
 *  * the Apache License, Version 2.0 (the "License"); you may
 *  * not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.mhisoft.fc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mhisoft.fc.ui.ConsoleRdProUIImpl;
import org.mhisoft.fc.utils.StrUtils;

import static org.junit.Assert.*;

/**
 * Unit tests for FileUtils
 *
 * @author Tony Xue
 * @since Dec, 2025
 */
public class FileUtilsTest {

    private File testFile;
    private ConsoleRdProUIImpl ui;

    @Before
    public void setUp() throws Exception {
        ui = new ConsoleRdProUIImpl();

        // Create a temporary test file
        testFile = File.createTempFile("testfile", ".tmp");

        // Write some content to the test file
        String testContent = "This is a test content for MD5 hash verification.";
        Files.write(testFile.toPath(), testContent.getBytes());
    }

    @After
    public void tearDown() throws Exception {
        // Clean up the test file
        if (testFile != null && testFile.exists()) {
            testFile.delete();
        }
    }

    @Test
    public void testReadFileContentHashAndHexConversion() throws NoSuchAlgorithmException, IOException {
        long t1 = System.currentTimeMillis();

        // Read file content hash
        byte[] md5Hash = FileUtils.readFileContentHash(testFile, ui);
        assertNotNull("MD5 hash should not be null", md5Hash);

        // Convert to hex string
        String hexString = StrUtils.toHexString(md5Hash);
        assertNotNull("Hex string should not be null", hexString);
        assertFalse("Hex string should not be empty", hexString.isEmpty());

        System.out.println("MD5 Hash (hex): " + hexString);
        System.out.println("Time taken: " + (System.currentTimeMillis() - t1) + " ms");

        // Verify that converting back from hex string gives the same byte array
        byte[] convertedBack = StrUtils.toByteArray(hexString);
        assertTrue("Converting hex back to byte array should match original",
                Arrays.equals(md5Hash, convertedBack));
    }

    @Test
    public void testGetHash() throws IOException {
        String testContent = "Test content for hash generation";
        byte[] contentBytes = testContent.getBytes();

        byte[] hash = FileUtils.getHash(contentBytes);
        assertNotNull("Hash should not be null", hash);
        assertEquals("MD5 hash should be 16 bytes", 16, hash.length);

        // Verify that same content produces same hash
        byte[] hash2 = FileUtils.getHash(contentBytes);
        assertTrue("Same content should produce same hash", Arrays.equals(hash, hash2));
    }

    @Test
    public void testReadFileContentHashConsistency() throws NoSuchAlgorithmException, IOException {
        // Read the same file twice and verify we get the same hash
        byte[] hash1 = FileUtils.readFileContentHash(testFile, ui);
        byte[] hash2 = FileUtils.readFileContentHash(testFile, ui);

        assertTrue("Reading the same file should produce the same hash",
                Arrays.equals(hash1, hash2));
    }

    @Test
    public void testCompressDirectory() throws IOException {
        // Create a temporary directory with test files
        Path tempDir = Files.createTempDirectory("testCompressDir");
        File testDir = tempDir.toFile();

        try {
            // Create test files in the directory
            File file1 = new File(testDir, "test1.txt");
            Files.write(file1.toPath(), "Test content 1".getBytes());

            File file2 = new File(testDir, "test2.txt");
            Files.write(file2.toPath(), "Test content 2 with more data".getBytes());

            File file3 = new File(testDir, "test3.dat");
            Files.write(file3.toPath(), "Binary test content".getBytes());

            // Create a subdirectory with a file
            File subDir = new File(testDir, "subdir");
            subDir.mkdir();
            File file4 = new File(subDir, "test4.txt");
            Files.write(file4.toPath(), "Test content in subdirectory".getBytes());

            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);

            // Setup RunTimeProperties
            RunTimeProperties.instance.setOverrideTarget(true);

            // Create target directory (not used in this case, but required by method)
            Path targetDir = Files.createTempDirectory("testTarget");

            // Test compress with all files (smallFileSizeThreashold = -1)
            FileUtils.CompressedPackageVO result = fileUtils.compressDirectory(
                    testDir.getAbsolutePath(),
                    targetDir.toFile().getAbsolutePath(),
                    false,  // recursive
                    -1     // include all files
            );

            // Verify the result
            assertNotNull("CompressedPackageVO should not be null", result);
            assertNotNull("Zip name should not be null", result.zipName);
            assertTrue("Zip name should start with prefix",
                    result.zipName.startsWith(RunTimeProperties.zip_prefix));
            assertTrue("Zip name should end with .zip", result.zipName.endsWith(".zip"));
            assertEquals("Original dirname should match",
                    testDir.getName(), result.originalDirname);
            assertEquals("Should have compressed 3 files", 3, result.getNumberOfFiles());
            assertTrue("Original directory last modified should be set",
                    result.originalDirLastModified > 0);

            // Verify the zip file was created
            File zipFile = new File(result.sourceZipFileWithPath);
            assertTrue("Zip file should exist", zipFile.exists());
            assertTrue("Zip file should have content", zipFile.length() > 0);

            // Verify the contents of the zip file
            try (ZipFile zip = new ZipFile(zipFile)) {
                assertEquals("Zip should contain 3 entries of the root folder", 3, zip.size());

                // Check for expected files
                assertNotNull("Should contain test1.txt", zip.getEntry("test1.txt"));
                assertNotNull("Should contain test2.txt", zip.getEntry("test2.txt"));
                assertNotNull("Should contain test3.dat", zip.getEntry("test3.dat"));
//                assertNotNull("Should contain subdir/test4.txt",
//                        zip.getEntry("subdir" + File.separator + "test4.txt"));
            }

            // Clean up the zip file
            zipFile.delete();

            // Clean up target directory
            deleteDirectory(targetDir.toFile());

        } finally {
            // Clean up test directory
            deleteDirectory(testDir);
        }
    }

    @Test
    public void testCompressDirectoryWithSizeThreshold() throws IOException {
        // Create a temporary directory with test files
        Path tempDir = Files.createTempDirectory("testCompressDirThreshold");
        File testDir = tempDir.toFile();

        try {
            // Create small test file (under threshold)
            File smallFile = new File(testDir, "small.txt");
            Files.write(smallFile.toPath(), "Small".getBytes());

            // Create large test file (over threshold)
            File largeFile = new File(testDir, "large.txt");
            byte[] largeContent = new byte[1000];
            Arrays.fill(largeContent, (byte) 'X');
            Files.write(largeFile.toPath(), largeContent);

            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);

            // Setup RunTimeProperties
            RunTimeProperties.instance.setOverrideTarget(true);

            // Create target directory
            Path targetDir = Files.createTempDirectory("testTargetThreshold");

            // Test compress with size threshold (only files <= 100 bytes)
            FileUtils.CompressedPackageVO result = fileUtils.compressDirectory(
                    testDir.getAbsolutePath(),
                    targetDir.toFile().getAbsolutePath(),
                    true,   // recursive
                    100     // only include files <= 100 bytes
            );

            // Verify the result
            assertNotNull("CompressedPackageVO should not be null", result);
            assertEquals("Should have compressed only 1 small file", 1, result.getNumberOfFiles());

            // Verify the zip file was created
            File zipFile = new File(result.sourceZipFileWithPath);
            assertTrue("Zip file should exist", zipFile.exists());

            // Verify the contents of the zip file
            try (ZipFile zip = new ZipFile(zipFile)) {
                assertEquals("Zip should contain 1 entry", 1, zip.size());
                assertNotNull("Should contain small.txt", zip.getEntry("small.txt"));
                assertNull("Should not contain large.txt", zip.getEntry("large.txt"));
            }

            // Clean up the zip file
            zipFile.delete();

            // Clean up target directory
            deleteDirectory(targetDir.toFile());

        } finally {
            // Clean up test directory
            deleteDirectory(testDir);
        }
    }

    @Test
    public void testCompressEmptyDirectory() throws IOException {
        // Create an empty temporary directory
        Path tempDir = Files.createTempDirectory("testCompressEmpty");
        File testDir = tempDir.toFile();

        try {
            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);

            // Setup RunTimeProperties
            RunTimeProperties.instance.setOverrideTarget(true);

            // Create target directory
            Path targetDir = Files.createTempDirectory("testTargetEmpty");

            // Test compress empty directory
            FileUtils.CompressedPackageVO result = fileUtils.compressDirectory(
                    testDir.getAbsolutePath(),
                    targetDir.toFile().getAbsolutePath(),
                    true,
                    -1
            );

            // Verify the result
            assertNotNull("CompressedPackageVO should not be null", result);
            assertEquals("Should have compressed 0 files", 0, result.getNumberOfFiles());

            // Verify the zip file was NOT created (empty directories are cleaned up)
            File zipFile = new File(result.sourceZipFileWithPath);
            assertFalse("Zip file should not exist for empty directory", zipFile.exists());

            // Clean up target directory
            deleteDirectory(targetDir.toFile());

        } finally {
            // Clean up test directory
            deleteDirectory(testDir);
        }
    }

    @Test
    public void testCompressDirectoryPreservesTimestamps() throws IOException, InterruptedException {
        // Create a temporary directory with test files
        Path tempDir = Files.createTempDirectory("testCompressTimestamp");
        File testDir = tempDir.toFile();

        try {
            // Create test file with specific timestamp
            File file1 = new File(testDir, "timestampTest.txt");
            Files.write(file1.toPath(), "Timestamp test".getBytes());

            // Set a specific last modified time
            long specificTime = System.currentTimeMillis() - 100000; // 100 seconds ago
            file1.setLastModified(specificTime);

            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);

            // Setup RunTimeProperties
            RunTimeProperties.instance.setOverrideTarget(true);

            // Create target directory
            Path targetDir = Files.createTempDirectory("testTargetTimestamp");

            // Compress the directory
            FileUtils.CompressedPackageVO result = fileUtils.compressDirectory(
                    testDir.getAbsolutePath(),
                    targetDir.toFile().getAbsolutePath(),
                    true,
                    -1
            );

            // Verify the zip file was created
            File zipFile = new File(result.sourceZipFileWithPath);
            assertTrue("Zip file should exist", zipFile.exists());

            // Verify the timestamp is preserved in the zip entry
            try (ZipFile zip = new ZipFile(zipFile)) {
                ZipEntry entry = zip.getEntry("timestampTest.txt");
                assertNotNull("Entry should exist", entry);

                long entryTime = entry.getLastModifiedTime().toMillis();
                // Allow small difference due to file system precision
                long timeDiff = Math.abs(entryTime - specificTime);
                assertTrue("Timestamp should be preserved (diff: " + timeDiff + " ms)",
                        timeDiff < 2000); // Within 2 seconds
            }

            // Clean up the zip file
            zipFile.delete();

            // Clean up target directory
            deleteDirectory(targetDir.toFile());

        } finally {
            // Clean up test directory
            deleteDirectory(testDir);
        }
    }

    /**
     * Helper method to recursively delete a directory
     */
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
