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

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.mhisoft.fc.FileCopyStatistics;
import org.mhisoft.fc.RunTimeProperties;

/**
 * Description:  Swing UI implementation.
 *
 * @author Tony Xue
 * @since Nov, 2014
 */
public class GraphicsUIImpl extends AbstractUIImpl {

	JTextArea outputTextArea;
	JLabel labelStatus;
	JProgressBar progressBar;
	public static int bufferLineThreshold = 9999;
	private int lineNumber = 0;

	public GraphicsUIImpl(JTextArea outputTextArea) {
		this.outputTextArea = outputTextArea;
	}

	public GraphicsUIImpl() {
	}

	public JTextArea getOutputTextArea() {
		return outputTextArea;
	}

	public void setOutputTextArea(JTextArea outputTextArea) {
		this.outputTextArea = outputTextArea;
	}

	public JLabel getLabelStatus() {
		return labelStatus;
	}

	public void setLabelStatus(JLabel labelStatus) {
		this.labelStatus = labelStatus;
	}

	public void setProgressBar(JProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	@Override
	public void print(String msg) {
		print(msg, false);
	}

	@Override
	public void print(final String msg, boolean force) {
		//invokeLater()
		//This method allows us to post a "job" to Swing, which it will then run
		// on the event dispatch thread at its next convenience.

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Here, we can safely update the GUI
				// because we'll be called from the
				// event dispatch thread
				if (lineNumber >= bufferLineThreshold) {
					outputTextArea.setText("");
					lineNumber = 0;
				}
				outputTextArea.append(msg);
				//outputTextArea.setCaretPosition(outputTextArea.getDocument().getLength());
				lineNumber++;
			}
		});

	}

	@Override
	public void printError(String msg) {
		print("[Error] " + msg, true);
	}

	@Override
	public void println(final String msg) {
		print(msg + "\n");
	}


	@Override
	public void println(final String msg, boolean force) {
		print(msg + "\n", force);
	}

	@Override
	public void printf(final String msg, Object args) {
		//
	}

	@Override
	public boolean isAnswerY(String question) {
		int dialogResult = JOptionPane.showConfirmDialog(null, question, "Please confirm", JOptionPane.YES_NO_OPTION);
		return dialogResult == JOptionPane.YES_OPTION;
	}

	@Override
	public Confirmation getConfirmation(String question, String... options) {
		int dialogResult = JOptionPane.showConfirmDialog(null, question, "Please confirm", JOptionPane.YES_NO_OPTION);
		if (JOptionPane.YES_OPTION == dialogResult) {
			return Confirmation.YES;
		} else
			return Confirmation.NO;

		//todo support presend a check box to check Yes for all future confirmations
		//return  Confirmation.YES_TO_ALL
	}

	@Override
	public void help() {
		printBuildAndDisclaimer();
	}

	@Override
	public RunTimeProperties parseCommandLineArguments(String[] args) {
		List<String> noneHyfenArgs = new ArrayList<String>();


		RunTimeProperties props = RunTimeProperties.instance;

		if (args.length < 1 || args[0] == null || args[0].trim().length() == 0) {
			//JOptionPane.showMessageDialog(null, "The root dir to start with can't be determined from args[].", "Error"
			//		, JOptionPane.ERROR_MESSAGE);
			//props.setSuccess(false);
			props.setSourceDir(null);
		} else {
//			if (args.length>=1)
//				props.setSourceDir(args[0]);
//			if (args.length>=2)
//				props.setDestDir(args[1]);
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-help")) {
					help();
				} else if (arg.equalsIgnoreCase("-v")) {
					props.setVerbose(true);
				} else if (arg.equalsIgnoreCase("-debug")) {
					props.setDebug(true);
				} else if (arg.equalsIgnoreCase("-w")) {
					try {
						props.setNumOfThreads(Integer.parseInt(args[i + 1]));
						i++; //skip the next arg, it is the target.
					} catch (NumberFormatException e) {
						props.setNumOfThreads(1);
					}

				} else if (arg.equalsIgnoreCase("-from")) {
					props.setSourceDir(args[i + 1]);
					i++; //skip the next arg

				} else if (arg.equalsIgnoreCase("-to")) {
					props.setDestDir(args[i + 1]);
					i++; //skip the next arg

				} else {
					if (arg.startsWith("-")) {
						System.err.println("The argument is not recognized:" + arg);
						props.setSuccess(false);
						return props;
					} else
						//not start with "-"
						if (arg != null && arg.trim().length() > 0)
							noneHyfenArgs.add(arg);
				}
			}


		}


		if (noneHyfenArgs.size() == 0) {
			if (props.getSourceDir() == null || props.getSourceDir().length() == 0)
				props.setSourceDir(System.getProperty("user.dir"));
		} else if (noneHyfenArgs.size() == 1) {
			//fc d:\temp -dest classes
			if (props.getDestDir() != null)
				props.setSourceDir(noneHyfenArgs.get(0));

			else {
				//rdpro classes
				props.setSourceDir(System.getProperty("user.dir"));
				props.setDestDir(noneHyfenArgs.get(0));
			}

		} else {
			props.setSourceDir(noneHyfenArgs.get(0));
			props.setDestDir(noneHyfenArgs.get(1));
		}

		if (props.getSourceDir() == null)
			props.setSourceDir(System.getProperty("user.dir"));


		if (props.getSourceDir() == null)
			props.setSourceDir(System.getProperty("user.dir"));

		return props;
	}

	long lastProgressTime = -1;

	@Override
	public void reset() {
		lastProgressTime = -1;
		labelStatus.setText("");
		progressBar.setValue(0);
	}

	//0..100
	public void showProgress(int value, FileCopyStatistics statistics) {
		if (lastProgressTime == -1 || (System.currentTimeMillis() - lastProgressTime) > 1000) {
			progressBar.setValue(value);
			labelStatus.setText(statistics.printOverallProgress());
			lastProgressTime = System.currentTimeMillis();
		}
	}
}
