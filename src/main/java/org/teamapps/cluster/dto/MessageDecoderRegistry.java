package org.teamapps.cluster.dto;

public interface MessageDecoderRegistry {

	MessageDecoder<? extends Message> getMessageDecoder(int id);

	boolean containsDecoder(int id);
}
