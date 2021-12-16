package org.teamapps.cluster.service;

import io.atomix.cluster.Member;
import io.atomix.cluster.MemberId;
import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;
import io.atomix.cluster.messaging.ClusterCommunicationService;
import io.atomix.core.Atomix;
import io.atomix.primitive.partition.MemberGroupStrategy;
import io.atomix.protocols.backup.partition.PrimaryBackupPartitionGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.cluster.crypto.AesCipher;
import org.teamapps.cluster.crypto.ShaHash;
import org.teamapps.cluster.dto.FileProvider;
import org.teamapps.cluster.dto.Message;
import org.teamapps.cluster.dto.MessageDecoder;
import org.teamapps.cluster.model.*;
import org.teamapps.common.util.ExceptionUtil;
import org.teamapps.event.Event;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TeamAppsCluster implements FileProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String CLUSTER_REQUEST_CHANNEL = "cr";
	private static final String FILE_TRANSFER_CHANNEL = "ft";
	private static final String SERVICES_PROPERTY = "services";
	private final String clusterId;
	private final AesCipher aesCipher;
	private final File fileTransferPath;
	private int retryMaxAttempts = 3;
	private Duration retryBackoffDuration = Duration.ofSeconds(3);
	private final Map<String, File> fileTransferMap = Collections.synchronizedMap(new LinkedHashMap<>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, File> eldest) {
			return size() > 25_000;
		}
	});

	public final Event<Member> onMemberAdded = new Event<>();
	public final Event<Member> onMemberRemoved = new Event<>();
	private String localId;
	private Atomix atomix;
	private final List<Member> members = Collections.synchronizedList(new ArrayList<>());
	private final Map<String, AbstractClusterService> localServices = new ConcurrentHashMap<>();
	private final Map<String, List<MemberId>> clusterServices = new ConcurrentHashMap<>();

	private ExecutorService executor = new ThreadPoolExecutor(1, 32,
			180L, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>());
	private ClusterCommunicationService communicationService;
	private Member localMember;

	public TeamAppsCluster(String clusterKey) throws IOException {
		this(clusterKey, null);
	}

	public TeamAppsCluster(String clusterKey, File tempDir) throws IOException {
		this.clusterId = ShaHash.createHash("ID-" + clusterKey);
		this.aesCipher = new AesCipher(clusterKey);
		this.fileTransferPath = tempDir != null ? tempDir : Files.createTempFile("temp", "temp").getParent().toFile();
	}

	private byte[] handleFileMessage(byte[] data) {
		try {
			byte[] decryptedMessage = aesCipher.decrypt(data);
			FileTransfer transferMessage = new FileTransfer(decryptedMessage);
			String fileId = transferMessage.getFileId();
			File file = new File(fileTransferPath, fileId + ".tmp");
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, true), 32_000);
			bos.write(transferMessage.getData());
			bos.close();
			FileTransferResponse fileTransferResponse = new FileTransferResponse().setReceivedData(file.length());
			if (transferMessage.getFinished()) {
				fileTransferMap.put(fileId, file);
				fileTransferResponse.setFinished(true);
			}
			return aesCipher.encrypt(fileTransferResponse.toBytes());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private byte[] handleClusterRequest(byte[] data) {
		try {
			byte[] decryptedMessage = aesCipher.decrypt(data);
			ClusterMessage clusterMessage = new ClusterMessage(decryptedMessage);
			byte[] messageData = clusterMessage.getMessageData();
			MemberId memberId = MemberId.from(clusterMessage.getMemberId());
			AbstractClusterService clusterService = localServices.get(clusterMessage.getClusterService());
			if (clusterService != null) {
				byte[] bytes = clusterService.handleMessage(clusterMessage.getClusterMethod(), messageData, this, file -> ExceptionUtil.softenExceptions(() -> sendFile(file, memberId, null)));
				return aesCipher.encrypt(bytes);
			} else {
				LOGGER.error("Could not find requested service {}", clusterMessage.getClusterService());
				return null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public File getFile(String fileId) {
		return fileTransferMap.get(fileId);
	}

	private CompletableFuture<byte[]> sendMessage(String subject, Message message, MemberId memberId) throws Exception {
		byte[] bytes = message.toBytes();
		byte[] encryptedMessage = aesCipher.encrypt(bytes);
		return communicationService.send(subject, encryptedMessage, memberId);
	}

	private String sendFile(File file, MemberId memberId, AtomicBoolean disposed) throws Exception {
		String fileId = UUID.randomUUID().toString().replace("-", ".");
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		byte[] buf = new byte[10_000];
		int read;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while ((read = bis.read(buf)) >= 0) {
			bos.write(buf, 0, read);
			if (bos.size() >= 1_000_000) {
				FileTransfer fileTransfer = new FileTransfer().setFileId(fileId).setData(bos.toByteArray());
				bos.reset();
				if (disposed != null && disposed.get()) {
					return null;
				}
				sendMessage(FILE_TRANSFER_CHANNEL, fileTransfer, memberId);
			}
		}
		FileTransfer fileTransfer = new FileTransfer().setFileId(fileId).setData(bos.toByteArray()).setFinished(true);
		if (disposed != null && disposed.get()) {
			return null;
		}
		CompletableFuture<byte[]> resultMessage = sendMessage(FILE_TRANSFER_CHANNEL, fileTransfer, memberId);
		byte[] bytes = resultMessage.get(60, TimeUnit.SECONDS);
		FileTransferResponse response = new FileTransferResponse(aesCipher.decrypt(bytes));
		if (response.getReceivedData() == file.length()) {
			return fileId;
		} else {
			throw new Exception("Error sending file transfer:" + response);
		}
	}

	public <REQUEST extends Message, RESPONSE extends Message> Mono<RESPONSE> createServiceTask(String serviceName, String messageType, REQUEST request, MessageDecoder<RESPONSE> responseDecoder) {
		Mono<RESPONSE> mono = Mono.<CompletableFuture<byte[]>>create(monoSink -> {
					List<MemberId> serviceProvider = clusterServices.getOrDefault(serviceName, Collections.emptyList());
					MemberId memberId = Utils.randomListEntry(serviceProvider);
					if (memberId == null) {
						LOGGER.warn("No cluster member available for service: {}, method: {}, with request: {}", serviceName, messageType, request);
						monoSink.error(new Exception("Error: no cluster member available!"));
						return;
					}
					LOGGER.debug("Create cluster task for member: " + memberId);
					AtomicBoolean disposed = new AtomicBoolean();
					monoSink.onDispose(() -> disposed.set(true));
					try {
						byte[] messageBytes = request.toBytes(file -> ExceptionUtil.softenExceptions(() -> sendFile(file, memberId, disposed)));
						if (disposed.get()) {
							return;
						}
						ClusterMessage clusterMessage = new ClusterMessage().setMemberId(localId).setClusterService(serviceName).setClusterMethod(messageType).setMessageData(messageBytes);
						byte[] data = aesCipher.encrypt(clusterMessage.toBytes());
						CompletableFuture<byte[]> message = communicationService.send(CLUSTER_REQUEST_CHANNEL, data, memberId, Duration.ofSeconds(60));
						monoSink.success(message);
					} catch (Exception e) {
						monoSink.error(e);
					}
				}).flatMap(Mono::fromFuture).map(bytes -> responseDecoder.decode(aesCipher.decryptSave(bytes), this))
				.subscribeOn(Schedulers.boundedElastic())
				.retryWhen(Retry.backoff(retryMaxAttempts, retryBackoffDuration));
		return mono;
	}


	public void connect(int localPort, String bootstrapNodes) {
		List<Node> nodes = bootstrapNodes == null || bootstrapNodes.isBlank() ? Collections.emptyList() : Arrays.stream(bootstrapNodes.split(";"))
				.map(host -> host.split(":"))
				.map(parts -> Node.builder().withHost(parts[0]).withPort(Integer.parseInt(parts[1])).build())
				.collect(Collectors.toList());

		atomix = Atomix.builder()
				.withClusterId(clusterId)
				.withPort(localPort)
				.withMembershipProvider(
						BootstrapDiscoveryProvider.builder()
								.withNodes(nodes)
								.withHeartbeatInterval(Duration.ofMillis(500))
								.withFailureTimeout(Duration.ofMillis(3_000))
								.build()
				)
				.withManagementGroup(
						PrimaryBackupPartitionGroup.builder("mmg")
								.withNumPartitions(71)
								.withMemberGroupStrategy(MemberGroupStrategy.RACK_AWARE)
								.build()
				)
				.withPartitionGroups(
						PrimaryBackupPartitionGroup.builder("data")
								.withMemberGroupStrategy(MemberGroupStrategy.RACK_AWARE)
								.withNumPartitions(71)
								.build()
				)
				.withShutdownHookEnabled()
				.build();


		CompletableFuture<Void> startFuture = atomix.start();
		localId = atomix.getMembershipService().getLocalMember().id().id();
		LOGGER.info("Start cluster with local-id: {} and cluster id: {}", localId, clusterId);


		communicationService = atomix.getCommunicationService();
		atomix.getMembershipService().addListener(event -> {
			Member member = event.subject();
			if (member.id().id().equals(localId)) {
				localMember = member;
			} else {
				LOGGER.info("Member update: {}", event);
				switch (event.type()) {
					case MEMBER_ADDED -> {
						handleNewMember(member);
					}
					case METADATA_CHANGED -> {
						handleNewServices(member);
					}
					case REACHABILITY_CHANGED -> {

					}
					case MEMBER_REMOVED -> {
						handleRemovedMember(member);
					}
				}
			}
		});

		communicationService.subscribe(FILE_TRANSFER_CHANNEL, this::handleFileMessage, Executors.newSingleThreadExecutor());
		communicationService.subscribe(CLUSTER_REQUEST_CHANNEL, this::handleClusterRequest, Executors.newWorkStealingPool(24));
		startFuture.join();
	}

	protected void registerService(AbstractClusterService clusterService) {
		localServices.put(clusterService.getServiceName(), clusterService);
		localMember.properties().setProperty(SERVICES_PROPERTY, String.join(",", localServices.keySet()));
	}

	private void handleNewMember(Member member) {
		members.add(member);
		onMemberAdded.fire(member);
		handleNewServices(member);
	}

	private void handleRemovedMember(Member member) {
		members.remove(member);
		MemberId memberId = member.id();
		for (String key : clusterServices.keySet()) {
			clusterServices.get(key).remove(memberId);
		}
		onMemberRemoved.fire(member);
	}

	private void handleNewServices(Member member) {
		MemberId memberId = member.id();
		String services = member.properties().getProperty(SERVICES_PROPERTY);
		if (services != null && !services.isBlank()) {
			for (String key : clusterServices.keySet()) {
				clusterServices.get(key).remove(memberId);
			}
			Arrays.stream(services.split(",")).forEach(service -> {
				LOGGER.info("Add cluster service {} for member {}", services, memberId.id());
				clusterServices.putIfAbsent(service, new ArrayList<>());
				clusterServices.get(service).add(memberId);
			});
		}
	}

	private int getRandomId(int max) {
		int id = max + 1;
		while (id > max) {
			id = (int) (Math.random() * max);
		}
		return id;
	}

	public void disconnect() {
		atomix.stop();
		atomix = null;
	}

	public void setRetryMaxAttempts(int retryMaxAttempts) {
		this.retryMaxAttempts = retryMaxAttempts;
	}

	public void setRetryBackoffDuration(Duration retryBackoffDuration) {
		this.retryBackoffDuration = retryBackoffDuration;
	}
}
