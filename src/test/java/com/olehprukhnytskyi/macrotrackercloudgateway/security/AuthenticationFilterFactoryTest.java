package com.olehprukhnytskyi.macrotrackercloudgateway.security;

import com.olehprukhnytskyi.macrotrackercloudgateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.Mockito.*;

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

    private AuthenticationFilterFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AuthenticationFilterFactory(jwtUtil);
    }

    @Test
    void apply_withMissingAuthorizationHeader_unauthorized() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(exchange.getResponse()).thenReturn(response);
        when(response.setStatusCode(HttpStatus.UNAUTHORIZED)).thenReturn(true);
        when(response.setComplete()).thenReturn(Mono.empty());

        GatewayFilter filter = factory.apply(new AuthenticationFilterFactory.Config());

        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).setComplete();
        verifyNoInteractions(jwtUtil, chain);
    }

    @Test
    void apply_withInvalidToken_unauthorized() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer invalidToken");
        when(jwtUtil.validateToken("invalidToken")).thenReturn(false);
        when(exchange.getResponse()).thenReturn(response);
        when(response.setStatusCode(HttpStatus.UNAUTHORIZED)).thenReturn(true);
        when(response.setComplete()).thenReturn(Mono.empty());

        GatewayFilter filter = factory.apply(new AuthenticationFilterFactory.Config());

        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).setComplete();
        verifyNoMoreInteractions(chain);
    }

    @Test
    void apply_withValidToken_ok() {
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer validToken");

        when(jwtUtil.validateToken("validToken")).thenReturn(true);
        when(jwtUtil.extractUserId("validToken")).thenReturn(123L);

        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(eq("X-User-Id"), eq("123"))).thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(request)).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        GatewayFilter filter = factory.apply(new AuthenticationFilterFactory.Config());

        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

        verify(chain).filter(exchange);
        verifyNoInteractions(response);
    }
}
