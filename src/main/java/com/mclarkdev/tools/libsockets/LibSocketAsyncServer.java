package com.mclarkdev.tools.libsockets;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.json.JSONObject;

import com.mclarkdev.tools.libsockets.lib.LibSocketConnectionCallback;

/**
 * LibSockets // LibSocketAsyncServer
 */
public class LibSocketAsyncServer {

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

	// Background thread for accepting client connections
	private Runnable socketAcceptor = new Runnable() {
		public void run() {

			// Rename the thread for debuggers
			Thread.currentThread().setName(//
					String.format("LibSocketAsyncServer (%d)", bindPort));

			Socket clientSocket;
			while (!Thread.currentThread().isInterrupted()) {

				try {

					// Accept new client connection
					clientSocket = serverSocket.accept();

					// Check valid socket
					if (clientSocket == null) {
						continue;
					}

					// Spawn the new client connection
					new LibSocketAsyncConnection(clientSocket, socketCallback);

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
			clients.put(connection.getConnectionId(), connection);

			// Call parent handler
			parentHandler.onConnect(connection);
		}

		@Override
		public void onMessage(LibSocketConnection connection, String message) {
			JSONObject wrapped = new JSONObject(message);
			String id = wrapped.getString("id");
			String body = wrapped.getString("body");

			// Check if response to inFlight request
			if (inFlight.contains(id)) {

				// Remove from inFlight map
				inFlight.remove(id);

				// Check if abandoned
				if (abandoned.contains(id)) {
					return;
				}

				// Add to responses map
				responses.put(id, body);
				return;
			}

			// Call parent handler
			parentHandler.onMessage(connection, message);
		}

		@Override
		public void onDiconnect(LibSocketConnection connection, Throwable e) {

			// Remove from list of connected clients
			clients.remove(connection.getConnectionId());

			// Call parent handler
			parentHandler.onDiconnect(connection, e);
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
		String messageID = UUID.randomUUID().toString();

		// Wrap the message in a JSON object
		JSONObject wrappedMessage = new JSONObject()//
				.put("id", messageID)//
				.put("body", message);

		// Create a new wire task to send the message
		byte[] bytes = wrappedMessage.toString().getBytes();

		try {
			// Write to socket
			connection.write(bytes);
		} catch (IOException e) {
			System.out.println("Network error.");
			e.printStackTrace(System.err);
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
	 */
	public String rx(String uid, long timeout) {

		// Calculate timeout time
		long timeTimeout = (System.currentTimeMillis() + timeout);

		// Continue while not timed out
		while (timeTimeout > System.currentTimeMillis()) {

			// Skip if still in-flight
			if (inFlight.contains(uid)) {
				continue;
			}

			// Return if in responses map
			if (responses.containsKey(uid)) {
				return responses.get(uid);
			}
		}

		// Add to abandoned map
		abandoned.add(uid);
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
	 */
	public String txrx(String client, String body, long timeout) {

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
