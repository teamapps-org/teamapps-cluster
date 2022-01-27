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
	public static final int MAX_MESSAGE_SIZE = 10_000_000;

	private final ConnectionHandler connectionHandler;
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

	private synchronized void closeConnection() {
		if (!active) {
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

	public synchronized void writeMessage(byte[] bytes) {
		try {
			dataOutputStream.writeInt(bytes.length);
			dataOutputStream.write(bytes);
			dataOutputStream.flush();
		} catch (Exception e) {
			closeConnection();
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
