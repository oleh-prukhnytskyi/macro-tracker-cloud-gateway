package com.olehprukhnytskyi.macrotrackercloudgateway.filter;

import com.olehprukhnytskyi.macrotrackercloudgateway.util.CustomHeaders;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String traceId = Optional.ofNullable(exchange.getRequest()
                        .getHeaders().getFirst(CustomHeaders.X_TRACE_ID))
                .orElse("no-trace-id");
        String method = Optional.of(exchange.getRequest().getMethod())
                .map(HttpMethod::name)
                .orElse("UNKNOWN");
        String path = exchange.getRequest().getURI().getPath();
        log.info("request={} method={} path={} traceId={}",
                method, path, exchange.getRequest().getQueryParams(), traceId);
        return chain.filter(exchange)
                .doFinally(signal -> {
                    int status = Optional.ofNullable(exchange.getResponse().getStatusCode())
                            .map(HttpStatusCode::value)
                            .orElse(0);
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("response={} method={} path={} status={} durationMs={} traceId={}",
                            signal, method, path, status, duration, traceId);
                });
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
