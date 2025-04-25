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

	private long submitted = 0;
	private Future<?> task = null;

	/**
	 * Create a new wire task for sending / receiving a message.
	 * 
	 * @param name     the name of the task
	 * @param runnable the runnable to execute
	 * @param timeout  the timeout for the task
	 */
	public LibSocketWireTask(String name, LibSocketRunnable runnable) {

		this.name = name;
		this.runnable = runnable;
	}

	/**
	 * Get the name of the task.
	 * 
	 * @return name of the task
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the runnable to execute.
	 * 
	 * @return runnable to execute
	 */
	public Runnable getRunnable() {
		return runnable;
	}

	/**
	 * Get the time task was submitted
	 * 
	 * @return time task was submitted
	 */
	public long getTimeSubmitted() {
		return submitted;
	}

	/**
	 * Check if the job is currently running.
	 * 
	 * @return the job is currently running
	 */
	public boolean isRunning() {
		return ((task != null) && !task.isDone());
	}

	/**
	 * Check if the job has completed.
	 * 
	 * @return the job has completed
	 */
	public boolean isComplete() {
		return (task == null) ? false : task.isDone();
	}

	/**
	 * Check if the job has been aborted.
	 * 
	 * @return the job has been aborted
	 */
	public boolean abort() {
		return (task == null) ? false : task.cancel(true);
	}

	/**
	 * Submit the task to a worker pool.
	 * 
	 * @param threadPool the worker pool
	 */
	public void submit(ExecutorService threadPool) {
		if (submitted != 0) {
			throw new IllegalArgumentException("already submitted");
		}

		this.task = threadPool.submit(runnable);
		this.submitted = System.currentTimeMillis();
	}

	/**
	 * Get the job details as a JSON object.
	 * 
	 * @return job details
	 */
	public JSONObject toJSON() {
		return new JSONObject() //
				.put("name", getName())//
				.put("submitted", getTimeSubmitted())//
				.put("running", isRunning())//
				.put("complete", isComplete());
	}

	/**
	 * Get the job details as a String (JSON Format)
	 */
	@Override
	public String toString() {
		return toJSON().toString();
	}
}
