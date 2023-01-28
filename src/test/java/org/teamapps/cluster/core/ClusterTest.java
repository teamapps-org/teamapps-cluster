package org.teamapps.cluster.core;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.teamapps.cluster.message.protocol.ClusterConfig;
import org.teamapps.cluster.message.protocol.ClusterNodeData;
import org.teamapps.cluster.message.test.protocol.AbstractClusterTest;
import org.teamapps.cluster.message.test.protocol.ClusterTestClient;
import org.teamapps.cluster.message.test.protocol.TestMethodRequest;
import org.teamapps.cluster.message.test.protocol.TestMethodResult;
import org.teamapps.cluster.utils.ExceptionUtil;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClusterTest {

	@Test
	public void testCluster() throws Exception {
		Cluster node1 = createServerNode(1);
		Cluster node2 = createServerNode(2);
		Cluster node3 = createClientNode(3, 1, 2);

		registerService(node1);
		registerService(node2);


		Thread.sleep(10);

		assertEquals(2, node1.getPeerNodes(true).size());
		assertEquals(2, node2.getPeerNodes(true).size());
		assertEquals(2, node3.getPeerNodes(true).size());
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-2")));
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-3")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-1")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-3")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-1")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-2")));


		ClusterTestClient client1 = new ClusterTestClient(node1);
		ClusterTestClient client2 = new ClusterTestClient(node2);
		ClusterTestClient client3 = new ClusterTestClient(node3);

		assertEquals("t1",execTestMethod(client1, "t1", 0).getAnswerPayload());
		assertEquals("t2",execTestMethod(client2, "t2", 0).getAnswerPayload());
		assertEquals("t3",execTestMethod(client3, "t3", 0).getAnswerPayload());

		ExecutorService executorService = Executors.newFixedThreadPool(10);
		int size = 100;
		AtomicInteger counter = new AtomicInteger();
		for (int i = 0; i < size; i++) {
			final int id = i;
			executorService.submit(() -> {
				TestMethodResult testMethodResult = client3.testServiceMethod(new TestMethodRequest().setAnswerPayload("test-" + id).setExecutionDelaySeconds(0));
				assertEquals("test-" + id, testMethodResult.getAnswerPayload());
				counter.incrementAndGet();
			});
		}
		executorService.shutdown();
		executorService.awaitTermination(1, TimeUnit.SECONDS);
		assertEquals(size, counter.get());

		shutdownCluster(node1, node2, node3);
	}

	@Test
	public void testReconnect() throws Exception {
		Cluster node1 = createServerNode(10);
		Cluster node2 = createServerNode(20);
		Cluster node3 = createClientNode(30, 10, 20);

		registerService(node1);
		registerService(node2);

		Thread.sleep(10);

		assertEquals(2, node1.getPeerNodes(true).size());
		assertEquals(2, node2.getPeerNodes(true).size());
		assertEquals(2, node3.getPeerNodes(true).size());
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-20")));
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-30")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-10")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-30")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-10")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-20")));

		node1.shutDown();
		Thread.sleep(10);
		assertEquals(0, node1.getPeerNodes(true).size());
		assertEquals(1, node2.getPeerNodes(true).size());
		assertEquals(1, node3.getPeerNodes(true).size());
		assertFalse(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-20")));
		assertFalse(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-30")));
		assertFalse(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-10")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-30")));
		assertFalse(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-10")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-20")));

		node1 = createServerNode(10);
		Thread.sleep(150);
		assertEquals(2, node1.getPeerNodes(true).size());
		assertEquals(2, node2.getPeerNodes(true).size());
		assertEquals(2, node3.getPeerNodes(true).size());
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-20")));
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-30")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-10")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-30")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-10")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-20")));
		shutdownCluster(node1, node2, node3);

	}

	@Test
	public void testNodeFailure() throws Exception {
		Cluster node1 = createServerNode(100);
		Cluster node2 = createServerNode(200);
		Cluster node3 = createServerNode(300);

		Cluster node4 = createClientNode(400, 100, 200, 300);

		registerService(node1);
		registerService(node2);
		registerService(node3);

		Thread.sleep(100);

		assertEquals(3, node1.getPeerNodes(true).size());
		assertEquals(3, node2.getPeerNodes(true).size());
		assertEquals(3, node3.getPeerNodes(true).size());
		assertEquals(3, node4.getPeerNodes(true).size());
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-200")));
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-300")));
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-400")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-100")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-300")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-400")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-100")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-200")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-400")));

		ClusterTestClient client3 = new ClusterTestClient(node3);

//		TestMethodResult testMethodResult = client3.testServiceMethod(new TestMethodRequest()
//						.setAnswerPayload("test")
//						.setExecutionDelaySeconds(0)
//						.setFailIfNodeId(node1.getLocalNode().getNodeId()));
//
//		assertEquals("test", testMethodResult.getAnswerPayload());

		//client3.testServiceMethod(new TestMethodRequest())

		ExecutorService executorService = Executors.newFixedThreadPool(10);
		int size = 100;
		AtomicInteger counter = new AtomicInteger();
		for (int i = 0; i < size; i++) {
			final int id = i;
			executorService.submit(() -> {
				TestMethodResult testMethodResult = client3.testServiceMethod(new TestMethodRequest()
						.setAnswerPayload("test-" + id)
						.setExecutionDelaySeconds(0)
						.setFailIfNodeId(node1.getLocalNode().getNodeId())
				);
				assertEquals("test-" + id, testMethodResult.getAnswerPayload());
				counter.incrementAndGet();
			});
		}
		executorService.shutdown();
		executorService.awaitTermination(1, TimeUnit.SECONDS);
		assertEquals(size, counter.get());

		shutdownCluster(node1, node2, node3);
	}

	private static void shutdownCluster(Cluster... clusters) {
		for (Cluster cluster : clusters) {
			cluster.shutDown();
		}
	}

	private static Cluster createServerNode(int id) {
		return createServerNode(id, null);
	}

	private static Cluster createServerNode(int id, Integer... knownPeers) {
		ClusterConfig clusterConfig = new ClusterConfig().setClusterSecret("secret").setPort(9_000 + id).setNodeId("node-" + id).setHost("localhost");
		clusterConfig.setPeerNodes(knownPeers == null ? null : Arrays.stream(knownPeers).map(val -> new ClusterNodeData().setHost("localhost").setPort(9_000 + val)).toList());
		return Cluster.start(clusterConfig);
	}

	private static Cluster createClientNode(int id, Integer... knownPeers) {
		ClusterConfig clusterConfig = new ClusterConfig().setClusterSecret("secret").setNodeId("node-" + id);
		clusterConfig.setPeerNodes(knownPeers == null ? null : Arrays.stream(knownPeers).map(val -> new ClusterNodeData().setHost("localhost").setPort(9_000 + val)).toList());
		return Cluster.start(clusterConfig);
	}

	private static void registerService(Cluster cluster) {
		new AbstractClusterTest(cluster) {
			@Override
			public TestMethodResult testServiceMethod(TestMethodRequest request) {
				ExceptionUtil.softenExceptions(() -> Thread.sleep(request.getExecutionDelaySeconds() * 1_000));
				String answerPayload = request.getAnswerPayload();
				if (request.getFailIfNodeId() != null && request.getFailIfNodeId().equals(cluster.getLocalNode().getNodeId())) {
					System.out.println("fail request");
					String s = null;
					int length = s.length();
				}
				return new TestMethodResult().setAnswerPayload(answerPayload).setExecutingNode(cluster.getLocalNode().getNodeId());
			}
		};
	}

	private static TestMethodResult execTestMethod(ClusterTestClient client, String payload, int wait) {
		TestMethodRequest testMethodRequest = new TestMethodRequest()
				.setAnswerPayload(payload)
				.setExecutionDelaySeconds(wait);
		return client.testServiceMethod(testMethodRequest);
	}

}