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
import java.io.FilenameFilter;

import org.mhisoft.fc.ui.RdProUI;

/**
 * Description: walk the directory and schedule works to remove the target files and directories.
 *
 * @author Tony Xue
 * @since Oct, 2014
 */
public class FileWalker {

	FastCopy.RunTimeProperties props;
	//Integer threads;
	boolean lastAnsweredDeleteAll = false;
	boolean initialConfirmation = false;
	Workers workerPool;
	RdProUI rdProUI;
	FileCopyStatistics statistics;

	public FileWalker(RdProUI rdProUI,
			Workers workerPool,
			FastCopy.RunTimeProperties props
			, FileCopyStatistics frs
	) {
		this.workerPool = workerPool;
		this.props = props;
		this.rdProUI = rdProUI;
		this.statistics = frs;
	}


	public void walk(final String[] files, final String destDir) {
		for (String file : files) {
			File f = new File(file);
			if (f.isFile()) {
				File targetFile = new File(destDir + File.separator + f.getName());
				//FileUtils.nioBufferCopy(f, targetFile, statistics, rdProUI);
				CopyFileThread t = new CopyFileThread(rdProUI, f, targetFile, props.verbose, statistics);
				workerPool.addTask(t);
			} else if (f.isDirectory()) {
				walkDir(f, destDir);
			}
		}

	}


	public void walkDir(final File rootDir, final String destRootDir) {

		String targetDir = destRootDir+File.separator+rootDir.getName();
		File targetDirF = new File(targetDir);

		FileUtils.createDir(targetDirF, rdProUI, statistics);

		File[] list = rootDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return true; //todo
			}
		});

		if (list == null)
			return;


		for (File childFile : list) {
			if (childFile.isDirectory()) {
					//keep walking down
				walkDir(childFile,  targetDir);

			}
			else {

				String newDestFile =  targetDir +File.separator + childFile.getName();
				//FileUtils.nioBufferCopy(childFile, new File(newDestFile), statistics, rdProUI);

				CopyFileThread t = new CopyFileThread(rdProUI, childFile, new File(newDestFile), props.verbose, statistics);
				workerPool.addTask(t);

			}

			if (FastCopy.isStopThreads()) {
				rdProUI.print("[warn]Cancelled by user.");
				return;
			}


		}   //loop all the files and dires under root

	}

}
