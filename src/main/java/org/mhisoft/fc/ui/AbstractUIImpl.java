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

import org.mhisoft.fc.RunTimeProperties;

/**
 * Description:
 *
 * @author Tony Xue
 * @since Nov, 2014
 */
public abstract class AbstractUIImpl implements UI {

	public void printBuildAndDisclaimer() {
		println("Fast Copy (" +
				version + build + " by Tony Xue, MHISoft)");
	}

	public void dumpArguments(String[] args, RunTimeProperties props) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			println("arg["+i+"]:" + arg);
		}

		println("parsed properties:") ;
		println(props.toString());

	}


}
