package com.olehprukhnytskyi.macrotrackercloudgateway.filter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.olehprukhnytskyi.macrotrackercloudgateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterFactoryTest {
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private ServerWebExchange exchange;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private HttpHeaders headers;
    @Mock
    private ServerHttpResponse response;
    @Mock
    private ServerHttpRequest.Builder requestBuilder;
    @Mock
    private GatewayFilterChain chain;
    @Mock
    private HttpHeaders responseHeaders;

    private AuthenticationFilterFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AuthenticationFilterFactory(jwtUtil);
    }

    @Test
    @DisplayName("When authorization header is missing, should return Unauthorized")
    void apply_whenMissingAuthorizationHeader_shouldReturnUnauthorized() {
        // Given
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(responseHeaders);
        when(response.setStatusCode(HttpStatus.UNAUTHORIZED)).thenReturn(true);
        when(response.setComplete()).thenReturn(Mono.empty());

        // When
        GatewayFilter filter = factory.apply(new AuthenticationFilterFactory.Config());
        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

        // Then
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).setComplete();
        verifyNoInteractions(jwtUtil, chain);
    }

    @Test
    @DisplayName("When token is invalid, should return Unauthorized")
    void apply_whenInvalidToken_shouldReturnUnauthorized() {
        // Given
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer invalidToken");
        when(jwtUtil.validateToken("invalidToken")).thenReturn(false);
        when(exchange.getResponse()).thenReturn(response);
        when(response.getHeaders()).thenReturn(responseHeaders);
        when(response.setStatusCode(HttpStatus.UNAUTHORIZED)).thenReturn(true);
        when(response.setComplete()).thenReturn(Mono.empty());

        // When
        GatewayFilter filter = factory.apply(new AuthenticationFilterFactory.Config());
        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

        // Then
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).setComplete();
        verifyNoMoreInteractions(chain);
    }

    @Test
    @DisplayName("When token is valid, should return OK")
    void apply_whenValidToken_shouldReturnOk() {
        // Given
        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);

        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer validToken");
        when(jwtUtil.validateToken("validToken")).thenReturn(true);
        when(jwtUtil.extractUserId("validToken")).thenReturn(123L);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), anyString()))
                .thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(request)).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        GatewayFilter filter = factory.apply(new AuthenticationFilterFactory.Config());
        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

        // Then
        verify(chain).filter(exchange);
        verifyNoInteractions(response);
    }
}
