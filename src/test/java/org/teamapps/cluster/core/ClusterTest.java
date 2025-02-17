/*-
 * ========================LICENSE_START=================================
 * TeamApps Cluster
 * ---
 * Copyright (C) 2021 - 2025 TeamApps.org
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
import org.teamapps.message.protocol.file.FileData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
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

		ExecutorService executorService = Executors.newFixedThreadPool(3);
		int size = 10;
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
		executorService.awaitTermination(10, TimeUnit.SECONDS);
		assertEquals(size, counter.get());

		shutdownCluster(node1, node2, node3);
	}

	@Test
	public void testServiceMethod() throws Exception {
		Cluster node1 = createServerNode(150);
		Cluster node2 = createServerNode(250);
		Cluster node3 = createServerNode(350);

		Cluster node4 = createClientNode(450, 150, 250, 350);

		registerService(node1);
		registerService(node2);
		registerService(node3);

		Thread.sleep(100);

		Path temp = Files.createTempFile("temp", ".tmp");
		String text = "THIS IS A TEST-FILE!";
		Files.writeString(temp, text, StandardCharsets.UTF_8);
		File file = temp.toFile();

		assertEquals(3, node1.getPeerNodes(true).size());
		assertEquals(3, node2.getPeerNodes(true).size());
		assertEquals(3, node3.getPeerNodes(true).size());
		assertEquals(3, node4.getPeerNodes(true).size());
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-250")));
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-350")));
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-450")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-150")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-350")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-450")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-150")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-250")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-450")));

		ClusterTestClient client4 = new ClusterTestClient(node4);

		ExecutorService executorService = Executors.newFixedThreadPool(2);
		int size = 1000;
		AtomicInteger counter = new AtomicInteger();
		for (int i = 0; i < size; i++) {
			final int id = i;
			executorService.submit(() -> {
				TestMethodResult testMethodResult = client4.testServiceMethod(new TestMethodRequest()
						.setAnswerPayload("test-" + id)
						.setExecutionDelaySeconds(0)
								.setTestFile(file)
						//.setFailIfNodeId(node1.getLocalNode().getNodeId())
				);
				assertEquals("test-" + id, testMethodResult.getAnswerPayload());
				assertEquals(file.length(), testMethodResult.getResultFile().getLength());
				counter.incrementAndGet();
			});
		}
		executorService.shutdown();
		executorService.awaitTermination(15, TimeUnit.SECONDS);
		assertEquals(size, counter.get());

		shutdownCluster(node1, node2, node3);
	}

	@Test
	public void testLeaderNode() throws Exception {
		Cluster node1 = createServerNode(120);
		Cluster node2 = createLeaderNode(220);
		Cluster node3 = createServerNode(320);
		Cluster node4 = createClientNode(420, 120, 220, 320);

		Thread.sleep(100);

		assertEquals(3, node1.getPeerNodes(true).size());
		assertEquals(3, node2.getPeerNodes(true).size());
		assertEquals(3, node3.getPeerNodes(true).size());
		assertEquals(3, node4.getPeerNodes(true).size());
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-220")));
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-320")));
		assertTrue(node1.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-420")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-120")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-320")));
		assertTrue(node2.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-420")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-120")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-220")));
		assertTrue(node3.getPeerNodes(true).stream().anyMatch(clusterNodeData -> clusterNodeData.getNodeId().equals("node-420")));

		assertEquals(node2.getLocalNode().getNodeId(), node1.getLeaderNode().getNodeId());
		assertEquals(node2.getLocalNode().getNodeId(), node2.getLeaderNode().getNodeId());
		assertEquals(node2.getLocalNode().getNodeId(), node3.getLeaderNode().getNodeId());
		assertEquals(node2.getLocalNode().getNodeId(), node4.getLeaderNode().getNodeId());
	}

	@Test
	public void testFileTransfer() throws Exception {
		Path temp = Files.createTempFile("temp", ".tmp");
		String text = "THIS IS A TEST-FILE!";
		Files.writeString(temp, text, StandardCharsets.UTF_8);

		Cluster node1 = createServerNode(110);
		Cluster node2 = createServerNode(210);
		Cluster node3 = createClientNode(310, 110, 210);

		registerService(node1);
		registerService(node2);
		Thread.sleep(10);

		ClusterTestClient client1 = new ClusterTestClient(node1);
		ClusterTestClient client2 = new ClusterTestClient(node2);
		ClusterTestClient client3 = new ClusterTestClient(node3);

		File testFile = temp.toFile();
		long length = testFile.length();
		TestMethodRequest methodRequest = new TestMethodRequest()
				.setTestFile(testFile)
				.setAnswerPayload("file");
		TestMethodResult testMethodResult = client3.testServiceMethod(methodRequest);
		FileData resultFile = testMethodResult.getResultFile();
		assertEquals(length, resultFile.getLength());
		String resultText = Files.readString(resultFile.copyToTempFile().toPath(), StandardCharsets.UTF_8);
		assertEquals(text, resultText);

		temp = Files.createTempFile("temp", ".tmp");
		testFile = temp.toFile();
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(testFile)));
		int size = 15_000_000;
		int loops = size / 8;
		for (int i = 0; i < loops; i++) {
			dos.writeLong(i + 1);
		}
		dos.close();
		methodRequest = new TestMethodRequest()
				.setTestFile(testFile)
				.setAnswerPayload("large-file");
		testMethodResult = client3.testServiceMethod(methodRequest);
		resultFile = testMethodResult.getResultFile();
		assertEquals(size, resultFile.getLength());

		DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(resultFile.copyToTempFile())));
		for (int i = 0; i < loops; i++) {
			long value = dis.readLong();
			assertEquals(i + 1, value);
		}
	}

	private static void shutdownCluster(Cluster... clusters) {
		for (Cluster cluster : clusters) {
			cluster.shutDown();
		}
	}

	private static Cluster createServerNode(int id) {
		return createServerNode(id, false, null);
	}

	private static Cluster createLeaderNode(int id) {
		return createServerNode(id, true, null);
	}

	private static Cluster createServerNode(int id, boolean leader, Integer... knownPeers) {
		ClusterConfig clusterConfig = new ClusterConfig().setClusterSecret("secret").setPort(9_000 + id).setNodeId("node-" + id).setHost("localhost");
		clusterConfig.setPeerNodes(knownPeers == null ? null : Arrays.stream(knownPeers).map(val -> new ClusterNodeData().setHost("localhost").setPort(9_000 + val)).toList());
		clusterConfig.setLeaderNode(leader);
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
				TestMethodResult testMethodResult = new TestMethodResult().setAnswerPayload(answerPayload).setExecutingNode(cluster.getLocalNode().getNodeId());

				try {
					if (request.getTestFile() != null) {
						File file = request.getTestFile().copyToTempFile();
						testMethodResult.setResultFile(file);
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				return testMethodResult;
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
