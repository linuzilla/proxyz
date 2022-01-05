package ncu.cc.proxyz.services;

import ncu.cc.proxyz.helpers.CookieHelper;
import ncu.cc.proxyz.properties.ProxyingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class WebGatewayServiceImpl implements WebGatewayService {
    private static final Logger logger = LoggerFactory.getLogger(WebGatewayServiceImpl.class);

    private final WebClient webClient;
    private final ProxyingProperties proxyingProperties;
    private final UserIdConverterService userIdConverterService;

    public WebGatewayServiceImpl(ProxyingProperties proxyingProperties, UserIdConverterService userIdConverterService) {
        this.proxyingProperties = proxyingProperties;
        this.userIdConverterService = userIdConverterService;
        this.webClient = WebClient.builder()
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)).build())
                .build();
    }

    private String manipulatePage(DataBuffer dataBuffer) {
        final var pageContext = new AtomicReference<>(dataBuffer.toString(StandardCharsets.UTF_8));

        logger.trace(">>> Data: {}", pageContext.get().substring(0, Math.min(pageContext.get().length(), 20)));
        return pageContext.get();
    }

    private URI shibbolethUri(ServerRequest serverRequest) {
        return UriComponentsBuilder.newInstance()
                .uri(serverRequest.uri())
                .host(proxyingProperties.getHost())
                .port(proxyingProperties.getPort())
                .scheme(proxyingProperties.getScheme())
                .build(true).toUri();
    }

    @Override
    public Mono<ServerResponse> proxying(ServerRequest serverRequest) {
        logger.trace("method: {}", serverRequest.method());

        return serverRequest.session()
                .flatMap(webSession -> serverRequest.principal()
                        .flatMap(principal -> {
                            final var mappedUserId = userIdConverterService.convert(principal);

                            if (!StringUtils.hasLength(mappedUserId)) {
                                logger.warn("mapping failed for {}", principal.getName());
                                return webSession.invalidate()
                                        .flatMap(v -> ServerResponse.status(HttpStatus.FOUND)
                                                .header(HttpHeaders.LOCATION, proxyingProperties.getLocationOnFail())
                                                .build());
                            } else {
                                logger.info("mapping user: {} to {}", principal.getName(), mappedUserId);
                            }

                            final var cookieStrings = new ArrayList<List<String>>();

                            final var requestBodySpec = this.webClient
                                    .method(Objects.requireNonNullElse(serverRequest.method(), HttpMethod.GET))
                                    .uri(shibbolethUri(serverRequest))
                                    .headers(httpHeaders -> sendingHeaderManipulate(serverRequest, httpHeaders, mappedUserId));

                            final var cookieString = CookieHelper.filterCookie(cookieStrings, strings -> CookieHelper.cookieNameFilter(strings[0]));

                            if (StringUtils.hasText(cookieString)) {
                                requestBodySpec.header(HttpHeaders.COOKIE, cookieString);
                            }

                            return serverRequest.headers().contentType()
                                    .map(mediaType -> serverRequest.bodyToMono(DataBuffer.class)
                                            .flatMap(dataBuffer -> {
                                                final var contentLength = serverRequest.headers().contentLength().orElse(0);
                                                requestBodySpec.contentLength(contentLength);
                                                requestBodySpec.contentType(mediaType);
                                                return requestBodySpec.body(BodyInserters.fromDataBuffers(Mono.just(dataBuffer)))
                                                        .exchangeToMono(clientResponse -> sendProxyRequest(serverRequest, clientResponse, principal));
                                            })
                                    )
                                    .orElseGet(() -> requestBodySpec.exchangeToMono(clientResponse -> sendProxyRequest(serverRequest, clientResponse, principal)));
                        }));
    }

    private void sendingHeaderManipulate(ServerRequest request, HttpHeaders httpHeaders, String mappedUserId) {
        request.headers().asHttpHeaders()
                .forEach((headerName, headerValues) -> {
                    if (!proxyingProperties.getHeaderName().equalsIgnoreCase(headerName)) {
                        httpHeaders.put(headerName, headerValues);
                    }
                });
        if (StringUtils.hasLength(mappedUserId)) {
            httpHeaders.put(proxyingProperties.getHeaderName(), List.of(mappedUserId));
        }
    }

    private void receivedHeaderManipulate(ClientResponse clientResponse, HttpHeaders httpHeaders) {
        clientResponse.headers()
                .asHttpHeaders()
                .forEach((headerName, headerValues) -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug(">>> Header: {}={}", headerName, headerValues.get(0));
                    }

                    httpHeaders.put(headerName, headerValues);
                });
    }

    private Mono<ServerResponse> sendProxyRequest(ServerRequest request, ClientResponse clientResponse, Principal principal) {
        final var httpStatus = clientResponse.statusCode();

        if (logger.isDebugEnabled()) {
            logger.debug(">>> Status: {}, {} [{}] {}",
                    httpStatus, request.method(),
                    request.headers().contentType().map(MimeType::toString).orElse("-"),
                    request.uri().toString());
        }

        final var builder = ServerResponse.status(httpStatus)
                .headers(httpHeaders -> {
                    receivedHeaderManipulate(clientResponse, httpHeaders);
                });

        if (httpStatus.is2xxSuccessful()) {
            AtomicBoolean gotDataBuffer = new AtomicBoolean(false);

            return clientResponse.bodyToMono(DataBuffer.class)
                    .doOnNext(dataBuffer -> gotDataBuffer.set(true))
                    .flatMap(dataBuffer -> clientResponse.headers().contentType()
                            .map(mediaType -> {
                                logger.debug(">>> [{}] Content-Type: {}", request.uri().toString(), mediaType.toString());
                                return mediaType;
                            })
                            .filter(mediaType -> mediaType.equalsTypeAndSubtype(MediaType.TEXT_HTML))
                            .map(mediaType -> builder.body(BodyInserters.fromPublisher(
                                    Mono.just(manipulatePage(dataBuffer)), String.class)))
                            .orElseGet(() -> builder.body(BodyInserters.fromDataBuffers(Mono.just(dataBuffer)))))
                    .doFinally(signalType -> {
                        if (!gotDataBuffer.get()) {
                            logger.warn("No dataBuffer get {}", httpStatus);
                        }
                    });
        } else {
            logger.debug("Status: {}", httpStatus);
        }

        return builder.bodyValue("");
    }
}
