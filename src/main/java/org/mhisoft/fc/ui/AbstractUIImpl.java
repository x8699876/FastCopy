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
 */

package org.mhisoft.fc.ui;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.mhisoft.fc.LogLevel;
import org.mhisoft.fc.RunTimeProperties;

/**
 * Description:
 *
 * @author Tony Xue
 * @since Nov, 2014
 */
public abstract class AbstractUIImpl implements UI {

	public void printBuildAndDisclaimer() {
		println("MHISoft FastCopy (v" +version + ", build " + build );
		println("(https://github.com/mhisoft/fastcopy)");
	}

	public void dumpArguments(String[] args, RunTimeProperties props) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			println("arg["+i+"]:" + arg);
		}

		println("parsed properties:") ;
		println(props.toString());

	}

	@Override
	public void printError(String msg, Exception e) {
		if (RunTimeProperties.instance.isDebug()) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			printError(sw.toString());
		}
		else
			printError(msg+", error=" + e.getMessage());


		e.printStackTrace();


	}


	@Override
	public void print(LogLevel logLevel, String msg) {
		switch (logLevel) {
			case debug :{
				if (RunTimeProperties.instance.isVerbose() || RunTimeProperties.instance.isDebug())
					print(msg);
				break;
			}
			case error : {
				printError(msg);
				break;
			}

		}
		
	}

	@Override
	public void println(LogLevel logLevel, final String msg) {
		print(logLevel, msg + "\n");
	}



}
