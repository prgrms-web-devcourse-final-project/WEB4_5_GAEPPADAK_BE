package site.kkokkio.infra.naver.news;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import site.kkokkio.infra.common.exception.RetryableExternalApiException;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "mock.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NaverNewsApiAdapterTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ExchangeFunction exchangeFunction() {
            return Mockito.mock(ExchangeFunction.class);
        }

        @Bean
        @Qualifier("naverWebClient")
        WebClient naverWebClient(ExchangeFunction ef) {
            return WebClient.builder()
                            .exchangeFunction(ef)
                            .build();
        }
    }

    @Autowired
    private NaverNewsApiAdapter adapter;

    @Autowired
    private ExchangeFunction exchangeFunction;

    @Autowired
    private CircuitBreakerRegistry cbRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private RateLimiterRegistry rlRegistry;

    private ClientResponse buildResponse(String json, HttpStatus status) {
        return ClientResponse.create(status, ExchangeStrategies.withDefaults())
                             .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                             .body(json)
                             .build();
    }

    @Test
    @DisplayName("Naver News 검색 API - 성공")
    void fetchNews_success() {
        // given
        String successJson = """
            {
              "items":[
                {
                  "title":"t",
                  "link":"l",
                  "originallink":"o",
                  "description":"d",
                  "pubDate":"Wed, 24 Jul 2024 14:34:00 +0900"
                }
              ]
            }
            """;
        when(exchangeFunction.exchange(any()))
            .thenReturn(Mono.just(buildResponse(successJson, HttpStatus.OK)));

        // when
        var result = adapter.fetchNews("키워드", 10, 1, "sim");

        // then
        StepVerifier.create(result)
                    .assertNext(list -> {
                        assertThat(list).hasSize(1);
                        assertThat(list.getFirst().title()).isEqualTo("t");
                    })
                    .verifyComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Naver News 검색 API - retry 확인")
    void fetchNews_retry() {
        // given
        String errorJson = "{\"errorCode\":\"E\",\"errorMessage\":\"err\"}";
        String successJson = """
            {
              "items":[
                {
                  "title":"s",
                  "link":"l",
                  "originallink":"o",
                  "description":"d",
                  "pubDate":"Wed, 24 Jul 2024 14:34:00 +0900"
                }
              ]
            }
            """;
        when(exchangeFunction.exchange(any()))
            .thenReturn(
                Mono.just(buildResponse(errorJson,   HttpStatus.SERVICE_UNAVAILABLE)),
                Mono.just(buildResponse(successJson, HttpStatus.OK))
            );

        // when
        var result = adapter.fetchNews("키워드", 10, 1, "sim");

        // then
        StepVerifier.create(result)
                    .assertNext(list -> {
                        assertThat(list).hasSize(1);
                        assertThat(list.getFirst().title()).isEqualTo("s");
                    })
                    .verifyComplete();

        // 2회 호출되었는지 검증
        verify(exchangeFunction, times(2)).exchange(any());
    }

    @Test
    @DisplayName("Naver News 검색 API - circuitBreaker 확인")
    void fetchNews_circuitBreaker() {
        // given
        String errorJson = "{\"errorCode\":\"E\",\"errorMessage\":\"err\"}";
        when(exchangeFunction.exchange(any()))
            .thenReturn(Mono.just(buildResponse(errorJson, HttpStatus.SERVICE_UNAVAILABLE)));

        // when: 첫 호출 (2회 retry 후 실패 -> RetryableExternalApiException)
        StepVerifier.create(adapter.fetchNews("k", 10, 1, "sim"))
                    .expectError(RetryableExternalApiException.class)
                    .verify();

        // then: 회로 열린 상태 확인
        CircuitBreaker cb = cbRegistry.circuitBreaker("NAVER_NEWS_CIRCUIT_BREAKER");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // when: 두 번째 호출 → CallNotPermittedException
        StepVerifier.create(adapter.fetchNews("k", 10, 1, "sim"))
                    .expectError(CallNotPermittedException.class)
                    .verify();

        // then: 호출 2회만 발생
        verify(exchangeFunction, times(2)).exchange(any());
    }

    @Test
    @DisplayName("Naver News 검색 API - rateLimiter 확인")
    void fetchNews_rateLimiter() {
        // given
        String emptyJson = "{\"items\":[]}";
        when(exchangeFunction.exchange(any()))
            .thenReturn(Mono.just(buildResponse(emptyJson, HttpStatus.OK)));

        // when & then: 두 번 정상 호출
        StepVerifier.create(adapter.fetchNews("k",10,1,"sim"))
                    .expectNext(Collections.emptyList())
                    .verifyComplete();
        StepVerifier.create(adapter.fetchNews("k",10,1,"sim"))
                    .expectNext(Collections.emptyList())
                    .verifyComplete();

        // when & then: 세 번째 호출 → RateLimiter 초과
        StepVerifier.create(adapter.fetchNews("k",10,1,"sim"))
                    .expectError(io.github.resilience4j.ratelimiter.RequestNotPermitted.class)
                    .verify();
    }
}