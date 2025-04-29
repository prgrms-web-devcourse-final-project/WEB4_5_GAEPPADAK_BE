package site.kkokkio.infra.naver.news;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import site.kkokkio.domain.source.dto.NewsDto;
import site.kkokkio.domain.source.port.out.NewsApiPort;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.infra.common.exception.ExternalApiErrorUtil;
import site.kkokkio.infra.common.exception.RetryableExternalApiException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverNewsApiAdapter implements NewsApiPort {

    @Qualifier("naverWebClient")
	private final WebClient naverWebClient;

	@Value("${naver.client-id}")
	private String NAVER_CLIENT_ID;

    @Value("${naver.client-secret}")
    private String NAVER_CLIENT_SECRET;

    @Value("${naver.search.news-path}")
    private String NAVER_SEARCH_NEWS_PATH;

    @Retry(name = "NAVER_NEWS_RETRY")
    @RateLimiter(name = "NAVER_NEWS_RATE_LIMITER")
    @CircuitBreaker(name = "NAVER_NEWS_CIRCUIT_BREAKER", fallbackMethod = "openCircuitFallback")
	@Override
	public Mono<List<NewsDto>> fetchNews(String keyword, Integer display, Integer start, String sort) {

        return naverWebClient.get()
            .uri(uri -> buildUri(uri, keyword, display, start, sort))
            .header("X-Naver-Client-Id", NAVER_CLIENT_ID)
            .header("X-Naver-Client-Secret", NAVER_CLIENT_SECRET)
            .retrieve()
			.onStatus(HttpStatusCode::isError, this::mapError)
            .bodyToMono(NaverNewsSearchResponse.class)
			// 서킷 오픈 시 CallNotPermittedException → RetryableExternalApiException으로 변환
			.onErrorMap(CallNotPermittedException.class,
				ex -> new RetryableExternalApiException(503, ex.getMessage()))
			// 5xx·429 에러(ExternalApiErrorUtil)가 이미 RetryableExternalApiException으로 Mapping됨
            .map(this::toNewsDtos);
	}

	private URI buildUri(UriBuilder uriBuilder, String query, Integer display, Integer start, String sort) {
		String encoded = encode(query);
		UriBuilder ub = uriBuilder.path(NAVER_SEARCH_NEWS_PATH).queryParam("query", encoded);
		if (display != null) {
			ub.queryParam("display", display);
		}
		if (start != null) {
			ub.queryParam("start", start);
		}
		if (sort != null) {
			ub.queryParam("sort", sort);
		}
		return ub.build();
	}

    private Mono<? extends Throwable> mapError(ClientResponse response) {
        return response.bodyToMono(NaverErrorResponse.class)
                   .defaultIfEmpty(new NaverErrorResponse())
                   .flatMap(err -> Mono.error(
                       ExternalApiErrorUtil.of(response.statusCode(), err.getCode(), err.getMessage())
                   ));
    }

    private List<NewsDto> toNewsDtos(NaverNewsSearchResponse response) {
        if (response == null || response.getItems() == null) {
            throw new ServiceException("502", "Empty response from Naver News API");
        }
        return response.getItems().stream()
                       .map(item -> NewsDto.builder()
                                           .title(item.getTitle())
                                           .link(item.getLink())
                                           .originalLink(item.getOriginalLink())
                                           .description(item.getDescription())
                                           .pubDate(item.getPubDate().toLocalDateTime())
                                           .build())
                       .toList();
    }

	private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
	}

}
