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
 * Description: PackageSmallFilesThread for zip up the small files
 *
 * @author Tony Xue
 * @since Nov 2018
 */
public class PackageSmallFilesThread implements Runnable {


	private String sSourceDir, sTargetDir;
	private FileCopyStatistics statistics;
	private UI rdProUI;


	private MultiThreadExecutorService fileCopyWorkersPool;


	public PackageSmallFilesThread(UI rdProUI
			, String sSourceDir, String sTargetDir
			, FileCopyStatistics frs
			, MultiThreadExecutorService fileCopyWorkersPool) {
		this.sSourceDir = sSourceDir;
		this.sTargetDir = sTargetDir;
		this.statistics = frs;
		this.rdProUI = rdProUI;
		this.fileCopyWorkersPool = fileCopyWorkersPool;
	}

	@Override
	public void run() {

		if (!RunTimeProperties.instance.isStopThreads()) {

			FileUtils.CompressedackageVO compressedackageVO = null;
			try {
				compressedackageVO = FileUtils.instance.compressDirectory(sSourceDir, sTargetDir, false, FileCopierService.SMALL_FILE_SIZE);
			} catch (Exception e) {
				rdProUI.printError("compressDirectory failed for " + sSourceDir, e);
				fallbackToCopyFilesDirectly();
				return;

			}


			if (compressedackageVO.getNumberOfFiles() > 0) {

				if (RunTimeProperties.instance.isDebug())
					rdProUI.println("[PackageSmallFilesThread] " + Thread.currentThread().getName() + " Starts");
				long t1 = System.currentTimeMillis();


				compressedackageVO.setDestDir(sTargetDir);
				File targetZipFile = new File(sTargetDir + File.separator + compressedackageVO.zipName);

				if (RunTimeProperties.instance.isDebug()) {
					rdProUI.println("[PackageSmallFilesThread] Ziped up the " + sSourceDir + " dir to zip:" + targetZipFile + ", size=" + compressedackageVO.zipFileSizeBytes + " Bytes");
				}


				//FileUtils.copyFile(new File(vo.sourceZipFileWithPath), targetZipFile, statistics, rdProUI, vo);
				/* then send it to the copier  thread */
				CopyFileThread t = new CopyFileThread(rdProUI //
						, new File(compressedackageVO.sourceZipFileWithPath), targetZipFile //
						, compressedackageVO //
						, statistics);

				fileCopyWorkersPool.addTask(t);


				if (RunTimeProperties.instance.isDebug())
					rdProUI.println("[PackageSmallFilesThread]\n"
							+ Thread.currentThread().getName() + " End. took " + (System.currentTimeMillis() - t1) + "ms");
			}

		} else {
			if (RunTimeProperties.instance.isDebug())
				rdProUI.println("[PackageSmallFilesThread]\n" + Thread.currentThread().getName() + "is stopped.");

		}

	}

	private void fallbackToCopyFilesDirectly() {
		File[] files = new File(sSourceDir).listFiles();
		rdProUI.println(LogLevel.debug, "Fall back to copy files directly for dir:" + sSourceDir );
		if (files!=null) {
			for (File childFile : files) {

				if (RunTimeProperties.instance.isStopThreads()) {
					rdProUI.println("[PackageSmallFilesThread]\n" + Thread.currentThread().getName() + "is stopped.");
					return;
				}


				String newDestFile = sTargetDir + File.separator + childFile.getName();
				File targetFile = new File(newDestFile);
				if (!targetFile.exists() || FileUtils.overrideTargetFile(childFile, targetFile)) {
					CopyFileThread t = new CopyFileThread(rdProUI
							, childFile, targetFile, null, statistics);
					fileCopyWorkersPool.addTask(t);
				} else {
						rdProUI.println(LogLevel.debug, String.format("\tFile %s exists on the target dir, skipped. ", newDestFile));
				}
			}

		}

	}


}
