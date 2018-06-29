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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;

import org.mhisoft.fc.ui.UI;

/**
 * Description:
 *
 * @author Tony Xue
 * @since Nov, 2014
 */
public class FileUtils {

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
				if (RunTimeProperties.instance.isVerbose())
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

	private static final int BUFFER = 4096 * 16;
	static final DecimalFormat df = new DecimalFormat("#,###.##");

	public static void showPercent(final UI rdProUI, double digital) {
		long p = (long) digital * 100;
		DecimalFormat df = new DecimalFormat("000");
		String s = df.format(p);

		rdProUI.printf("\u0008\u0008\u0008\u0008%s", df.format(p) + "%");
	}


	public static void nioBufferCopy(final File source, final File target, FileCopyStatistics statistics, final UI rdProUI) {
		FileChannel in = null;
		FileChannel out = null;
		long totalFileSize = 0;
		rdProUI.showProgress(0, statistics);

		try {
			in = new FileInputStream(source).getChannel();
			out = new FileOutputStream(target).getChannel();
			totalFileSize = in.size();
			//double size2InKB = size / 1024 ;
			if (RunTimeProperties.instance.isVerbose())
				rdProUI.print(String.format("\n\tCopying file %s-->%s, size:%s KBytes", source.getAbsolutePath(),
						target.getAbsolutePath(), df.format(totalFileSize / 1024)));

			ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER);
			int readSize = in.read(buffer);
			long totalRead = 0;
			int progress = 0;

			long startTime, endTime;

			while (readSize != -1) {

				if (FastCopy.isStopThreads()) {
					rdProUI.println("[warn]Cancelled by user. Stoping copying.", true);
					rdProUI.println("\t" + Thread.currentThread().getName() + "is stopped.", true);
					return;
				}
				startTime = System.currentTimeMillis();
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


				endTime = System.currentTimeMillis();
				statistics.addToTotalFileSizeAndTime(totalFileSize, readSize / 1024, (endTime - startTime));
			}

			statistics.setFilesCount(statistics.getFilesCount() + 1);


		} catch (IOException e) {
			rdProUI.println(String.format("[error] Copy file %s to %s: %s", source.getAbsoluteFile(), target.getAbsolutePath(), e.getMessage()));
		} finally {
			close(in);
			close(out);
			try {
				boolean b = target.setLastModified(source.lastModified());
				//rdProUI.println("modify file date to: " + b + "," + new Timestamp(target.lastModified()));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
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


}
