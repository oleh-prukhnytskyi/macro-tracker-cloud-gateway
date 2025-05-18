package com.olehprukhnytskyi.macrotrackercloudgateway.filter;

import com.olehprukhnytskyi.macrotrackercloudgateway.util.CustomHeaders;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class IdempotencyFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (request.getMethod() == HttpMethod.POST) {
            return exchange.getRequest().getBody()
                    .collectList()
                    .flatMap(dataBuffers -> {
                        StringBuilder bodyBuilder = new StringBuilder();
                        dataBuffers.forEach(buffer -> {
                            byte[] bytes = new byte[buffer.readableByteCount()];
                            buffer.read(bytes);
                            DataBufferUtils.release(buffer);
                            bodyBuilder.append(new String(bytes, StandardCharsets.UTF_8));
                        });

                        String body = bodyBuilder.toString();
                        String userId = request.getHeaders().getFirst(CustomHeaders.X_USER_ID);
                        String path = request.getURI().getPath();

                        String raw = userId + "|" + path + "|" + body;
                        String hash = DigestUtils.sha256Hex(raw);

                        ServerHttpRequest mutated = request.mutate()
                                .header(CustomHeaders.X_REQUEST_ID, hash)
                                .build();
                        return chain.filter(exchange.mutate().request(mutated).build());
                    });
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
