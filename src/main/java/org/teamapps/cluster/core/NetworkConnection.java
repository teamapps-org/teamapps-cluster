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
package org.teamapps.cluster.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.crypto.AesCipher;
import org.teamapps.cluster.protocol.ClusterInfo;
import org.teamapps.cluster.protocol.ClusterMessageFilePart;
import org.teamapps.cluster.protocol.ClusterMethodExecution;
import org.teamapps.protocol.schema.MessageObject;
import org.teamapps.protocol.schema.ModelRegistry;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class NetworkConnection implements Connection {

	public static final int MAX_MESSAGE_SIZE = 100_000_000;
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

	private HostAddress remoteHostAddress;
	private MessageQueue messageQueue;
	private ConnectionHandler connectionHandler;
	private ModelRegistry modelRegistry;
	private AesCipher aesCipher;
	private File tempDir;
	private volatile boolean connected;
	private Socket socket;
	private DataInputStream dataInputStream;
	private DataOutputStream dataOutputStream;
	private long lastMessageTimestamp;
	private long sentBytes;
	private long receivedBytes;
	private long sentMessages;
	private long receivedMessages;


	public NetworkConnection(Socket socket, MessageQueue messageQueue, ConnectionHandler connectionHandler, ModelRegistry modelRegistry, File tempDir, String clusterSecret) {
		this.socket = socket;
		this.messageQueue = messageQueue;
		this.modelRegistry = modelRegistry;
		this.connectionHandler = connectionHandler;
		this.tempDir = tempDir;
		this.aesCipher = new AesCipher(clusterSecret);
		this.connected = true;
		this.remoteHostAddress = new HostAddress(socket.getRemoteSocketAddress().toString(), socket.getPort());
		handleSocket(socket);
		startReaderThread();
		startWriterThread();
	}

	public NetworkConnection(HostAddress hostAddress, MessageQueue messageQueue, ConnectionHandler connectionHandler, ModelRegistry modelRegistry, File tempDir, String clusterSecret) {
		this.messageQueue = messageQueue;
		this.modelRegistry = modelRegistry;
		this.connectionHandler = connectionHandler;
		this.tempDir = tempDir;
		this.aesCipher = new AesCipher(clusterSecret);
		this.remoteHostAddress = hostAddress;
		connect(hostAddress);
		if (connected) {
			startReaderThread();
			startWriterThread();
			writeDirectMessage(connectionHandler.getClusterInfo().setInitialMessage(true));
		}
	}

	private void connect(HostAddress hostAddress) {
		try {
			socket = new Socket(hostAddress.getHost(), hostAddress.getPort());
			connected = true;
			handleSocket(socket);
		} catch (IOException e) {
			close();
		}
	}

	protected void handleSocket(Socket socket) {
		try {
			socket.setKeepAlive(true);
			socket.setTcpNoDelay(true);
			dataInputStream = new DataInputStream(socket.getInputStream());
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			close();
		}
	}

	private void startReaderThread() {
		Thread thread = new Thread(() -> {
			while (connected) {
				try {
					int messageSize = dataInputStream.readInt();
					if (messageSize > 0 && messageSize < MAX_MESSAGE_SIZE) {
						byte[] data = new byte[messageSize];
						dataInputStream.readFully(data);
						byte[] messageData = aesCipher.decrypt(data);
						handleMessageData(messageData);
						receivedBytes += messageSize + 4;
						receivedMessages++;
						lastMessageTimestamp = System.currentTimeMillis();
					} else {
						close();
					}
				} catch (Exception e) {
					close();
				}
			}
		});
		thread.setName("connection-reader-" + socket.getInetAddress().getHostAddress() + "-" + socket.getPort());
		thread.setDaemon(true);
		thread.start();
	}

	private void startWriterThread() {
		Thread thread = new Thread(() -> {
			while (connected) {
				try {
					MessageQueueEntry queueEntry = messageQueue.getNext();
					if (queueEntry.isServiceExecution()) {
						byte[] value = queueEntry.getMessage() == null ? null : queueEntry.getMessage().toBytes(this);
						ClusterMethodExecution clusterMethodExecution = new ClusterMethodExecution()
								.setRequestId(queueEntry.getRequestId())
								.setServiceName(queueEntry.getServiceName())
								.setServiceMethod(queueEntry.getServiceMethod())
								.setResponse(queueEntry.isServiceResponse())
								.setData(value);
						byte[] bytes = clusterMethodExecution.toBytes();
						byte[] data = aesCipher.encrypt(bytes);
						writeData(data);
					} else {
						MessageObject message = queueEntry.getMessage();
						byte[] bytes = message.toBytes(this);
						byte[] data = aesCipher.encrypt(bytes);
						writeData(data);
					}
					messageQueue.commitLastMessage();
				} catch (InterruptedException ignore) {
				} catch (Exception exception) {
					close();
				}
			}
		});
		thread.setDaemon(true);
		thread.setName("connection-writer-" + socket.getInetAddress().getHostAddress() + "-" + socket.getPort());
		thread.start();
	}

	private void handleMessageData(byte[] messageData) {
		try {
			String objectUuid = MessageObject.readMessageObjectUuid(messageData);
			switch (objectUuid) {
				case ClusterMessageFilePart.OBJECT_UUID:
					handleClusterMessageFilePart(new ClusterMessageFilePart(messageData, this));
					break;
				case ClusterInfo.OBJECT_UUID:
					handleClusterInfo(new ClusterInfo(messageData, this));
					break;
				case ClusterMethodExecution.OBJECT_UUID:
					handleClusterMethodExecution(new ClusterMethodExecution(messageData, this));
					break;
				default:
					connectionHandler.handleMessage(new MessageObject(messageData, modelRegistry, this, null));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void handleClusterMethodExecution(ClusterMethodExecution methodExecution) throws IOException {
		MessageObject messageObject = methodExecution.getData() == null ? null : new MessageObject(methodExecution.getData(), modelRegistry, this, null);
		if (!methodExecution.isResponse()) {
			connectionHandler.handleClusterExecutionRequest(methodExecution.getServiceName(), methodExecution.getServiceMethod(), messageObject, methodExecution.getRequestId());

		} else {
			connectionHandler.handleClusterExecutionResult(messageObject, methodExecution.getRequestId());
		}
	}

	private void writeDirectMessage(MessageObject message) {
		try {
			if (connected) {
				byte[] bytes = message.toBytes(this);
				byte[] data = aesCipher.encrypt(bytes);
				writeData(data);
			}
		} catch (Exception e) {
			close();
		}
	}

	private synchronized void writeData(byte[] bytes) {
		try {
			dataOutputStream.writeInt(bytes.length);
			dataOutputStream.write(bytes);
			dataOutputStream.flush();
			sentBytes += bytes.length + 4;
			sentMessages++;
			lastMessageTimestamp = System.currentTimeMillis();
		} catch (Exception e) {
			close();
		}
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public void close() {
		if (!connected) {
			return;
		}
		LOGGER.info("Closed connection {}", remoteHostAddress);
		try {
			connected = false;
			if (socket != null) {
				socket.close();
			}
			messageQueue.recycleQueue();
		} catch (Exception ignore) {
		} finally {
			socket = null;
			dataOutputStream = null;
			dataInputStream = null;
			connectionHandler.handleConnectionClosed();
		}
	}

	@Override
	public long getLastMessageTimestamp() {
		return lastMessageTimestamp;
	}

	@Override
	public long getSentBytes() {
		return sentBytes;
	}

	@Override
	public long getReceivedBytes() {
		return receivedBytes;
	}

	@Override
	public long getSentMessages() {
		return sentMessages;
	}

	@Override
	public long getReceivedMessages() {
		return receivedMessages;
	}

	@Override
	public void sendKeepAlive() {

	}

	private void handleClusterInfo(ClusterInfo clusterInfo) {
		LOGGER.info("Handle cluster info:" + clusterInfo);
		if (!clusterInfo.isResponse()) {
			writeDirectMessage(connectionHandler.getClusterInfo().setResponse(true).setInitialMessage(clusterInfo.isInitialMessage()));
		}
		if (clusterInfo.isInitialMessage()) {
			connectionHandler.handleConnectionEstablished(this, clusterInfo);
		} else {
			connectionHandler.handleClusterInfoUpdate(clusterInfo);
		}
	}

	private void handleClusterMessageFilePart(ClusterMessageFilePart filePart) {
		try {
			long length = appendFileTransferData(filePart.getFileId(), filePart.getData(), filePart.isInitialMessage());
			if (filePart.isLastMessage() && length != filePart.getTotalLength()) {
				LOGGER.error("Wrong cluster message file size, expected: {}, actual: {}", filePart.getTotalLength(), length);
				close();
			}
		} catch (IOException e) {
			close();
		}
	}


	private File getClusterTransferFile(String fileId) {
		return new File(tempDir, fileId + ".tmp");
	}

	private long appendFileTransferData(String fileId, byte[] bytes, boolean initialData) throws IOException {
		File file = getClusterTransferFile(fileId);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, !initialData), 32_000);
		bos.write(bytes);
		bos.close();
		return file.length();
	}

	@Override
	public File getFile(String fileId) {
		return getClusterTransferFile(fileId);
	}

	@Override
	public String handleFile(File file) throws IOException {
		if (file == null || file.length() == 0 || !connected) {
			return null;
		}
		String fileId = UUID.randomUUID().toString().replace("-", ".");
		long fileLength = file.length();
		int maxContentSize = 10_000_000;
		int parts = (int) ((fileLength - 1) / maxContentSize) + 1;
		if (parts == 1) {
			byte[] bytes = Files.readAllBytes(file.toPath());
			writeDirectMessage(new ClusterMessageFilePart()
					.setFileId(fileId)
					.setTotalLength(fileLength)
					.setLastMessage(true)
					.setData(bytes));
		} else {
			DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
			for (int i = 0; i < parts; i++) {
				int length = maxContentSize;
				boolean lastMessage = i + 1 == parts;
				if (lastMessage) {
					length = (int) (fileLength - (i * maxContentSize));
				}
				byte[] bytes = new byte[length];
				dis.readFully(bytes);
				writeDirectMessage(new ClusterMessageFilePart()
						.setFileId(fileId)
						.setTotalLength(fileLength)
						.setLastMessage(lastMessage)
						.setData(bytes));
			}
			dis.close();
		}
		return fileId;
	}
}
