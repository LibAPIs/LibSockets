package com.mclarkdev.tools.libsockets;

import java.io.IOException;
import java.net.ServerSocket;

public class LibSocketServer {

	private final ServerSocket server;

	public LibSocketServer(int port) throws IOException {

		server = new ServerSocket(port);
	}
}
