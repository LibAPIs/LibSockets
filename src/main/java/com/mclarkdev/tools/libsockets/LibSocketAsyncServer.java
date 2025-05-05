package com.mclarkdev.tools.libsockets;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import com.mclarkdev.tools.libsockets.lib.LibSocketConnectionCallback;

/**
 * LibSockets // LibSocketAsyncServer
 */
public class LibSocketAsyncServer {

	private volatile long sequence = 0;

	private final HashMap<String, LibSocketConnection> clients;

	private final Set<String> inFlight;
	private final List<String> abandoned;

	private final Map<String, String> responses;

	private final int bindPort;

	private final ServerSocket serverSocket;

	private final Thread serverThread;

	private final LibSocketConnectionCallback parentHandler;

	/**
	 * Creates a new AsyncMessageController.
	 * 
	 * @param port     peer to peer message bind port
	 * @param callback socket callback for user implemented logic
	 * @throws IOException failure binding to socket
	 */
	public LibSocketAsyncServer(int port, //
			LibSocketConnectionCallback callback) throws IOException {

		// Map of connected clients
		this.clients = new HashMap<>();

		// Map of in-flight requests
		this.inFlight = new HashSet<>();
		this.abandoned = new ArrayList<>();

		// Map of received responses
		this.responses = new HashMap<>();

		// Bind to the socket
		this.bindPort = port;
		this.serverSocket = new ServerSocket(port);
		this.serverSocket.setSoTimeout(2500);

		this.parentHandler = callback;

		// Start server thread
		this.serverThread = new Thread(socketAcceptor);
		this.serverThread.start();
	}

	/**
	 * Returns the number of currently in flight requests.
	 * 
	 * @return number of currently in flight requests
	 */
	public int countInFlight() {
		return inFlight.size();
	}

	/**
	 * Returns the number of abandoned requests.
	 * 
	 * @return number of abandoned requests
	 */
	public int countAbandoned() {
		return abandoned.size();
	}

	// Background thread for accepting client connections
	private Runnable socketAcceptor = new Runnable() {
		public void run() {

			// Rename the thread for debuggers
			Thread.currentThread().setName(//
					String.format("LibSocketAsyncServer (:%d)", bindPort));

			while (!Thread.currentThread().isInterrupted()) {

				try {

					// Accept and spawn the new client connections
					new LibSocketAsyncConnection(//
							serverSocket.accept(), socketCallback);

				} catch (SocketTimeoutException e) {

					continue;

				} catch (Exception | Error e) {

					System.out.println("Network error.");
					e.printStackTrace(System.err);
				}
			}
		}
	};

	private final LibSocketConnectionCallback socketCallback = new LibSocketConnectionCallback() {

		@Override
		public void onConnect(LibSocketConnection connection) {

			// Add to list of connected clients
			synchronized (clients) {
				clients.put(connection.getConnectionId(), connection);
			}

			// Call parent handler
			parentHandler.onConnect(connection);
		}

		@Override
		public void onMessage(LibSocketConnection connection, String message) {
			JSONObject wrapped = new JSONObject(message);

			boolean isReply = wrapped.has("ack");

			String id = (isReply) ? //
					wrapped.getString("ack") : wrapped.getString("id");

			boolean isInFlight = inFlight.contains(id);
			boolean isAbandoned = isReply && !isInFlight;

			// Drop abandoned message
			if (isAbandoned) {
				return;
			}

			// Check if response to inFlight request
			if (isInFlight) {

				// Remove from inFlight map
				inFlight.remove(id);

				// Add to responses map
				responses.put(id, //
						wrapped.getString("body"));
				return;
			}

			// Call parent handler
			parentHandler.onMessage(connection, message);
		}

		@Override
		public void onDisconnect(LibSocketConnection connection, Throwable e) {

			synchronized (clients) {

				// Check if client is still connected
				if (clients.containsKey(connection.getConnectionId())) {

					// Remove from list of connected clients
					clients.remove(connection.getConnectionId());
				}
			}

			// Call parent handler
			parentHandler.onDisconnect(connection, e);
		}
	};

	/**
	 * Get a list of all connected clients.
	 * 
	 * @return list of all connected clients
	 */
	public Collection<LibSocketConnection> clients() {

		return clients.values();
	}

	/**
	 * Get a client connection by ID.
	 * 
	 * @param id the connection ID
	 * @return the client connection
	 */
	public LibSocketConnection client(String id) {

		return clients.get(id);
	}

	/**
	 * Write a message to a client.
	 * 
	 * Returns the message UID for tracking the response.
	 * 
	 * @param client  the ID of client to write to
	 * @param message the body of the message to write
	 * @return the tracking ID of the message
	 */
	public String tx(final String client, String message) {

		// Get requested client connection
		LibSocketConnection connection = clients.get(client);

		// Fail if client not connected
		if (connection == null) {
			throw new IllegalArgumentException("client is null");
		}

		// Generate tracking ID for the message
		String messageID = String.format("sq-%012d;", (sequence++));

		// Wrap the message in a JSON object
		JSONObject wrappedMessage = new JSONObject()//
				.put("id", messageID)//
				.put("body", message);

		// Create a new wire task to send the message
		byte[] bytes = wrappedMessage.toString().getBytes();

		try {

			// Write to socket
			connection.write(bytes);
			inFlight.add(messageID);
		} catch (IOException e) {

			System.out.println("Network error.");
			e.printStackTrace(System.err);
			connection.shutdown();
			return null;
		}

		// Return the tracking ID
		return messageID;
	}

	/**
	 * Wait for a response to a message.
	 * 
	 * Times out after the given amount of time if no response received.
	 * 
	 * Responses received at a later time will be dropped.
	 * 
	 * @param uid     the tracking ID of the message
	 * @param timeout timeout to wait for a response
	 * @return the message body returned
	 * @throws InterruptedException
	 */
	public String rx(String uid, long timeout) throws InterruptedException {

		// Skip if not in-flight
		if (!inFlight.contains(uid)) {
			return null;
		}

		// Calculate timeout time
		long timeTimeout = (System.currentTimeMillis() + timeout);

		// Continue while not timed out
		while (timeTimeout > System.currentTimeMillis()) {

			// Return if in responses map
			if (responses.containsKey(uid)) {
				return responses.remove(uid);
			}

			// Delay and loop
			Thread.yield();
		}

		// Add to abandoned map
		abandoned.add(uid);
		inFlight.remove(uid);
		System.err.println("Abandoned message: " + uid);
		return null;
	}

	/**
	 * Writes a message top a client and waits for a response.
	 * 
	 * Times out after the given amount of time if no response received.
	 * 
	 * @param client  the ID of the client to write to
	 * @param body    the body of the message to write
	 * @param timeout timeout to wait for a response
	 * @return the message body returned
	 * @throws InterruptedException interrupted
	 */
	public String txrx(String client, String body, long timeout) throws InterruptedException {

		return rx(tx(client, body), timeout);
	}

	/**
	 * Request a graceful shutdown of the controller.
	 */
	public void shutdown() {

		// Interrupt the main listener thread
		this.serverThread.interrupt();

		// Loop all connected clients
		for (Map.Entry<String, LibSocketConnection> entry : clients.entrySet()) {

			// Disconnect the client
			entry.getValue().shutdown();
		}
	}
}
