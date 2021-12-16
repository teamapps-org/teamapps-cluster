package org.teamapps.cluster.dto;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public interface MessageDecoder<MESSAGE extends Message> {

	MESSAGE decode(DataInputStream dis, FileProvider fileProvider);

	default MESSAGE decode(byte[] bytes, FileProvider fileProvider) {
		return decode(new DataInputStream(new ByteArrayInputStream(bytes)), fileProvider);
	}

}
