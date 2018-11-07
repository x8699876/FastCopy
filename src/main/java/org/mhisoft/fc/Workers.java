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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mhisoft.fc.ui.UI;

/**
 * Description: a pool of workers
 *
 * @author Tony Xue
 * @since Oct, 2014
 */
public class Workers {

	private  ThreadPoolExecutor executor;
	static int QUESIZE = 5000;
	private UI rdProUI;


	//creating the ThreadPoolExecutor
	public Workers(final int corePoolSize, final UI _rdProUI) {
		//executor = Executors.newFixedThreadPool(corePoolSize);
		this.rdProUI = _rdProUI;

		ThreadFactory threadFactory =new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = Executors.defaultThreadFactory().newThread(r);
				t.setDaemon(true);
				return t;
			}
		};

		if (RunTimeProperties.instance.isDebug()) {
			_rdProUI.println("Create the executor, corePoolSize=" + corePoolSize );
		}

		executor = new ThreadPoolExecutor(corePoolSize, corePoolSize
				 , 10, TimeUnit.SECONDS
				//LinkedBlockingQueue is an Unbounded queues.
				// Thus, no more than corePoolSize threads will ever be created.
				// (And the value of the maximumPoolSize therefore doesn't have any effect.)
				, new LinkedBlockingQueue<Runnable>()
				, threadFactory
				, new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				rdProUI.println("[warn]rejected thread:" + r.toString());
			}
		});

	}

	public  ExecutorService getExecutor() {
		return this.executor;
	}

	public int getNotCompletedTaskCount() {
		int queued = executor.getQueue().size();
		int active = executor.getActiveCount();
		int notCompleted = queued + active; // approximate
		return notCompleted;
	}

//	public  void shutDown() {
//		executor.shutdown();
//	}

	public  void addTask(Runnable task) {
		this.executor.execute(task);
	}

	public void shutDownandWaitForAllThreadsToComplete() {
		executor.shutdown();
		while (!this.executor.isTerminated()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void shutDown() {
		List<Runnable> pendingWorkers =  executor.shutdownNow();
	}

}
