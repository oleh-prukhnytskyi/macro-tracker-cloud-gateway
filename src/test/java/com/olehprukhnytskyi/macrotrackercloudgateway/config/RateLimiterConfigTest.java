package com.olehprukhnytskyi.macrotrackercloudgateway.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.olehprukhnytskyi.macrotrackercloudgateway.util.CustomHeaders;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RateLimiterConfigTest {
    private RateLimiterConfig rateLimiterConfig;

    @Mock
    private KeyResolver ipAddressResolver;
    @Mock
    private KeyResolver principalKeyResolver;

    @BeforeEach
    void setUp() {
        rateLimiterConfig = new RateLimiterConfig();
    }

    @Test
    void testIpAddressResolver_WithXForwardedForHeader() {
        // Given
        KeyResolver resolver = rateLimiterConfig.ipAddressResolver();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header(CustomHeaders.X_FORWARDED_FOR, "192.168.1.1, 10.0.0.1")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<String> result = resolver.resolve(exchange).cast(String.class);

        // Then
        StepVerifier.create(result)
                .expectNext("192.168.1.1")
                .verifyComplete();
    }

    @Test
    void testIpAddressResolver_WithXForwardedForHeaderSingleIp() {
        // Given
        KeyResolver resolver = rateLimiterConfig.ipAddressResolver();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header(CustomHeaders.X_FORWARDED_FOR, "192.168.1.1")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<String> result = resolver.resolve(exchange).cast(String.class);

        // Then
        StepVerifier.create(result)
                .expectNext("192.168.1.1")
                .verifyComplete();
    }

    @Test
    void testIpAddressResolver_WithoutXForwardedForHeader() throws Exception {
        // Given
        KeyResolver resolver = rateLimiterConfig.ipAddressResolver();
        InetSocketAddress remoteAddress = new InetSocketAddress(
                InetAddress.getByName("192.168.1.100"), 8080);
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .remoteAddress(remoteAddress)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<String> result = resolver.resolve(exchange).cast(String.class);

        // Then
        StepVerifier.create(result)
                .expectNext("192.168.1.100")
                .verifyComplete();
    }

    @Test
    void testIpAddressResolver_WithUnknownXForwardedFor() throws Exception {
        // Given
        KeyResolver resolver = rateLimiterConfig.ipAddressResolver();
        InetSocketAddress remoteAddress = new InetSocketAddress(
                InetAddress.getByName("192.168.1.100"), 8080);
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header(CustomHeaders.X_FORWARDED_FOR, "unknown")
                .remoteAddress(remoteAddress)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<String> result = resolver.resolve(exchange).cast(String.class);

        // Then
        StepVerifier.create(result)
                .expectNext("192.168.1.100")
                .verifyComplete();
    }

    @Test
    void testPrincipalKeyResolver_WithXUserIdHeader() {
        // Given
        KeyResolver resolver = rateLimiterConfig.principalKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header(CustomHeaders.X_USER_ID, "user123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<String> result = resolver.resolve(exchange).cast(String.class);

        // Then
        StepVerifier.create(result)
                .expectNext("user123")
                .verifyComplete();
    }

    @Test
    void testPrincipalKeyResolver_WithoutXUserIdHeader() {
        // Given
        KeyResolver resolver = rateLimiterConfig.principalKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<String> result = resolver.resolve(exchange);

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testPrincipalKeyResolver_WithEmptyXUserIdHeader() {
        // Given
        KeyResolver resolver = rateLimiterConfig.principalKeyResolver();
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header(CustomHeaders.X_USER_ID, "")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<String> result = resolver.resolve(exchange);

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void testHybridKeyResolver_WithPrincipal() throws Exception {
        // Given
        KeyResolver ipResolver = rateLimiterConfig.ipAddressResolver();
        KeyResolver principalResolver = rateLimiterConfig.principalKeyResolver();
        KeyResolver hybridResolver = rateLimiterConfig
                .hybridKeyResolver(ipResolver, principalResolver);

        // Створюємо запит з remoteAddress
        InetSocketAddress remoteAddress = new InetSocketAddress(
                InetAddress.getByName("192.168.1.100"), 8080);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .header("X-User-Id", "user123")
                .remoteAddress(remoteAddress) // Додаємо remoteAddress
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<String> result = hybridResolver.resolve(exchange).cast(String.class);

        // Then
        StepVerifier.create(result)
                .expectNext("user123")
                .verifyComplete();
    }

    @Test
    void testHybridKeyResolver_WithoutPrincipal() throws Exception {
        // Given
        KeyResolver ipResolver = rateLimiterConfig.ipAddressResolver();
        KeyResolver principalResolver = rateLimiterConfig.principalKeyResolver();
        KeyResolver hybridResolver = rateLimiterConfig
                .hybridKeyResolver(ipResolver, principalResolver);

        InetSocketAddress remoteAddress = new InetSocketAddress(
                InetAddress.getByName("192.168.1.100"), 8080);
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/test")
                .remoteAddress(remoteAddress)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<String> result = hybridResolver.resolve(exchange).cast(String.class);

        // Then
        StepVerifier.create(result)
                .expectNext("anonymous:192.168.1.100")
                .verifyComplete();
    }

    @Test
    void testAllBeansAreCreated() {
        // When
        KeyResolver ipResolver = rateLimiterConfig.ipAddressResolver();
        KeyResolver principalResolver = rateLimiterConfig.principalKeyResolver();
        KeyResolver hybridResolver = rateLimiterConfig
                .hybridKeyResolver(ipResolver, principalResolver);

        // Then
        assertNotNull(ipResolver);
        assertNotNull(principalResolver);
        assertNotNull(hybridResolver);
    }
}
