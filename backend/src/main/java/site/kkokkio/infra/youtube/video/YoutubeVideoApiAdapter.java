package site.kkokkio.infra.youtube.video;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import site.kkokkio.domain.source.dto.VideoDto;
import site.kkokkio.domain.source.port.out.VideoApiPort;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.infra.common.exception.ExternalApiErrorUtil;
import site.kkokkio.infra.common.exception.RetryableExternalApiException;
import site.kkokkio.infra.youtube.video.dto.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    @Retry(name = "YOUTUBE_VIDEO_RETRY") // Resilience4j 애노테이션 (나중에 설정 추가 후 활성화)
    @CircuitBreaker(name = "YOUTUBE_VIDEO_CIRCUIT_BREAKER") // Resilience4j 애노테이션 (나중에 설정 추가 후 활성화)
    @RateLimiter(name = "YOUTUBE_VIDEO_RATE_LIMITER") // Resilience4j 애노테이션 (나중에 설정 추가 후 활성화)
    public Mono<List<VideoDto>> fetchVideos(String keyword, int count) {

        // WebClient를 사용하여 YouTube API 호출
        return youtubeWebClient.get()
                // 요청 URI 빌드 (기본 URL + 경로 + 쿼리 파라미터)
                .uri(uri -> buildYoutubeVideosUri(uri, keyword, count))
//                .header("X-API-KEY", YOUTUBE_API_KEY)
                .retrieve()
                // HTTP 에러 응답 처리 (4xx, 5xx)
                .onStatus(HttpStatusCode::isError, this::mapYoutubeError)
                // 응답 본문을 Youtube API 응답 구조 DTO로 변환
                .bodyToMono(YoutubeVideosSearchResponse.class)
                // 서킷 오픈 시 예외 처리
                .onErrorMap(CallNotPermittedException.class, ex -> new RetryableExternalApiException(503, ex.getMessage()))
                // Youtube API 응답 DTO를 VideoDTo 목록으로 변환
                .map(this::toVideoDtos);
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

    /**
     * Youtube API 응답 DTO (YoutubeVideosSearchResponse)를
     * 내부 DTO (List<VideoDto>) 목록으로 변환합니다.
     *
     * @param response Youtube API 검색 응답 DTO
     * @return VideoDto 목록
     */
    private List<VideoDto> toVideoDtos(YoutubeVideosSearchResponse response) {

        // API 응답이 null이거나 items 리스트가 null 또는 비어있는 경우 빈 리스트 반환
        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            log.warn("Youtube API 응답이 비어있음.");
            return Collections.emptyList();
        }

        // YoutubeSearchItem 리스트를 순회하며 VideoDto로 변환
        return response.getItems().stream()
                // 각 YoutubeSearchItem을 VideoDto로 변환하는 헬퍼 메소드 호출
                .map(this::toVideoDto)
                // 변환 중 실패하여 Optional.empty()가 된 항목 제거
                .filter(Optional::isPresent)
                // Optional에서 실제 VideoDto 객체 추출
                .map(Optional::get)
                // List로 수집
                .collect(Collectors.toList());
    }

    /**
     * 단일 YoutubeSearchItem 객체를 VideoDto로 변환합니다.
     * 유효하지 않은 항목은 Optional.empty()를 반환합니다.
     *
     * @param item YoutubeSearchItem 객체
     * @return VideoDto 객체를 담은 Optional (변환 실패 시 Optional.empty())
     */
    private Optional<VideoDto> toVideoDto(YoutubeSearchItem item) {

        // YoutubeSearchItem의 필수 필드 (id, snippet) 및
        // 그 하위 필드(videoId, title, publishedAt)가 null이 아닌지 검증
        if (
                item == null ||
                        item.getId() == null ||
                        item.getId().getVideoId() == null ||
                        item.getSnippet() == null ||
                        item.getSnippet().getTitle() == null ||
                        item.getSnippet().getPublishedAt() == null
        ) {
            log.warn("Youtube Item 변환 실패: 필수 필드 누락 또는 null. item={}", item);
            return Optional.empty();
        }

        SearchSnippet snippet = item.getSnippet();
        ResourceId id = item.getId();

        // 썸네일 URL 가져오기: 여러 사이즈 중 하나 선택 (default / medium)
        String thumbnailUrl = null;

        if (snippet.getThumbnails() != null) {
            ThumbnailDetails defaultThumbnail = snippet.getThumbnails().get("default");

            if (defaultThumbnail != null && defaultThumbnail.getUrl() != null) {
                thumbnailUrl = defaultThumbnail.getUrl();
            } else {
                // default 썸네일이 없으면 다른 사이즈 시도
                ThumbnailDetails mediumThumbnail = snippet.getThumbnails().get("medium");

                if (mediumThumbnail != null && mediumThumbnail.getUrl() != null) {
                    thumbnailUrl = mediumThumbnail.getUrl();
                }

                // 다른 사이즈도 없으면 thumbnailUrl은 null 유지
            }
        }

        try {
            return Optional.of(VideoDto.builder()
                    .id(id.getVideoId())
                    .title(snippet.getTitle())
                    .description(snippet.getDescription())
                    .thumbnailUrl(thumbnailUrl)
                    .publishedAt(snippet.getPublishedAt().toLocalDateTime())
                    .build());
        } catch (Exception e) {
            log.error("Youtube Item -> VideoDto 변환 중 예외 발생. item={}", item, e);
            return Optional.empty();
        }
    }

    /**
     * Youtube API 에러 응답 (4xx, 5xx)을 처리하고 적절한 예외로 변환합니다.
     * onStatus 연산자에 사용됩니다.
     *
     * @param response ClientResponse (HTTP 에러 응답)
     * @return 적절한 ServiceException 하위 예외를 담은 Mono
     */
    private Mono<? extends Throwable> mapYoutubeError(ClientResponse response) {

        // HTTP 상태 코드를 가져옵니다.
        HttpStatusCode statusCode = response.statusCode();
        int statusCodeValue = statusCode.value();

        // 에러 응답 본문을 YoutubeErrorResponse DTO로 변환 시도
        return response.bodyToMono(YoutubeErrorResponse.class)
                // 응답 본문 파싱 실패나 비어있는 응답 본문의 경우 처리
                .onErrorResume(e -> {
                    // 파싱 실패 시 로그 기록 및 RetryableExternalApiException 반환
                    log.error("Youtube API 에러 응답 본문 파싱 또는 처리 중 예외 발생. status={}", statusCodeValue, e);
                    return Mono.error(new RetryableExternalApiException(
                            statusCodeValue,
                            "Youtube API 에러 응답 처리 중 예외 발생"
                    ));
                })
                // 파싱된 YoutubeErrorResponse 객체를 사용하여 예외 생성
                .flatMap(errorResponse -> {
                    // 에러 정보 (error 객체) 추출
                    YoutubeError youtubeError = errorResponse != null ?
                            errorResponse.getError() : null;

                    // 오류 코드 및 메시지 추출
                    String vendorCode = null;
                    String vendorMessage = null;

                    if (youtubeError != null) {
                        // 일반 에러 메시지 사용
                        vendorMessage = youtubeError.getMessage();

                        // 상세 에러 목록이 있다면 첫 번째 항목의 reason과 message를 사용
                        // getErrors()가 null이 아니고 비어있지 않은 경우에만 getFirst 접근
                        if (youtubeError.getErrors() != null && !youtubeError.getErrors().isEmpty()) {
                            YoutubeErrorDetail detail = youtubeError.getErrors().getFirst();

                            if (detail != null) {
                                // 상세 에러의 'reason'을 vendorCode로 사용
                                vendorCode = detail.getReason();

                                // 상세 에러의 'message'가 있다면 사용, 없으면 일반 메시지 사용
                                vendorMessage = (detail.getMessage() != null &&
                                        !detail.getMessage().isBlank() ?
                                        detail.getMessage() : vendorMessage);
                            }
                        }
                    }

                    // ExternalApiErrorUtil을 사용하여 적절한 ServiceException 하위 예외 생성
                    // HTTP 상태 코드, 벤더 오류 코드, 벤더 오류 메시지를 전달
                    ServiceException serviceException = ExternalApiErrorUtil.of(
                            response.statusCode(),
                            vendorCode,
                            vendorMessage
                    );

                    // 생성된 예외를 Mono로 감싸 반환
                    return Mono.error(serviceException);
                });
    }
}
