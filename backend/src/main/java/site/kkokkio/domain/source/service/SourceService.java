package site.kkokkio.domain.source.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.dto.NewsDto;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.port.out.NewsApiPort;
import site.kkokkio.domain.source.repository.PostSourceRepository;
import site.kkokkio.domain.source.repository.SourceRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.util.HashUtils;
import site.kkokkio.infra.common.exception.RetryableExternalApiException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceService {

	private final PostSourceRepository postSourceRepository;
    private final SourceRepository sourceRepository;
	private final PostService postService;
	private final NewsApiPort newsApi;

	private static final int MAX_SOURCE_COUNT_PER_POST = 10;
	private final Platform NEWS_PLATFORM = Platform.NAVER_NEWS;
	private final Platform VIDEO_PLATFORM = Platform.YOUTUBE;

	public List<SourceDto> getTop10NewsSourcesByPostId(Long postId) {
		postService.getPostById(postId);
		PageRequest pageRequest = PageRequest.of(0, MAX_SOURCE_COUNT_PER_POST);
		List<PostSource> postSources = postSourceRepository.findAllWithSourceByPostIdAndPlatform(
			postId,
			NEWS_PLATFORM,
			pageRequest);
		return postSources.stream()
			.map(ps -> SourceDto.from(ps.getSource()))
			.toList();
	}

	public List<SourceDto> getTop10VideoSourcesByPostId(Long postId) {
		postService.getPostById(postId);
		PageRequest pageRequest = PageRequest.of(0, MAX_SOURCE_COUNT_PER_POST);
		List<PostSource> postSources = postSourceRepository.findAllWithSourceByPostIdAndPlatform(
			postId,
			VIDEO_PLATFORM,
			pageRequest);
		return postSources.stream()
			.map(ps -> SourceDto.from(ps.getSource()))
			.toList();
	}

	public void searchNews(String keyword) {
		try {
            List<NewsDto> newsList = fetchNewsBlocking(keyword);

            if (newsList.isEmpty()) {
                log.warn("Naver API 응답이 비어있음. keyword={}", keyword);
                return;
            }
            saveSources(convertToSources(newsList));
        } catch (RetryableExternalApiException retryEx) {
            log.warn("외부 API 실패 → DB fallback. cause={}", retryEx.getMessage());
            saveSources(fetchFallbackSources(keyword));
        }
    }

    /** Naver News API 호출
	 * null/empty 시 빈 리스트로 반환
	 */
    private List<NewsDto> fetchNewsBlocking(String keyword) {
        return Optional.ofNullable(
                   newsApi.fetchNews(keyword, MAX_SOURCE_COUNT_PER_POST, 1, "sim")
                          .block()
               )
               .orElseGet(Collections::emptyList);
    }

    /** NewsDto → Source Mapping */
    private List<Source> convertToSources(List<NewsDto> dtos) {
        return dtos.stream()
                   .map(dto -> Source.builder()
                       .fingerprint(HashUtils.sha256Hex(dto.getLink()))
                       .normalizedUrl(dto.getLink())
                       .title(dto.getTitle())
                       .description(dto.getDescription())
                       .thumbnailUrl(null)
                       .publishedAt(dto.getPubDate())
                       .platform(NEWS_PLATFORM)
                       .build()
                   )
                   .toList();
    }

    /** fallback용 DB 조회 */
    private List<Source> fetchFallbackSources(String keyword) {
		PageRequest pageRequest = PageRequest.of(0, MAX_SOURCE_COUNT_PER_POST);
        return sourceRepository
                .findLatest10ByPlatformAndKeyword(NEWS_PLATFORM, keyword, pageRequest);
    }

    private void saveSources(List<Source> sources) {
        if (!sources.isEmpty()) {
            sourceRepository.saveAll(sources);
        }
    }
}
