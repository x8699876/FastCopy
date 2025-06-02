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
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

import org.mhisoft.fc.ui.ConsoleRdProUIImpl;
import org.mhisoft.fc.ui.UI;
import org.mhisoft.fc.utils.StrUtils;

import junit.framework.Assert;

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
			, final FileUtils.CompressedackageVO compressedackageVO) {

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
				rdProUI.println(String.format("\tCopied file %s-->%s, size:%s (bytes), took %s. %s"
						, source.getAbsolutePath(), target.getAbsolutePath()
						, df.format(source.length())
						, StrUtils.getDisplayTime(vo.took)
						, vo.verified != null ? (vo.verified ? "Verified" : "Verify Error!") : ""

						)
				);
			else
				rdProUI.println(String.format("\tCopied file %s-->%s, size:%s (Kb), took %s. %s"
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
			if (compressedackageVO != null) {


				try {
					//create destdir
					File destZipDir = new File(compressedackageVO.getDestDir());
					//+File.separator + compressedackageVO.originalDirname);
					FileUtils.createDir(compressedackageVO.originalDirLastModified, destZipDir, rdProUI, statistics);

					unzipFile(target, destZipDir, statistics);

					if (RunTimeProperties.instance.isVerbose()) {
						rdProUI.println("\tUnzipped under " + destZipDir);
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

		if (compressedackageVO == null) { //
			try {
				setFileLastModified(target.getAbsolutePath(), source.lastModified());
			} catch (Exception e) {
				rdProUI.printError("setLastModified() failed.", e);
			}
		}

	}

	public  BasicFileAttributes getFileAttributes(Path sourceFile) throws IOException {
		BasicFileAttributes attr = Files.readAttributes(sourceFile, BasicFileAttributes.class);
		return attr;
	}


	public  void setFileLastModified(String targetFile, long millis)  {
		if (RunTimeProperties.instance.isKeepOriginalFileDates()) {
			Path tPath = Paths.get(targetFile);
			BasicFileAttributeView attributes = Files.getFileAttributeView(tPath, BasicFileAttributeView.class);
			FileTime time = FileTime.fromMillis(millis);
			try {
				attributes.setTimes(time, time, null);
			} catch (IOException e) {
				rdProUI.print(LogLevel.debug, "Failed to set last modified timestamp for " + targetFile);
			}
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


	/*	if (!targetDir.exists()) {
			//ui.println("creating directory: " + theDir.getName());
			boolean result = false;
			try {
				targetDir.mkdir();
				result = true;

//				if (originalDirLastModified != -1) {
//					try {
//						boolean b = targetDir.setLastModified(originalDirLastModified);
//					} catch (Exception e) {
//						ui.printError("error in createDir()", e);
//					}
//				}


			} catch (SecurityException se) {
				ui.println(String.format("[error] Failed to create directory: %s", targetDir.getName()));
			}
			if (result) {
				if (RunTimeProperties.instance.isVerbose() && RunTimeProperties.instance.isDebug())
					ui.println(String.format("Directory created: %s", targetDir.getName()));
				frs.incrementDirCount();
			}
		}*/
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


	public static class CompressedackageVO {
		String zipName; //  _originalDirname.zip
		String originalDirname;
		long originalDirLastModified;
		String sourceZipFileWithPath;
		String destDir;
		long zipFileSizeBytes;
		int numberOfFiles = 0;

		public CompressedackageVO(String zipName, String originalDirname, String zipFileWithPath) {
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

	public CompressedackageVO compressDirectory(final String dirPath, final String targetDir, final boolean recursive
			, final long smallFileSizeThreashold) throws IOException {
		Path sourcePath = Paths.get(dirPath);

		//put the zip under the same sourcePath.
		String zipName = RunTimeProperties.zip_prefix + sourcePath.getFileName().toString() + ".zip";
		final String zipFileName = dirPath.concat(File.separator).concat(zipName);

		CompressedackageVO compressedackageVO = new CompressedackageVO(zipName, sourcePath.getFileName().toString(), zipFileName);
		compressedackageVO.originalDirLastModified = sourcePath.toFile().lastModified();
		ZipOutputStream outputStream = null;

		try {
			outputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
			outputStream.setLevel(Deflater.BEST_COMPRESSION);

			MyZipFileVisitor visitor = new MyZipFileVisitor(compressedackageVO, targetDir, smallFileSizeThreashold, zipName, sourcePath, outputStream, false);

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

			if (compressedackageVO.getNumberOfFiles() == 0) {
				deleteFile(zipFileName, rdProUI);
			}

		}
		return compressedackageVO;
	}


	class MyZipFileVisitor extends SimpleFileVisitor<Path> {

		CompressedackageVO compressedackageVO;
		String targetDir;
		long smallFileSizeThreashold;
		String zipName;
		Path sourcePath;
		ZipOutputStream outputStream;
		boolean recursive;

		public MyZipFileVisitor(CompressedackageVO compressedackageVO, String targetDir, long smallFileSizeThreashold, String zipName, Path sourcePath, ZipOutputStream outputStream
				, boolean recursive) {
			this.compressedackageVO = compressedackageVO;
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

					compressedackageVO.incrementFileCount(1);

					Path targetFile = sourcePath.relativize(file);
					ZipEntry ze = new ZipEntry(targetFile.toString());
					ze.setLastModifiedTime(FileTime.fromMillis(file.toFile().lastModified()));
					//note read whole file into memory. it is what we wanted for small size files.
					byte[] bytes = Files.readAllBytes(file);
					compressedackageVO.zipFileSizeBytes = bytes.length;

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


				filesCount++;
				File destFile = new File(destDir, zipEntry.getName());

				FileOutputStream fos = new FileOutputStream(destFile);
				InputStream inputStream = zipFile.getInputStream(zipEntry);
				int len;
				while ((len = inputStream.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();

				setFileLastModified(destFile.getAbsolutePath(), zipEntry.getTime());


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


		statistics.addFileCount(filesCount - 1);//exclude the zip file itself.

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


	public static void main(String[] args) {
		try {
			long t1 = System.currentTimeMillis();
			byte[] md51 = FileUtils.readFileContentHash(new File("D:\\temp\\test2\\Local\\Resmon.ResmonCfg")
					, new ConsoleRdProUIImpl());
			String s = StrUtils.toHexString(md51);
			System.out.println(s);
			System.out.println("took " + (System.currentTimeMillis() - t1));
			Assert.assertTrue(Arrays.equals(md51, StrUtils.toByteArray(s)));

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}



