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

	private final BufferedReader reader;

	private double rxBytes = 0;
	private double rxMessages = 0;

	private double txBytes = 0;
	private double txMessages = 0;

	public LibSocketConnection(LibSocketConnectionCallback callback, Socket socket) throws IOException {

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

		// Open input stream
		this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		// Start thread
		new Thread(worker).start();
	}

	public String getConnectionId() {
		return id;
	}

	public LibSocketConnectionCallback getCallback() {
		return callback;
	}

	public Socket getSocket() {
		return socket;
	}

	public String getAddress() {
		return hostAddr;
	}

	public long getTimeConnected() {
		return connected;
	}

	public double getRxBytes() {
		return rxBytes;
	}

	public double getRxMessages() {
		return rxMessages;
	}

	public double getTxBytes() {
		return txBytes;
	}

	public double getTxMessages() {
		return txMessages;
	}

	public void write(byte[] bytes) throws IOException {

		// Send the data
		this.socket.getOutputStream().write(bytes);
		this.socket.getOutputStream().flush();

		// Hit the counters
		this.txMessages += 1;
		this.txBytes += bytes.length;
	}

	public void shutdown() {
		if (socket.isClosed()) {
			return;
		}

		try {
			socket.close();
		} catch (IOException e) {
		}
	}

	private final Runnable worker = new Runnable() {
		public void run() {

			Thread.currentThread().setName(//
					String.format("LibSocketConnection (%s)", hostAddr));

			// Call implemented callback
			callback.onConnect(LibSocketConnection.this);

			try {
				String line;
				while ((line = reader.readLine()) != null) {

					// Hit the counters
					rxMessages += 1;
					rxBytes += line.length();

					// Call implemented handler
					callback.onMessage(LibSocketConnection.this, line);
				}
			} catch (Exception | Error e) {
				callback.onDiconnect(LibSocketConnection.this, e);
			}

			shutdown();
		}
	};;
}
