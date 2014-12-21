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

import org.mhisoft.fc.ui.UI;

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
	UI rdProUI;
	FileCopyStatistics statistics;

	public FileWalker(UI rdProUI,
			Workers workerPool,
			FastCopy.RunTimeProperties props
			, FileCopyStatistics frs
	) {
		this.workerPool = workerPool;
		this.props = props;
		this.rdProUI = rdProUI;
		this.statistics = frs;
	}


	public void walk(final String[] dirs, final String destDir) {

		FileUtils.createDir(new File(destDir), rdProUI, statistics);

		for (String dir : dirs) {

			if (FastCopy.isStopThreads()) {
				rdProUI.println("[warn]Cancelled by user. stop walk. ");
				return;
			}

			File f = new File(dir);
			if (f.isFile()) {
				String sTarget = destDir + File.separator + f.getName();
				File targetFile = new File(sTarget);
				if (overrideTargetFile(f, targetFile)) {
					CopyFileThread t = new CopyFileThread(rdProUI, f, targetFile, props.verbose, statistics);
					workerPool.addTask(t);
				} else
					rdProUI.println(String.format("\tFile %s exists on the target dir. Skip. ", sTarget));
			} else if (f.isDirectory()) {
				walkSubDir(f, destDir);
			}
		}

	}


	private boolean overrideTargetFile(final File srcFile, final File targetFile) {
		if (targetFile.exists()) { //target file exists
			if (props.overwrite) {
				return true;
			} else if (props.isOverwriteIfNewerOrDifferent()) {
				if (srcFile.lastModified() > targetFile.lastModified()
						|| (srcFile.length() != targetFile.length())
						)
					return true;
				else
					return false;

			}
			else
				return false;


		}
		return true;

	}


	public void walkSubDir(final File rootDir, final String destRootDir) {

		if (FastCopy.isStopThreads()) {
			rdProUI.println("[warn]Cancelled by user. stop walk. ");
			return;
		}

		String targetDir;

	/*	if (!props.flatCopy) {
			//create the mirror dir in the dest
			targetDir = destRootDir + File.separator + rootDir.getName();
			FileUtils.createDir(new File(targetDir), rdProUI, statistics);
		} else {
			targetDir = props.getDestDir();
		}
*/

		File[] list = rootDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return true; //todo
			}
		});

		if (list == null)
			return;


		for (File childFile : list) {

			if (FastCopy.isStopThreads()) {
				rdProUI.println("[warn]Cancelled by user. stop walk. ");
				return;
			}

			if (childFile.isDirectory()) {
				//keep walking down

				if (!props.flatCopy) {
					//create the mirror child dir
					targetDir = destRootDir + File.separator + childFile.getName();
					FileUtils.createDir(new File(targetDir), rdProUI, statistics);
				} else {
					targetDir = props.getDestDir();
				}

				walkSubDir(childFile, targetDir);

			}
			else {


				if (!props.flatCopy) {
					targetDir = destRootDir ;
				} else {
					targetDir = props.getDestDir();
				}


				String newDestFile = targetDir + File.separator + childFile.getName();
				File targetFile = new File(newDestFile);
				if (overrideTargetFile(childFile, targetFile)) {
					CopyFileThread t = new CopyFileThread(rdProUI, childFile, targetFile, props.verbose, statistics);
					workerPool.addTask(t);
				} else {
					rdProUI.println(String.format("\tFile %s exists on the target dir. Skip based on the input. ", newDestFile));
				}

			}


		}   //loop all the files and dires under root

	}

}
