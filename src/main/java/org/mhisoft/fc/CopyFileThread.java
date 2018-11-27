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

	private FileCopyStatistics statistics;
	private UI rdProUI;

	private File sourceFile;
	private File targetFile;
	private FileUtils.CompressedackageVO compressedackageVO;


	public CopyFileThread(UI rdProUI
			, File sourceFile, File targetFile
			, FileUtils.CompressedackageVO compressedackageVO
			, FileCopyStatistics frs) {
		this.sourceFile = sourceFile;
		this.targetFile = targetFile;
		this.statistics = frs;
		this.rdProUI = rdProUI;
		this.compressedackageVO = compressedackageVO;
	}

	@Override
	public void run() {

		if (!RunTimeProperties.instance.isStopThreads()) {

			if (RunTimeProperties.instance.isDebug())
				rdProUI.println(Thread.currentThread().getName() + " Starts");
			long t1 = System.currentTimeMillis();

			FileUtils.instance.copyFile(sourceFile, targetFile, statistics, rdProUI, compressedackageVO);


			if (RunTimeProperties.instance.isDebug())
				rdProUI.println("\n"
						+ Thread.currentThread().getName() + " End. took " + (System.currentTimeMillis() - t1) + "ms");
		} else {
			if (RunTimeProperties.instance.isDebug())
				rdProUI.println("\n" + Thread.currentThread().getName() + "is stopped.");

		}

	}


}
