package com.mclarkdev.tools.libsockets;

/**
 * LibSockets // LibSocketListener
 */
public interface LibSocketConnectionCallback {

	public void onConnect(LibSocketConnection connection);

	public void onMessage(LibSocketConnection connection, String message);

	public void onDiconnect(LibSocketConnection connection, Throwable e);
}
