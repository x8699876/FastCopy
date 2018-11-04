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

import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;

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


	public static void copyFile(final File source, final File target, FileCopyStatistics statistics, final UI rdProUI) {

		long timeTook = 0;
		if (source.length() < SMALL_FILE_SIZE) {
			timeTook = FileUtils.copySmallFiles(source, target, statistics, rdProUI);
		} else
			timeTook = FileUtils.nioBufferCopy(source, target, statistics, rdProUI);


		if (RunTimeProperties.instance.isVerbose()) {
			if (source.length() < 1024)
				rdProUI.print(String.format("\n\tCopy file %s-->%s, size:%s (bytes), took %s (ms)"
						, source.getAbsolutePath(), target.getAbsolutePath()
						, df.format(source.length())
						, timeTook)
				);
			else
				rdProUI.print(String.format("\n\tCopy file %s-->%s, size:%s (Kb), took %s (ms)"
						, source.getAbsolutePath(), target.getAbsolutePath()
						, df.format(source.length() / 1024)
						, timeTook)
				);
		}


		statistics.getBucket(source.length()).incrementFileCount();

		try {
			boolean b = target.setLastModified(source.lastModified());
			//rdProUI.println("modify file date to: " + b + "," + new Timestamp(target.lastModified()));
		} catch (Exception e) {
			rdProUI.printError(e.getMessage());
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

			//done
			endTime = System.currentTimeMillis();
			rdProUI.showProgress(100, statistics);

			statistics.addToTotalFileSizeAndTime(totalFileSize, (endTime - startTime));
			statistics.setFilesCount(statistics.getFilesCount() + 1);
			rdProUI.showProgress(100, statistics);

		} catch (IOException e) {
			rdProUI.println(String.format("[error] Copy file %s to %s: %s", source.getAbsoluteFile(), target.getAbsolutePath(), e.getMessage()));
		} finally {
			close(inChannel);
			close(outChannel);
		}
		return (endTime - startTime);
	}


	/*public static long copyDirectory(final File source, final File target
			, FileCopyStatistics statistics, final UI rdProUI) {
		long startTime = 0, endTime = 0;
		Path sourcePath = Paths.get(source.getAbsolutePath());
		Path targetPath = Paths.get(target.getAbsolutePath());


		startTime = System.currentTimeMillis();
		try {
			List<CopyOption> options = new ArrayList<>();
			options.add(StandardCopyOption.COPY_ATTRIBUTES);
			if (RunTimeProperties.instance.overwrite)
				options.add(StandardCopyOption.REPLACE_EXISTING);

			Files.copy(sourcePath, targetPath, options.toArray(new CopyOption[0]));

			endTime = System.currentTimeMillis();
			long totalFileSize = getDirectoryStats(source);
			statistics.addToTotalFileSizeAndTime(totalFileSize, (endTime - startTime));
			statistics.setFilesCount(statistics.getFilesCount() + 1);
			rdProUI.print(String.format("\n\tCopying direcotry %s-->%s, size:%s (Kb), took %s (ms)"
					, source.getAbsolutePath(), target.getAbsolutePath()
					, df.format(totalFileSize / 1024)
					, df.format(source.length()), dfLong.format(endTime - startTime))
			);

		} catch (IOException e) {
			e.printStackTrace();
			rdProUI.println(String.format("[error] Copy dir from %s to %s failed: %s"
					, source.getAbsolutePath(), target.getAbsolutePath(), e.getMessage()));
		}

		return (endTime - startTime);
	}*/


	private static long nioBufferCopy(final File source, final File target, FileCopyStatistics statistics, final UI rdProUI) {
		FileChannel in = null;
		FileChannel out = null;
		long totalFileSize = 0;
		rdProUI.showProgress(0, statistics);
		long startTime, endTime = 0;

		startTime = System.currentTimeMillis();

		try {
			in = new FileInputStream(source).getChannel();
			out = new FileOutputStream(target).getChannel();
			totalFileSize = in.size();


			ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER);
			int readSize = in.read(buffer);
			long totalRead = 0;
			int progress = 0;


			while (readSize != -1) {

				if (FastCopy.isStopThreads()) {
					rdProUI.println("[warn]Cancelled by user. Stoping copying.", true);
					rdProUI.println("\t" + Thread.currentThread().getName() + "is stopped.", true);
					return 0;
				}

				totalRead = totalRead + readSize;

				progress = (int) (totalRead * 100 / totalFileSize);
				rdProUI.showProgress(progress, statistics);

				buffer.flip();

				while (buffer.hasRemaining()) {
					out.write(buffer);
					//System.out.printf(".");
					//showPercent(rdProUI, totalSize/size );
				}
				buffer.clear();
				readSize = in.read(buffer);

			}

			endTime = System.currentTimeMillis();

			statistics.addToTotalFileSizeAndTime(totalFileSize, (endTime - startTime));
			statistics.setFilesCount(statistics.getFilesCount() + 1);
			rdProUI.showProgress(100, statistics);


		} catch (IOException e) {
			rdProUI.println(String.format("[error] Copy file %s to %s: %s", source.getAbsoluteFile(), target.getAbsolutePath(), e.getMessage()));
		} finally {
			close(in);
			close(out);
		}
		return (endTime - startTime);
	}

	private static void close(Closeable closable) {
		if (closable != null) {
			try {
				closable.close();
			} catch (IOException e) {
				if (FastCopy.debug)
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
	public static DirecotryStat getDirectoryStats(final File rootDir) {

		final AtomicLong size = new AtomicLong(0);
		final AtomicLong fileCount = new AtomicLong(0);
		Path rootPath = rootDir.toPath();
		DirecotryStat ret = new DirecotryStat();
		try {
			Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					size.addAndGet(attrs.size());
					fileCount.incrementAndGet();
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
					//todo 
					System.out.println("skipped: " + file + " (" + exc + ")");
					// Skip folders that can't be traversed
					return FileVisitResult.CONTINUE;
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


	public static void createDir(final File theDir, final UI ui, final FileCopyStatistics frs) {
		// if the directory does not exist, create it
		if (!theDir.exists()) {
			//ui.println("creating directory: " + theDir.getName());
			boolean result = false;
			try {
				theDir.mkdir();
				result = true;
			} catch (SecurityException se) {
				ui.println(String.format("[error] Failed to create directory: %s", theDir.getName()));
			}
			if (result) {
				if (RunTimeProperties.instance.isVerbose() && RunTimeProperties.instance.isDebug())
					ui.println(String.format("Directory created: %s", theDir.getName()));
				frs.setDirCount(frs.getDirCount() + 1);
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


	/**
	 * Compress the directory contains small files.
	 * @param dirPath   The directory
	 * @param recursive recursive or not.
	 * @param smallFileSizeThreashold if the file size is smaller than this, it is included.    if -1, it does not apply
	 */
	public static void compressDirectory(final String dirPath, final boolean recursive, final long smallFileSizeThreashold) {
		Path sourcePath = Paths.get(dirPath);

		//put the zip under the same sourcePath.
		String name = "_"+sourcePath.getFileName().toString()+".zip";
		final String zipFileName = dirPath.concat(File.separator).concat(name);
		try {
			ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
			Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
					try {
						if (( smallFileSizeThreashold==-1 ||file.toFile().length()<=smallFileSizeThreashold) //
						    && !file.getFileName().toString().equals(name)) { //exclude the zip file itself. 

							Path targetFile = sourcePath.relativize(file);
							outputStream.putNextEntry(new ZipEntry(targetFile.toString()));
							//note read whole file into memory. it is what we wanted for small size files.
							byte[] bytes = Files.readAllBytes(file);
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
						return recursive?FileVisitResult.CONTINUE:FileVisitResult.SKIP_SUBTREE;
				}

			});
			outputStream.close();
		} catch (IOException e) {
			throw new RuntimeException("compressDirectory() failed", e);
		}
	}


	public static void main(String[] args) {
		String dir = "S:\\src\\b1611-trunk\\dev-light\\apps\\learning\\sapui5-modules\\browse-catalog\\src\\main\\control";
		compressDirectory(dir, true, 20000 );
	}
}



