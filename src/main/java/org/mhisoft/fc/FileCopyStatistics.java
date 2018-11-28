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


/**
 * Description: Statistics on files and directories copied.
 *
 * @author Tony Xue
 * @since Nov, 2014
 */
public class FileCopyStatistics {
	private AtomicLong filesCount= new AtomicLong(0);
	private AtomicLong dirCount=new AtomicLong(0);
	private AtomicLong totalFileSize=new AtomicLong(0);
	private AtomicLong totalTime=new AtomicLong(0);


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
		AtomicLong size =new AtomicLong(0);
		AtomicLong totalSize =new AtomicLong(0); //bytes
		AtomicLong totalTime =new AtomicLong(0);   // milli seconds

		AtomicLong fileCount =new AtomicLong(0);

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

	public void addFileCount(long value) {
		filesCount.addAndGet(value);
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

	public void incrementFileCount() {
		this.filesCount.incrementAndGet();
	}

    public void incrementDirCount() {
		this.dirCount.incrementAndGet();
	}



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

			sb.append("Files ").append(entry.name).append(": ")
					.append( "Total Time:").append(getDisplayTime(entry.totalTime.get()))
					.append( ", count:").append(entry.fileCount)
					.append(String.format(", Avg Speed:%s (Mb/s)", avgSpeed ))
					.append("\n");

		}

		return sb.toString();

	}


	public static String getDisplayTime(final long millis) {
		double _d= millis;
		if (millis<1000) {
			return  millis + " (ms)";
		}
		else {
			return  df.format(_d/1000) + " (s)";
		}
	}

	public static String getAvgSpeedString(double fileSizeInKB, long totalTimeInMillis) {
		double avgSpeed = fileSizeInKB*1000/totalTimeInMillis;
		return df.format(avgSpeed);
	}



	String sOverall = "Directories: %s, Total Files: %s, Total size: %s Mb, Took: %s, Overall Avg Speed=%s Mb/s";

	public String printOverallProgress() {
		double fsize = getTotalFileSize() / 1024 /1024;
		return String.format(sOverall
				, df.format(getDirCount())
				, df.format(getFilesCount())
				, df.format(fsize)
				, getDisplayTime(this.totalTime.get())
				, df.format(fsize*1000/totalTime.get()));
	}



}
