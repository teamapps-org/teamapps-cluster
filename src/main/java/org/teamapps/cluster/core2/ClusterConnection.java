package org.teamapps.cluster.core2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.crypto.AesCipher;
import org.teamapps.cluster.message.protocol.*;
import org.teamapps.message.protocol.file.*;
import org.teamapps.message.protocol.message.Message;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClusterConnection implements FileDataWriter, FileDataReader {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final int MAX_MESSAGE_SIZE = 100_000_000;

	private final ArrayBlockingQueue<Message> messageQueue = new ArrayBlockingQueue<>(100_000);
	private final ClusterNodeData remoteHostAddress;
	private final AesCipher aesCipher;
	private final File tempDir;
	private final boolean incomingConnection;
	private volatile boolean connected;
	private Cluster cluster;
	private Socket socket;
	private DataInputStream dataInputStream;
	private DataOutputStream dataOutputStream;
	private ClusterNode clusterNode;

	private long lastMessageTimestamp;
	private long sentBytes;
	private long receivedBytes;
	private long sentMessages;
	private long receivedMessages;

	public ClusterConnection(Cluster cluster, Socket socket) {
		this.cluster = cluster;
		this.socket = socket;
		this.aesCipher = new AesCipher(cluster.getClusterConfig().getClusterSecret());
		this.tempDir = cluster.getTempDir();
		this.connected = true;
		this.remoteHostAddress = new ClusterNodeData().setHost(socket.getRemoteSocketAddress().toString()).setPort(socket.getPort());
		this.incomingConnection = true;
		handleSocket(socket);
		startReaderThread();
		startWriterThread();
	}

	public ClusterConnection(Cluster cluster, ClusterNodeData peerNode, List<String> localServices) {
		this.cluster = cluster;
		this.aesCipher = new AesCipher(cluster.getClusterConfig().getClusterSecret());
		this.remoteHostAddress = peerNode;
		this.tempDir = cluster.getTempDir();
		this.incomingConnection = false;
		connect(peerNode);
		if (connected) {
			String[] localServicesArray = localServices.isEmpty() ? null : localServices.toArray(localServices.toArray(new String[0]));
			startReaderThread();
			startWriterThread();
			writeDirectMessage(new ClusterConnectionRequest().setLocalNode(cluster.getLocalNode()).setLocalServices(localServicesArray));
		}
	}


	private void connect(ClusterNodeData clusterNode) {
		try {
			socket = new Socket(clusterNode.getHost(), clusterNode.getPort());
			connected = true;
			handleSocket(socket);
		} catch (IOException e) {
			close();
		}
	}

	private void handleSocket(Socket socket) {
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
						Message message = new Message(messageData);
						switch (message.getMessageDefUuid()) {
							case ClusterServiceMethodRequest.OBJECT_UUID -> handleClusterServiceMethodRequest(ClusterServiceMethodRequest.remap(message));
							case ClusterServiceMethodResult.OBJECT_UUID -> handleClusterServiceMethodResult(ClusterServiceMethodResult.remap(message));
							case ClusterMessageFilePart.OBJECT_UUID -> handleClusterMessageFilePart(ClusterMessageFilePart.remap(message));
							case ClusterAvailableServicesUpdate.OBJECT_UUID -> handleClusterAvailableServicesUpdate(ClusterAvailableServicesUpdate.remap(message));
							case ClusterNewPeerInfo.OBJECT_UUID -> handleClusterNewPeerInfo(ClusterNewPeerInfo.remap(message));
							case ClusterConnectionRequest.OBJECT_UUID -> handleClusterConnectionRequest(ClusterConnectionRequest.remap(message));
							case ClusterConnectionResult.OBJECT_UUID -> handleClusterConnectionResult(ClusterConnectionResult.remap(message));

							case ClusterNodeShutDownInfo.OBJECT_UUID -> {
								LOGGER.info("Cluster node {} - cluster peer is shutting down {}:{}", cluster.getLocalNode().getNodeId(), remoteHostAddress.getHost(), remoteHostAddress.getPort());
								close();
							}
						}
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

	private void handleClusterNewPeerInfo(ClusterNewPeerInfo newPeerInfo) {
		cluster.handleClusterNewPeerInfo(newPeerInfo, clusterNode);
	}

	private void handleClusterAvailableServicesUpdate(ClusterAvailableServicesUpdate availableServicesUpdate) {
		cluster.handleClusterAvailableServicesUpdate(availableServicesUpdate, clusterNode);
	}

	private void handleClusterConnectionRequest(ClusterConnectionRequest request) {
		ClusterConnectionResult connectionResult = cluster.handleConnectionRequest(request, this);
		writeDirectMessage(connectionResult);
	}

	private void handleClusterConnectionResult(ClusterConnectionResult result) {
		cluster.handleConnectionResult(result, result.getLocalNode(), this);
	}

	private void handleClusterServiceMethodRequest(ClusterServiceMethodRequest request) {
		cluster.handleServiceMethodExecutionRequest(request, clusterNode);
	}

	private void handleClusterServiceMethodResult(ClusterServiceMethodResult result) {
		cluster.handleServiceMethodExecutionResult(result, clusterNode);
	}

	private void startWriterThread() {
		Thread thread = new Thread(() -> {
			while (connected) {
				try {
					Message message = messageQueue.poll(30, TimeUnit.SECONDS);
					if (message != null) {
						byte[] bytes = message.toBytes(this);
						byte[] data = aesCipher.encrypt(bytes);
						writeData(data);
					}
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

	public void writeMessage(Message message) {
		if (!messageQueue.offer(message)) {
			LOGGER.warn("Cluster node {} - error: connection message queue is full: {}:{}", cluster.getLocalNode().getNodeId(), remoteHostAddress.getHost(), remoteHostAddress.getPort());
			close();
		}
	}

	private void writeDirectMessage(Message message) {
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

	private void handleClusterMessageFilePart(ClusterMessageFilePart filePart) {
		try {
			long length = appendFileTransferData(filePart.getFileId(), filePart.getData(), filePart.isInitialMessage());
			if (filePart.isLastMessage() && length != filePart.getTotalLength()) {
				LOGGER.error("Cluster node {} - wrong cluster message file size, expected: {}, actual: {}", cluster.getLocalNode().getNodeId(), filePart.getTotalLength(), length);
				close();
			}
		} catch (IOException e) {
			close();
		}
	}

	@Override
	public FileData writeFileData(FileData fileData) throws IOException {
		if (fileData.getType() == FileDataType.CLUSTER_STORE) {
			return fileData;
		} else {
			String fileId = UUID.randomUUID().toString().replace("-", ".");
			long fileLength = fileData.getLength();
			int maxContentSize = 10_000_000;
			int parts = (int) ((fileLength - 1) / maxContentSize) + 1;
			if (parts == 1) {
				byte[] bytes = fileData.toBytes();
				ClusterMessageFilePart messageFilePart = new ClusterMessageFilePart()
						.setFileId(fileId)
						.setTotalLength(fileLength)
						.setLastMessage(true)
						.setData(bytes);
				writeDirectMessage(messageFilePart);
			} else {
				DataInputStream dis = new DataInputStream(new BufferedInputStream(fileData.getInputStream()));
				for (int i = 0; i < parts; i++) {
					int length = maxContentSize;
					boolean lastMessage = i + 1 == parts;
					if (lastMessage) {
						length = (int) (fileLength - (i * maxContentSize));
					}
					byte[] bytes = new byte[length];
					dis.readFully(bytes);
					ClusterMessageFilePart messageFilePart = new ClusterMessageFilePart()
							.setFileId(fileId)
							.setTotalLength(fileLength)
							.setLastMessage(lastMessage)
							.setData(bytes);
					writeDirectMessage(messageFilePart);
				}
				dis.close();
			}
			return new GenericFileData(fileData.getType(), fileData.getFileName(), fileData.getLength(), fileId);
		}
	}

	@Override
	public FileData readFileData(FileDataType type, String fileName, long length, String descriptor, boolean encrypted, String encryptionKey) throws IOException {
		if (type == FileDataType.CLUSTER_STORE) {
			return new GenericFileData(type, fileName, length, descriptor, encrypted, encryptionKey);
		} else {
			File clusterTransferFile = getClusterTransferFile(descriptor);
			if (!clusterTransferFile.exists() || clusterTransferFile.length() != length) {
				throw new RuntimeException("Error reading file data:" + fileName + ", " + length + ", " + clusterTransferFile);
			}
			return FileData.create(clusterTransferFile, fileName);
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

	public void close() {
		if (!connected) {
			return;
		}
		LOGGER.info("Cluster node {} - closed connection {}:{}", cluster.getLocalNode().getNodeId(), remoteHostAddress.getHost(), remoteHostAddress.getPort());
		try {
			connected = false;
			if (socket != null) {
				socket.close();
			}
		} catch (Exception ignore) {
		} finally {
			socket = null;
			dataOutputStream = null;
			dataInputStream = null;
			if (clusterNode != null) {
				clusterNode.handleConnectionClosed();
			}
		}
	}

	public void setClusterNode(ClusterNode clusterNode) {
		this.clusterNode = clusterNode;
	}

	public boolean isConnected() {
		return connected;
	}

	public long getLastMessageTimestamp() {
		return lastMessageTimestamp;
	}

	public long getSentBytes() {
		return sentBytes;
	}

	public long getReceivedBytes() {
		return receivedBytes;
	}

	public long getSentMessages() {
		return sentMessages;
	}

	public long getReceivedMessages() {
		return receivedMessages;
	}

	public boolean isIncomingConnection() {
		return incomingConnection;
	}
}
