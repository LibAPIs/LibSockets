package com.mclarkdev.tools.libsockets;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * LibSockets // LibSocketClient
 */
public class LibSocketClient implements LibSocketListener {

	private final String connHost;
	private final int connPort;
	private LibSocketConnection client;

	private boolean reconnect = true;
	private LibSocketListener listener = null;

	public LibSocketClient(LibSocketListener listener, String host, int port) {
		this.listener = listener;
		this.connHost = host;
		this.connPort = port;
	}

	public void setAutoReconnect(boolean reconnect) {
		this.reconnect = reconnect;
	}

	public void connect() throws UnknownHostException, IOException {
		client = LibSocketConnection.createConnection(this, connHost, connPort);
	}

	@Override
	public void onConnect() {
		listener.onConnect();
	}

	@Override
	public void onMessage(LibSocketConnection connection, String message) {
		listener.onMessage(connection, message);
	}

	@Override
	public void onDiconnect(IOException e) {
		listener.onDiconnect(e);
		client.shutdown();
		client = null;
		if (reconnect) {
			try {
				connect();
			} catch (IOException ex) {
				e.printStackTrace();
			}
		}
	}
}
