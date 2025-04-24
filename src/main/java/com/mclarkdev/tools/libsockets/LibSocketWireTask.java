package com.mclarkdev.tools.libsockets;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.json.JSONObject;

/**
 * LibSockets // LibSocketWireTask
 */
public class LibSocketWireTask {

	private final String name;
	private final LibSocketRunnable runnable;
	private final long timeout;

	private long submitted = 0;
	private Future<?> task = null;

	public LibSocketWireTask(String name, LibSocketRunnable runnable, long timeout) {

		this.name = name;
		this.runnable = runnable;
		this.timeout = timeout;
	}

	public String getName() {
		return name;
	}

	public Runnable getRunnable() {
		return runnable;
	}

	public long getTimeout() {
		return timeout;
	}

	public long getTimeSubmitted() {
		return submitted;
	}

	public boolean isRunning() {
		return ((task != null) && !task.isDone());
	}

	public boolean isComplete() {
		return (task == null) ? false : task.isDone();
	}

	public boolean abort() {
		return (task == null) ? false : task.cancel(true);
	}

	public boolean isOverdue() {
		return (submitted == 0) ? false : //
				(System.currentTimeMillis() > (submitted + timeout));
	}

	public void submit(ExecutorService threadPool) {
		if (submitted != 0) {
			throw new IllegalArgumentException("already submitted");
		}

		this.task = threadPool.submit(runnable);
		this.submitted = System.currentTimeMillis();
	}

	public JSONObject toJSON() {
		return new JSONObject() //
				.put("name", name)//
				.put("submitted", submitted)//
				.put("complete", isComplete());
	}

	public String toString() {
		return toJSON().toString();
	}
}
