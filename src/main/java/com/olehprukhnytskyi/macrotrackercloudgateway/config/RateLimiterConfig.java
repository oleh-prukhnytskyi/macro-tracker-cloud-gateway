package com.olehprukhnytskyi.macrotrackercloudgateway.config;

import com.olehprukhnytskyi.macrotrackercloudgateway.util.CustomHeaders;
import java.util.Objects;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {
    @Bean
    public KeyResolver ipAddressResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getHeaders().getFirst(CustomHeaders.X_FORWARDED_FOR);
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                        .getAddress()
                        .getHostAddress();
            }
            if (ip.contains(",")) {
                ip = ip.substring(0, ip.indexOf(',')).trim();
            }
            return Mono.just(ip);
        };
    }

    @Bean
    public KeyResolver principalKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(CustomHeaders.X_USER_ID);
            if (userId != null && !userId.isEmpty()) {
                return Mono.just(userId);
            }
            return Mono.empty();
        };
    }

    @Bean
    @Primary
    public KeyResolver hybridKeyResolver(KeyResolver ipAddressResolver,
                                         KeyResolver principalKeyResolver) {
        return exchange -> principalKeyResolver.resolve(exchange)
                .flatMap(Mono::just)
                .switchIfEmpty(ipAddressResolver.resolve(exchange).map(ip -> "anonymous:" + ip));
    }
}
