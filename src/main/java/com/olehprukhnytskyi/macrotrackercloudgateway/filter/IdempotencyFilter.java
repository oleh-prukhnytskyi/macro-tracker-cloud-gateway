package com.olehprukhnytskyi.macrotrackercloudgateway.filter;

import com.olehprukhnytskyi.macrotrackercloudgateway.util.CustomHeaders;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class IdempotencyFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (request.getMethod() == HttpMethod.POST) {
            return DataBufferUtils.join(request.getBody())
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);

                        String body = new String(bytes, StandardCharsets.UTF_8);
                        String userId = request.getHeaders().getFirst(CustomHeaders.X_USER_ID);
                        String path = request.getURI().getPath();

                        String raw = (userId == null ? "" : userId) + "|" + path + "|" + body;
                        String hash = DigestUtils.sha256Hex(raw);

                        ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(request) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                return Flux.defer(() -> {
                                    DataBuffer buffer = exchange.getResponse()
                                            .bufferFactory().wrap(bytes);
                                    return Mono.just(buffer);
                                });
                            }
                        };

                        mutatedRequest = mutatedRequest.mutate()
                                .header(CustomHeaders.X_REQUEST_ID, hash)
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    });
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
