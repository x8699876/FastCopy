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
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

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
}
