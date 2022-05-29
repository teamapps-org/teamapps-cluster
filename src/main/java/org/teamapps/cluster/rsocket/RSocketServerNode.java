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
