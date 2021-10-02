package com.mclarkdev.tools.libsockets;

import java.io.IOException;

public interface LibSocketListener {

	public void onConnect();

	public void onMessage(LibSocketConnection connection, String message);

	public void onDiconnect(IOException e);
}
