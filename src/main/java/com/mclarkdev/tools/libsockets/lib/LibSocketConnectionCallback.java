package com.mclarkdev.tools.libsockets.lib;

import com.mclarkdev.tools.libsockets.LibSocketConnection;

/**
 * LibSockets // LibSocketListener
 */
public interface LibSocketConnectionCallback {

	/**
	 * Called when the socket connection is established.
	 * 
	 * @param connection the client connection object
	 */
	public void onConnect(LibSocketConnection connection);

	/**
	 * Called when receiving a message from the connected client.
	 * 
	 * @param connection the client connection object
	 * @param message    the message received
	 */
	public void onMessage(LibSocketConnection connection, String message);

	/**
	 * Called when the socket connection is closed.
	 * 
	 * @param connection the client connection object
	 * @param e          the error associated with the disconnect
	 */
	public void onDiconnect(LibSocketConnection connection, Throwable e);
}
