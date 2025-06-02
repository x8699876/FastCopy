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
package org.mhisoft.fc.ui;

import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.mhisoft.fc.FileCopyStatistics;
import org.mhisoft.fc.LogLevel;
import org.mhisoft.fc.RunTimeProperties;

/**
 * Description: Console UI
 *
 * @author Tony Xue
 * @since Nov, 2014
 */
public class ConsoleRdProUIImpl extends AbstractUIImpl {

	@Override
	public void print(final String msg) {
		System.out.print(msg);
	}

	@Override
	public void print(String msg, boolean force) {
		print(msg);
	}

	@Override
	public void printError(String msg) {
		System.err.print("[error]" + msg);
	}

	@Override
	public  void println(final String msg, boolean force) {
		println(msg);
	}
	public  void println(final String msg) {
		System.out.println(msg);
	}

	@Override
	public  void printf(final String msg, Object args) {
		System.out.printf(msg, args);
	}


	@Override
	public  boolean isAnswerY(String question) {
		Confirmation a = getConfirmation(question, "y", "n", "h", "q");
		if (a==Confirmation.QUIT)
			System.exit(-2);

		if (a==Confirmation.HELP) {
			help();
			return false;
		} else if (Confirmation.YES!=a) {
			return false;
		}
		return true;
	}



	@Override
	public Confirmation getConfirmation(String question, String... options) {

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		print(question);
		String a = null;

		List<String> optionsList = new ArrayList<String>();
		for (String option : options) {
			optionsList.add(option.toLowerCase());
			optionsList.add(option.toUpperCase());
		}

		try {
			while (a == null || a.trim().length() == 0 ) {
				a = br.readLine();
				if ( a!=null && !optionsList.contains(a)) {
					print("\tresponse \"" + a + "\" not recognized. input again:");
					a=null; //keep asking
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (a.equalsIgnoreCase("h")) {
			return Confirmation.HELP;
		}
		if (a.equalsIgnoreCase("all")) {
			return Confirmation.YES_TO_ALL;
		}
		else if (a.equalsIgnoreCase("y")) {
			return Confirmation.YES;
		}
		else if (a.equalsIgnoreCase("n")) {
			return Confirmation.NO;
		}
		else if (a.equalsIgnoreCase("q")) {
			return Confirmation.QUIT;
		}
		return Confirmation.NO;
	}

	public  void help() {
		printBuildAndDisclaimer();
		println("Usages:");
		println("\t fc [option] -from source_dir -to target_dir ");
		println("\t source-dir: The source files and directories delimited with semicolon.");
		println("\t target-dir: The target directory.");
		println("\t Options: ");
		println("\t\t -v      verbose mode.");
		println("\t\t -verify verify each file copy by comparing the file content hash.");
		println("\t\t -m      use multiple threads, best for copying across the SSD drives.");
		println("\t\t -w      number of worker threads in the multi threads mode, default:" + RunTimeProperties.DEFAULT_THREAD_NUM+".");
		println("\t\t -o      always override.");
		println("\t\t -n      override only when the source file newer or different in size.");
		println("\t\t -f      flat copy, copy everything to the same target directory.");
		println("\t\t -pack   Package the small files first to speed up the copy, requires write access on the source folder or drive.");
		println("\t\t -k      Keep the original file timestamp.");
		println("\t\t -sf     Create the same source folder under the target and copies to it.");
		println("Examples:");
		println("\t\t copy from current dir to the backup directory: fastcopy t:\\backup");
		println("\t\t fastcopy -from s:\\projects\\dir1;s:\\projects\\dir2 -to t:\\backup");
	}


	public RunTimeProperties parseCommandLineArguments(String[] args) {

		RunTimeProperties props = RunTimeProperties.instance;
		List<String> noneHyfenArgs = new ArrayList<String>();

		if (args==null || args.length==0) {
			help();
			props.setSuccess(false);
			return props;
		}

		props.setNumOfThreads( 1 );

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-help")) {
				help();
				props.setSuccess(false);
				return props;
			} else if (arg.equalsIgnoreCase("-v")) {
				props.setVerbose(true);
			}
			else if (arg.equalsIgnoreCase("-debug")) {
				props.setDebug(true);
			}else if (arg.equalsIgnoreCase("-m")) {
				props.setNumOfThreads(4);
			}
			else if (arg.equalsIgnoreCase("-o")) {
				props.setOverrideTarget(true);
			}else if (arg.equalsIgnoreCase("-pack")) {
				props.setPackageSmallFiles(true);
			}
			else if (arg.equalsIgnoreCase("-k")) {
				props.setKeepOriginalFileDates(true);
			}
			else if (arg.equalsIgnoreCase("-n")) {
				props.setOverwriteIfNewerOrDifferent(true);
			}else if (arg.equalsIgnoreCase("-f")) {
				props.setFlatCopy(true);
			}else if (arg.equalsIgnoreCase("-verify")) {
				props.setVerifyAfterCopy(true);
			}else if (arg.equalsIgnoreCase("-sf")) {
				props.setCreateTheSameSourceFolderUnderTarget(true);
			}
			else if (arg.equalsIgnoreCase("-w")) {

				try {
					props.setNumOfThreads(Integer.parseInt(args[i + 1]));
					i++; //skip the next arg, it is the target.
				} catch (NumberFormatException e) {
					props.setNumOfThreads( 1 );
				}
			} else if (arg.equalsIgnoreCase("-from") ) {

				if (args.length>i+1)
					props.setSourceDir(args[i + 1]);
				else  {
					System.err.println("No value for -from is specified");
					props.setSuccess(false);
					return props;
				}
				i++; //skip the next arg

			} else if (arg.equalsIgnoreCase("-to") ) {
				if (args.length>i+1)
					props.setDestDir(args[i + 1]);
				else  {
					System.err.println("No value for -to is specified");
					props.setSuccess(false);
					return props;
				}

				i++; //skip the next arg
			} else {
				if (arg.startsWith("-")) {
					System.err.println("The option argument is not recognized:" + arg);
					props.setSuccess(false);
					return props;
				} else
					//not start with "-"
					if (arg!=null && arg.trim().length()>0)
						noneHyfenArgs.add(arg);
			}
		}


		//use the none hyfen args to fill in the source and  dest if needed.
		//when -from and -to are not specified. 
		if (props.getSourceDir()==null && noneHyfenArgs.size()>=1)
			props.setSourceDir(noneHyfenArgs.get(0));

		if (props.getDestDir()==null && noneHyfenArgs.size()>=2) {
				props.setDestDir(noneHyfenArgs.get(1));
		}

		//now default to user current directory for source
		// fc -to target_dir
		if (props.getSourceDir()==null || props.getSourceDir().length()==0)
			props.setSourceDir(System.getProperty("user.dir"));

		println("");

		if (props.getDestDir()==null) {
			System.err.println("Specify the target directory to copy to by using -to dest_dir");
			props.setSuccess(false);
			return props;
		}

		props.setSuccess(true);
		return props;

	}

	@Override
	public void showProgress(int value, FileCopyStatistics statistics) {
		//none
	}

	@Override
	public void reset() {
		//
	}

	@Override
	public void showCurrentDir(String text) {
		println(text);
	}

	@Override
	public void print(LogLevel logLevel, String msg) {
		
	}

	@Override
	public void println(LogLevel logLevel, String msg) {

	}

	@Override
	public void updateWallClock(long startTime) {
		//
	}
}
