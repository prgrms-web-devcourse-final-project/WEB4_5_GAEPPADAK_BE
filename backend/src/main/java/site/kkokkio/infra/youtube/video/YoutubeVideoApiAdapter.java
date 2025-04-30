package site.kkokkio.infra.youtube.video;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import site.kkokkio.domain.source.dto.VideoDto;
import site.kkokkio.domain.source.port.out.VideoApiPort;
import site.kkokkio.infra.common.exception.RetryableExternalApiException;
import site.kkokkio.infra.youtube.video.dto.YoutubeVideosSearchResponse;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class YoutubeVideoApiAdapter implements VideoApiPort {

    // Youtube API 호출에 사용할 WebClient
    @Qualifier("youtubeWebClient")
    private final WebClient youtubeWebClient;

    // .env 등 설정 파일에서 읽어올 Youtube API 설정 값
    @Value("${youtube.api.key}")
    private String YOUTUBE_API_KEY;

    // 설정 파일에 정의해야 할 기본 URL
    @Value("${youtube.api.base-url}")
    private String YOUTUBE_BASE_URL;

    // 영상 검색 엔드포인트 경로
    @Value("${youtube.api.search.path}")
    private String YOUTUBE_PATH;

    // VideoApiPort 인터페이스의 메소드 구현
    @Override
//    @Retry(name = "YOUTUBE_RETRY") // Resilience4j 애노테이션 (나중에 설정 추가 후 활성화)
//    @CircuitBreaker(name = "YOUTUBE_CIRCUIT_BREAKER") // Resilience4j 애노테이션 (나중에 설정 추가 후 활성화)
//    @RateLimiter(name = "YOUTUBE_RATE_LIMITER") // Resilience4j 애노테이션 (나중에 설정 추가 후 활성화)
    public Mono<List<VideoDto>> fetchVideos(String keyword, int count) {

        // WebClient를 사용하여 YouTube API 호출
        return youtubeWebClient.get()
                // 요청 URI 빌드 (기본 URL + 경로 + 쿼리 파라미터)
                .uri(uri -> buildYoutubeVideosUri(uri, keyword, count))
//                .header("X-API-KEY", YOUTUBE_API_KEY)
                .retrieve()
                // HTTP 에러 응답 처리 (4xx, 5xx)
                .onStatus(HttpStatusCode::isError, this.mapYoutubeError)
                // 응답 본문을 Youtube API 응답 구조 DTO로 변환
                .bodyToMono(YoutubeVideosSearchResponse.class)
                // 서킷 오픈 시 예외 처리
                .onErrorMap(CallNotPermittedException.class, ex -> new RetryableExternalApiException(503, ex.getMessage()))
                // Youtube API 응답 DTO를 VideoDTo 목록으로 변환
                .map(this::toVedioDtos);
    }

    // Youtube API 요청 URI를 빌드하는 헬퍼 메소드
    private URI buildYoutubeVideosUri(UriBuilder uriBuilder, String keyword, int count) {
        /**
         * Youtube Data API v3 - videos.list 엔드포인트 사용
         * 필수 파라미터: part (snippet, contentDetails 등), chart (mostPopular), regionCode (KR)
         * 검색어 관련 필터링 파라미터: q (검색어), videoCategoryId 등
         * 결과 개수: maxResults (1-50)
         * API 키: key
         */
        UriBuilder ub = uriBuilder.path(YOUTUBE_PATH)
                .queryParam("part", "snippet")
                .queryParam("type", "video")
                .queryParam("q", keyword)
                .queryParam("regionCode", "KR")
                .queryParam("maxResults", count)
                .queryParam("order", "viewCount")
                .queryParam("key", YOUTUBE_API_KEY);

        return ub.build();
    }
}
