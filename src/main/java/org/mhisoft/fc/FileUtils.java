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

	private static final int BUFFER = 4096*16;
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
		long t1 = System.currentTimeMillis();
		double size = 0;
		rdProUI.showProgress(0, statistics);

		try {
			in = new FileInputStream(source).getChannel();
			out = new FileOutputStream(target).getChannel();
			size = in.size();
			double size2InKB = size / 1024 ;
			rdProUI.print(String.format("\nCopying file %s, size:%s KBytes", target.getAbsolutePath(), df.format(size2InKB)));

			ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER);
			int readSize = in.read(buffer);
			long totalSize = 0;
			int progress = 0;

			long startTime, endTime  ;
			long overallT1 =  System.currentTimeMillis();

			while (readSize != -1) {

				if (FastCopy.isStopThreads()) {
					rdProUI.println("[warn]Cancelled by user. Stoping copying.");
					return;
				}

				totalSize = totalSize + readSize;
				startTime = System.currentTimeMillis();
				progress = (int) (totalSize * 100 / size);
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
				statistics.addToTotalFileSizeAndTime(readSize / 1024, (endTime - startTime));
			}

			statistics.setFilesCount(statistics.getFilesCount()+1);
			long overallT2 =  System.currentTimeMillis();
			statistics.setSpeedForBucket(size2InKB, 0,  (overallT2 - overallT1));


			//long t2 = System.currentTimeMillis();

//			double speed = 0;
//			if (size > 0 && t2 - t1 > 0) {
//				speed = size2InKB * (10 ^ 6) / (t2 - t1);  //KB/s
//				statistics.setSpeedForBucket(size2InKB, speed, t2 - t1);
//			}

			//rdProUI.println(String.format(", speed:%s KByte/Second.", df.format(speed)));

		} catch (IOException e) {
			rdProUI.println(String.format("[error] Copy file %s to %s: %s", source.getAbsoluteFile(), target.getAbsolutePath(), e.getMessage()));
		} finally {
			close(in);
			close(out);

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
