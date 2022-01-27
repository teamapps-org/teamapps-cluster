package {package};

import org.teamapps.cluster.dto.*;
import org.teamapps.cluster.service.*;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class {type} extends AbstractClusterServiceClient {

    public {type}(ServiceRegistry registry) {
        super(registry, "{serviceName}");
    }

{methods}

}