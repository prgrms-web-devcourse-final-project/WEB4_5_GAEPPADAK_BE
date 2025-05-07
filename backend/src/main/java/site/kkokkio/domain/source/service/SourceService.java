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
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.controller.dto.TopSourceListResponse;
import site.kkokkio.domain.source.dto.NewsDto;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.dto.TopSourceItemDto;
import site.kkokkio.domain.source.dto.VideoDto;
import site.kkokkio.domain.source.entity.KeywordSource;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.port.out.NewsApiPort;
import site.kkokkio.domain.source.port.out.VideoApiPort;
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
	private final VideoApiPort videoApi;

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

	@Transactional(readOnly = true)
	public List<SourceDto> getTop5SourcesByPosts(List<PostDto> postDtoList) {
		if (postDtoList == null || postDtoList.isEmpty()) {
			return Collections.emptyList();
		}
		List<Long> postIds = postDtoList.stream().map(PostDto::postId).toList();

		PageRequest pageRequest = PageRequest.of(0, 5); // 5개 고정
		List<Source> top5SourcesByPostIds = sourceRepository.findByPostIdsOrderByPublishedAtDesc(postIds, pageRequest);

		return top5SourcesByPostIds.stream().map(SourceDto::from).toList();
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

		sources = sources.stream().distinct().toList();

        // 3. Source 저장
        sourceRepository.insertIgnoreAll(sources);

        // 4. KeywordSource 저장
        keywordSourceRepository.insertIgnoreAll(keywordSources);

		// 5. 비동기로 OpenGraph 정보 보강
        sources.forEach(openGraphService::enrichAsync);
    }

	/**
	 * Youtube API를 호출하여 현재 실시간 키워드 Top10에 대한 영상 소스를 검색하고, DB에 저장합니다.
	 */
	@Transactional
	public void searchYoutube() {
		log.info(">>>>>>> searchYoutube 메소드 시작");

		// 1. 최신 Top10 키워드 조회
		List<KeywordMetricHourlyDto> topKeywords = keywordMetricHourlyService.findHourlyMetrics();
		List<Source> allSourcesToSave = new ArrayList<>();
		List<KeywordSource> allKeywordSourcesToSave = new ArrayList<>();

		// 2. 키워드별 영상 검색 및 Entity 변환
		for (KeywordMetricHourlyDto metric : topKeywords) {
			String text = metric.text();
			Keyword keywordRef = Keyword.builder().id(metric.keywordId()).build();

			// Youtube API 호출 및 Reactive 결과 처리
			List<VideoDto> videoList = Optional.ofNullable(
					videoApi.fetchVideos(text, MAX_SOURCE_COUNT_PER_POST)
							.onErrorResume(e -> {
								// API 호출 실패 시 로그 기록 및 빈 목록 반환하여 전체 중단 방지
								log.error("Youtube API 실패. keyword={}, error={}",
										text, e.toString());
								return Mono.just(Collections.emptyList());
							}).block()
			).orElseGet(Collections::emptyList);

			if (videoList.isEmpty()) {
				log.warn("Youtube API 응답이 비어있음. keyword={}", text);
				continue; // 다음 키워드로 넘어감
			}

			// Source <-> Keyword 매핑 및 Entity 변환
			for (VideoDto video : videoList) {
				// VideoDto를 Source 엔티티로 변환
				Source src = video.toEntity(VIDEO_PLATFORM);
				allSourcesToSave.add(src); // Source 리스트에 추가

				// Keyword와 Source 연결하는 KeywordSource 엔티티 생성
				allKeywordSourcesToSave.add(KeywordSource.builder()
						.keyword(keywordRef)
						.source(src)
						.build()
				);
			}
		}

		// 3. 저장할 데이터가 있는지 확인
		if (allSourcesToSave.isEmpty()) {
			log.info("저장할 Youtube Source가 없습니다.");
			return; // 저장할 데이터 없으면 종료
		}

		// 4. Source 리스트에서 중복 제거 (Optional)
		List<Source> distinctSources = allSourcesToSave.stream().distinct().toList();
		log.info("처리할 중복 제거된 Source {}개", distinctSources.size());

		// 5. Source 데이터 저장 (INSERT IGNORE 사용)
		// sourceRepositoryCustom는 @Autowired 필요
		sourceRepository.insertIgnoreAll(distinctSources);
		log.info("{}개의 Youtube Source 저장 시도 (INSERT IGNORE 사용)", distinctSources.size());

		// 6. KeywordSource 데이터 저장 (INSERT IGNORE 사용)
		// distinctSources에 있는 Source의 fingerprint를 기준으로 allKeywordSourcesToSave를 필터링
		// distinct() 과정에서 제거된 Source와 연결된 KeywordSource는 저장 대상에서 제외
		List<String> distinctSourceFingerprints = distinctSources.stream()
				.map(Source::getFingerprint).toList();
		List<KeywordSource> distinctKeywordSources = allKeywordSourcesToSave.stream()
						.filter(ks -> ks.getSource() != null &&
								ks.getSource().getFingerprint() != null &&
								distinctSourceFingerprints.contains(ks.getSource()
										.getFingerprint())).toList();

		// keywordSourceRepository는 @Autowired 필요
		keywordSourceRepository.insertIgnoreAll(distinctKeywordSources);
		log.info(">>> {}개의 KeywordSource 저장 시도 (INSERT IGNORE 사용)",
				distinctSourceFingerprints.size());

		// 7. OpenGraph 정보 비동기 보강 (Source 엔티티에 URL 필드 필요)
		distinctSources.forEach(openGraphService::enrichAsync);

		log.info(">>>>>>> searchYoutube 메소드 완료");
	}

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

		// PostSourceRepository에서 인기 키워드 ID 목록과 플랫폼(YOUTUBE),
		// Pageable 정보를 사용하여 조회 및 DTO 변환
		Page<TopSourceItemDto> dtoPage = postSourceRepository
				.findTopSourcesByKeywordIdsAndPlatformOrderedByScore(
						topKeywordIds,
						Platform.YOUTUBE,
						pageable
				);

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

		// PostSourceRepository에서 인기 키워드 ID 목록과 플랫폼(NAVER_NEWS),
		// Pageable 정보를 사용하여 조회 및 DTO 변환
		Page<TopSourceItemDto> dtoPage = postSourceRepository
				.findTopSourcesByKeywordIdsAndPlatformOrderedByScore(
						topKeywordIds,
						Platform.NAVER_NEWS,
						pageable
				);

		// Page<TopSourceItemDto>를 TopSourceListResponse 객체로 변환하여 반환
		return TopSourceListResponse.from(dtoPage);
	}
}
