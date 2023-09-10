package com.mclarkdev.tools.libsockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * LibSockets // LibSocketConnection
 */
public class LibSocketConnection {

	private final LibSocketListener listener;

	private final Socket socket;
	private final BufferedReader reader;
	private final PrintWriter writer;

	private LibSocketConnection(LibSocketListener listener, Socket socket) throws IOException {

		this.listener = listener;
		this.socket = socket;

		this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.writer = new PrintWriter(socket.getOutputStream(), true);

		new Thread() {
			public void run() {
				try {
					String line;
					while ((line = reader.readLine()) != null) {
						listener.onMessage(LibSocketConnection.this, line);
					}
				} catch (IOException e) {
					listener.onDiconnect(e);
				}
			}
		}.start();
		this.listener.onConnect();
	}

	public void shutdown() {
		if (socket != null && !socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
	}

	public Socket getSocket() {
		return socket;
	}

	public static LibSocketConnection createConnection(LibSocketListener listener, String host, int port)
			throws UnknownHostException, IOException {

		Socket s = new Socket(host, port);
		return new LibSocketConnection(listener, s);
	}
}
