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

import org.mhisoft.fc.FileCopyStatistics;
import org.mhisoft.fc.RunTimeProperties;

/**
 * Description: The RdPro User Interface
 *
 * @author Tony Xue
 * @since Nov, 2014
 */
public interface UI {

	public static final String version = "v0.9 ";
	public static final String build = "build 102";

	public  enum Confirmation {
		YES, NO, YES_TO_ALL, HELP
	}


	/**
	 * log th emessage
	 * @param msg
	 */
	void print(String msg);

	/**
	 * log the msg
	 * @param msg
	 */
	void println(String msg);

	public  void printf(final String msg, Object args);

	public void printError(final String msg) ;

	/**
	 * Present a confirmation and return true if confirmed.
	 * @param question
	 * @return
	 */
	public  boolean isAnswerY(String question);

	/**
	 * Display help
	 */
	public  void help();


	/**
	 * Parse the arguments passed to the program
	 * @param args
	 * @return
	 */
	public RunTimeProperties parseCommandLineArguments(String[] args);


	/**
	 * Get a confirmation to the question.
	 * @param question
	 * @param options
	 * @return
	 */
	public Confirmation getConfirmation(String question, String... options);

	/**
	 * show progress
	 * @param value
	 */
	public void showProgress(int value, FileCopyStatistics statistics);

	public void reset() ;

	public void dumpArguments(String[] args, RunTimeProperties props) ;
}
