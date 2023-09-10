package com.mclarkdev.tools.libsockets;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * LibSockets // LibSocketServer
 */
public class LibSocketServer {

	private final ServerSocket server;

	/**
	 * Instantiate a new instance of the LibSocketServer.
	 * 
	 * @param port the port to bind
	 * @throws IOException failed creating the server
	 */
	public LibSocketServer(int port) throws IOException {

		server = new ServerSocket(port);
	}

	/**
	 * Returns the underlying ServerSocket.
	 * 
	 * @return the underlying ServerSocket
	 */
	public ServerSocket getServer() {

		return server;
	}
}
