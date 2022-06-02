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
package org.teamapps.cluster.rsocket;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.publisher.Mono;

public class RSocketServerNode {

	private int port = 9_000;

	private int counter;
	private long time = System.currentTimeMillis();


	public void start() {
		RSocket rSocket = new RSocket() {
			@Override
			public Mono<Payload> requestResponse(Payload payload) {
				String dataUtf8 = payload.getDataUtf8();
				counter++;
				if (counter % 100_000 == 0) {
					System.out.println("Count:" + counter + ", time: " + (System.currentTimeMillis() - time));
					time = System.currentTimeMillis();
				}
				return Mono.empty();
			}
		};

		RSocketServer
				.create(SocketAcceptor.with(rSocket))
				.bind(TcpServerTransport.create(port))
				.block();
	}

	public static void main(String[] args) throws InterruptedException {
		RSocketServerNode serverNode = new RSocketServerNode();
		serverNode.start();
		Thread.sleep(60 * 60_000);
	}
}
