package site.kkokkio.domain.source.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyDto;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.controller.dto.TopSourceListResponse;
import site.kkokkio.domain.source.dto.NewsDto;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.dto.TopSourceItemDto;
import site.kkokkio.domain.source.entity.KeywordSource;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.port.out.NewsApiPort;
import site.kkokkio.domain.source.repository.KeywordSourceRepository;
import site.kkokkio.domain.source.repository.PostSourceRepository;
import site.kkokkio.domain.source.repository.SourceRepository;
import site.kkokkio.global.enums.Platform;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceService {

	private final PostSourceRepository postSourceRepository;
    private final SourceRepository sourceRepository;
	private final PostService postService;
	private final KeywordMetricHourlyService keywordMetricHourlyService;
    private final OpenGraphService openGraphService;
	private final NewsApiPort newsApi;

	private static final int MAX_SOURCE_COUNT_PER_POST = 10;
	private final Platform NEWS_PLATFORM = Platform.NAVER_NEWS;
	private final Platform VIDEO_PLATFORM = Platform.YOUTUBE;
	private final KeywordSourceRepository keywordSourceRepository;

	@Transactional(readOnly = true)
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

	@Transactional(readOnly = true)
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

	/**
	 * Naver News API를 호출하여 현재 실시간 키워드 Top10에 대한 뉴스 소스를 검색하고, DB에 저장합니다.
	 */
	@Transactional
	public void searchNews() {
        // 1. 최신 Top10 키워드 조회
        List<KeywordMetricHourlyDto> topKeywords = keywordMetricHourlyService.findHourlyMetrics();
        List<Source> sources = new ArrayList<>();
        List<KeywordSource> keywordSources = new ArrayList<>();

        // 2. 키워드별 뉴스 검색 및 Entity 변환
        for (KeywordMetricHourlyDto metric : topKeywords) {
            String text = metric.text();
            Keyword keywordRef = Keyword.builder().id(metric.keywordId()).build();

            List<NewsDto> newsList = Optional.ofNullable(
                newsApi.fetchNews(text, MAX_SOURCE_COUNT_PER_POST, 1, "sim")
                    .onErrorResume(e -> {
                        log.error("Naver API 실패. keyword={}, error={}", text, e.toString());
                        return Mono.just(Collections.emptyList());
                    })
                    .block()
            ).orElseGet(Collections::emptyList);

            if (newsList.isEmpty()) {
                log.warn("Naver API 응답이 비어있음. keyword={}", text);
                continue;
            }

            // Source <-> Keyword 매핑
            for (NewsDto dto : newsList) {
                Source src = dto.toEntity(NEWS_PLATFORM);
                sources.add(src);
                keywordSources.add(KeywordSource.builder()
                    .keyword(keywordRef)
                    .source(src)
                    .build());
            }
        }

        if (sources.isEmpty()) {
            log.info("저장할 Source가 없습니다.");
            return;
        }

        // 3. Source 저장
        List<Source> saved = sourceRepository.saveAll(sources);

        // 4. KeywordSource 저장
        keywordSourceRepository.saveAll(keywordSources);

		// 5. 비동기로 OpenGraph 정보 보강
        saved.forEach(openGraphService::enrichAsync);
    }

	/**
	 * Youtube API를 호출하여 현재 실시간 키워드 Top10에 대한 영상 소스를 검색하고, DB에 저장합니다.
	 */
	public void searchYoutube() { }

	/**
	 * 실시간 인기 키워드와 관련된 Youtube Source 목록을 페이지네이션하여 조회
	 * @param pageable 페이지네이션 및 정렬 정보.
	 * @return 페이지네이션된 TopSourceListResponse 객체.
	 */
	@Transactional(readOnly = true)
	public TopSourceListResponse getTopYoutubeSources(Pageable pageable) {

		// 현재 시스템의 실시간 인기 키워드 목록(ID)을 가져옵니다. (데이터 파이프라인의 Task 2 결과물)
		List<KeywordMetricHourlyDto> topKeywords = keywordMetricHourlyService.findHourlyMetrics();
		List<Long> topKeywordIds = topKeywords.stream()
				.map(KeywordMetricHourlyDto::keywordId)
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
	@Transactional(readOnly = true)
	public TopSourceListResponse getTopNaverNewsSources(Pageable pageable) {

		// 현재 시스템의 실시간 인기 키워드 목록(ID)을 가져옵니다. (데이터 파이프라인의 Task 2 결과물)
		List<KeywordMetricHourlyDto> topKeywords = keywordMetricHourlyService.findHourlyMetrics();
		List<Long> topKeywordIds = topKeywords.stream()
				.map(KeywordMetricHourlyDto::keywordId)
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
