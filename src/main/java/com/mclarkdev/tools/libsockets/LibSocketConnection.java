package com.mclarkdev.tools.libsockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.UUID;

/**
 * LibSockets // LibSocketConnection
 */
public class LibSocketConnection {

	private final String id = UUID.randomUUID().toString();

	private final LibSocketConnectionCallback callback;

	private final Socket socket;
	private final String hostAddr;

	private final long connected;

	private double rxBytes = 0;
	private double rxMessages = 0;

	private double txBytes = 0;
	private double txMessages = 0;

	private final Thread readerThread;

	/**
	 * Creates a new LibSocketConnection object for managing low-level socket
	 * messaging with a client.
	 * 
	 * @param callback the callback for socket events
	 * @param socket   the client socket connection
	 * @throws IOException failure establishing communication channel
	 */
	private LibSocketConnection(LibSocketConnectionCallback callback, Socket socket) {

		// Check callback
		if (callback != null) {
			this.callback = callback;
		} else {
			throw new IllegalArgumentException("callback is null");
		}

		// Check socket
		if (socket != null) {
			this.socket = socket;

		} else {
			throw new IllegalArgumentException("socket is null");
		}

		// Get host address
		this.hostAddr = socket.getRemoteSocketAddress().toString();

		// Time socket was connected
		this.connected = System.currentTimeMillis();

		// Create and start worker thread
		this.readerThread = new Thread(socketReader);
		this.readerThread.start();
	}

	// Background thread for reading client stream
	private final Runnable socketReader = new Runnable() {

		public void run() {

			// Rename the thread for debuggers
			Thread.currentThread().setName(//
					String.format("LibSocketConnection (%s)", hostAddr));

			try {

				// Open input stream
				BufferedReader reader = new BufferedReader(//
						new InputStreamReader(socket.getInputStream()));

				// Call implemented handler
				callback.onConnect(LibSocketConnection.this);

				String line;
				while ((line = reader.readLine()) != null) {

					// Hit the counters
					rxMessages += 1;
					rxBytes += line.length();

					// Call implemented handler
					callback.onMessage(LibSocketConnection.this, line);
				}
			} catch (Exception | Error e) {

				// Call implemented handler
				callback.onDiconnect(LibSocketConnection.this, e);
			} finally {

				// Cleanup
				shutdown();
			}
		}
	};;

	/**
	 * Get the connection ID.
	 * 
	 * @return connection ID
	 */
	public String getConnectionId() {
		return id;
	}

	/**
	 * Get the raw callback object.
	 * 
	 * @return raw callback object
	 */
	public LibSocketConnectionCallback getCallback() {
		return callback;
	}

	/**
	 * Get the raw Socket object.
	 * 
	 * @return raw Socket object
	 */
	public Socket getSocket() {
		return socket;
	}

	/**
	 * Get the address of the connected socket.
	 * 
	 * @return address of the connected socket
	 */
	public String getAddress() {
		return hostAddr;
	}

	/**
	 * Get the time at which the socket was connected.
	 * 
	 * @return time when the socket was connected
	 */
	public long getTimeConnected() {
		return connected;
	}

	/**
	 * Get the number of bytes received.
	 * 
	 * @return number of bytes received
	 */
	public double getRxBytes() {
		return rxBytes;
	}

	/**
	 * Get the number of messages received.
	 * 
	 * @return number of messages received
	 */
	public double getRxMessages() {
		return rxMessages;
	}

	/**
	 * Get the number of bytes sent.
	 * 
	 * @return number of bytes sent
	 */
	public double getTxBytes() {
		return txBytes;
	}

	/**
	 * Get the number of messages sent.
	 * 
	 * @return number of messages sent
	 */
	public double getTxMessages() {
		return txMessages;
	}

	/**
	 * Check if the connection is alive and connected.
	 * 
	 * @return connection is alive
	 */
	public boolean alive() {
		boolean isAlive = true;

		isAlive = isAlive && this.readerThread.isAlive();
		isAlive = isAlive && this.socket.isConnected();
		isAlive = isAlive && !this.socket.isClosed();

		return isAlive;
	}

	/**
	 * Write a raw message to the connected client.
	 * 
	 * @param bytes raw bytes to send
	 * @throws IOException failure sending message
	 */
	public void write(byte[] bytes) throws IOException {

		// Send the data
		socket.getOutputStream().write(bytes);
		socket.getOutputStream().flush();

		// Hit the counters
		txMessages += 1;
		txBytes += bytes.length;
	}

	/**
	 * Shutdown the socket connection.
	 */
	public void shutdown() {

		try {
			// Try to close socket
			if (!socket.isClosed()) {
				socket.close();
			}
		} catch (IOException e) {
		}

		// Try to shutdown reader thread
		if (this.readerThread.isAlive()) {
			this.readerThread.interrupt();
		}
	}

	/**
	 * Spawn a new LibSocketConnection object.
	 * 
	 * @param callback the callback for socket events
	 * @param socket   the client socket connection
	 * @return the newly spawned connection object
	 */
	public static LibSocketConnection spawn(LibSocketConnectionCallback callback, Socket socket) {
		return (new LibSocketConnection(callback, socket));
	}
}
