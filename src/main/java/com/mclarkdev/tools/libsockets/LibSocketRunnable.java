package com.mclarkdev.tools.libsockets;

import org.json.JSONObject;

/**
 * LibSockets // LibSocketRunnable
 */
public abstract class LibSocketRunnable implements Runnable {

	private final byte[] bytes;

	/**
	 * Create a new SocketRunnable from an array of bytes.
	 * 
	 * @param bytes the array of bytes to be sent
	 */
	public LibSocketRunnable(byte[] bytes) {
		this.bytes = bytes;
	}

	/**
	 * Create a new SocketRunnable from a String message.
	 * 
	 * @param message the message to be sent
	 */
	public LibSocketRunnable(String message) {
		this(message.getBytes());
	}

	/**
	 * Create a new SocketRunnable from a JSONObject.
	 * 
	 * @param object the object to be sent
	 */
	public LibSocketRunnable(JSONObject object) {
		this(object.toString());
	}

	/**
	 * Get the raw message bytes associated with the runnable.
	 * 
	 * @return raw message bytes
	 */
	public byte[] getBytes() {
		return bytes;
	}

	@Override
	public void run() {
		run(bytes);
	}

	/**
	 * The implemented action to execute in a thread.
	 * 
	 * @param msg the message to send to the client
	 */
	public abstract void run(byte[] msg);
}
