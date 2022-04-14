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
package org.teamapps.cluster.service;

import org.junit.Test;
import org.teamapps.cluster.network.NodeAddress;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

public class TeamAppsClusterTest {


	@Test
	public void testCluster() throws Exception {
		AtomicBoolean connectedA = new AtomicBoolean(false);
		AtomicBoolean connectedB = new AtomicBoolean(false);
		TeamAppsCluster clusterA = new TeamAppsCluster("secret", "clusterA", nodeAddress -> connectedA.set(true), 10_120);
		TeamAppsCluster clusterB = new TeamAppsCluster("secret", "clusterB", nodeAddress -> connectedB.set(true), 10_121, new NodeAddress("localhost", 10_120));

		List<Integer> listA = new ArrayList<>();
		ClusterTopic topicA = clusterA.createTopic("test-topic", clusterTopicMessage -> listA.add(new BigInteger(clusterTopicMessage.getData()).intValue()));

		List<Integer> listB = new ArrayList<>();
		ClusterTopic topicB = clusterB.createTopic("test-topic", clusterTopicMessage -> listB.add(new BigInteger(clusterTopicMessage.getData()).intValue()));

		long time = System.currentTimeMillis();
		while (!connectedA.get() || !connectedB.get()) {
			Thread.sleep(5);
		}
		System.out.println("TIME:" + (System.currentTimeMillis() - time));

		for (int i = 0; i < 1_000; i++) {
			topicA.sendMessageAsync(BigInteger.valueOf(i).toByteArray());
			topicB.sendMessageAsync(BigInteger.valueOf(i).toByteArray());
		}

		Thread.sleep(1_000);

		assertEquals(1_000, listA.size());
		assertEquals(1_000, listB.size());

		clusterA.shutDown();
		clusterB.shutDown();
	}

}
