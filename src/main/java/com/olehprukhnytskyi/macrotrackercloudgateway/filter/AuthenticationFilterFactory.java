package com.olehprukhnytskyi.macrotrackercloudgateway.filter;

import com.olehprukhnytskyi.macrotrackercloudgateway.util.CustomHeaders;
import com.olehprukhnytskyi.macrotrackercloudgateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Lazy
@Component
@RequiredArgsConstructor
public class AuthenticationFilterFactory implements
        GatewayFilterFactory<AuthenticationFilterFactory.Config> {
    private final JwtUtil jwtUtil;

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (request.getMethod() == HttpMethod.OPTIONS) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing or invalid Authorization Header",
                        HttpStatus.UNAUTHORIZED);
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return onError(exchange, "Invalid Token", HttpStatus.UNAUTHORIZED);
            }

            Long userId = jwtUtil.extractUserId(token);
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(CustomHeaders.X_USER_ID, userId.toString())
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String errorMessage,
                               HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                "GET, POST, PUT, DELETE, OPTIONS");
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                "Content-Type, Authorization");
        if (status == HttpStatus.UNAUTHORIZED) {
            response.getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return response.setComplete();
    }

    @Override
    public Class<Config> getConfigClass() {
        return Config.class;
    }

    public static class Config {
    }
}
