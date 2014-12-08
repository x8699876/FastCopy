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
import java.text.DecimalFormat;

/**
* Description: Statistics on files and directories copied.
*
* @author Tony Xue
* @since Nov, 2014
*/
public class FileCopyStatistics {
	long filesCount;
	long dirCount;
	long totalFileSize =0;

	public void reset() {
		filesCount =0;
		dirCount =0;
		totalFileSize =0;
		this.bucketBySizeList = new ArrayList<BucketBySize>();
		//4k, 1M, 100M, 500M
		bucketBySizeList.add(new BucketBySize(4L, "<4K"));
		bucketBySizeList.add(new BucketBySize(1000L, "4K-1M"));
		bucketBySizeList.add(new BucketBySize(100000L, "1M-100M"));
		bucketBySizeList.add(new BucketBySize(500000L, "100M-500M"));
		bucketBySizeList.add(new BucketBySize(-1L, "500M+"));
	}

	public static class BucketBySize {
		String name ;
		long size ;
		double  speed ; //Byte Per Seconds
		double  minSpeed=0 ; //Byte Per Seconds
		double  maxSpeed=0; //Byte Per Seconds

		public BucketBySize(long size, String name) {
			this.size = size;
			this.name = name;
		}
	}

	List<BucketBySize> bucketBySizeList;

	public FileCopyStatistics() {
		reset();
	}

	public long getFilesCount() {
		return filesCount;
	}


	public long getTotalFileSize() {
		return totalFileSize;
	}

	public void setFilesCount(long filesCount) {
		this.filesCount = filesCount;
	}

	public long getDirCount() {
		return dirCount;
	}

	public void setDirCount(long dirCount) {
		this.dirCount = dirCount;
	}

	//fsize is in KB.
	public void setSpeed(double fsize, double speed) {

		BucketBySize bucketBySize = null;
		for (BucketBySize entry : bucketBySizeList) {
			if (fsize<entry.size) {
				bucketBySize = entry;
				break;
			}
		}
		if (bucketBySize==null) {
			bucketBySize =  bucketBySizeList.get(4);
		}

		if (speed>0) {
			bucketBySize.speed = speed;
			if (bucketBySize.minSpeed==0 || speed < bucketBySize.minSpeed)
				bucketBySize.minSpeed = speed;
			if (bucketBySize.maxSpeed==0 || speed > bucketBySize.maxSpeed)
				bucketBySize.maxSpeed = speed;
		}
	}

	DecimalFormat df = new DecimalFormat("#,###.##");

	public String  printSpeed() {

		StringBuilder sb = new StringBuilder();

		for (BucketBySize entry : bucketBySizeList) {
			sb.append("Files ").append(entry.name).append(": ")
					.append(String.format("Min Speed:%s KB/s, Max Speed: %s KB/s", df.format(entry.minSpeed), df.format(entry.maxSpeed)))
					.append("\n");

		}

		return sb.toString();

	}

	String s12=  "Total Files: %s, Total size: %s MB";
	public String printOverallProgress() {
		double fsize = getTotalFileSize()/1024;
		return  String.format(s12, df.format(getFilesCount()),  df.format(fsize));
	}


	//in KB
	public void addFileSize(final long fsize) {
		totalFileSize = totalFileSize + fsize;
	}


}
