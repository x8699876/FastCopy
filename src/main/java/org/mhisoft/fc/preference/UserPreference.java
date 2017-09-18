/*
 *
 *  * Copyright (c) 2014- MHISoft LLC and/or its affiliates. All rights reserved.
 *  * Licensed to MHISoft LLC under one or more contributor
 *  * license agreements. See the NOTICE file distributed with
 *  * this work for additional information regarding copyright
 *  * ownership. MHISoft LLC licenses this file to you under
 *  * the Apache License, Version 2.0 (the "License"); you may
 *  * not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 *
 */

package org.mhisoft.fc.preference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Description:
 *
 * @author Tony Xue
 * @since Apr, 2016
 */
public class UserPreference implements Serializable {


	private static final long serialVersionUID = 1L;
	public static final String userHome = System.getProperty("user.home") + File.separator;

	public static final String settingsFile = userHome + "FastCopySettings.dat";

	static UserPreference instance = new UserPreference();

//
	public static UserPreference getInstance() {
		return instance;
	}



	private int fontSize;
	private int dimensionX;
	private int dimensionY;



	public int getFontSize() {
		return fontSize == 0 ? 20 : fontSize;
	}

	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}



	public int getDimensionX() {
		return dimensionX == 0 ? 1200 : dimensionX;
	}

	public void setDimensionX(int dimensionX) {
		this.dimensionX = dimensionX;
	}

	public int getDimensionY() {
		return dimensionY == 0 ? 800 : dimensionY;
	}

	public void setDimensionY(int dimensionY) {
		this.dimensionY = dimensionY;
	}



	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("UserPreference{");
		sb.append(", fontSize=").append(fontSize);
		sb.append(", dimensionX=").append(dimensionX);
		sb.append(", dimensionY=").append(dimensionY);
		sb.append('}');
		return sb.toString();
	}



	/**
	 * Save the settings to file
	 */
	public void saveSettingsToFile() {
		ObjectOutputStream outputStream = null;
		try {
			outputStream = new ObjectOutputStream(new FileOutputStream(UserPreference.settingsFile));
			outputStream.writeObject(UserPreference.getInstance());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (outputStream != null)
				try {
					outputStream.close();
				} catch (IOException e) {
					//
				}
		}
	}

	public void readSettingsFromFile() {
		ObjectInputStream stream = null;
		try {

			stream = new ObjectInputStream(new FileInputStream(UserPreference.settingsFile));
			instance = (UserPreference) stream.readObject();

		}
		catch (IOException | ClassNotFoundException e) {
			//e.printStackTrace();
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
					//
				}
		}
	}


}
