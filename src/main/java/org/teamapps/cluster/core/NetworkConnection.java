package org.teamapps.cluster.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.crypto.AesCipher;
import org.teamapps.cluster.protocol.ClusterInfo;
import org.teamapps.cluster.protocol.ClusterMessageFilePart;
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


	public NetworkConnection(Socket socket, ConnectionHandler connectionHandler, ModelRegistry modelRegistry, File tempDir, String clusterSecret) {
		this.socket = socket;
		this.modelRegistry = modelRegistry;
		this.connectionHandler = connectionHandler;
		this.tempDir = tempDir;
		this.aesCipher = new AesCipher(clusterSecret);
		this.connected = true;
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
		connect(hostAddress);
		startReaderThread();
		startWriterThread();
		writeDirectMessage(connectionHandler.getClusterInfo().setInitialMessage(true));
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
					MessageObject message = messageQueue.getNext();
					byte[] bytes = message.toBytes(this);
					byte[] data = aesCipher.encrypt(bytes);
					writeData(data);
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
				default:
					connectionHandler.handleMessage(new MessageObject(messageData, modelRegistry, this, null));
			}
		} catch (Exception e) {
			e.printStackTrace();
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
	public long lastMessageTimestamp() {
		return lastMessageTimestamp;
	}

	@Override
	public long sendBytes() {
		return sentBytes;
	}

	@Override
	public long receivedBytes() {
		return receivedBytes;
	}

	@Override
	public void sendKeepAlive() {

	}

	private void handleClusterInfo(ClusterInfo clusterInfo) {
		if (!clusterInfo.isResponse()) {
			writeDirectMessage(connectionHandler.getClusterInfo().setResponse(true).setInitialMessage(clusterInfo.isInitialMessage()));
		}
		if (clusterInfo.isInitialMessage()) {
			connectionHandler.handleConnectionEstablished(this, clusterInfo);
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
