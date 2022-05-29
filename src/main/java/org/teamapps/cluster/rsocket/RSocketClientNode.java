package org.teamapps.cluster.rsocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;

public class RSocketClientNode {

	private final Mono<RSocket> rSocketMono;
	private RetryBackoffSpec retrySpec;
	private String host;
	private int port;

	public RSocketClientNode(String host, int port) {
		this.host = host;
		this.port = port;
		retrySpec = Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(1));
		rSocketMono = RSocketConnector
				.create()
				.reconnect(retrySpec)
				.connect(TcpClientTransport.create(host, port));
	}

	public void sendMessage(String msg) {
		Payload payload = rSocketMono
				.flatMap(rSocket -> rSocket.requestResponse(DefaultPayload.create(msg)))
				.retryWhen(retrySpec)
				.block();
		payload.release();
	}


	public static void main(String[] args) throws InterruptedException {
		RSocketClientNode clientNode = new RSocketClientNode("localhost", 9000);
		int size = 10_000_000;
		for (int i = 0; i < size; i++) {
			clientNode.sendMessage("Message " + i);
		}
	}
}
