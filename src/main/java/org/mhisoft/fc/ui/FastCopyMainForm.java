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

import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.mhisoft.fc.FastCopy;
import org.mhisoft.fc.RunTimeProperties;

import com.googlecode.vfsjfilechooser2.VFSJFileChooser;
import com.googlecode.vfsjfilechooser2.accessories.DefaultAccessoriesPanel;

/**
 * Description:
 *
 * @author Tony Xue
 * @since Nov, 2014
 */
public class FastCopyMainForm {

	JFrame frame;
	FastCopy fastCopy;
	RunTimeProperties props;


	JCheckBox chkMultiThread;
	JCheckBox chkShowInfo;

	JPanel layoutPanel1;
	JLabel labelDirName;
	JTextArea outputTextArea;
	JScrollPane outputTextAreaScrollPane;
	private JButton btnOk;
	private JButton btnCancel;
	private JButton btnHelp;
	private JTextField fldTargetDir;
	private JLabel labelStatus;
	private JTextField fldSourceDir;
	private JCheckBox chkOverrideAlways;
	private JProgressBar progressBar1;
	private JCheckBox chkFlat;
	private JCheckBox overrideOnlyIfNewerCheckBox;
	private JPanel progressPanel;
	private JButton btnSourceBrowse;
	private JButton btnTargetBrowse;
	private JCheckBox chkCreateTheSameSourceCheckBox;

	GraphicsUIImpl uiImpl;
	DoItJobThread doItJobThread;

	public void setRdProUI(GraphicsUIImpl rdProUI) {
		this.uiImpl = rdProUI;
	}

	public void stopIt() {
		fastCopy.setStopThreads(true);
		progressPanel.setVisible(false);

		fastCopy.stopWorkers();

		//main thread
		doItJobThread.interrupt();


		//set running false only afer all threads are shutdown.
		fastCopy.setRunning(false);
		btnCancel.setText("Close");

	}


	public FastCopyMainForm() {

		chkMultiThread.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//outputTextArea.append("Value of the checkbox:" + chkForceDelete.isSelected());
			}
		});

		chkShowInfo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showHideInfo(chkShowInfo.isSelected());
			}
		});

		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (fastCopy.isRunning()) {
					stopIt();

				} else {
					frame.dispose();
					System.exit(0);
				}
			}
		});


		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//Don't block the EDT
				//probably using the Swing thread which is waiting
				// for your code to execute before it can update the UI. Try using a separate thread for that loop.
				//just do invokeLater() as below does not work.


//				SwingUtilities.invokeLater(new Runnable() {
//					@Override
//					public void run() {
				//doit();
//					}
//				});

				doItJobThread = new DoItJobThread();
				doItJobThread.start();

			}
		});

		btnHelp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				outputTextArea.setText("");
				showHideInfo(true);
				fastCopy.getRdProUI().help();
				scrollToTop();

			}
		});
		overrideOnlyIfNewerCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (overrideOnlyIfNewerCheckBox.isSelected())
					chkOverrideAlways.setSelected(false);
			}


		});


		btnSourceBrowse.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				String dir = fldSourceDir.getText().trim();
				int k = dir.indexOf(";");
				if (k > 0)
					dir = dir.substring(0, k);

				File[] files = chooseFiles(new File(dir), VFSJFileChooser.SELECTION_MODE.FILES_AND_DIRECTORIES )  ;
				if (files!=null && files.length>0)   {
					StringBuilder builder = new StringBuilder();
					for (File file : files) {
						if (builder.length() > 0)
							builder.append(";");
						builder.append(file.getAbsolutePath());

					}
					props.setSourceDir(builder.toString());
					fldSourceDir.setText(props.getSourceDir());
				}


			}
		});


		btnTargetBrowse.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				String dir = fldTargetDir.getText().trim();
				int k = dir.indexOf(";");
				if (k > 0)
					dir = dir.substring(0, k);

				File[] files = chooseFiles(new File(dir), VFSJFileChooser.SELECTION_MODE.DIRECTORIES_ONLY )  ;
				if (files!=null && files.length>0)   {
					props.setDestDir(files[0].getAbsolutePath().toString());
					fldTargetDir.setText(props.getDestDir());
				}


			}
		});


	}


	File[] chooseFiles(final File currentDir, VFSJFileChooser.SELECTION_MODE selectionMode) {
		// create a file chooser
		final VFSJFileChooser fileChooser = new VFSJFileChooser();

		// configure the file dialog
		fileChooser.setAccessory(new DefaultAccessoriesPanel(fileChooser));
		fileChooser.setFileHidingEnabled(false);
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setFileSelectionMode(selectionMode);
		fileChooser.setCurrentDirectory(currentDir);

		// show the file dialog
		VFSJFileChooser.RETURN_TYPE answer = fileChooser.showOpenDialog(null);

		// check if a file was selected
		if (answer == VFSJFileChooser.RETURN_TYPE.APPROVE) {
			final File[] files = fileChooser.getSelectedFiles();

//			// remove authentication credentials from the file path
//			final String safeName = VFSUtils.getFriendlyName(aFileObject.toString());
//
//			System.out.printf("%s %s", "You selected:", safeName);
			return files;
		}
		return null;
	}

	public void showHideInfo(boolean visible) {
		outputTextArea.setVisible(visible);
		outputTextAreaScrollPane.setVisible(visible);

		if (visible) {
//			frame.setPreferredSize(new Dimension(500, 500));
//			frame.pack();

		}
		frame.pack();

		chkShowInfo.setSelected(visible);
	}

	public void scrollToBottom() {
		outputTextArea.validate();
		JScrollBar vertical = outputTextAreaScrollPane.getVerticalScrollBar();
		vertical.setValue(vertical.getMaximum());
	}

	public void scrollToTop() {
		outputTextArea.validate();
		JScrollBar vertical = outputTextAreaScrollPane.getVerticalScrollBar();
		vertical.setValue(vertical.getMinimum());
	}


	public void init() {
		frame = new JFrame("Fast Copy " + UI.version);
		frame.setContentPane(layoutPanel1);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//progressBar1.setVisible(false);
		progressBar1.setMaximum(100);
		progressBar1.setMinimum(0);

		progressPanel.setVisible(false);
		frame.setPreferredSize(new Dimension(600, 580));

		frame.pack();

		/*position it*/
		//frame.setLocationRelativeTo(null);  // *** this will center your app ***
		PointerInfo a = MouseInfo.getPointerInfo();
		Point b = a.getLocation();
		int x = (int) b.getX();
		int y = (int) b.getY();
		frame.setLocation(x + 100, y);

		btnHelp.setBorder(null);

		frame.setVisible(true);

	}

	private void createUIComponents() {
		// TODO: place custom component creation code here
	}


	class DoItJobThread extends Thread {
		@Override
		public void run() {
			doit();
		}
	}


	public boolean refreshDataModel() {
		if (fldSourceDir.getText() == null || fldSourceDir.getText().trim().length() == 0) {
			JOptionPane.showMessageDialog(null, "Specify the source directory.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		} else
			props.setSourceDir(fldSourceDir.getText());

		if (fldTargetDir.getText() == null || fldTargetDir.getText().trim().length() == 0) {
			JOptionPane.showMessageDialog(null, "Specify the target directory.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		} else
			props.setDestDir(fldTargetDir.getText());

		if (chkMultiThread.isSelected()) {
			props.setNumOfThreads(FastCopy.DEFAULT_THREAD_NUM);
		} else {
			props.setNumOfThreads(1);
		}

		props.setOverwrite(chkOverrideAlways.isSelected());
		props.setOverwriteIfNewerOrDifferent(overrideOnlyIfNewerCheckBox.isSelected());
		props.setVerbose(chkShowInfo.isSelected());
		props.setFlatCopy(chkFlat.isSelected());
		props.setCreateTheSameSourceFolderUnderTarget(chkCreateTheSameSourceCheckBox.isSelected());
		return true;

	}


	public void doit() {
		if (refreshDataModel()) {

			fastCopy.getRdProUI().println("working...");
			labelStatus.setText("Working...");
			labelStatus.setText("");
			btnCancel.setText("Cancel");
			btnOk.setEnabled(false);
			progressPanel.setVisible(true);

			fastCopy.run(props);

			labelStatus.setText(fastCopy.getStatistics().printOverallProgress());
			progressPanel.setVisible(false);

			btnOk.setEnabled(true);
			btnCancel.setText("Close");
			//labelStatus.setText("Dir copied:" + fastCopy.getStatistics().getDirCount() + ", Files copied:" + fastCopy.getStatistics().getFilesCount());
		}
	}

	public static void main(String[] args) {
		FastCopyMainForm rdProMain = new FastCopyMainForm();
		rdProMain.init();
		GraphicsUIImpl rdProUI = new GraphicsUIImpl();
		rdProMain.setRdProUI(rdProUI);
		rdProUI.setOutputTextArea(rdProMain.outputTextArea);
		rdProUI.setLabelStatus(rdProMain.labelStatus);
		rdProUI.setProgressBar(rdProMain.progressBar1);


		//default it to current dir
		rdProMain.fastCopy = new FastCopy(rdProUI);

		rdProMain.props = rdProUI.parseCommandLineArguments(args);


		if (FastCopy.debug || rdProMain.props.isDebug()) {
			int i = 0;
			for (String arg : args) {
				rdProUI.println("arg[" + i + "]=" + arg);
				i++;
			}
			rdProUI.println(rdProMain.props.toString());

		}

		rdProMain.fldSourceDir.setText(rdProMain.props.getSourceDir());
		rdProMain.fldTargetDir.setText(rdProMain.props.getDestDir());


	}
}
