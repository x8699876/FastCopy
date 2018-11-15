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

import java.io.File;

import org.mhisoft.fc.ui.UI;

/**
 * Description: CopyFileThread
 *
 * @author Tony Xue
 * @since Dec 2014
 */
public class CopyFileThread implements Runnable {

	static final int TRIGGER_MULTI_THREAD_THRESHHOLD = 20;

	private String sRootDir, sTargetDir;
	private FileCopyStatistics statistics;
	private UI rdProUI;

	File sourceFile;
	File targetFile;
	boolean compress;



	public CopyFileThread(UI rdProUI
			, String sRootDir, String sTargetDir
			, File sourceFile, File targetFile
			, boolean compress
			, FileCopyStatistics frs) {
		this.sRootDir = sRootDir;
		this.sTargetDir = sTargetDir;
		this.sourceFile = sourceFile;
		this.targetFile = targetFile;
		this.statistics = frs;
		this.compress = compress;
		this.rdProUI = rdProUI;
	}

	@Override
	public void run() {

		if (!FastCopy.isStopThreads()) {

			if (RunTimeProperties.instance.isDebug())
				rdProUI.println(Thread.currentThread().getName() + " Starts");
			long t1 = System.currentTimeMillis();

			if (compress) {

				FileUtils.CompressedackageVO vo = FileUtils.compressDirectory(sRootDir, false, FileWalker.SMALL_FILE_SIZE);
				vo.setDestDir(sTargetDir);
			    File targetZipFile = new File(sTargetDir + File.separator + vo.zipName);
				FileUtils.copyFile(new File( vo.sourceZipFileWithPath ) , targetZipFile , statistics, rdProUI, vo);
				if (RunTimeProperties.instance.isDebug()) {
					rdProUI.println("Zip up the "+ sRootDir +" dir to zip:"+ targetZipFile + ", size=" + vo.zipFileSizeBytes +" Bytes");
				}
			}
			else
				FileUtils.copyFile(sourceFile, targetFile, statistics, rdProUI, null);



			if (RunTimeProperties.instance.isDebug())
				rdProUI.println("\n"
						+ Thread.currentThread().getName() + " End. took " + (System.currentTimeMillis() - t1) + "ms");
		} else {
			rdProUI.println("\n" + Thread.currentThread().getName() +   "is stopped.");

		}

	}


}
