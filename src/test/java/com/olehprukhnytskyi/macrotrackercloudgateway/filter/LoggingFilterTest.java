package com.olehprukhnytskyi.macrotrackercloudgateway.filter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.olehprukhnytskyi.macrotrackercloudgateway.util.CustomHeaders;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LoggingFilterTest {
    @Mock
    private ServerWebExchange exchange;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private ServerHttpResponse response;
    @Mock
    private GatewayFilterChain chain;

    private LoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoggingFilter();
    }

    @Test
    @DisplayName("Should log request and response and call chain")
    void shouldLogRequestAndResponseAndCallChain() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add(CustomHeaders.X_TRACE_ID, "test-trace-id");

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("/api/test"));
        when(request.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

        // Then
        verify(chain).filter(exchange);
        verify(request).getMethod();
        verify(request).getURI();
        verify(request).getHeaders();
        verify(response).getStatusCode();
    }

    @Test
    @DisplayName("When header is missing, should use default traceId")
    void whenHeaderMissing_shouldUseDefaultTraceId() {
        // Given
        HttpHeaders headers = new HttpHeaders();

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getURI()).thenReturn(URI.create("/api/submit"));
        when(request.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());
        when(response.getStatusCode()).thenReturn(HttpStatus.CREATED);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

        // Then
        verify(chain).filter(exchange);
        verify(request).getHeaders();
    }
}
