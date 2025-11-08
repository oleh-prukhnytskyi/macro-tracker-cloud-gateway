package com.olehprukhnytskyi.macrotrackercloudgateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.olehprukhnytskyi.macrotrackercloudgateway.util.CustomHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TraceIdFilterTest {
    @InjectMocks
    private TraceIdFilter traceIdFilter;

    @Mock
    private GatewayFilterChain chain;
    @Mock
    private ServerWebExchange exchange;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private ServerHttpResponse response;
    @Mock
    private ServerHttpRequest.Builder requestBuilder;

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @Test
    @DisplayName("When header is missing, should generate new traceId")
    void shouldGenerateTraceIdWhenMissing() {
        // Given
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(eq(CustomHeaders.X_TRACE_ID), anyString()))
                .thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);
        when(exchange.mutate()).thenReturn(mock(ServerWebExchange.Builder.class, RETURNS_SELF));
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        StepVerifier.create(traceIdFilter.filter(exchange, chain))
                .expectComplete()
                .verify();

        // Then
        verify(requestBuilder).header(eq(CustomHeaders.X_TRACE_ID), anyString());
        verify(chain).filter(any());
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("Should use existing traceId if present in header")
    void shouldUseExistingTraceId() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add(CustomHeaders.X_TRACE_ID, "existing-trace-id");

        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(CustomHeaders.X_TRACE_ID, "existing-trace-id"))
                .thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);
        when(exchange.mutate()).thenReturn(mock(ServerWebExchange.Builder.class, RETURNS_SELF));
        when(chain.filter(any())).thenReturn(Mono.empty());

        // When
        StepVerifier.create(traceIdFilter.filter(exchange, chain))
                .expectComplete()
                .verify();

        // Then
        verify(requestBuilder).header(CustomHeaders.X_TRACE_ID, "existing-trace-id");
        verify(chain).filter(any());
        assertThat(MDC.get("traceId")).isNull();
    }
}
