package {package};

import org.teamapps.cluster.dto.*;
import org.teamapps.cluster.service.*;
import java.io.IOException;

public abstract class {type} extends AbstractClusterService {

    public {type}(ServiceRegistry registry) {
        super(registry, "{serviceName}");
    }

{methods}

	@Override
	public byte[] handleMessage(String method, byte[] bytes, FileProvider fileProvider, FileSink fileSink) throws IOException {
		switch (method) {
{cases}
		}
		return null;
	}

}