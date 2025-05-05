package com.mclarkdev.tools.libsockets;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.mclarkdev.tools.libsockets.lib.LibSocketConnectionCallback;

/**
 * LibSockets // LibSocketAsyncClient
 */
public class LibSocketAsyncConnection extends LibSocketConnection {

	private static final ExecutorService clientWorkerPool;

	static {
		clientWorkerPool = Executors.newFixedThreadPool(8, new ThreadFactory() {
			private final AtomicInteger count = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName(String.format(//
						"LibSocketAsyncWorker <%02d>", count.getAndIncrement()));
				return t;
			}
		});
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
			public void onDisconnect(LibSocketConnection connection, Throwable e) {
				clientWorkerPool.submit(new Runnable() {
					public void run() {
						callback.onDisconnect(connection, e);
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
					shutdown();
				}
			}
		});
	}
}
