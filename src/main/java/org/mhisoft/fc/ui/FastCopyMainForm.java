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

import java.util.List;
import java.awt.Component;
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
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.mhisoft.fc.FastCopy;
import org.mhisoft.fc.FileUtils;
import org.mhisoft.fc.RunTimeProperties;
import org.mhisoft.fc.ViewHelper;
import org.mhisoft.fc.preference.UserPreference;

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
	RunTimeProperties props = RunTimeProperties.instance;


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
	private JSpinner fldFontSize;
	private JLabel labelFontSize;
	private JCheckBox ckVerify;
	private JLabel labelCurrentDir;

	GraphicsUIImpl uiImpl;
	DoItJobThread doItJobThread;

	File lastSrourceFileLocation =null;
	File lastTargetFileLocation =null;

	public void setRdProUI(GraphicsUIImpl rdProUI) {
		this.uiImpl = rdProUI;
	}

	public void stopIt() {
		RunTimeProperties.instance.setStopThreads(true);
		progressPanel.setVisible(false);

		fastCopy.stopWorkers();

		//main thread
		doItJobThread.interrupt();


		//set running false only afer all threads are shutdown.
		RunTimeProperties.instance.setRunning(false);
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
				//showHideInfo(chkShowInfo.isSelected());
				RunTimeProperties.instance.setVerbose(chkShowInfo.isSelected());
			}
		});

		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (RunTimeProperties.instance.isRunning()) {
					stopIt();

				} else {
					updateAndSavePreferences();
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
				doItJobThread.setDaemon(true);
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

				if (dir.length()==0)
					dir = RunTimeProperties.userHome;

				int k = dir.indexOf(";");
				if (k > 0)
					dir = dir.substring(0, k);

				if (lastSrourceFileLocation ==null)
					lastSrourceFileLocation = new File(dir);

				File[] files = chooseFiles(lastSrourceFileLocation
						, VFSJFileChooser.SELECTION_MODE.FILES_AND_DIRECTORIES);

				if (files != null && files.length > 0) {
					StringBuilder builder = new StringBuilder();

					//append to existing
					if (fldSourceDir.getText() != null && fldSourceDir.getText().length() > 0) {
						builder.append(fldSourceDir.getText()) ;
					}

					//now append the new directories. 
					for (File file : files) {
						if (builder.length() > 0)
							builder.append(";");
						builder.append(file.getAbsolutePath());
						lastSrourceFileLocation =   file;

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

				if (lastTargetFileLocation==null)
					lastTargetFileLocation =new File(dir);

				File[] files = chooseFiles(lastTargetFileLocation, VFSJFileChooser.SELECTION_MODE.DIRECTORIES_ONLY);
				if (files != null && files.length > 0) {
					props.setDestDir(files[0].getAbsolutePath().toString());
					fldTargetDir.setText(props.getDestDir());
					lastTargetFileLocation = files[0];
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
		fileChooser.setFileHidingEnabled(true);  //show hidden files
		fileChooser.setPreferredSize( new Dimension(800, 500));


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
		frame = new JFrame("MHISoft FastCopy " + UI.version);
		frame.setContentPane(layoutPanel1);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//progressBar1.setVisible(false);
		progressBar1.setMaximum(100);
		progressBar1.setMinimum(0);

		progressPanel.setVisible(false);
		//frame.setPreferredSize(new Dimension(1200, 800));
		frame.setPreferredSize(new Dimension(UserPreference.getInstance().getDimensionX(), UserPreference.getInstance().getDimensionY()));

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


		componentsList = ViewHelper.getAllComponents(frame);
		setupFontSpinner();
		ViewHelper.setFontSize(componentsList, UserPreference.getInstance().getFontSize());



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
			props.setNumOfThreads(RunTimeProperties.DEFAULT_THREAD_NUM);
		} else {
			props.setNumOfThreads(1);
		}

		if (ckVerify.isSelected()) {
			props.setVerifyAfterCopy(true);
		}
		else {
			props.setVerifyAfterCopy(false);
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


	List<Component> componentsList;

	/**
	 * Use the font spinner to increase and decrease the font size.
	 */
	public void setupFontSpinner() {

		int fontSize = UserPreference.getInstance().getFontSize();

		SpinnerModel spinnerModel = new SpinnerNumberModel(fontSize, //initial value
				10, //min
				fontSize + 20, //max
				2); //step
		fldFontSize.setModel(spinnerModel);
		fldFontSize.addChangeListener(new ChangeListener() {
										  @Override
										  public void stateChanged(ChangeEvent e) {
											  SpinnerModel spinnerModel = fldFontSize.getModel();
											  int newFontSize = (Integer) spinnerModel.getValue();
											  ViewHelper.setFontSize(componentsList, newFontSize);
										  }
									  }
		);


	}

	public static void main(String[] args) {
		UserPreference.getInstance().readSettingsFromFile();
		FastCopyMainForm main = new FastCopyMainForm();
		main.init();
		GraphicsUIImpl uiImpl = new GraphicsUIImpl();
		main.setRdProUI(uiImpl);
		FileUtils.instance.setRdProUI(uiImpl);

		uiImpl.setOutputTextArea(main.outputTextArea);
		uiImpl.setLabelStatus(main.labelStatus);
		uiImpl.setLabelCurrentDir(main.labelCurrentDir);
		uiImpl.setProgressBar(main.progressBar1);


		//default it to current dir
		main.fastCopy = new FastCopy(uiImpl);

		main.props = uiImpl.parseCommandLineArguments(args);


		if (main.props.isDebug()) {
			int i = 0;
			for (String arg : args) {
				uiImpl.println("arg[" + i + "]=" + arg);
				i++;
			}
		}

		main.fldSourceDir.setText(main.props.getSourceDir());
		main.fldTargetDir.setText(main.props.getDestDir());

		uiImpl.help();


	}


	public void updateAndSavePreferences() {
		//save the settings
		Dimension d = frame.getSize();
		UserPreference.getInstance().setDimensionX(d.width);
		UserPreference.getInstance().setDimensionY(d.height);
		SpinnerModel spinnerModel = fldFontSize.getModel();
		int newFontSize = (Integer) spinnerModel.getValue();
		UserPreference.getInstance().setFontSize(newFontSize);
		UserPreference.getInstance().saveSettingsToFile();
	}


}
