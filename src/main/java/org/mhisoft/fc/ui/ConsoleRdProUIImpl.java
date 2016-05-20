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
	public void printError(String msg) {
		System.err.print("[error]" + msg);
	}

	@Override
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
		else if (!a.equalsIgnoreCase("y")) {
			return Confirmation.NO;
		}
		else if (!a.equalsIgnoreCase("q")) {
			return Confirmation.QUIT;
		}
		return Confirmation.YES;
	}

	public  void help() {
		printBuildAndDisclaimer();
		println("Usages:");
		println("\t fastcopy [option] -from source-dir -to target-dir ");
		println("\t source-dir: The source files and directories delimited with semicolon.");
		println("\t target-dir: The target directory.");
		println("\t Options: ");
		println("\t\t -v verbose mode");
		println("\t\t -m use multi thread for SSD");
		println("\t\t -o override");
		println("\t\t -f flat copy, copy everything to one flat target directory");
		println("\t\t -n override if new or different");
		/*println("\t -w number of worker threads, default 5");*/
		println("Examples:");
		println("\t\t fastcopy t:\\backup");
		println("\t\t fastcopy s:\\projects\\dir1;s:\\projects\\dir2 t:\\backup");
	}


	public RunTimeProperties parseCommandLineArguments(String[] args) {

		RunTimeProperties props = new RunTimeProperties();
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
			}else if (arg.equalsIgnoreCase("-o")) {
				props.setOverwrite(true);
			}else if (arg.equalsIgnoreCase("-n")) {
				props.setOverwriteIfNewerOrDifferent(true);
			}else if (arg.equalsIgnoreCase("-f")) {
				props.setFlatCopy(true);
			}
			else if (arg.equalsIgnoreCase("-w")) {

				try {
					props.setNumOfThreads(Integer.parseInt(args[i + 1]));
					i++; //skip the next arg, it is the target.
				} catch (NumberFormatException e) {
					props.setNumOfThreads( 1 );
				}

			} else if (arg.equalsIgnoreCase("-from") ) {
				props.setSourceDir(args[i + 1]);
				i++; //skip the next arg

			} else if (arg.equalsIgnoreCase("-to") ) {
				props.setDestDir(args[i + 1]);
				i++; //skip the next arg else {
				if (arg.startsWith("-")) {
					System.err.println("The argument is not recognized:" + arg);
					props.setSuccess(false);
					return props;
				} else
					//not start with "-"
					if (arg!=null && arg.trim().length()>0)
						noneHyfenArgs.add(arg);
			}
		}


		if (noneHyfenArgs.size() == 0) {
			props.setSourceDir(System.getProperty("user.dir"));
		}
		else if (noneHyfenArgs.size() == 1) {
			//fc d:\temp -dest classes
			if (props.getDestDir() != null)
				props.setSourceDir(noneHyfenArgs.get(0));

			else {
				//rdpro classes
				props.setSourceDir(System.getProperty("user.dir"));
				props.setDestDir(noneHyfenArgs.get(0));
			}

		}
		else {
				props.setSourceDir(noneHyfenArgs.get(0));
				props.setDestDir(noneHyfenArgs.get(1));
		}

		if (props.getSourceDir() == null)
			props.setSourceDir(System.getProperty("user.dir"));


		println("");


		if (props.getDestDir() == null) {
			if (!isAnswerY("Specify a destination directory:"))
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
}
