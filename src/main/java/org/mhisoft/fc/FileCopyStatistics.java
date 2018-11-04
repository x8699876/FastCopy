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
package org.mhisoft.fc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.text.DecimalFormat;

//import com.sun.jna.Library;
//import com.sun.jna.Native;
//import com.sun.jna.WString;

/**
 * Description: Statistics on files and directories copied.
 *
 * @author Tony Xue
 * @since Nov, 2014
 */
public class FileCopyStatistics {
	private AtomicLong filesCount;
	private AtomicLong dirCount;
	private AtomicLong totalFileSize;
	private AtomicLong totalTime;


	private List<BucketBySize> bucketBySizeList;

	public FileCopyStatistics() {
		reset();
	}

	public void reset() {
		filesCount.set(0);
		dirCount.set(0);
		totalFileSize.set(0);
		totalTime.set(0);
		this.bucketBySizeList = new ArrayList<BucketBySize>();
		//4k, 1M, 100M, 500M
		bucketBySizeList.add(new BucketBySize(4*1024		, "<4K       "));
		bucketBySizeList.add(new BucketBySize(1000*1024		, "4K-1M     "));
		bucketBySizeList.add(new BucketBySize(100000*1024	, "1M-100M   "));
		bucketBySizeList.add(new BucketBySize(500000*1024	, "100M-500M "));
		bucketBySizeList.add(new BucketBySize(-1L		, "500M+     "));
	}

	public static class BucketBySize {
		String name;
		AtomicLong size;
		AtomicLong totalSize; //bytes

		AtomicLong totalTime;   // milli seconds
		double speed; //Byte Per Seconds
		double minSpeed = 0; //Byte Per Seconds
		double maxSpeed = 0; //Byte Per Seconds
		AtomicLong fileCount;

		public BucketBySize(long size, String name) {
			this.size.set( size );
			this.name = name;
		}

		/**
		 *
		 * @param size in bytes
		 * @param time in ms
		 */
		public void atomicAdd(long size, long time) {
			totalSize.addAndGet(size);
			totalTime.addAndGet(time);
		}

		public void incrementFileCount(){
			fileCount.incrementAndGet();
		}

	}



	public long getFilesCount() {
		return filesCount.get();
	}


	public double getTotalFileSize() {
		return totalFileSize.get();
	}


	public long getDirCount() {
		return dirCount.get();
	}





	public BucketBySize getBucket(long fsizeInBytes) {

		BucketBySize bucketBySize = null;
		for (BucketBySize entry : bucketBySizeList) {
			if (fsizeInBytes < entry.size.get()) {
				bucketBySize = entry;
				break;
			}
		}
		if (bucketBySize == null) {
			bucketBySize = bucketBySizeList.get(4);
		}
		return bucketBySize;
	}



	//in bytes and milli seconds, (ms)
	public void addToTotalFileSizeAndTime(final long totalFsizeInBytes, final long ftime) {

		this.totalFileSize.addAndGet(totalFsizeInBytes);
		this.totalTime.addAndGet(ftime);

		BucketBySize bucketBySize = getBucket(totalFsizeInBytes);
		//synchronized (bucketBySize) {
			bucketBySize.atomicAdd(totalFsizeInBytes, ftime);
		//}

	}



	/**
	 * @param fsize    in KB
	 * @param speed    KB/s
	 * @param fileTime milli sec
	 */
	/*public void setSpeedForBucket(double fsize, double speed, long fileTime) {
		if (fileTime<=0)
			return;

        BucketBySize bucketBySize = getBucket(fsize);
		bucketBySize.atomicAdd(fsize, fileTime);

		if (speed > 0) {
			bucketBySize.speed = speed;
			if (bucketBySize.minSpeed == 0 || speed < bucketBySize.minSpeed)
				bucketBySize.minSpeed = speed;
			if (bucketBySize.maxSpeed == 0 || speed > bucketBySize.maxSpeed)
				bucketBySize.maxSpeed = speed;
		}
	}*/

	static DecimalFormat df = new DecimalFormat("###,###.##");

	public String printBucketSpeedSummary() {

		StringBuilder sb = new StringBuilder();
		String avgSpeed; //MB/s

		for (BucketBySize entry : bucketBySizeList) {

			if (entry.totalTime.get() > 0) {
				// bytes/milliseconds convert to Mb/s
				double d= (entry.totalSize.get()*1000);
				d = d/(entry.totalTime.get()*1024*1024);
				avgSpeed = df.format(d);
			}
				else
				avgSpeed = "NA";

			long totalTimeInSeconds = entry.totalTime.get()/1000;

			sb.append("Files ").append(entry.name).append(": ")
//					.append(String.format("Max Speed: %s KB/s, Avg Speed:%s MB/s, ", df.format(entry.maxSpeed),avgSpeed ))
					.append( "Total Time:").append(totalTimeInSeconds).append(" (s)")
					.append( ", count:").append(entry.fileCount)
					.append(String.format(", Avg Speed:%s (Mb/s)", avgSpeed ))
					.append("\n");

		}

		return sb.toString();

	}

	public static String getAvgSpeedString(double fileSizeInKB, long totalTimeInMillis) {
		double avgSpeed = fileSizeInKB*1000/totalTimeInMillis;
		return df.format(avgSpeed);
	}



	String sOverall = "Total Files: %s, Total size: %s Mb, Took: %s ms, Overall Avg Speed=%s Mb/s";

	public String printOverallProgress() {
		double fsize = getTotalFileSize() / 1024 /1024;
		return String.format(sOverall
				, df.format(getFilesCount())
				, df.format(fsize)
				, df.format(this.totalTime)
				, df.format(fsize*1000/totalTime.get()));
	}


//	interface Kernel32 extends Library {
//		public int GetFileAttributesW(WString fileName);
//	}
//
//	static Kernel32 lib = null;
//	public static int getWin32FileAttributes(File f) throws IOException {
//		if (lib == null) {
//			synchronized (Kernel32.class) {
//				lib = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);
//			}
//		}
//		return lib.GetFileAttributesW(new WString(f.getCanonicalPath()));
//	}

//	public static boolean isJunctionOrSymlink(File f) throws IOException {
//		if (!f.exists()) { return false; }
//		int attributes = getWin32FileAttributes(f);
//		if (-1 == attributes) { return false; }
//		return ((0x400 & attributes) != 0);
//	}
//
//	public static void main(String[] args) {
//		try {
//			String link = "D:\\plateau-talent-management-b1408\\webapps\\learning";
//			boolean b = isJunctionOrSymlink( new File (link));
//			System.out.println("isJunctionOrSymlink=" + b);
//			Path p =  Paths.get(link);
//			boolean isSymbolicLink = Files.isSymbolicLink(p);
//			System.out.println("isSymbolicLink=" + isSymbolicLink);
//
//
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}



}
