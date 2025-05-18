package com.olehprukhnytskyi.macrotrackercloudgateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.olehprukhnytskyi.macrotrackercloudgateway.util.CustomHeaders;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class IdempotencyFilterTest {
    private IdempotencyFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new IdempotencyFilter();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("When non-POST request, should pass through without adding header")
    void filter_whenNonPost_shouldNotAddHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor
                .forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        ServerWebExchange capturedExchange = captor.getValue();

        assertNull(capturedExchange.getRequest().getHeaders()
                .getFirst(CustomHeaders.X_REQUEST_ID));
    }

    @Test
    @DisplayName("When POST request with body and userId, should add X-Request-Id header")
    void filter_whenPostRequest_shouldAddRequestIdHeader() {
        // Given
        String body = "{\"some\":\"data\"}";
        String userId = "123";
        String path = "/api/test";

        MockServerHttpRequest request = MockServerHttpRequest.post(path)
                .header(CustomHeaders.X_USER_ID, userId)
                .body(body);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor
                .forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        ServerHttpRequest mutatedRequest = captor.getValue().getRequest();
        String expectedHash = DigestUtils.sha256Hex(userId + "|" + path + "|" + body);
        assertEquals(expectedHash, mutatedRequest.getHeaders()
                .getFirst(CustomHeaders.X_REQUEST_ID));
    }

    @Test
    @DisplayName("When POST request without body, should add X-Request-Id based on empty body")
    void filter_whenPostWithoutBody_shouldAddRequestIdHeaderWithEmptyBody() {
        // Given
        String userId = "123";
        String path = "/api/empty";

        MockServerHttpRequest request = MockServerHttpRequest.post(path)
                .header(CustomHeaders.X_USER_ID, userId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor
                .forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        ServerHttpRequest mutatedRequest = captor.getValue().getRequest();
        String expectedHash = DigestUtils.sha256Hex(userId + "|" + path + "|");
        assertEquals(expectedHash, mutatedRequest.getHeaders()
                .getFirst(CustomHeaders.X_REQUEST_ID));
    }

    @Test
    @DisplayName("When X-User-Id header is missing,"
            + " should still add X-Request-Id based on null userId")
    void filter_whenUserIdMissing_shouldHashWithNullUserId() {
        // Given
        String body = "{\"some\":\"data\"}";
        String path = "/api/test";

        MockServerHttpRequest request = MockServerHttpRequest.post(path)
                .body(body);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result).verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor
                .forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        ServerHttpRequest mutatedRequest = captor.getValue().getRequest();
        String expectedHash = DigestUtils.sha256Hex("null" + "|" + path + "|" + body);
        assertEquals(expectedHash, mutatedRequest.getHeaders()
                .getFirst(CustomHeaders.X_REQUEST_ID));
    }

    @Test
    @DisplayName("Filter should have highest priority (Ordered negative)")
    void getOrder_shouldBeNegativeOne() {
        // When
        int order = filter.getOrder();

        // Then
        assertEquals(-1, order);
    }
}
