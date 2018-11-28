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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;

import org.mhisoft.fc.ui.ConsoleRdProUIImpl;
import org.mhisoft.fc.ui.UI;

/**
 * Description: Recursive Delete Pro
 *
 * @author Tony Xue
 * @since Sept 2014
 */
public class FastCopy {

	FileCopyStatistics frs = new FileCopyStatistics();
	public UI rdProUI;
	private MultiThreadExecutorService fileCopyWorkersPool = null;
	private MultiThreadExecutorService packageSmallFilesWorkersPool = null;


	public FastCopy(UI rdProUI) {
		this.rdProUI = rdProUI;

	}

	public UI getRdProUI() {
		return rdProUI;
	}

	public FileCopyStatistics getStatistics() {
		return frs;
	}



	public void stopWorkers() {
		if (fileCopyWorkersPool != null)
			fileCopyWorkersPool.shutDown();
		if (packageSmallFilesWorkersPool != null)
			packageSmallFilesWorkersPool.shutDown();
	}


	static DecimalFormat df = new DecimalFormat("#,###.##");

	public void run(RunTimeProperties props) {

		rdProUI.println(props.toString());

		frs.reset();


		try {
			fileCopyWorkersPool = new MultiThreadExecutorService(RunTimeProperties.instance.getNumOfThreads(), rdProUI);
			packageSmallFilesWorkersPool = new MultiThreadExecutorService(RunTimeProperties.instance.getNumberOfThreadsForPackageSmallFiles(), rdProUI);
			FileCopierService fileCopierService = new FileCopierService(rdProUI, props, frs, fileCopyWorkersPool, packageSmallFilesWorkersPool);
			long t1 = System.currentTimeMillis();

			RunTimeProperties.instance.setRunning( true );
			String[] files = props.sourceDir.split(";");
			fileCopierService.walkTreeAndCopy(0, files, props.getDestDir(), -1);

		} catch (Exception e) {
			rdProUI.printError("", e);
		} finally {

			if (packageSmallFilesWorkersPool != null) {
				packageSmallFilesWorkersPool.shutDownandWaitForAllThreadsToComplete();
				packageSmallFilesWorkersPool = null;
			}
			
			if (fileCopyWorkersPool != null) {
				fileCopyWorkersPool.shutDownandWaitForAllThreadsToComplete();
				fileCopyWorkersPool = null;

			}

			//reset the flags
			RunTimeProperties.instance.setRunning(false);
			RunTimeProperties.instance.setStopThreads(false);

		}

		rdProUI.println("");
		rdProUI.println("Done.");
		rdProUI.println("Copied from " + props.sourceDir + " to " + props.getDestDir());
		if (RunTimeProperties.instance.isDebug()) {
			rdProUI.println("\tFile copier workers count:" + RunTimeProperties.instance.getNumOfThreads());
			rdProUI.println("\tisPackageSmallFiles:" + RunTimeProperties.instance.isPackageSmallFiles());
			rdProUI.println("\tPackage Small Files workers count:" + RunTimeProperties.instance.getNumberOfThreadsForPackageSmallFiles());
		}

		rdProUI.println(frs.printBucketSpeedSummary());
		rdProUI.println("Dir copied:" + frs.getDirCount() + ", Files copied:" + frs.getFilesCount());
		rdProUI.println(frs.printOverallProgress());
	}


	public static void main(String[] args) {
		UI ui = new ConsoleRdProUIImpl();
		FastCopy fastCopy = new FastCopy(ui);
		FileUtils.instance.setRdProUI(ui);

		RunTimeProperties props = fastCopy.getRdProUI().parseCommandLineArguments(args);
		if (!props.isSuccess()) {
			System.exit(-1);
		}

		if (props.isDebug())
			fastCopy.getRdProUI().dumpArguments(args, props);

		if (props.getSourceDir() != null) {
			Path path = Paths.get(props.getSourceDir());
			if (Files.notExists(path)) {
				fastCopy.getRdProUI().printError("The source dir does not exist:" + props.getSourceDir());
				System.exit(-2);
			}


			boolean b = fastCopy.getRdProUI().isAnswerY(
					"Start to copy everything under \"" + props.getSourceDir() + "\"" +
							" to \"" + props.getDestDir() + "\"" +
							" (y/n/q or h for help)?");


			if (!b)
				System.exit(-2);
		}

		fastCopy.getRdProUI().print("working.");
		fastCopy.run(props);
	}

}


