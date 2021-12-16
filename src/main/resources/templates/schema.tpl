package {package};

import org.teamapps.cluster.dto.*;
import java.util.HashMap;
import java.util.Map;

public class {type} implements MessageDecoderRegistry {

    public static MessageSchema SCHEMA = new MessageSchema({id}, "{name}");
    public static MessageDecoderRegistry REGISTRY = new {type}();
	private final static Map<Integer, MessageDecoder<? extends Message>> DECODERS = new HashMap<>();

    static {
{data}
{registry}
    }

	public MessageDecoder<? extends Message> getMessageDecoder(int id) {
		return DECODERS.get(id);
	}

	@Override
	public boolean containsDecoder(int id) {
		return DECODERS.containsKey(id);
	}

}