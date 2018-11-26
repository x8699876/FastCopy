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
import java.util.Formatter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

import org.mhisoft.fc.ui.ConsoleRdProUIImpl;
import org.mhisoft.fc.ui.UI;

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


	public static void copyFile(final File source, final File target, FileCopyStatistics statistics, final UI rdProUI
			, final FileUtils.CompressedackageVO compressedackageVO) {

		long timeTook = 0;
		try {
			if (source.length() < SMALL_FILE_SIZE) {
				timeTook = FileUtils.copySmallFiles(source, target, statistics, rdProUI);
			} else
				timeTook = FileUtils.nioBufferCopy(source, target, statistics, rdProUI);
		} catch (Exception e) {
			rdProUI.printError(e.getMessage());
			RunTimeProperties.instance.setStopThreads(true);
			return;
		}


		if (RunTimeProperties.instance.isVerbose()) {
			if (source.length() < 4096)
				rdProUI.println(String.format("\tCopied file %s-->%s, size:%s (bytes), took %s (ms)"
						, source.getAbsolutePath(), target.getAbsolutePath()
						, df.format(source.length())
						, timeTook)
				);
			else
				rdProUI.println(String.format("\tCopied file %s-->%s, size:%s (Kb), took %s (ms)"
						, source.getAbsolutePath(), target.getAbsolutePath()
						, df.format(source.length() / 1024)
						, timeTook)
				);
		}

		//todo need to adjust for packaged zip
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
		} catch (IOException e) {
			rdProUI.printError(e.getMessage());
		}


		try {
			boolean b = target.setLastModified(source.lastModified());
			//rdProUI.println("modify file date to: " + b + "," + new Timestamp(target.lastModified()));
		} catch (Exception e) {
			rdProUI.printError(e.getMessage());
		}

	}


	public static void deleteFile(String file, final UI rdProUI) {
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


	public static void showPercent(final UI rdProUI, double digital) {
		long p = (long) digital * 100;
		DecimalFormat df = new DecimalFormat("000");
		String s = df.format(p);

		rdProUI.printf("\u0008\u0008\u0008\u0008%s", df.format(p) + "%");
	}


	private static long copySmallFiles(final File source, final File target, FileCopyStatistics statistics, final UI rdProUI) {

		long startTime = 0, endTime = 0;
		FileChannel inChannel = null, outChannel = null;

		try {
			inChannel = new FileInputStream(source).getChannel();
			outChannel = new FileOutputStream(target).getChannel();
			long totalFileSize = inChannel.size();

			startTime = System.currentTimeMillis();

			//do the copy
			inChannel.transferTo(0, inChannel.size(), outChannel);


			//verify
			if (RunTimeProperties.instance.isVerifyAfterCopy()) {
				byte[] sourceHash = readFileContentHash(source, rdProUI);
				byte[] targetHash = readFileContentHash(target, rdProUI);
				if(!Arrays.equals(sourceHash, targetHash)) {
					//rdProUI.printError("Failed to verify the copy:" + target.getAbsolutePath());
					throw new RuntimeException("Failed to verify the copy:" + target.getAbsolutePath());
				}
				else {
					rdProUI.print("Verified.");
				}
			}


			//done
			endTime = System.currentTimeMillis();
			rdProUI.showProgress(100, statistics);
			statistics.addToTotalFileSizeAndTime(totalFileSize, (endTime - startTime));
			statistics.incrementFileCount();


		} catch (IOException | NoSuchAlgorithmException e) {
			rdProUI.println(String.format("[error] Copy file %s to %s: %s", source.getAbsoluteFile(), target.getAbsolutePath(), e.getMessage()));
		} finally {
			close(inChannel);
			close(outChannel);
		}
		return (endTime - startTime);
	}



	private static long nioBufferCopy(final File source, final File target, FileCopyStatistics statistics
			, final UI rdProUI
//			, boolean isCalculateDigest
//			, int bufferCapacity

	) {
		ReadableByteChannel inChannel = null;
		WritableByteChannel outChannel = null;
		long totalFileSize = 0;
		rdProUI.showProgress(0, statistics);
		long startTime, endTime = 0;
		MessageDigest md5In = null, md5Out = null;

		startTime = System.currentTimeMillis();
		InputStream inputStream;
		OutputStream outputStream;

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
					if (RunTimeProperties.instance.isDebug())
					rdProUI.println("\t" + Thread.currentThread().getName() + "is stopped.", true);
					return 0;
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
				if(!Arrays.equals(sourceFileMD5, targetHash)) {
					throw new RuntimeException("Failed to verify the copy:" + target.getAbsolutePath());
				}
				else {
					rdProUI.print("Verified.");
				}
			}



			endTime = System.currentTimeMillis();

			statistics.addToTotalFileSizeAndTime(totalFileSize, (endTime - startTime));
			statistics.incrementFileCount();
			rdProUI.showProgress(100, statistics);


		} catch (IOException e) {
			rdProUI.println(String.format("[error] Copy file %s to %s: %s", source.getAbsoluteFile(), target.getAbsolutePath(), e.getMessage()));
		} catch (NoSuchAlgorithmException e) {
			rdProUI.printError(e.getMessage());
			throw new RuntimeException(e);
		} finally {
			close(inChannel);
			close(outChannel);
		}
		return (endTime - startTime);
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
		if (!targetDir.exists()) {
			//ui.println("creating directory: " + theDir.getName());
			boolean result = false;
			try {
				targetDir.mkdir();
				result = true;

				if (originalDirLastModified != -1) {
					try {
						boolean b = targetDir.setLastModified(originalDirLastModified);
					} catch (Exception e) {
						ui.printError(e.getMessage());
					}
				}


			} catch (SecurityException se) {
				ui.println(String.format("[error] Failed to create directory: %s", targetDir.getName()));
			}
			if (result) {
				if (RunTimeProperties.instance.isVerbose() && RunTimeProperties.instance.isDebug())
					ui.println(String.format("Directory created: %s", targetDir.getName()));
				frs.incrementDirCount();
			}
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


	public static class CompressedackageVO {
		String zipName; //  _originalDirname.zip
		String originalDirname;
		long originalDirLastModified;
		String sourceZipFileWithPath;
		String destDir;
		long zipFileSizeBytes;

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
	}

	/**
	 * Compress the directory contains small files.
	 *
	 * @param dirPath                 The directory
	 * @param recursive               recursive or not.
	 * @param smallFileSizeThreashold if the file size is smaller or equals than this, it is included.    if -1, it does not apply
	 * @return zip file name without path
	 */

	//todo if target file exists and override is not check. need to skip zipping it.
	//todo verfiry the MD5 of the exploded files. 
	public static CompressedackageVO compressDirectory(final String dirPath, final boolean recursive, final long smallFileSizeThreashold) {
		Path sourcePath = Paths.get(dirPath);

		//put the zip under the same sourcePath.
		String name = RunTimeProperties.zip_prefix + sourcePath.getFileName().toString() + ".zip";
		final String zipFileName = dirPath.concat(File.separator).concat(name);

		CompressedackageVO ret = new CompressedackageVO(name, sourcePath.getFileName().toString(), zipFileName);
		ret.originalDirLastModified = sourcePath.toFile().lastModified();

		try {
			ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
			outputStream.setLevel(Deflater.BEST_COMPRESSION);
			Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
					try {
						if ((smallFileSizeThreashold == -1 || file.toFile().length() <= smallFileSizeThreashold) //
								&& !file.getFileName().toString().equals(name)) { //exclude the zip file itself.

							Path targetFile = sourcePath.relativize(file);
							ZipEntry ze = new ZipEntry(targetFile.toString());
							ze.setLastModifiedTime(FileTime.fromMillis(file.toFile().lastModified()));
							outputStream.putNextEntry(ze);
							//note read whole file into memory. it is what we wanted for small size files.
							byte[] bytes = Files.readAllBytes(file);
							ret.zipFileSizeBytes = bytes.length;
							//todo
							//get MD5 of the bytes.

							//ze.setExtra();

							outputStream.write(bytes, 0, bytes.length);
							outputStream.closeEntry();
						}
					} catch (IOException e) {
						throw new RuntimeException("compressDirectory() failed", e);
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

			});
			outputStream.close();
		} catch (IOException e) {
			throw new RuntimeException("compressDirectory() failed", e);
			//todo rdProUI.printError(e.getMessage());
		}
		return ret;
	}

	/**
	 * Unzip the zipFile to the deskDir
	 *
	 * @param zipFile
	 * @param destDir
	 * @throws IOException
	 */
	protected static void unzipFile(File zipFile, File destDir, FileCopyStatistics statistics) throws IOException {
		byte[] buffer = new byte[4096];
		ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
		ZipEntry zipEntry = zis.getNextEntry();
		long filesCount;
		try {
			filesCount = 0;
			while (zipEntry != null) {
				filesCount++;
				File destFile = new File(destDir, zipEntry.getName());
				FileOutputStream fos = new FileOutputStream(destFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
				destFile.setLastModified(zipEntry.getTime());
				zipEntry = zis.getNextEntry();
			}
		} finally {
			zis.closeEntry();
			zis.close();
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


	public static byte[] readFileContentHash(final File source
	, UI rdProUI ) throws NoSuchAlgorithmException, IOException {
		InputStream fis =null;
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
		}
		finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

	}


	public static void main(String[] args) {
		try {
			long t1 = System.currentTimeMillis();
			byte[] md51 = FileUtils.readFileContentHash(new File("Z:\\SOFTWARE\\win8\\Windows.iso")
			, new ConsoleRdProUIImpl());
			System.out.println(toHexString(md51));
			System.out.println("took " + (System.currentTimeMillis()-t1));
			
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static  String toHexString(byte[] bytes) {
		if (bytes==null)
			return "";
		Formatter formatter = new Formatter();
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		String hex = formatter.toString();
		return hex;
	}


}



