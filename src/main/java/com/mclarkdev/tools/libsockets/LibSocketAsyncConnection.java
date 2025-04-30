package com.mclarkdev.tools.libsockets;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mclarkdev.tools.libsockets.lib.LibSocketConnectionCallback;

/**
 * LibSockets // LibSocketAsyncClient
 */
public class LibSocketAsyncConnection extends LibSocketConnection {

	private static final ExecutorService clientWorkerPool;

	static {

		clientWorkerPool = Executors.newFixedThreadPool(8);
	}

	public LibSocketAsyncConnection(Socket socket, LibSocketConnectionCallback callback) {
		super(socket, new LibSocketConnectionCallback() {

			@Override
			public void onConnect(LibSocketConnection connection) {
				clientWorkerPool.submit(new Runnable() {
					public void run() {
						callback.onConnect(connection);
					}
				});
			}

			@Override
			public void onMessage(LibSocketConnection connection, String message) {
				clientWorkerPool.submit(new Runnable() {
					public void run() {
						callback.onMessage(connection, message);
					}
				});
			}

			@Override
			public void onDiconnect(LibSocketConnection connection, Throwable e) {
				clientWorkerPool.submit(new Runnable() {
					public void run() {
						callback.onDiconnect(connection, e);
					}
				});
			}
		});
	}

	@Override
	public void write(byte[] bytes) throws IOException {

		clientWorkerPool.submit(new Runnable() {
			public void run() {
				try {
					LibSocketAsyncConnection.super.write(bytes);
				} catch (IOException e) {
					System.out.println("Network error.");
					e.printStackTrace(System.err);
				}
			}
		});
	}
}
