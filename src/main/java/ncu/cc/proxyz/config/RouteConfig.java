package ncu.cc.proxyz.config;

import ncu.cc.proxyz.constants.Routes;
import ncu.cc.proxyz.services.ReactiveProxyingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

@Configuration
public class RouteConfig {
    private final ReactiveProxyingService reactiveProxyingService;

    public RouteConfig(ReactiveProxyingService reactiveProxyingService) {
        this.reactiveProxyingService = reactiveProxyingService;
    }

    @Bean
    RouterFunction<?> routes() {
        return RouterFunctions.route(RequestPredicates.path(Routes.IDP + "/**"), reactiveProxyingService::proxying);
    }

}
