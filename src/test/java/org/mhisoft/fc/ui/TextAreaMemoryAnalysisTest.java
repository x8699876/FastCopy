/*
 * Copyright (c) 2014- MHISoft LLC and/or its affiliates. All rights reserved.
 * Licensed to MHISoft LLC under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. MHISoft LLC licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.mhisoft.fc.ui;

import javax.swing.JTextArea;

import org.junit.Test;

/**
 * Memory analysis test for JTextArea with bufferLineThreshold lines
 *
 * @author Tony Xue
 * @since Dec, 2025
 */
public class TextAreaMemoryAnalysisTest {

    @Test
    public void testMemoryUsageForBufferLineThreshold() {
        int bufferLineThreshold = GraphicsUIImpl.bufferLineThreshold; // 9999

        System.out.println("=== Memory Analysis for JTextArea ===");
        System.out.println("Buffer Line Threshold: " + bufferLineThreshold);
        System.out.println();

        // Test with different typical line lengths
        testScenario("Short lines (50 chars)", 50, bufferLineThreshold);
        testScenario("Medium lines (100 chars)", 100, bufferLineThreshold);
        testScenario("Long lines (200 chars)", 200, bufferLineThreshold);
        testScenario("Very long lines (500 chars)", 500, bufferLineThreshold);

        System.out.println("\n=== Actual Memory Test ===");
        measureActualMemoryUsage(100, bufferLineThreshold);
    }

    private void testScenario(String scenarioName, int avgCharsPerLine, int lineCount) {
        System.out.println("\n" + scenarioName + ":");

        // Calculate character storage
        long totalChars = (long) avgCharsPerLine * lineCount;

        // In Java, char is 2 bytes (UTF-16)
        long charMemoryBytes = totalChars * 2;

        // String overhead: approximately 24 bytes object header + 8 bytes for char[] reference
        // + 4 bytes for hash + 4 bytes for length = 40 bytes
        long stringOverhead = 40;

        // JTextArea internal structures overhead (Document, Elements, etc.)
        // Approximate: 100-200 bytes per line for Element objects and structure
        long lineOverhead = 150L * lineCount;

        // Total estimated memory
        long totalMemoryBytes = charMemoryBytes + stringOverhead + lineOverhead;

        System.out.println("  Average chars per line: " + avgCharsPerLine);
        System.out.println("  Total characters: " + String.format("%,d", totalChars));
        System.out.println("  Character storage: " + formatBytes(charMemoryBytes));
        System.out.println("  Line overhead: " + formatBytes(lineOverhead));
        System.out.println("  Total estimated memory: " + formatBytes(totalMemoryBytes));
    }

    private void measureActualMemoryUsage(int avgCharsPerLine, int lineCount) {
        // Force garbage collection
        System.gc();
        System.gc();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Create JTextArea and fill it
        JTextArea textArea = new JTextArea();
        StringBuilder testLine = new StringBuilder();
        for (int i = 0; i < avgCharsPerLine; i++) {
            testLine.append("x");
        }
        testLine.append("\n");

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < lineCount; i++) {
            textArea.append(testLine.toString());
        }
        long endTime = System.currentTimeMillis();

        // Force garbage collection again
        System.gc();
        System.gc();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = afterMemory - beforeMemory;

        System.out.println("\nActual measurement with " + avgCharsPerLine + " chars per line:");
        System.out.println("  Lines added: " + String.format("%,d", lineCount));
        System.out.println("  Time taken: " + (endTime - startTime) + " ms");
        System.out.println("  Actual memory used: " + formatBytes(memoryUsed));
        System.out.println("  Memory per line: " + formatBytes(memoryUsed / lineCount));

        // Calculate text content size
        String content = textArea.getText();
        long contentSize = content.length() * 2; // 2 bytes per char
        System.out.println("  Text content size: " + formatBytes(contentSize));
        System.out.println("  Overhead ratio: " + String.format("%.2f", (double) memoryUsed / contentSize));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB (%,d bytes)", bytes / 1024.0, bytes);
        } else {
            return String.format("%.2f MB (%,d bytes)", bytes / (1024.0 * 1024.0), bytes);
        }
    }

    @Test
    public void testTypicalFileCopyLogMessages() {
        System.out.println("\n=== Analysis of Typical Log Messages ===");

        // Typical messages from the code
        String[] typicalMessages = {
            "\tCopied file D:\\source\\folder\\subfolder\\filename.txt-->D:\\dest\\folder\\subfolder\\filename.txt, size:1,234,567 (bytes), took 123 ms. \n",
            "[PackageSmallFilesThread] Thread-1 Starts\n",
            "\tFile D:\\long\\path\\to\\file\\that\\exists.txt exists on the target dir, skipped. \n",
            "[warn]Cancelled by user. Stoping copying.\n",
            "Directory created: some_directory_name\n"
        };

        int totalLength = 0;
        for (String msg : typicalMessages) {
            totalLength += msg.length();
            System.out.println("Message length: " + msg.length() + " chars - " + msg.trim());
        }

        int avgLength = totalLength / typicalMessages.length;
        System.out.println("\nAverage message length: " + avgLength + " characters");

        // Calculate for bufferLineThreshold
        int bufferLineThreshold = GraphicsUIImpl.bufferLineThreshold;
        long totalChars = (long) avgLength * bufferLineThreshold;
        long estimatedMemory = totalChars * 2 + (150L * bufferLineThreshold);

        System.out.println("\nFor " + bufferLineThreshold + " lines at avg " + avgLength + " chars:");
        System.out.println("  Estimated memory: " + formatBytes(estimatedMemory));
    }
}
