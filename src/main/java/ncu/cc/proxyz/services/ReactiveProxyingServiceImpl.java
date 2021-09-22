package ncu.cc.proxyz.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Service
public class ReactiveProxyingServiceImpl implements ReactiveProxyingService {
    private static final Logger logger = LoggerFactory.getLogger(ReactiveProxyingServiceImpl.class);

    private final WebGatewayService webGatewayService;

    public ReactiveProxyingServiceImpl(WebGatewayService webGatewayService) {
        this.webGatewayService = webGatewayService;
    }

    @Override
    public Mono<ServerResponse> proxying(ServerRequest serverRequest) {
        return webGatewayService.proxying(serverRequest);
    }
}
