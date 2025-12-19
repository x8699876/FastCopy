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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

import org.mhisoft.fc.ui.UI;
import org.mhisoft.fc.utils.StrUtils;

/**
 * Description:
 *
 * @author Tony Xue
 * @since Nov, 2014
 */
public class FileUtils {

	private static final int BUFFER = 4096 * 16;
	private static final int SMALL_FILE_SIZE = 20000;
	static final DecimalFormat df = new DecimalFormat("#,###.##");
	static final DecimalFormat dfLong = new DecimalFormat("#,###");
	UI rdProUI;

	public UI getRdProUI() {
		return rdProUI;
	}

	public void setRdProUI(UI rdProUI) {
		this.rdProUI = rdProUI;
	}

	public static FileUtils instance = new FileUtils();


	public void copyFile(final File source, final File target, FileCopyStatistics statistics, final UI rdProUI
			, final CompressedPackageVO compressedPackageVO) {

		CopyFileResultVO vo;
		try {
			if (source.length() < SMALL_FILE_SIZE) {
				vo = FileUtils.instance.copySmallFiles(source, target, statistics, rdProUI);
			} else
				vo = FileUtils.instance.nioBufferCopy(source, target, statistics, rdProUI);

		} catch (Exception e) {
			rdProUI.printError("Copy file failed for " + source.getAbsolutePath(), e);
			return;
		}

		rdProUI.showCurrentDir("Copying files under directory: " + source.getParent());


		if (RunTimeProperties.instance.isVerbose()) {
			if (source.length() < 4096)
				rdProUI.println(String.format("Copied file %s-->%s, size:%s (bytes), took %s. %s"
						, source.getAbsolutePath(), target.getAbsolutePath()
						, df.format(source.length())
						, StrUtils.getDisplayTime(vo.took)
						, vo.verified != null ? (vo.verified ? "Verified" : "Verify Error!") : ""

						)
				);
			else
				rdProUI.println(String.format("Copied file %s-->%s, size:%s (Kb), took %s. %s"
						, source.getAbsolutePath(), target.getAbsolutePath()
						, df.format(source.length() / 1024)
						, StrUtils.getDisplayTime(vo.took)
						, vo.verified != null ? (vo.verified ? "Verified" : "Verify Error!") : ""
						)
				);
		}
		if (vo.verified != null && !vo.verified) {
			rdProUI.printError("Verify copy of file failed:" + target.getAbsolutePath());
			//delete it.
			target.delete();

		}

		statistics.getBucket(source.length()).incrementFileCount();


		try {
			//exploded it  the target zip file on the dest dir
			if (compressedPackageVO != null) {


				try {
					//create destdir
					File destZipDir = new File(compressedPackageVO.getDestDir());
					//+File.separator + compressedackageVO.originalDirname);
					FileUtils.createDir(compressedPackageVO.originalDirLastModified, destZipDir, rdProUI, statistics);

					unzipFile(target, destZipDir, statistics);

					if (RunTimeProperties.instance.isVerbose()) {
						rdProUI.println("\tUnzipped under " + destZipDir + ",("
                                + compressedPackageVO.getNumberOfFiles()  + " files).");
					}

				} finally {
					//delete the source zip
					source.delete();

					//delete the target zip
					deleteFile(target.getAbsolutePath(), rdProUI);
				}


			}
		} catch (IOException | NoSuchAlgorithmException e) {
			rdProUI.printError("Exploding the zip failed", e);
		}

		if (compressedPackageVO == null) { // Not zipped
			try {
                preserveFileTImesAndAttributes(source, target);
            } catch (Exception e) {
				rdProUI.printError("Failed to preserve file attributes.", e);
			}
		}

	}

    private void preserveFileTImesAndAttributes(File source, File target) {
        // Preserve all file times and permissions
        if (RunTimeProperties.instance.isPreserveFileTimesAndAccessAttributes()) {
            preserveAllFileTimes(source.getAbsolutePath(), target.getAbsolutePath());
            preserveFilePermissions(source.getAbsolutePath(), target.getAbsolutePath());
        }
    }


    /**
	 * Preserve all three file times (modified, access, creation) from source to target.
	 * This provides complete timestamp preservation for accurate file copying.
	 * @param sourceFile the source file path
	 * @param targetFile the target file path
	 */
	public void preserveAllFileTimes(String sourceFile, String targetFile) {
		if (RunTimeProperties.instance.isPreserveFileTimesAndAccessAttributes()) {
			try {
				Path sourcePath = Paths.get(sourceFile);
				Path targetPath = Paths.get(targetFile);

				// Read all time attributes from source file
				BasicFileAttributes sourceAttrs = Files.readAttributes(sourcePath, BasicFileAttributes.class);

				// Get the attribute view for target file
				BasicFileAttributeView targetAttrs = Files.getFileAttributeView(targetPath, BasicFileAttributeView.class);

				// Set all three times: modified, access, and creation
				targetAttrs.setTimes(
					sourceAttrs.lastModifiedTime(),
					sourceAttrs.lastAccessTime(),
					sourceAttrs.creationTime()
				);

                if (RunTimeProperties.instance.isDebug()) {
					rdProUI.println(LogLevel.debug, "\tPreserved all file times for " + targetFile);
				}
			} catch (IOException e) {
				rdProUI.printError("\tFailed to preserve file times for " + targetFile + ": " + e.getMessage());
			}
		}

	}

	/**
	 * Set only the last modified time on a file.
	 * This is used when only a single timestamp is available (e.g., from ZIP entries).
	 * @param targetFile the target file path
	 * @param millis the last modified time in milliseconds
	 */
	public void setFileLastModified(String targetFile, long millis) {
		if (RunTimeProperties.instance.isPreserveFileTimesAndAccessAttributes()) {
			Path tPath = Paths.get(targetFile);
			BasicFileAttributeView attributes = Files.getFileAttributeView(tPath, BasicFileAttributeView.class);
			FileTime time = FileTime.fromMillis(millis);
			try {
				// Only set modified time, leave access and creation times unchanged
				attributes.setTimes(time, null, null);
			} catch (IOException e) {
				rdProUI.printError( "Failed to set last modified timestamp for " + targetFile);
			}
		}
	}

	/**
	 * Preserve file permissions including executable attributes from source to target.
	 * Works on POSIX-compliant systems (macOS, Linux, Unix).
	 * @param sourceFile the source file path
	 * @param targetFile the target file path
	 */
	public void preserveFilePermissions(String sourceFile, String targetFile) {
		try {
			Path sourcePath = Paths.get(sourceFile);
			Path targetPath = Paths.get(targetFile);

			// Check if the file system supports POSIX permissions (macOS, Linux, Unix)
			if (Files.getFileStore(sourcePath).supportsFileAttributeView("posix")) {
				// Get permissions from source file
				Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(sourcePath);

				// Set permissions on target file
				Files.setPosixFilePermissions(targetPath, permissions);

				if (RunTimeProperties.instance.isDebug()) {
					rdProUI.println(LogLevel.debug, "\tPreserved permissions for " + targetFile + ": " + PosixFilePermissions.toString(permissions));
				}
			} else {
				// Fallback for non-POSIX systems (Windows)
				File source = new File(sourceFile);
				File target = new File(targetFile);

				if (source.canExecute()) {
					target.setExecutable(true, false);
				}
				if (source.canRead()) {
					target.setReadable(true, false);
				}
				if (source.canWrite()) {
					target.setWritable(true, false);
				}
			}
		} catch (IOException e) {
			rdProUI.printError( "\tFailed to preserve permissions for " + targetFile + ": " + e.getMessage());
		} catch (UnsupportedOperationException e) {
			// POSIX not supported, silently ignore
			rdProUI.printError("\tFile permissions preservation not supported on this file system");
		}
	}

	/**
	 * Preserve file permissions from a ZIP entry to the extracted file.
	 * ZIP entries can store Unix file permissions in extra field with custom header 0x504D.
	 * @param zipEntry the ZIP entry containing permission information
	 * @param targetFile the extracted file to apply permissions to
	 */
	public void preserveZipEntryPermissions(ZipEntry zipEntry, File targetFile) {
		try {
			byte[] extraData = zipEntry.getExtra();

			if (extraData != null && extraData.length >= 8) {
				// Search for our custom header ID (0x504D = "PM") in the extra field
				int offset = 0;
				Integer unixPerms = null;

				while (offset + 8 <= extraData.length) {
					int headerId = (extraData[offset] & 0xFF) | ((extraData[offset + 1] & 0xFF) << 8);
					int dataSize = (extraData[offset + 2] & 0xFF) | ((extraData[offset + 3] & 0xFF) << 8);

					if (headerId == 0x504D && dataSize == 4 && offset + 8 <= extraData.length) {
						// Found our permission header, read unix mode (4 bytes, little-endian)
						unixPerms = (extraData[offset + 4] & 0xFF)
							| ((extraData[offset + 5] & 0xFF) << 8)
							| ((extraData[offset + 6] & 0xFF) << 16)
							| ((extraData[offset + 7] & 0xFF) << 24);
						break;
					}

					// Move to next extra field block
					offset += 4 + dataSize;
				}

				if (unixPerms != null && unixPerms != 0) {
					Path targetPath = targetFile.toPath();

					// Check if the file system supports POSIX permissions
					if (Files.getFileStore(targetPath).supportsFileAttributeView("posix")) {
						Set<PosixFilePermission> permissions = permissionsFromUnixMode(unixPerms);
						Files.setPosixFilePermissions(targetPath, permissions);

						if (RunTimeProperties.instance.isDebug()) {
							rdProUI.println(LogLevel.debug, "\tPreserved ZIP permissions for " + targetFile.getName() + ": " + PosixFilePermissions.toString(permissions));
						}
					} else {
						// Fallback for non-POSIX systems (Windows)
						// Check if owner has execute permission (bit 6)
						if ((unixPerms & 0100) != 0) {
							targetFile.setExecutable(true, false);
						}
						// Check if owner has read permission (bit 8)
						if ((unixPerms & 0400) != 0) {
							targetFile.setReadable(true, false);
						}
						// Check if owner has write permission (bit 7)
						if ((unixPerms & 0200) != 0) {
							targetFile.setWritable(true, false);
						}
					}
				}
			}
		} catch (Exception e) {
			rdProUI.printError( "Failed to preserve ZIP entry permissions for " + targetFile.getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Convert Unix permission mode (octal) to Java PosixFilePermission set.
	 * @param mode Unix permission mode (e.g., 0755, 0644)
	 * @return Set of PosixFilePermission
	 */
	private Set<PosixFilePermission> permissionsFromUnixMode(int mode) {
		Set<PosixFilePermission> permissions = new java.util.HashSet<>();

		// Owner permissions
		if ((mode & 0400) != 0) permissions.add(PosixFilePermission.OWNER_READ);
		if ((mode & 0200) != 0) permissions.add(PosixFilePermission.OWNER_WRITE);
		if ((mode & 0100) != 0) permissions.add(PosixFilePermission.OWNER_EXECUTE);

		// Group permissions
		if ((mode & 040) != 0) permissions.add(PosixFilePermission.GROUP_READ);
		if ((mode & 020) != 0) permissions.add(PosixFilePermission.GROUP_WRITE);
		if ((mode & 010) != 0) permissions.add(PosixFilePermission.GROUP_EXECUTE);

		// Others permissions
		if ((mode & 04) != 0) permissions.add(PosixFilePermission.OTHERS_READ);
		if ((mode & 02) != 0) permissions.add(PosixFilePermission.OTHERS_WRITE);
		if ((mode & 01) != 0) permissions.add(PosixFilePermission.OTHERS_EXECUTE);

		return permissions;
	}

	/**
	 * Store file permissions in a ZIP entry's extra field.
	 * This allows permissions to be preserved when the ZIP is extracted.
	 * Uses custom extra field format with header ID 0x504D ("PM" for Permission Mode).
	 * Format: Header ID (2 bytes) + Data Size (2 bytes) + Unix Mode (4 bytes)
	 * @param filePath the source file path
	 * @param zipEntry the ZIP entry to store permissions in
	 */
	private void storeFilePermissionsInZipEntry(Path filePath, ZipEntry zipEntry) {
		try {
			int unixMode = 0;

			// Check if the file system supports POSIX permissions
			if (Files.getFileStore(filePath).supportsFileAttributeView("posix")) {
				Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(filePath);
				unixMode = unixModeFromPermissions(permissions);

				if (RunTimeProperties.instance.isDebug()) {
					rdProUI.println(LogLevel.debug, "\tStoring permissions in ZIP for " + filePath.getFileName() + ": " + PosixFilePermissions.toString(permissions) + " (0" + Integer.toOctalString(unixMode) + ")");
				}
			} else {
				// Fallback for non-POSIX systems (Windows)
				File file = filePath.toFile();
				unixMode = 0644; // default readable/writable

				if (file.canExecute()) {
					unixMode = 0755; // executable
				}
			}

			// Create extra field data with custom header 0x504D ("PM")
			byte[] permExtraData = new byte[8]; // 2 (header) + 2 (size) + 4 (mode)
			permExtraData[0] = 0x4D; // 'M' (little-endian: 0x504D)
			permExtraData[1] = 0x50; // 'P'
			permExtraData[2] = 0x04; // data size = 4 bytes (low byte)
			permExtraData[3] = 0x00; // data size = 4 bytes (high byte)
			// Store unix mode as 4 bytes in little-endian
			permExtraData[4] = (byte) (unixMode & 0xFF);
			permExtraData[5] = (byte) ((unixMode >> 8) & 0xFF);
			permExtraData[6] = (byte) ((unixMode >> 16) & 0xFF);
			permExtraData[7] = (byte) ((unixMode >> 24) & 0xFF);

			// Append to existing extra field if present
			byte[] existingExtra = zipEntry.getExtra();
			if (existingExtra != null && existingExtra.length > 0) {
				byte[] combinedExtra = new byte[existingExtra.length + permExtraData.length];
				System.arraycopy(existingExtra, 0, combinedExtra, 0, existingExtra.length);
				System.arraycopy(permExtraData, 0, combinedExtra, existingExtra.length, permExtraData.length);
				zipEntry.setExtra(combinedExtra);
			} else {
				zipEntry.setExtra(permExtraData);
			}

		} catch (Exception e) {
			rdProUI.printError( "Failed to store permissions in ZIP entry for " + filePath.getFileName() + ": " + e.getMessage());
		}
	}

	/**
	 * Convert Java PosixFilePermission set to Unix permission mode (octal).
	 * @param permissions Set of PosixFilePermission
	 * @return Unix permission mode (e.g., 0755, 0644)
	 */
	private int unixModeFromPermissions(Set<PosixFilePermission> permissions) {
		int mode = 0;

		// Owner permissions
		if (permissions.contains(PosixFilePermission.OWNER_READ)) mode |= 0400;
		if (permissions.contains(PosixFilePermission.OWNER_WRITE)) mode |= 0200;
		if (permissions.contains(PosixFilePermission.OWNER_EXECUTE)) mode |= 0100;

		// Group permissions
		if (permissions.contains(PosixFilePermission.GROUP_READ)) mode |= 040;
		if (permissions.contains(PosixFilePermission.GROUP_WRITE)) mode |= 020;
		if (permissions.contains(PosixFilePermission.GROUP_EXECUTE)) mode |= 010;

		// Others permissions
		if (permissions.contains(PosixFilePermission.OTHERS_READ)) mode |= 04;
		if (permissions.contains(PosixFilePermission.OTHERS_WRITE)) mode |= 02;
		if (permissions.contains(PosixFilePermission.OTHERS_EXECUTE)) mode |= 01;

		return mode;
	}

	/**
	 * Store all three file times (modified, access, creation) in ZIP entry extra field.
	 * Uses proper ZIP extra field format with custom tag ID.
	 * Format: Header ID (2 bytes) + Data Size (2 bytes) + Data (24 bytes: 3 longs)
	 *
	 * @param filePath the source file path
	 * @param zipEntry the ZIP entry to store times in
	 */
	private void storeAllFileTimesInZipEntry(Path filePath, ZipEntry zipEntry) {
		try {
			// Read all three times from source file
			BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);

			long modifiedMillis = attrs.lastModifiedTime().toMillis();
			long accessMillis = attrs.lastAccessTime().toMillis();
			long creationMillis = attrs.creationTime().toMillis();

			// Create our custom extra field data
			// Format: Header ID (0x5449 = "TI") + Size (24) + Modified (8) + Access (8) + Creation (8)
			byte[] ourExtraData = new byte[28]; // 2 + 2 + 8 + 8 + 8

			// Header ID: 0x5449 ("TI" for Time Info) - custom tag
			ourExtraData[0] = 0x49;
			ourExtraData[1] = 0x54;

			// Data size: 24 bytes (3 longs)
			ourExtraData[2] = 24;
			ourExtraData[3] = 0;

			// Modified time (8 bytes)
			writeLong(ourExtraData, 4, modifiedMillis);

			// Access time (8 bytes)
			writeLong(ourExtraData, 12, accessMillis);

			// Creation time (8 bytes)
			writeLong(ourExtraData, 20, creationMillis);

			// Get existing extra field (may contain extended timestamp, etc.)
			byte[] existingExtra = zipEntry.getExtra();

			if (existingExtra != null && existingExtra.length > 0) {
				// Append our data to existing extra field
				byte[] combinedExtra = new byte[existingExtra.length + ourExtraData.length];
				System.arraycopy(existingExtra, 0, combinedExtra, 0, existingExtra.length);
				System.arraycopy(ourExtraData, 0, combinedExtra, existingExtra.length, ourExtraData.length);
				zipEntry.setExtra(combinedExtra);
			} else {
				// No existing extra field, use ours
				zipEntry.setExtra(ourExtraData);
			}

			if (RunTimeProperties.instance.isDebug()) {
				rdProUI.println(LogLevel.debug, "\tStored all file times in ZIP for " + filePath.getFileName());
		}
	} catch (Exception e) {
		rdProUI.printError("\tFailed to store file times in ZIP entry for " + filePath.getFileName() + ": " + e.getMessage());
	}
}

	/**
	 * Helper method to write a long value to a byte array in little-endian format.
	 */
	private void writeLong(byte[] buffer, int offset, long value) {
		buffer[offset] = (byte) (value & 0xFF);
		buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
		buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
		buffer[offset + 3] = (byte) ((value >> 24) & 0xFF);
		buffer[offset + 4] = (byte) ((value >> 32) & 0xFF);
		buffer[offset + 5] = (byte) ((value >> 40) & 0xFF);
		buffer[offset + 6] = (byte) ((value >> 48) & 0xFF);
		buffer[offset + 7] = (byte) ((value >> 56) & 0xFF);
	}

	/**
	 * Helper method to read a long value from a byte array in little-endian format.
	 */
	private long readLong(byte[] buffer, int offset) {
		return (buffer[offset] & 0xFFL)
			| ((buffer[offset + 1] & 0xFFL) << 8)
			| ((buffer[offset + 2] & 0xFFL) << 16)
			| ((buffer[offset + 3] & 0xFFL) << 24)
			| ((buffer[offset + 4] & 0xFFL) << 32)
			| ((buffer[offset + 5] & 0xFFL) << 40)
			| ((buffer[offset + 6] & 0xFFL) << 48)
			| ((buffer[offset + 7] & 0xFFL) << 56);
	}

	/**
	 * Restore all three file times from ZIP entry extra field.
	 * Reads the custom format stored by storeAllFileTimesInZipEntry.
	 * Falls back to using only lastModifiedTime if extra data not found.
	 *
	 * @param zipEntry the ZIP entry containing time data
	 * @param targetFile the extracted file to apply times to
	 */
	private void restoreAllFileTimesFromZipEntry(ZipEntry zipEntry, File targetFile) {
		try {
			byte[] extraData = zipEntry.getExtra();

			if (extraData != null && extraData.length >= 28) {
				// Search for our custom header ID (0x5449 = "TI") in the extra field
				// Extra field can contain multiple blocks with different header IDs
				int offset = 0;
				while (offset + 28 <= extraData.length) {
					int headerId = (extraData[offset] & 0xFF) | ((extraData[offset + 1] & 0xFF) << 8);
					int dataSize = (extraData[offset + 2] & 0xFF) | ((extraData[offset + 3] & 0xFF) << 8);

					// Check if this is our custom time info block (0x5449)
					if (headerId == 0x5449 && dataSize == 24) {
						// Read the three timestamps
						long modifiedMillis = readLong(extraData, offset + 4);
						long accessMillis = readLong(extraData, offset + 12);
						long creationMillis = readLong(extraData, offset + 20);

						// Apply all three times
						Path targetPath = targetFile.toPath();
						BasicFileAttributeView targetAttrs = Files.getFileAttributeView(targetPath, BasicFileAttributeView.class);

						targetAttrs.setTimes(
							FileTime.fromMillis(modifiedMillis),
							FileTime.fromMillis(accessMillis),
							FileTime.fromMillis(creationMillis)
						);

						if (RunTimeProperties.instance.isDebug()) {
							rdProUI.print(LogLevel.debug, "\tRestored all three file times for " + targetFile.getName());
						}
						return; // Success!
					}

					// Move to next block (header + size + data)
					offset += 4 + dataSize;
				}
			}

			// Fallback: Use only the standard ZIP lastModifiedTime
			setFileLastModified(targetFile.getAbsolutePath(), zipEntry.getTime());

	} catch (Exception e) {
		// If anything fails, fall back to basic timestamp
		rdProUI.printError("\tFailed to restore all file times for " + targetFile.getName() + ", using modified time only: " + e.getMessage());
		setFileLastModified(targetFile.getAbsolutePath(), zipEntry.getTime());
	}
	}

	public void deleteFile(String file, final UI rdProUI) {
		try {
			Files.deleteIfExists(Paths.get(file));
		} catch (NoSuchFileException e) {
			rdProUI.printError("Can not delete file:" + file + ", No such file/directory exists");
		} catch (DirectoryNotEmptyException e) {
			rdProUI.printError("Can not delete file:" + file + ",Directory is not empty.");
		} catch (IOException e) {
			rdProUI.printError("Can not delete file:" + file + ",Invalid permissions." + e.getMessage());
		}

	}


	public void showPercent(final UI rdProUI, double digital) {
		long p = (long) digital * 100;
		DecimalFormat df = new DecimalFormat("000");
		String s = df.format(p);

		rdProUI.printf("\u0008\u0008\u0008\u0008%s", df.format(p) + "%");
	}

	class CopyFileResultVO {
		long took;
		Boolean verified;


	}

	private CopyFileResultVO copySmallFiles(final File source, final File target, FileCopyStatistics statistics, final UI rdProUI)
			throws IOException, NoSuchAlgorithmException {

		long startTime = 0, endTime = 0;
		FileChannel inChannel = null, outChannel = null;
		CopyFileResultVO vo = new CopyFileResultVO();
		long totalFileSize;
		try {
			inChannel = new FileInputStream(source).getChannel();
			outChannel = new FileOutputStream(target).getChannel();
			totalFileSize = inChannel.size();

			startTime = System.currentTimeMillis();

			//do the copy
			inChannel.transferTo(0, inChannel.size(), outChannel);


			//verify
			if (RunTimeProperties.instance.isVerifyAfterCopy()) {
				byte[] sourceHash = readFileContentHash(source, rdProUI);
				byte[] targetHash = readFileContentHash(target, rdProUI);
				if (!Arrays.equals(sourceHash, targetHash)) {
					//rdProUI.printError("Failed to verify the copy:" + target.getAbsolutePath());
					vo.verified = false;
				} else {
					vo.verified = true;
				}
			}

		} catch (IOException | NoSuchAlgorithmException e) {
			throw e;
		} finally {
			close(inChannel);
			close(outChannel);
		}
		//done
		endTime = System.currentTimeMillis();
		rdProUI.showProgress(100, statistics);
		statistics.addToTotalFileSizeAndTime(totalFileSize, (endTime - startTime));
		statistics.incrementFileCount();


		vo.took = (endTime - startTime);
		return vo;
	}


	private CopyFileResultVO nioBufferCopy(final File source, final File target, FileCopyStatistics statistics
			, final UI rdProUI
//			, boolean isCalculateDigest
//			, int bufferCapacity

	) throws IOException, NoSuchAlgorithmException {
		ReadableByteChannel inChannel = null;
		WritableByteChannel outChannel = null;
		long totalFileSize = 0;
		rdProUI.showProgress(0, statistics);
		long startTime, endTime = 0;
		MessageDigest md5In = null, md5Out = null;

		startTime = System.currentTimeMillis();
		InputStream inputStream;
		OutputStream outputStream;

		CopyFileResultVO vo = new CopyFileResultVO();

		try {
			totalFileSize = source.length();

			if (RunTimeProperties.instance.isVerifyAfterCopy()) {
				md5In = MessageDigest.getInstance("MD5");
				inputStream = new DigestInputStream(new FileInputStream(source), md5In);
			} else {
				inputStream = new FileInputStream(source);
			}

			inChannel = Channels.newChannel(inputStream);


			if (RunTimeProperties.instance.isVerifyAfterCopy()) {
				md5Out = MessageDigest.getInstance("MD5");
				outputStream = new DigestOutputStream(new FileOutputStream(target), md5Out);
			} else {
				outputStream = new FileOutputStream(target);
			}
			outChannel = Channels.newChannel(outputStream);


			ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER);
			int readSize = inChannel.read(buffer);
			long totalRead = 0;
			int progress = 0;


			while (readSize != -1) {

				if (RunTimeProperties.instance.isStopThreads()) {
					rdProUI.println("[warn]Cancelled by user. Stoping copying.", true);
					close(outChannel);
					deleteFile(target.getAbsolutePath(), rdProUI);
					if (RunTimeProperties.instance.isDebug())
						rdProUI.println("\t" + Thread.currentThread().getName() + "is stopped.", true);
					return vo;
				}

				totalRead = totalRead + readSize;

				progress = (int) (totalRead * 100 / totalFileSize);
				rdProUI.showProgress(progress, statistics);

				buffer.flip();

				while (buffer.hasRemaining()) {
					outChannel.write(buffer);
					//System.out.printf(".");
					//showPercent(rdProUI, totalSize/size );
				}
				buffer.clear();
				readSize = inChannel.read(buffer);

			}

			//verify
			if (RunTimeProperties.instance.isVerifyAfterCopy()) {
				byte[] sourceFileMD5 = md5In.digest();
				byte[] targetHash = readFileContentHash(target, rdProUI);
				if (!Arrays.equals(sourceFileMD5, targetHash)) {
					vo.verified = false;
				} else {
					vo.verified = true;
				}
			}


		} finally {
			if (inChannel != null) {
				try {
					inChannel.close();
				} catch (IOException e) {
					rdProUI.printError("failed to close the inChannel", e);
				}
			}
			if (outChannel != null) {
				try {
					outChannel.close();
				} catch (IOException e) {
					rdProUI.printError("failed to close the outChannel", e);
				}
			}
		}
		endTime = System.currentTimeMillis();


		statistics.addToTotalFileSizeAndTime(totalFileSize, (endTime - startTime));
		statistics.incrementFileCount();
		rdProUI.showProgress(100, statistics);

		vo.took = (endTime - startTime);
		return vo;
	}

	private static void close(Closeable closable) {
		if (closable != null) {
			try {
				closable.close();
			} catch (IOException e) {
				if (RunTimeProperties.instance.isDebug())
					e.printStackTrace();
			}
		}
	}


	 /*
	public static long getFolderSize(String dir)  {
		try {
			return Files.walk(new File(dir).toPath())
					.map(f -> f.toFile())
					.filter(f -> f.isFile())
					.mapToLong(f -> f.length()).sum();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	*/


	/**
	 * Get total size of all the files immediately under this rootDir.
	 * It does not count the sub directories.
	 *
	 * @param rootDir
	 * @return
	 */
	public static DirecotryStat getDirectoryStats(final File rootDir, final long smallFileSizeThreashold) {

		final AtomicLong size = new AtomicLong(0);
		final AtomicLong fileCount = new AtomicLong(0);
		Path rootPath = rootDir.toPath();
		final DirecotryStat ret = new DirecotryStat();
		try {
			Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					size.addAndGet(attrs.size());
					fileCount.incrementAndGet();
					if (attrs.size() <= smallFileSizeThreashold) {
						ret.incrementSmallFileCount();
						ret.addToTotalSmallFileSize(attrs.size());
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (dir.equals(rootPath))
						return FileVisitResult.CONTINUE;
					else
						return FileVisitResult.SKIP_SUBTREE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) {
					ret.setFailMsg("visitFileFailed for: " + file + " (" + exc.getMessage() + ")");
					ret.setFail(true);
					// Skip folders that can't be traversed
					return FileVisitResult.TERMINATE;
				}

//				@Override
//				public FileVisitResult postVisitDirectory(Path rootDir, IOException exc) {
//
//					if (exc != null)
//						System.out.println("had trouble traversing: " + rootDir + " (" + exc + ")");
//					// Ignore errors traversing a folder
//					return FileVisitResult.CONTINUE;
//				}
			});
		} catch (IOException e) {
			throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
		}

		ret.setTotalFileSize(size.get());
		ret.setNumberOfFiles(fileCount.get());
		return ret;
	}


	public static void createDir(long originalDirLastModified, final File targetDir, final UI ui, final FileCopyStatistics frs) {
		// if the directory does not exist, create it

		try {
			//todo time it.
			Files.createDirectory(Paths.get(targetDir.getAbsolutePath()));
			frs.incrementDirCount();
		} catch (FileAlreadyExistsException e) {
			//ignore.
		} catch (IOException | SecurityException | UnsupportedOperationException e) {
			ui.printError("createDir() failed", e);
			throw new RuntimeException(e);
		}

	}


	private static void copyFileUsingFileChannels(File source, File dest)
			throws IOException {
		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		try {
			inputChannel = new FileInputStream(source).getChannel();
			outputChannel = new FileOutputStream(dest).getChannel();
			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		} finally {
			inputChannel.close();
			outputChannel.close();
		}
	}


	public static class CompressedPackageVO {
		String zipName; //  _originalDirname.zip
		String originalDirname;
		long originalDirLastModified;
		String sourceZipFileWithPath;
		String destDir;
		long zipFileSizeBytes;
		int numberOfFiles = 0;

		public CompressedPackageVO(String zipName, String originalDirname, String zipFileWithPath) {
			this.zipName = zipName;
			this.originalDirname = originalDirname;
			this.sourceZipFileWithPath = zipFileWithPath;
		}

		public String getDestDir() {
			return destDir;
		}

		public void setDestDir(String destDir) {
			this.destDir = destDir;
		}

		public int getNumberOfFiles() {
			return numberOfFiles;
		}

		public void setNumberOfFiles(int numberOfFiles) {
			this.numberOfFiles = numberOfFiles;
		}

		public void incrementFileCount(int v) {
			this.numberOfFiles += v;
		}
	}

	/**
	 * Compress the directory contains small files.
	 *
	 * @param dirPath                 The directory
	 * @param recursive               recursive or not.
	 * @param smallFileSizeThreashold if the file size is smaller or equals than this, it is included.    if -1, it does not apply
	 * @return zip file name without path
	 */

	public CompressedPackageVO compressDirectory(final String dirPath, final String targetDir, final boolean recursive
			, final long smallFileSizeThreashold) throws IOException {
		Path sourcePath = Paths.get(dirPath);

		//put the zip under the same sourcePath.
		String zipName = RunTimeProperties.zip_prefix + sourcePath.getFileName().toString() + ".zip";
		final String zipFileName = dirPath.concat(File.separator).concat(zipName);

		CompressedPackageVO compressedPackageVO = new CompressedPackageVO(zipName, sourcePath.getFileName().toString(), zipFileName);
		compressedPackageVO.originalDirLastModified = sourcePath.toFile().lastModified();
		ZipOutputStream outputStream = null;

		try {
			outputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
			outputStream.setLevel(Deflater.BEST_COMPRESSION);

			MyZipFileVisitor visitor = new MyZipFileVisitor(compressedPackageVO, targetDir, smallFileSizeThreashold, zipName, sourcePath, outputStream, recursive);

			Files.walkFileTree(sourcePath, visitor);


		} catch (IOException e) {
			if (outputStream != null) {
				try {
					outputStream.close();
					outputStream = null;
				} catch (IOException e2) {
					//
				}
			}
			deleteFile(zipFileName, rdProUI);
			throw e;
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					//
				}
			}

			if (compressedPackageVO.getNumberOfFiles() == 0) {
				deleteFile(zipFileName, rdProUI);
			}

		}
		return compressedPackageVO;
	}


	class MyZipFileVisitor extends SimpleFileVisitor<Path> {

		CompressedPackageVO compressedPackageVO;
		String targetDir;
		long smallFileSizeThreashold;
		String zipName;
		Path sourcePath;
		ZipOutputStream outputStream;
		boolean recursive;

		public MyZipFileVisitor(CompressedPackageVO compressedPackageVO, String targetDir, long smallFileSizeThreashold, String zipName, Path sourcePath, ZipOutputStream outputStream
				, boolean recursive) {
			this.compressedPackageVO = compressedPackageVO;
			this.targetDir = targetDir;
			this.smallFileSizeThreashold = smallFileSizeThreashold;
			this.zipName = zipName;
			this.sourcePath = sourcePath;
			this.outputStream = outputStream;
			this.recursive = recursive;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
			boolean include = true;
			if (!RunTimeProperties.instance.isOverrideTarget()) {
				//target file
				File _targetFile = new File(targetDir + File.separator + file.getFileName().toString());
				if (_targetFile.exists()) {
					include = overrideTargetFile(file.toFile(), _targetFile);
					if (!include) {
						rdProUI.println(LogLevel.debug, "\tFile " + _targetFile.getAbsolutePath() + " exists, skipped.");
					}
				} else {
					include = true;
				}
			} else
				include = true;


			if (include) {
				if ((smallFileSizeThreashold == -1 || file.toFile().length() <= smallFileSizeThreashold) //
						&& !file.getFileName().toString().equals(zipName)) { //exclude the zip file itself.

					compressedPackageVO.incrementFileCount(1);

				Path targetFile = sourcePath.relativize(file);
				ZipEntry ze = new ZipEntry(targetFile.toString());
				ze.setLastModifiedTime(FileTime.fromMillis(file.toFile().lastModified()));

				// Store all three file times in ZIP entry for complete preservation
				storeAllFileTimesInZipEntry(file, ze);

				// Store Unix file permissions in ZIP entry for later extraction
				storeFilePermissionsInZipEntry(file, ze);

					//note read whole file into memory. it is what we wanted for small size files.
					byte[] bytes = Files.readAllBytes(file);
					compressedPackageVO.zipFileSizeBytes = bytes.length;

					//set the MD5 to the extra of the entry. this is source MD5. 
					if (RunTimeProperties.instance.isVerifyAfterCopy()) {
						ze.setComment(StrUtils.toHexString(getHash(bytes)));
					}
					outputStream.putNextEntry(ze);
					outputStream.write(bytes, 0, bytes.length);
					outputStream.closeEntry();
				}
			}
			return FileVisitResult.CONTINUE;
		}


		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (dir.equals(sourcePath))
				return FileVisitResult.CONTINUE;
			else
				return recursive ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
		}

	}

	/**
	 * Unzip the zipFile to the deskDir
	 *
	 * @param file
	 * @param destDir
	 * @throws IOException
	 */
    protected void unzipFile(File file, File destDir, FileCopyStatistics statistics) throws NoSuchAlgorithmException, IOException {

        long filesCount = 0;
        byte[] buffer = new byte[4096];
        //zip input stream does not read zip entry comments. use ZipFile.
        //ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipFile zipFile = new ZipFile(file);

        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();

                File destFile = new File(destDir, zipEntry.getName());

                // Create parent directories if they don't exist
                File parentDir = destFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                // Skip directory entries (they end with /)
                if (zipEntry.isDirectory()) {
                    destFile.mkdirs();
                    continue;
                }

                // Only count actual files, not directories
                filesCount++;

                FileOutputStream fos = new FileOutputStream(destFile);
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
			fos.close();

			// Restore file times and permissions from ZIP entry
			if (RunTimeProperties.instance.isPreserveFileTimesAndAccessAttributes()) {
				// Restore all three file times (modified, access, creation)
				restoreAllFileTimesFromZipEntry(zipEntry, destFile);

				// Preserve file permissions (Unix attributes stored in external file attributes)
				preserveZipEntryPermissions(zipEntry, destFile);
			}

			//verify
                if (RunTimeProperties.instance.isVerifyAfterCopy()) {
                    byte[] sourceHash = StrUtils.toByteArray(zipEntry.getComment());
                    byte[] targetHash = readFileContentHash(destFile, this.rdProUI);
                    if (!Arrays.equals(sourceHash, targetHash)) {
                        rdProUI.printError("\tVerify file failed:" + destFile.getAbsolutePath());
                        //delete it.
                        destFile.delete();
                    } else {
                        rdProUI.println(LogLevel.debug, "\tVerified file:" + destFile.getAbsolutePath());
                    }
                }


            }


        } finally {
            zipFile.close();
        }


        statistics.addFileCount(filesCount);

    }

	/**
	 * Split the file with full patch into three tokens. 1. dir, 2.filename, 3. extension
	 * no slash at the end and no dots on the file ext.
	 *
	 * @param fileWithPath
	 * @return
	 */
	public static String[] splitFileParts(final String fileWithPath) {
		if (fileWithPath == null || fileWithPath.trim().length() == 0)
			return null;

		String[] ret = new String[3];
		int k = fileWithPath.lastIndexOf(File.separator);
		String dir = null;
		String fileName = null;
		String fileExt = null;
		if (k > -1) {
			dir = fileWithPath.substring(0, k);                         // no slash at the end
			fileName = fileWithPath.substring(k + 1, fileWithPath.length());
		} else
			fileName = fileWithPath;


		if (fileName.length() > 0) {
			String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
			fileName = tokens[0];
			if (tokens.length > 1)
				fileExt = tokens[1];
		} else
			fileName = null;


		ret[0] = dir;
		ret[1] = fileName;
		ret[2] = fileExt;


		return ret;
	}


	public static byte[] getHash(byte[] input) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] md5 = digest.digest(input);
			return md5;
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	public static byte[] readFileContentHash(final File source
			, UI rdProUI) throws NoSuchAlgorithmException, IOException {
		InputStream fis = null;
		ReadableByteChannel inChannel = null;
		try {
			MessageDigest md5In = MessageDigest.getInstance("MD5");
			fis = new DigestInputStream(new FileInputStream(source), md5In);
			/*use channel*/
//			inChannel = Channels.newChannel(fis);
//			ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER);
//
//			int readSize = inChannel.read(buffer);
//			while (readSize != -1) {
//
//				if (FastCopy.isStopThreads()) {
//					rdProUI.println("[warn]Cancelled by user. Stoping copying.", true);
//					rdProUI.println("\t" + Thread.currentThread().getName() + "is stopped.", true);
//					return null;
//				}
//
//				buffer.flip();
//
//				// write it out
//				//				while (buffer.hasRemaining()) {
//				//					//outChannel.write(buffer);
//				//				}
//				buffer.clear();
//
//				readSize = inChannel.read(buffer);
//
//			}


//			/* compare the digest */
//			byte[] sourceFileMD5 = md5In.digest();
//		    return sourceFileMD5;



			/*use file inputstream*/
			int i = 0;
			do {

				if (RunTimeProperties.instance.isStopThreads()) {
					rdProUI.println("[warn]Cancelled by user. readFileContentHash() stops.", true);
					return null;
				}
				byte[] buf = new byte[10240];
				i = fis.read(buf);
			} while (i != -1);

			return md5In.digest();
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}

	/**
	 * do the copy if return true
	 *
	 * @param srcFile
	 * @param targetFile
	 * @return
	 */
	public static boolean overrideTargetFile(final File srcFile, final File targetFile) {

		if (RunTimeProperties.instance.overrideTarget)
			return true;

		if (RunTimeProperties.instance.isOverwriteIfNewerOrDifferent()) {
			if (targetFile.exists()) {    //File IO
				if (srcFile.lastModified() - targetFile.lastModified() > 1000
						|| (srcFile.length() != targetFile.length()))
					return true;
				else
					return false;
			}
			return true;
		} else
			return false;
	}




}
