package com.olehprukhnytskyi.macrotrackercloudgateway.filter;

import com.olehprukhnytskyi.macrotrackercloudgateway.util.CustomHeaders;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceIdFilter implements GlobalFilter, Ordered {
    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(CustomHeaders.X_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID_KEY, traceId);
        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header(CustomHeaders.X_TRACE_ID, traceId)
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> MDC.remove(TRACE_ID_KEY));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
