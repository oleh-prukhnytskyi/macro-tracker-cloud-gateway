package com.olehprukhnytskyi.macrotrackercloudgateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsGlobalFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        if (request.getMethod() == HttpMethod.OPTIONS) {
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                    "GET, POST, PUT, DELETE, OPTIONS");
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    "Content-Type, Authorization, X-Requested-With");
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
            response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.setStatusCode(HttpStatus.OK);
            return response.setComplete();
        }

        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                "GET, POST, PUT, DELETE, OPTIONS");
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                "Content-Type, Authorization, X-Requested-With");
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                "Content-Disposition");
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

        return chain.filter(exchange);
    }
}
