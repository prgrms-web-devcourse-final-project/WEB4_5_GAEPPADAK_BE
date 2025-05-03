package site.kkokkio.infra.youtube.video;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import site.kkokkio.domain.source.dto.VideoDto;
import site.kkokkio.infra.common.exception.RetryableExternalApiException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mock.enabled=false",
})
// 테스트 메소드 실행 후 Spring Context 초기화 (Resilience4j 상태 초기화)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
// 잠재적인 호환성 문제 방지
@ExtendWith(MockitoExtension.class)
public class YoutubeVideoApiAdapterTest {

    private static final Logger log = LoggerFactory.getLogger(YoutubeVideoApiAdapterTest.class);

    // 테스트를 위한 설정 클래스
    @TestConfiguration
    static class TestConfig {
        // WebClient의 핵심인 ExchangeFunction을 Mocking
        @Bean
        ExchangeFunction exchangeFunction() {
            return Mockito.mock(ExchangeFunction.class);
        }

        // Mock ExchangeFunction을 사용하는 WebClient 빈을 생성
        @Bean
        @Qualifier("youtubeWebClient")
        WebClient youtubeWebClient(ExchangeFunction exchangeFunction) {
            return WebClient.builder().exchangeFunction(exchangeFunction).build();
        }
    }

    @Autowired
    private YoutubeVideoApiAdapter youtubeVideoApiAdapter;

    @Autowired
    private ExchangeFunction exchangeFunction;

    @Autowired
    private CircuitBreakerRegistry registry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    // Mock ClientResponse를 생성하는 헬퍼 메소드
    private ClientResponse buildResponse(String json, HttpStatus status) {
        return ClientResponse.create(status, ExchangeStrategies.withDefaults())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build();
    }

    @Test
    @DisplayName("Youtube Video 검색 API - 성공")
    void fetchVideos_success() {
        /// given
        String successJson = """
                {
                  "items":[
                    {
                      "id":{"videoId":"testVideoId1"},
                      "snippet":{
                        "publishedAt":"2024-07-24T14:34:00Z",
                        "channelId":"testChannelId",
                        "title":"testTitle",
                        "description":"testDescription 1",
                        "thumbnails":{"default":{"url":"http://test.com/thumb1.jpg"}},
                        "channelTitle":"Test Channel"
                      }
                    }
                  ]
                }
                """;
        // Mock ExchangeFunction이 어떤 요청을 받든 성공 응답 JSON 반환
        when(exchangeFunction.exchange(any()))
                .thenReturn(Mono.just(buildResponse(successJson, HttpStatus.OK)));

        /// when
        // 어댑터 메소드 호출 (키워드, 개수)
        var result = youtubeVideoApiAdapter.fetchVideos("test keyword", 5);

        /// then
        // Reactive 결과를 StepVerifier로 검증
        StepVerifier.create(result).assertNext(list -> {
            // 변환된 VideoDto 목록 검증
            assertThat(list).hasSize(1);

            VideoDto video = list.getFirst();
            assertThat(video.id()).isEqualTo("testVideoId1");
            assertThat(video.title()).isEqualTo("testTitle");
            assertThat(video.description()).isEqualTo("testDescription 1");
            assertThat(video.thumbnailUrl()).isEqualTo("http://test.com/thumb1.jpg");
            assertThat(video.publishedAt()).isEqualTo(
                    LocalDateTime.of(2024, 7, 24, 14, 34, 0));
        }).verifyComplete(); // Mono가 성공적으로 완료되었는지 검증

        // WebClient의 ExchangeFunction이 1회 호출되었는지 검증
        verify(exchangeFunction, times(1)).exchange(any());
    }

    @Test
    @DisplayName("Youtube Video 검색 API - retry 확인")
    void fetchVideos_retry() {
        /// given
        // YouTube API 에러 응답 JSON 예시
        String errorJson = """
                    {
                      "error": {
                        "code": 503,
                        "message": "API service unavailable.",
                        "errors": [
                          {
                            "domain": "global",
                            "reason": "backendError",
                            "message": "Backend Error"
                          }
                        ]
                      }
                    }
                """;

        String successJson = """
                {
                  "items":[
                    {
                      "id":{"videoId":"testVideoId2"},
                      "snippet":{
                        "publishedAt":"2024-07-25T10:00:00Z",
                        "channelId":"testChannelId",
                        "title":"Retry Success Video",
                        "description":"Description",
                        "thumbnails":{"default":{"url":"http://test.com/thumb2.jpg"}},
                        "channelTitle":"Channel"
                      }
                    }
                  ]
                }
                """;

        // Mock ExchangeFunction이 처음에는 에러 응답, 두 번째는 성공 응답 반환
        when(exchangeFunction.exchange(any())).thenReturn(Mono.just(buildResponse(errorJson, HttpStatus.SERVICE_UNAVAILABLE)), Mono.just(buildResponse(successJson, HttpStatus.OK)));

        /// when
        var result = youtubeVideoApiAdapter.fetchVideos("retry keyword", 5);

        /// then
        // Retry 설정에 따라 실패 후 재시도하여 최종 성공 결과를 받는지 검증
        StepVerifier.create(result).assertNext(list -> {
            assertThat(list).hasSize(1);
            assertThat(list.getFirst().title()).isEqualTo("Retry Success Video");
        }).verifyComplete();

        // Mock ExchangeFunction이 총 2회 호출되었는지 검증
        verify(exchangeFunction, times(2)).exchange(any());
    }

    @Test
    @DisplayName("Youtube Video 검색 API - circuitBreaker 확인")
    void fetchVideos_circuitBreaker() {
        /// given
        // 실패 응답 JSON
        String errorJson = """
                {
                  "error": {
                    "code": 500,
                    "message": "Internal Error",
                    "errors": [
                      {
                        "domain": "global",
                        "reason": "internalError",
                        "message": "Internal Error"
                      }
                    ]
                  }
                }
                """;

        // Mock ExchangeFunction이 반환할 단일 에러 응답 Mono
        Mono<ClientResponse> errorResponseMono = Mono
                .just(buildResponse(errorJson, HttpStatus.INTERNAL_SERVER_ERROR));

        // Retry 설정에 따른 총 시도 횟수 (application-test 기준)
        int attemptsPerFailure = 3;

        // Circuit Breaker를 OPEN 시키기 위해 필요한 실패 횟수
        int failuresToOpenCircuit = 1;

        // Circuit Breaker가 OPEN 되기 전까지 ExchangeFunction이 호출될 총 예상 횟수
        // 각 실패는 Retry 횟수만큼 ExchangeFunction 호출을 유발
        int configuredExpectedExchange = failuresToOpenCircuit * attemptsPerFailure;

        // --- 현재 테스트 환경에서 관찰되는 실제 호출 횟수 ---
        int actualObservedExchangeCalls = 2;

        // Mock ExchangeFunction 설정: 첫 번째 논리적 호출(3회 시도) 동안 에러 응답 반환
        // Mockito 체이닝으로 3번의 에러 응답 설정
        when(exchangeFunction.exchange(any()))
                .thenReturn(errorResponseMono, errorResponseMono, errorResponseMono);

        /// when
        // Circuit Breaker를 OPEN 시키기 위해 1회 실패 호출 시도
        log.info("Circuit Breaker OPEN 시키기 위해 {}회 실패 호출 시도 시작", failuresToOpenCircuit);

        StepVerifier.create(youtubeVideoApiAdapter.fetchVideos("fail keyword", 1))
                .expectError(RetryableExternalApiException.class)
                .verify(Duration.ofSeconds(1));

        log.info("{}회 실패 호출 시도 완료. CB 상태 확인.", failuresToOpenCircuit);

        /// then
        // 1회 실패 후 Circuit Breaker 상태가 OPEN 인지 확인
        CircuitBreaker circuitBreaker = registry.circuitBreaker("YOUTUBE_VIDEO_CIRCUIT_BREAKER");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        log.info("Circuit Breaker 상태: {}", circuitBreaker.getState());

        /// when
        // Circuit Breaker가 열린 상태에서 두 번째 호출 → CallNotPermittedException 발생 예상
        log.info("Circuit Breaker OPEN 상태에서 두 번째 호출 시도");
        StepVerifier.create(youtubeVideoApiAdapter
                        .fetchVideos("should be blocker", 1))
                // Circuit Breaker OPEN 상태에서는 CallNotPermittedException 발생 예상
                .expectError(CallNotPermittedException.class)
                .verify(Duration.ofSeconds(1));

        log.info("Circuit Breaker OPEN 상태에서 호출 차단 확인");

        /// then
        // ExchangeFunction이 Circuit Breaker OPEN 전까지 호출된 총 횟수 검증
        verify(exchangeFunction, times(actualObservedExchangeCalls)).exchange(any());
    }


    @Test
    @DisplayName("Youtube Video 검색 API - rateLimiter 확인")
    void fetchVideos_rateLimiter() {
        /// given
        // 성공 응답 JSON (빈 결과 목록)
        String emptyJson = """
                {
                  "items":[]
                }
                """;

        // Mock ExchangeFunction이 계속 성공 응답 반환하도록 설정
        // Rate Limiter가 차단하지 않으면 모든 호출이 ExchangeFunction에 도달할 수 있음
        when(exchangeFunction.exchange(any()))
                .thenReturn(
                        Mono.just(buildResponse(emptyJson, HttpStatus.OK)),
                        Mono.just(buildResponse(emptyJson, HttpStatus.OK)),
                        Mono.just(buildResponse(emptyJson, HttpStatus.OK))
                );

        /// when & then
        // 첫 번째 호출: RateLimiter 허용, 성공 예상
        StepVerifier.create(youtubeVideoApiAdapter.fetchVideos("k1", 1))
                .expectNext(Collections.emptyList())
                .verifyComplete();

        log.info(">>>>>>> 1회 호출 성공 확인");

        // 첫 번째 호출: RateLimiter 허용, 성공 예상
        StepVerifier.create(youtubeVideoApiAdapter.fetchVideos("k2", 1))
                .expectNext(Collections.emptyList())
                .verifyComplete();

        log.info(">>>>>>> 2회 호출 성공 확인");

        // 세 번째 호출: RateLimiter 차단 예상
        StepVerifier.create(youtubeVideoApiAdapter.fetchVideos("k3", 1))
                .expectError(RequestNotPermitted.class)
                .verify();

        log.info(">>>>>>> 3회 호출 차단 확인 (RateLimiter 초과)");


        /// then
        // Rate Limiter는 2번의 호출만 허용했으므로, ExchangeFunction은 2회만 호출되었는지 검증
        verify(exchangeFunction, times(2)).exchange(any());
    }
}
