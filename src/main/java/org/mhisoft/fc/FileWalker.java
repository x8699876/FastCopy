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

	//Integer threads;
	boolean lastAnsweredDeleteAll = false;
	boolean initialConfirmation = false;
	Workers workerPool;
	UI rdProUI;
	FileCopyStatistics statistics;

	public FileWalker(UI rdProUI,
			Workers workerPool,
			RunTimeProperties props
			, FileCopyStatistics frs
	) {
		this.workerPool = workerPool;
		this.rdProUI = rdProUI;
		this.statistics = frs;
		rdProUI.reset();
	}


	public void walk(final String[] sourceFileDirs,  final String destDir) {

		FileUtils.createDir(new File(destDir), rdProUI, statistics);
		rdProUI.println("Copying files under directory " + destDir);
		String _targetDir =destDir;

		for (String source : sourceFileDirs) {

			if (FastCopy.isStopThreads()) {
				rdProUI.println("[warn]Cancelled by user. stop walk. ");
				return;
			}

			File fSource = new File(source);
			if (fSource.isFile()) {
				String sTarget = destDir + File.separator + fSource.getName();
				File targetFile = new File(sTarget);
				if (overrideTargetFile(fSource, targetFile)) {
					CopyFileThread t = new CopyFileThread(rdProUI, fSource, targetFile,   statistics);
					workerPool.addTask(t);
				} else {
					if (RunTimeProperties.instance.isVerbose())
					rdProUI.println(String.format("\tFile %s exists on the target dir. Skip. ", sTarget));
				}
			} else if (fSource.isDirectory()) {
				//   get the last dir of the source and make it under dest
				//ext  /Users/me/doc --> /Users/me/target make /Users/me/target/doc

				if (RunTimeProperties.instance.isCreateTheSameSourceFolderUnderTarget())   {
					//String sourceDirName =

					_targetDir=destDir+File.separator + fSource.getName() ;
					if (!new File(_targetDir).exists())
						FileUtils.createDir(new File(_targetDir), rdProUI, statistics);
				}
				walkSubDir(fSource, _targetDir);
			}
		}

	}


	private boolean overrideTargetFile(final File srcFile, final File targetFile) {
		if (targetFile.exists()) { //target file exists
			if (RunTimeProperties.instance.overwrite) {
				return true;
			} else if (RunTimeProperties.instance.isOverwriteIfNewerOrDifferent()) {
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
			rdProUI.println("[warn]Cancelled by user. stop walk. ", true);
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


		rdProUI.println("Copying files under directory " + destRootDir);

		//let's do some analysis on this directory first.
		//FileUtils.copyDirectory(rootDir, new File(destRootDir), statistics, rdProUI);

		copyFilesOneByOne(destRootDir, list);
		
		if (FastCopy.isStopThreads())
			return;


		//let copying files finish before moving on
		while ( workerPool.getNotCompletedTaskCount()>0) {
			//wait
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				//s
			}
		}

		/* walk down the directory recursively */
		 for (File childFile : list) {

			if (FastCopy.isStopThreads()) {
				rdProUI.println("[warn]Cancelled by user. stop walk. ", true);
				return;
			}

			if (childFile.isDirectory()) {
				//keep walking down

				if (!RunTimeProperties.instance.flatCopy) {
					//create the mirror child dir
					targetDir = destRootDir + File.separator + childFile.getName();
					FileUtils.createDir(new File(targetDir), rdProUI, statistics);
				} else {
					targetDir = RunTimeProperties.instance.getDestDir();
				}

				walkSubDir(childFile, targetDir);

			}


		}   //loop all the files and dires under root

	}

	/**
	 *
	 * @param destRootDir
	 * @param list
	 * @return was it interrupted by user.
	 */
	private boolean copyFilesOneByOne(String destRootDir, File[] list) {
		String targetDir;
		for (File childFile : list) {

			if (FastCopy.isStopThreads()) {
				rdProUI.println("[warn]Cancelled by user. stop walk. ", true);
				return true;
			}

			/* copy all the files under this dir first */
			if (childFile.isFile()) {

				if (!RunTimeProperties.instance.flatCopy) {
					targetDir = destRootDir ;
				} else {
					targetDir = RunTimeProperties.instance.getDestDir();
				}


				String newDestFile = targetDir + File.separator + childFile.getName();
				File targetFile = new File(newDestFile);
				if (overrideTargetFile(childFile, targetFile)) {
					CopyFileThread t = new CopyFileThread(rdProUI, childFile, targetFile,  statistics);
					workerPool.addTask(t);
				} else {
					if (RunTimeProperties.instance.isVerbose())
					rdProUI.println(String.format("\tFile %s exists on the target dir. Skip based on the input. ", newDestFile));
				}

			}


		}   //loop all the files
		return false;
	}

}
