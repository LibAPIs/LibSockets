package com.mclarkdev.tools.libsockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;

import org.json.JSONObject;

import com.mclarkdev.tools.libsockets.lib.LibSocketConnectionCallback;

/**
 * LibSockets // LibSocketConnection
 */
public class LibSocketConnection {

	private final String id = UUID.randomUUID().toString();

	private final LibSocketConnectionCallback callback;

	private final Socket socket;
	private final String hostAddr;

	private final long connected;

	private volatile double rxBytes = 0;
	private volatile double rxMessages = 0;

	private volatile double txBytes = 0;
	private volatile double txMessages = 0;

	private final Thread readerThread;

	/**
	 * Creates a new LibSocketConnection object for managing low-level socket
	 * messaging with a client.
	 * 
	 * @param callback the callback for socket events
	 * @param socket   the client socket connection
	 */
	public LibSocketConnection(Socket socket, LibSocketConnectionCallback callback) {

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
				callback.onDisconnect(LibSocketConnection.this, e);
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
	public boolean isAlive() {
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

		// Grab socket output stream
		OutputStream out = //
				socket.getOutputStream();

		// Sync and send the data
		synchronized (out) {
			out.write(bytes);
			out.write('\n');
			out.flush();
		}

		// Hit the counters
		txMessages += 1;
		txBytes += (bytes.length + 1);
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
	 * Get the connection details as a JSON object.
	 * 
	 * @return connection details
	 */
	public JSONObject toJSON() {
		return new JSONObject()//
				.put("id", getConnectionId())//
				.put("host", getAddress())//
				.put("alive", isAlive())//
				.put("connected", getTimeConnected())//
				.put("bytesTX", getTxBytes())//
				.put("bytesRX", getRxBytes())//
				.put("messagesTX", getTxMessages())//
				.put("messagesRX", getRxMessages());
	}

	/**
	 * Get the connection details as a String (JSON Format)
	 */
	@Override
	public String toString() {
		return toJSON().toString();
	}
}
