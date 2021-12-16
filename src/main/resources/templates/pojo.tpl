package {package};

import org.teamapps.cluster.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;


public class {type} extends Message {
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public final static Function<byte[], {type}> DECODER_FUNCTION = bytes -> {
		try {
			return new {type}(bytes);
		} catch (IOException e) {
			LOGGER.error("Error creating {type} instance", e);
		}
		return null;
	};

	private final static MessageDecoder<{type}> decoder = (dis, fileProvider) -> {
		try {
			return new {type}(dis, fileProvider);
		} catch (IOException e) {
			LOGGER.error("Error creating {type} instance", e);
		}
		return null;
	};

	public static MessageDecoder<{type}> getMessageDecoder() {
		return decoder;
	}

	public {type}() {
		super({schema}.SCHEMA.getFieldById({fieldId}), new ArrayList<>());
	}

	public {type}(ByteBuffer buf) {
		super(buf, {schema}.SCHEMA);
	}

	public {type}(DataInputStream dis) throws IOException {
		super(dis, {schema}.SCHEMA);
	}

	public {type}(DataInputStream dis, FileProvider fileProvider) throws IOException {
		super(dis, {schema}.SCHEMA, fileProvider, {schema}.REGISTRY);
	}

	public {type}(byte[] bytes) throws IOException {
		super(bytes, {schema}.SCHEMA);
	}

	public {type}(byte[] bytes, FileProvider fileProvider) throws IOException {
		super(bytes, {schema}.SCHEMA, fileProvider, {schema}.REGISTRY);
	}

{methods}

}