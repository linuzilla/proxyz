package ncu.cc.proxyz.services;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface ReactiveProxyingService {
    Mono<ServerResponse> proxying(ServerRequest serverRequest);
}
