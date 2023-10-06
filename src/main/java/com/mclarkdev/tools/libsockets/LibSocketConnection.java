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

	private final Thread runner;

	private final BufferedReader reader;

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

		// Open input stream
		this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		// Create listener thread
		this.runner = new Thread() {
			public void run() {

				setName(String.format("LibSocketConnection (%s)", hostAddr));

				try {
					String line;
					while ((line = reader.readLine()) != null) {
						callback.onMessage(LibSocketConnection.this, line);
					}
				} catch (Exception | Error e) {
					callback.onDiconnect(LibSocketConnection.this, e);
				}

				shutdown();
			}
		};

		// Call implemented callback
		this.callback.onConnect(LibSocketConnection.this);

		// Start thread
		this.runner.start();
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

	public void write(byte[] bytes) throws IOException {
		this.socket.getOutputStream().write(bytes);
		this.socket.getOutputStream().flush();
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
}
