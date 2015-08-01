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
	public static boolean debug = Boolean.getBoolean("debug");
	public static final int DEFAULT_THREAD_NUM =5;

	FileCopyStatistics frs = new FileCopyStatistics();
	Workers workerPool;

	public UI rdProUI;

	public FastCopy(UI rdProUI) {
		this.rdProUI = rdProUI;
	}

	public UI getRdProUI() {
		return rdProUI;
	}

	public FileCopyStatistics getStatistics() {
		return frs;
	}

	public static boolean stopThreads=false;
	private boolean running;

	public static boolean isStopThreads() {
		return stopThreads;
	}

	public static void setStopThreads(boolean stopThreads) {
		FastCopy.stopThreads = stopThreads;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	static DecimalFormat df = new DecimalFormat("#,###.##");

	public void run(RunTimeProperties props) {
		workerPool = new Workers(props.getNumOfThreads(), rdProUI);

		frs.reset();
		FileWalker fw = new FileWalker(rdProUI, workerPool, props, frs);
		long t1 = System.currentTimeMillis();

		running= true;

		String[] files = props.sourceDir.split(";");
		fw.walk( files, props.getDestDir());

		workerPool.shutDownandWaitForAllThreadsToComplete();

		//reset the flags
		running= false;
		stopThreads = false;

		rdProUI.println("");
		rdProUI.println("Done.");
		rdProUI.println(frs.printSpeed());
		rdProUI.println("Dir copied:" + frs.getDirCount() + ", Files copied:" + frs.getFilesCount());
		rdProUI.println(frs.printOverallProgress());
	}


	public static void main(String[] args) {
		FastCopy fastCopy = new FastCopy(new ConsoleRdProUIImpl());
		RunTimeProperties props = fastCopy.getRdProUI().parseCommandLineArguments(args);

		if (props.isDebug())
			fastCopy.getRdProUI().dumpArguments(args, props);

		Path path = Paths.get(props.getSourceDir()) ;
		if (Files.notExists(path)) {
			fastCopy.getRdProUI().printError("The source dir does not exist:" +  props.getSourceDir()) ;
			System.exit(-2);
		}


		boolean b =fastCopy.getRdProUI().isAnswerY(
				"Start to copy everything under \"" + props.getSourceDir() + "\"" +
						"to \"" +props.getDestDir()+"\""  +
						" (y/n/q or h for help)?");


		if (!b)
			System.exit(-2);


		if (props.isSuccess()) {
			fastCopy.getRdProUI().print("working.");
			fastCopy.run(props);
		}
	}

}


