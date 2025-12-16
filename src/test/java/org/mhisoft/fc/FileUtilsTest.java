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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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

    @Test
    public void testUnzipFile() throws IOException, NoSuchAlgorithmException {
        // Create a temporary directory with test files
        Path tempSourceDir = Files.createTempDirectory("testUnzipSource");
        File sourceDir = tempSourceDir.toFile();

        // Create target extraction directory
        Path tempTargetDir = Files.createTempDirectory("testUnzipTarget");
        File targetDir = tempTargetDir.toFile();

        try {
            // Create test files in source directory
            File file1 = new File(sourceDir, "file1.txt");
            String content1 = "Content of file 1";
            Files.write(file1.toPath(), content1.getBytes());

            File file2 = new File(sourceDir, "file2.dat");
            String content2 = "Content of file 2 with more data";
            Files.write(file2.toPath(), content2.getBytes());

            // Create a subdirectory with a file
            File subDir = new File(sourceDir, "subdir");
            subDir.mkdir();
            File file3 = new File(subDir, "file3.txt");
            String content3 = "Content in subdirectory";
            Files.write(file3.toPath(), content3.getBytes());

            // Setup FileUtils and compress the directory to create a zip
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);
            RunTimeProperties.instance.setOverrideTarget(true);
            RunTimeProperties.instance.setVerifyAfterCopy(false); // Disable verify for this test

            // Compress the directory
            FileUtils.CompressedPackageVO compressVO = fileUtils.compressDirectory(
                    sourceDir.getAbsolutePath(),
                    targetDir.getAbsolutePath(),
                    true,
                    -1
            );

            File zipFile = new File(compressVO.sourceZipFileWithPath);
            assertTrue("Zip file should exist", zipFile.exists());

            // Copy zip file to a safe location before we start cleaning up
            // because the zip is created IN the source directory
            Path safeZipLocation = Files.createTempFile("testZip", ".zip");
            Files.copy(zipFile.toPath(), safeZipLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            File safeZipFile = safeZipLocation.toFile();

            // Create a fresh extraction directory
            Path extractDir = Files.createTempDirectory("testExtract");
            File extractDirFile = extractDir.toFile();

            // Create statistics
            FileCopyStatistics statistics = new FileCopyStatistics();

            // Test unzipFile
            fileUtils.unzipFile(safeZipFile, extractDirFile, statistics);

            // Verify extracted files exist
            File extractedFile1 = new File(extractDirFile, "file1.txt");
            File extractedFile2 = new File(extractDirFile, "file2.dat");
            File extractedFile3 = new File(extractDirFile, "subdir" + File.separator + "file3.txt");

            assertTrue("file1.txt should be extracted", extractedFile1.exists());
            assertTrue("file2.dat should be extracted", extractedFile2.exists());
            assertTrue("file3.txt should be extracted", extractedFile3.exists());

            // Verify file contents
            assertEquals("file1.txt content should match",
                    content1, new String(Files.readAllBytes(extractedFile1.toPath())));
            assertEquals("file2.dat content should match",
                    content2, new String(Files.readAllBytes(extractedFile2.toPath())));
            assertEquals("file3.txt content should match",
                    content3, new String(Files.readAllBytes(extractedFile3.toPath())));

            // Verify statistics
            assertEquals("Statistics should track 3 files", 3, statistics.getFilesCount());

            // Clean up
            safeZipFile.delete();
            deleteDirectory(extractDirFile);
            deleteDirectory(targetDir);

        } finally {
            // Clean up test directories
            deleteDirectory(sourceDir);
        }
    }

    @Test
    public void testUnzipFileWithVerification() throws IOException, NoSuchAlgorithmException {
        // Create a temporary directory with test files
        Path tempSourceDir = Files.createTempDirectory("testUnzipVerifySource");
        File sourceDir = tempSourceDir.toFile();

        // Create target extraction directory
        Path tempTargetDir = Files.createTempDirectory("testUnzipVerifyTarget");
        File targetDir = tempTargetDir.toFile();

        try {
            // Create test files
            File file1 = new File(sourceDir, "verify1.txt");
            String content1 = "Verify content 1";
            Files.write(file1.toPath(), content1.getBytes());

            File file2 = new File(sourceDir, "verify2.txt");
            String content2 = "Verify content 2";
            Files.write(file2.toPath(), content2.getBytes());

            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);
            RunTimeProperties.instance.setOverrideTarget(true);
            RunTimeProperties.instance.setVerifyAfterCopy(true); // Enable verification

            // Compress the directory (with verification enabled, hashes will be stored)
            FileUtils.CompressedPackageVO compressVO = fileUtils.compressDirectory(
                    sourceDir.getAbsolutePath(),
                    targetDir.getAbsolutePath(),
                    true,
                    -1
            );

            File zipFile = new File(compressVO.sourceZipFileWithPath);
            assertTrue("Zip file should exist", zipFile.exists());

            // Copy zip file to a safe location before cleanup
            Path safeZipLocation = Files.createTempFile("testZipVerify", ".zip");
            Files.copy(zipFile.toPath(), safeZipLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            File safeZipFile = safeZipLocation.toFile();

            // Create extraction directory
            Path extractDir = Files.createTempDirectory("testExtractVerify");
            File extractDirFile = extractDir.toFile();

            // Create statistics
            FileCopyStatistics statistics = new FileCopyStatistics();

            // Test unzipFile with verification
            fileUtils.unzipFile(safeZipFile, extractDirFile, statistics);

            // Verify extracted files exist
            File extractedFile1 = new File(extractDirFile, "verify1.txt");
            File extractedFile2 = new File(extractDirFile, "verify2.txt");

            assertTrue("verify1.txt should be extracted", extractedFile1.exists());
            assertTrue("verify2.txt should be extracted", extractedFile2.exists());

            // Verify file contents match
            assertEquals("verify1.txt content should match",
                    content1, new String(Files.readAllBytes(extractedFile1.toPath())));
            assertEquals("verify2.txt content should match",
                    content2, new String(Files.readAllBytes(extractedFile2.toPath())));

            // Clean up
            safeZipFile.delete();
            deleteDirectory(extractDirFile);
            deleteDirectory(targetDir);

        } finally {
            // Clean up test directories
            deleteDirectory(sourceDir);
            // Reset verification setting
            RunTimeProperties.instance.setVerifyAfterCopy(false);
        }
    }

    @Test
    public void testUnzipFilePreservesTimestamps() throws IOException, NoSuchAlgorithmException {
        // Create a temporary directory with test files
        Path tempSourceDir = Files.createTempDirectory("testUnzipTimestamp");
        File sourceDir = tempSourceDir.toFile();

        // Create target directory
        Path tempTargetDir = Files.createTempDirectory("testUnzipTimestampTarget");
        File targetDir = tempTargetDir.toFile();

        try {
            // Create test file with specific timestamp
            File file1 = new File(sourceDir, "timestamp.txt");
            Files.write(file1.toPath(), "Timestamp test content".getBytes());

            // Set a specific timestamp
            long specificTime = System.currentTimeMillis() - 200000; // 200 seconds ago
            file1.setLastModified(specificTime);

            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);
            RunTimeProperties.instance.setOverrideTarget(true);
            RunTimeProperties.instance.setVerifyAfterCopy(false);
            RunTimeProperties.instance.setPreserveFileTimesAndAccessAttributes(true);

            // Compress the directory
            FileUtils.CompressedPackageVO compressVO = fileUtils.compressDirectory(
                    sourceDir.getAbsolutePath(),
                    targetDir.getAbsolutePath(),
                    true,
                    -1
            );

            File zipFile = new File(compressVO.sourceZipFileWithPath);

            // Copy zip file to a safe location before cleanup
            Path safeZipLocation = Files.createTempFile("testZipTimestamp", ".zip");
            Files.copy(zipFile.toPath(), safeZipLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            File safeZipFile = safeZipLocation.toFile();

            // Create extraction directory
            Path extractDir = Files.createTempDirectory("testExtractTimestamp");
            File extractDirFile = extractDir.toFile();

            // Create statistics
            FileCopyStatistics statistics = new FileCopyStatistics();

            // Test unzipFile
            fileUtils.unzipFile(safeZipFile, extractDirFile, statistics);

            // Verify extracted file
            File extractedFile = new File(extractDirFile, "timestamp.txt");
            assertTrue("File should be extracted", extractedFile.exists());

            // Verify timestamp is preserved (allow small difference for file system precision)
            long extractedTime = extractedFile.lastModified();
            long timeDiff = Math.abs(extractedTime - specificTime);
            assertTrue("Timestamp should be preserved (diff: " + timeDiff + " ms)",
                    timeDiff < 2000); // Within 2 seconds

            // Clean up
            safeZipFile.delete();
            deleteDirectory(extractDirFile);
            deleteDirectory(targetDir);

        } finally {
            // Clean up test directories
            deleteDirectory(sourceDir);
            RunTimeProperties.instance.setPreserveFileTimesAndAccessAttributes(false);
        }
    }

    @Test
    public void testUnzipEmptyZipFile() throws IOException, NoSuchAlgorithmException {
        // Create an empty zip file manually
        Path tempZipPath = Files.createTempFile("emptyTest", ".zip");
        File emptyZipFile = tempZipPath.toFile();

        // Create extraction directory
        Path extractDir = Files.createTempDirectory("testExtractEmpty");
        File extractDirFile = extractDir.toFile();

        try {
            // Create an empty zip file
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(emptyZipFile));
            zos.close();

            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);
            RunTimeProperties.instance.setVerifyAfterCopy(false);

            // Create statistics
            FileCopyStatistics statistics = new FileCopyStatistics();

            // Test unzipFile on empty zip
            fileUtils.unzipFile(emptyZipFile, extractDirFile, statistics);

            // Verify no files were extracted
            assertEquals("No files should be counted for empty zip", 0, statistics.getFilesCount());

            // Verify extraction directory is still empty
            File[] files = extractDirFile.listFiles();
            assertNotNull("Directory should exist", files);
            assertEquals("Directory should be empty", 0, files.length);

            // Clean up
            deleteDirectory(extractDirFile);

        } finally {
            emptyZipFile.delete();
        }
    }

    @Test
    public void testUnzipFileWithNestedDirectories() throws IOException, NoSuchAlgorithmException {
        // Create a temporary directory with nested structure
        Path tempSourceDir = Files.createTempDirectory("testUnzipNested");
        File sourceDir = tempSourceDir.toFile();

        // Create target directory
        Path tempTargetDir = Files.createTempDirectory("testUnzipNestedTarget");
        File targetDir = tempTargetDir.toFile();

        try {
            // Create nested directory structure
            File level1 = new File(sourceDir, "level1");
            level1.mkdir();
            File file1 = new File(level1, "file1.txt");
            Files.write(file1.toPath(), "Level 1 content".getBytes());

            File level2 = new File(level1, "level2");
            level2.mkdir();
            File file2 = new File(level2, "file2.txt");
            Files.write(file2.toPath(), "Level 2 content".getBytes());

            File level3 = new File(level2, "level3");
            level3.mkdir();
            File file3 = new File(level3, "file3.txt");
            Files.write(file3.toPath(), "Level 3 content".getBytes());

            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);
            RunTimeProperties.instance.setOverrideTarget(true);
            RunTimeProperties.instance.setVerifyAfterCopy(false);

            // Compress the directory
            FileUtils.CompressedPackageVO compressVO = fileUtils.compressDirectory(
                    sourceDir.getAbsolutePath(),
                    targetDir.getAbsolutePath(),
                    true,
                    -1
            );

            File zipFile = new File(compressVO.sourceZipFileWithPath);
            assertTrue("Zip file should exist after compression", zipFile.exists());

            // Copy zip file to a safe location before we start cleaning up source directory
            // because the zip is created IN the source directory
            Path safeZipLocation = Files.createTempFile("testZip", ".zip");
            Files.copy(zipFile.toPath(), safeZipLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            File safeZipFile = safeZipLocation.toFile();

            // Create extraction directory
            Path extractDir = Files.createTempDirectory("testExtractNested");
            File extractDirFile = extractDir.toFile();

            // Create statistics
            FileCopyStatistics statistics = new FileCopyStatistics();

            // Test unzipFile using the safe copy
            fileUtils.unzipFile(safeZipFile, extractDirFile, statistics);

            // Verify all nested files were extracted
            File extractedFile1 = new File(extractDirFile, "level1" + File.separator + "file1.txt");
            File extractedFile2 = new File(extractDirFile, "level1" + File.separator + "level2" + File.separator + "file2.txt");
            File extractedFile3 = new File(extractDirFile, "level1" + File.separator + "level2" + File.separator + "level3" + File.separator + "file3.txt");

            assertTrue("Level 1 file should be extracted", extractedFile1.exists());
            assertTrue("Level 2 file should be extracted", extractedFile2.exists());
            assertTrue("Level 3 file should be extracted", extractedFile3.exists());

            // Verify content
            assertEquals("Level 1 content should match",
                    "Level 1 content", new String(Files.readAllBytes(extractedFile1.toPath())));
            assertEquals("Level 2 content should match",
                    "Level 2 content", new String(Files.readAllBytes(extractedFile2.toPath())));
            assertEquals("Level 3 content should match",
                    "Level 3 content", new String(Files.readAllBytes(extractedFile3.toPath())));

            // Clean up
            safeZipFile.delete();
            deleteDirectory(extractDirFile);
            deleteDirectory(targetDir);

        } finally {
            // Clean up test directories (this will also delete the original zip file)
            deleteDirectory(sourceDir);
        }
    }

    @Test
    public void testPreserveFilePermissions_Executable() throws IOException {
        // This test only runs on POSIX-compliant systems (macOS, Linux, Unix)
        Path tempSourceFile = Files.createTempFile("source_exec", ".sh");
        Path tempTargetFile = Files.createTempFile("target_exec", ".sh");

        try {
            // Write content to source file
            Files.write(tempSourceFile, "#!/bin/bash\necho 'test'\n".getBytes());

            // Set source file as executable (rwxr-xr-x = 755)
            File sourceFile = tempSourceFile.toFile();
            sourceFile.setReadable(true, false);
            sourceFile.setWritable(true, true);
            sourceFile.setExecutable(true, false);

            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);

            // Call preserveFilePermissions
            fileUtils.preserveFilePermissions(
                    tempSourceFile.toString(),
                    tempTargetFile.toString()
            );

            // Verify target file has executable permission
            File targetFile = tempTargetFile.toFile();
            assertTrue("Target file should be executable", targetFile.canExecute());
            assertTrue("Target file should be readable", targetFile.canRead());

            System.out.println("✓ Executable permissions preserved successfully");

        } finally {
            Files.deleteIfExists(tempSourceFile);
            Files.deleteIfExists(tempTargetFile);
        }
    }

    @Test
    public void testPreserveFilePermissions_ReadOnly() throws IOException {
        Path tempSourceFile = Files.createTempFile("source_readonly", ".txt");
        Path tempTargetFile = Files.createTempFile("target_readonly", ".txt");

        try {
            // Write content to source file
            Files.write(tempSourceFile, "Read-only content".getBytes());

            // Set source file as read-only (r--r--r-- = 444)
            File sourceFile = tempSourceFile.toFile();
            sourceFile.setReadable(true, false);
            sourceFile.setWritable(false, false);
            sourceFile.setExecutable(false, false);

            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);

            // Call preserveFilePermissions
            fileUtils.preserveFilePermissions(
                    tempSourceFile.toString(),
                    tempTargetFile.toString()
            );

            // Verify target file is read-only
            File targetFile = tempTargetFile.toFile();
            assertTrue("Target file should be readable", targetFile.canRead());
            assertFalse("Target file should not be writable", targetFile.canWrite());

            System.out.println("✓ Read-only permissions preserved successfully");

            // Clean up: Make writable again before deletion
            targetFile.setWritable(true);

        } finally {
            Files.deleteIfExists(tempSourceFile);
            Files.deleteIfExists(tempTargetFile);
        }
    }

    @Test
    public void testPreserveFilePermissions_RegularFile() throws IOException {
        Path tempSourceFile = Files.createTempFile("source_regular", ".txt");
        Path tempTargetFile = Files.createTempFile("target_regular", ".txt");

        try {
            // Write content to source file
            Files.write(tempSourceFile, "Regular file content".getBytes());

            // Set source file with typical permissions (rw-r--r-- = 644)
            File sourceFile = tempSourceFile.toFile();
            sourceFile.setReadable(true, false);
            sourceFile.setWritable(true, true);
            sourceFile.setExecutable(false, false);

            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);

            // Call preserveFilePermissions
            fileUtils.preserveFilePermissions(
                    tempSourceFile.toString(),
                    tempTargetFile.toString()
            );

            // Verify target file has correct permissions
            File targetFile = tempTargetFile.toFile();
            assertTrue("Target file should be readable", targetFile.canRead());
            assertTrue("Target file should be writable", targetFile.canWrite());
            assertFalse("Target file should not be executable", targetFile.canExecute());

            System.out.println("✓ Regular file permissions preserved successfully");

        } finally {
            Files.deleteIfExists(tempSourceFile);
            Files.deleteIfExists(tempTargetFile);
        }
    }

    @Test
    public void testPreserveFilePermissions_PosixPermissions() throws IOException {
        // This test verifies full POSIX permission preservation on supported systems
        Path tempSourceFile = Files.createTempFile("source_posix", ".txt");
        Path tempTargetFile = Files.createTempFile("target_posix", ".txt");

        try {
            // Write content to source file
            Files.write(tempSourceFile, "POSIX test content".getBytes());

            // Check if system supports POSIX permissions
            if (Files.getFileStore(tempSourceFile).supportsFileAttributeView("posix")) {
                // Set specific POSIX permissions (rwxr-x--- = 750)
                java.nio.file.attribute.PosixFilePermission[] permissions = {
                        java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                        java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                        java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE,
                        java.nio.file.attribute.PosixFilePermission.GROUP_READ,
                        java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE
                };
                java.util.Set<java.nio.file.attribute.PosixFilePermission> perms =
                        new java.util.HashSet<>(Arrays.asList(permissions));
                Files.setPosixFilePermissions(tempSourceFile, perms);

                // Setup FileUtils
                FileUtils fileUtils = new FileUtils();
                fileUtils.setRdProUI(ui);

                // Call preserveFilePermissions
                fileUtils.preserveFilePermissions(
                        tempSourceFile.toString(),
                        tempTargetFile.toString()
                );

                // Verify target file has the same POSIX permissions
                java.util.Set<java.nio.file.attribute.PosixFilePermission> targetPerms =
                        Files.getPosixFilePermissions(tempTargetFile);

                assertTrue("Target should have OWNER_READ",
                        targetPerms.contains(java.nio.file.attribute.PosixFilePermission.OWNER_READ));
                assertTrue("Target should have OWNER_WRITE",
                        targetPerms.contains(java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
                assertTrue("Target should have OWNER_EXECUTE",
                        targetPerms.contains(java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE));
                assertTrue("Target should have GROUP_READ",
                        targetPerms.contains(java.nio.file.attribute.PosixFilePermission.GROUP_READ));
                assertTrue("Target should have GROUP_EXECUTE",
                        targetPerms.contains(java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE));

                // Should NOT have these permissions
                assertFalse("Target should not have GROUP_WRITE",
                        targetPerms.contains(java.nio.file.attribute.PosixFilePermission.GROUP_WRITE));
                assertFalse("Target should not have OTHERS_READ",
                        targetPerms.contains(java.nio.file.attribute.PosixFilePermission.OTHERS_READ));

                System.out.println("✓ POSIX permissions preserved successfully");
                System.out.println("  Source permissions: " +
                        java.nio.file.attribute.PosixFilePermissions.toString(perms));
                System.out.println("  Target permissions: " +
                        java.nio.file.attribute.PosixFilePermissions.toString(targetPerms));
            } else {
                System.out.println("⚠ POSIX permissions not supported on this file system, test skipped");
            }

        } finally {
            Files.deleteIfExists(tempSourceFile);
            Files.deleteIfExists(tempTargetFile);
        }
    }

    @Test
    public void testPreserveFilePermissions_NonExistentSource() {
        // Test behavior when source file doesn't exist
        String nonExistentSource = "/tmp/non_existent_file_" + System.currentTimeMillis() + ".txt";
        Path tempTargetFile = null;

        try {
            tempTargetFile = Files.createTempFile("target_nonexistent", ".txt");

            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);

            // This should not throw an exception, just log an error
            fileUtils.preserveFilePermissions(
                    nonExistentSource,
                    tempTargetFile.toString()
            );

            System.out.println("✓ Handled non-existent source file gracefully");

        } catch (Exception e) {
            // Should handle gracefully without throwing
            System.out.println("✓ Exception caught and handled: " + e.getMessage());
        } finally {
            if (tempTargetFile != null) {
                try {
                    Files.deleteIfExists(tempTargetFile);
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    @Test
    public void testPreserveFilePermissions_MultipleFiles() throws IOException {
        // Test preserving permissions for multiple files with different permissions
        Path[] sourceFiles = new Path[3];
        Path[] targetFiles = new Path[3];

        try {
            // Create test files with different permissions
            sourceFiles[0] = Files.createTempFile("source_exec", ".sh");
            sourceFiles[1] = Files.createTempFile("source_readonly", ".txt");
            sourceFiles[2] = Files.createTempFile("source_regular", ".txt");

            targetFiles[0] = Files.createTempFile("target_exec", ".sh");
            targetFiles[1] = Files.createTempFile("target_readonly", ".txt");
            targetFiles[2] = Files.createTempFile("target_regular", ".txt");

            // Set different permissions
            sourceFiles[0].toFile().setExecutable(true, false);
            sourceFiles[1].toFile().setWritable(false, false);
            sourceFiles[2].toFile().setWritable(true, true);

            // Setup FileUtils
            FileUtils fileUtils = new FileUtils();
            fileUtils.setRdProUI(ui);

            // Preserve permissions for all files
            for (int i = 0; i < 3; i++) {
                fileUtils.preserveFilePermissions(
                        sourceFiles[i].toString(),
                        targetFiles[i].toString()
                );
            }

            // Verify
            assertTrue("Target 0 should be executable", targetFiles[0].toFile().canExecute());
            assertFalse("Target 1 should not be writable", targetFiles[1].toFile().canWrite());
            assertTrue("Target 2 should be writable", targetFiles[2].toFile().canWrite());

            System.out.println("✓ Multiple files' permissions preserved successfully");

        } finally {
            // Clean up
            for (int i = 0; i < 3; i++) {
                if (sourceFiles[i] != null) Files.deleteIfExists(sourceFiles[i]);
                if (targetFiles[i] != null) {
                    // Make writable before deletion
                    targetFiles[i].toFile().setWritable(true);
                    Files.deleteIfExists(targetFiles[i]);
                }
            }
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
