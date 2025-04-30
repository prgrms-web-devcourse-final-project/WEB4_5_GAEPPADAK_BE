package site.kkokkio.domain.source.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyResponse;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.dto.NewsDto;
import site.kkokkio.domain.source.controller.dto.TopSourceListResponse;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.dto.TopSourceItemDto;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.port.out.NewsApiPort;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.repository.PostSourceRepository;
import site.kkokkio.domain.source.repository.SourceRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.infra.common.exception.RetryableExternalApiException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 기능 위주라서 추가했습니다.
public class SourceService {

	private final PostSourceRepository postSourceRepository;
    private final SourceRepository sourceRepository;
	private final PostService postService;
	private final NewsApiPort newsApi;

	private static final int MAX_SOURCE_COUNT_PER_POST = 10;
	private final Platform NEWS_PLATFORM = Platform.NAVER_NEWS;
	private final Platform VIDEO_PLATFORM = Platform.YOUTUBE;
	private final KeywordMetricHourlyService keywordMetricHourlyService;

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

    		List<Source> sources = newsList.stream()
				.map(dto -> dto.toEntity(NEWS_PLATFORM))
				.toList();

            saveSources(sources);
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

	/**
	 * 실시간 인기 키워드와 관련된 Youtube Source 목록을 페이지네이션하여 조회
	 * @param pageable 페이지네이션 및 정렬 정보.
	 * @return 페이지네이션된 TopSourceListResponse 객체.
	 */
	public TopSourceListResponse getTopYoutubeSources(Pageable pageable) {

		// 현재 시스템의 실시간 인기 키워드 목록(ID)을 가져옵니다. (데이터 파이프라인의 Task 2 결과물)
		List<KeywordMetricHourlyResponse> topKeywords = keywordMetricHourlyService.findHourlyMetrics();
		List<Long> topKeywordIds = topKeywords.stream()
				.map(KeywordMetricHourlyResponse::keywordId)
				.collect(Collectors.toList());

		// 인기 키워드가 없으면 빈 페이지 반환
		if (topKeywordIds.isEmpty()) {
			return TopSourceListResponse.from(Page.empty(pageable));
		}

		// Repository에서 인기 키워드 ID 목록과 플랫폼(YOUTUBE), Pageable 정보를 사용하여 Source 엔티티들을 페이지네이션하여 조회
		Page<Source> sourcePage = postSourceRepository.findSourcesByTopKeywordIdsAndPlatform(
				topKeywordIds,
				Platform.YOUTUBE,
				pageable
		);

		// 조회된 Page<Source>를 Page<TopSourceItemDto>로 변환
		Page<TopSourceItemDto> dtoPage = sourcePage.map(TopSourceItemDto::fromSource);

		// Page<TopSourceItemDto>를 TopSourceListResponse 객체로 변환하여 반환
		return TopSourceListResponse.from(dtoPage);
	}

	/**
	 * 실시간 인기 키워드와 관련된 네이버 뉴스 Source 목록을 페이지네이션하여 조회
	 * @param pageable 페이지네이션 및 정렬 정보.
	 * @return 페이지네이션된 TopSourceListResponse 객체.
	 */
	public TopSourceListResponse getTopNaverNewsSources(Pageable pageable) {

		// 현재 시스템의 실시간 인기 키워드 목록(ID)을 가져옵니다. (데이터 파이프라인의 Task 2 결과물)
		List<KeywordMetricHourlyResponse> topKeywords = keywordMetricHourlyService.findHourlyMetrics();
		List<Long> topKeywordIds = topKeywords.stream()
				.map(KeywordMetricHourlyResponse::keywordId)
				.collect(Collectors.toList());

		// 인기 키워드가 없으면 빈 페이지 반환
		if (topKeywordIds.isEmpty()) {
			return TopSourceListResponse.from(Page.empty(pageable));
		}

		// Repository에서 인기 키워드 ID 목록과 플랫폼(NAVER_NEWS), Pageable 정보를 사용하여 Source 엔티티들을 페이지네이션하여 조회
		Page<Source> sourcePage = postSourceRepository.findSourcesByTopKeywordIdsAndPlatform(
				topKeywordIds,
				Platform.NAVER_NEWS,
				pageable
		);

		// 조회된 Page<Source>를 Page<TopSourceItemDto>로 변환
		Page<TopSourceItemDto> dtoPage = sourcePage.map(TopSourceItemDto::fromSource);

		// Page<TopSourceItemDto>를 TopSourceListResponse 객체로 변환하여 반환
		return TopSourceListResponse.from(dtoPage);
	}
}
