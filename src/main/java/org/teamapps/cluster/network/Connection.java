/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2022 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.cluster.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.Socket;

public class Connection {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	public static final int MAX_MESSAGE_SIZE = 25_000_000;

	private ConnectionHandler connectionHandler;
	private final boolean outgoing;
	private volatile boolean active = true;
	private NodeAddress nodeAddress;
	private Socket socket;
	private DataInputStream dataInputStream;
	private DataOutputStream dataOutputStream;

	public Connection(ConnectionHandler connectionHandler, Socket socket, NodeAddress nodeAddress) {
		this.connectionHandler = connectionHandler;
		this.socket = socket;
		this.nodeAddress = nodeAddress;
		this.outgoing = false;
		connect();
	}

	public Connection(ConnectionHandler connectionHandler, NodeAddress nodeAddress) {
		this.connectionHandler = connectionHandler;
		this.nodeAddress = nodeAddress;
		this.outgoing = true;
		connect();
	}

	private void connect() {
		try {
			LOGGER.info("Connect, outgoing: {}, {}", outgoing, nodeAddress);
			openSocket();
			initializeSocket();
			startReaderThread();
		} catch (IOException e) {
			closeConnection();
		}
	}

	public void setConnectionHandler(ConnectionHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
	}

	public synchronized void closeConnection() {
		if (!active) {
			if (connectionHandler != null) {
				connectionHandler.handleConnectionClosed();
			}
			return;
		}
		LOGGER.info("Close connection, outgoing: {}, {}", outgoing, nodeAddress);
		try {
			active = false;
			socket.close();
		} catch (Exception ignore) {
		} finally {
			socket = null;
			dataOutputStream = null;
			dataInputStream = null;
			connectionHandler.handleConnectionClosed();
		}
	}

	private void openSocket() throws IOException {
		if (outgoing) {
			socket = new Socket(nodeAddress.getHost(), nodeAddress.getPort());
		}
		dataInputStream = new DataInputStream(socket.getInputStream());
		dataOutputStream = new DataOutputStream(socket.getOutputStream());
	}

	private void initializeSocket() throws IOException {
		socket.setKeepAlive(true);
		socket.setTcpNoDelay(true);
	}

	private void startReaderThread() {
		String threadName = "connection-reader-" + socket.getInetAddress().getHostAddress() + "-" + socket.getPort();
		Thread thread = new Thread(() -> {
			while (active) {
				try {
					int messageSize = dataInputStream.readInt();
					if (messageSize > 0 && messageSize < MAX_MESSAGE_SIZE) {
						byte[] data = new byte[messageSize];
						dataInputStream.readFully(data);
						connectionHandler.handleMessage(data);
					} else {
						closeConnection();
					}
				} catch (Exception e) {
					closeConnection();
				}
			}
		});
		thread.setName(threadName);
		thread.setDaemon(true);
		thread.start();
	}

	public synchronized boolean writeMessage(byte[] bytes) {
		try {
			dataOutputStream.writeInt(bytes.length);
			dataOutputStream.write(bytes);
			dataOutputStream.flush();
			return true;
		} catch (Exception e) {
			closeConnection();
			return false;
		}
	}

	public boolean isActive() {
		return active;
	}

	public boolean isOutgoing() {
		return outgoing;
	}

	public NodeAddress getNodeAddress() {
		return nodeAddress;
	}
}
