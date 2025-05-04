package site.kkokkio.domain.source.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import reactor.core.publisher.Mono;
import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyDto;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.controller.dto.TopSourceListResponse;
import site.kkokkio.domain.source.dto.NewsDto;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.dto.TopSourceItemDto;
import site.kkokkio.domain.source.dto.VideoDto;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.port.out.NewsApiPort;
import site.kkokkio.domain.source.port.out.VideoApiPort;
import site.kkokkio.domain.source.repository.KeywordSourceRepository;
import site.kkokkio.domain.source.repository.PostSourceRepository;
import site.kkokkio.domain.source.repository.SourceRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.infra.common.exception.RetryableExternalApiException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SourceServiceTest {

    @InjectMocks
    private SourceService sourceService;

    @Mock private PostService postService;
    @Mock private PostSourceRepository postSourceRepository;
    @Mock private SourceRepository sourceRepository;
    @Mock private NewsApiPort newsApi;
    @Mock private VideoApiPort videoApi;
    @Mock private KeywordMetricHourlyService keywordMetricHourlyService;
    @Mock private OpenGraphService openGraphService;
    @Mock private KeywordSourceRepository keywordSourceRepository;


	private List<Source> newsSources;
	private List<PostSource> newsPostSources;
	private List<Source> youtubeSources;
	private List<PostSource> youtubePostSources;
    private Long postId = 1L;
    private Post dummyPost;

	@BeforeEach
	void setUp() {
        dummyPost = Post.builder().id(postId).build();
		newsSources = Arrays.asList(
			Source.builder().fingerprint("f1").normalizedUrl("https://news1").title("뉴스1").description("뉴스1 설명")
                .thumbnailUrl("thumb1").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
			Source.builder().fingerprint("f2").normalizedUrl("https://news2").title("뉴스2").description("뉴스2 설명")
                .thumbnailUrl("thumb2").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
			Source.builder().fingerprint("f3").normalizedUrl("https://news3").title("뉴스3").description("뉴스3 설명")
                .thumbnailUrl("thumb3").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build()
		);

        newsPostSources = Arrays.asList(
            PostSource.builder().id(101L).post(dummyPost).source(newsSources.get(0)).build(),
            PostSource.builder().id(102L).post(dummyPost).source(newsSources.get(1)).build(),
            PostSource.builder().id(103L).post(dummyPost).source(newsSources.get(2)).build()
        );
		youtubeSources = Arrays.asList(
			Source.builder().fingerprint("f1").normalizedUrl("https://youtube1").title("유튜브1").description("유튜브1 설명")
                .thumbnailUrl("thumb1").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
			Source.builder().fingerprint("f2").normalizedUrl("https://youtube2").title("유튜브2").description("유튜브2 설명")
                .thumbnailUrl("thumb2").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
			Source.builder().fingerprint("f3").normalizedUrl("https://youtube3").title("유튜브3").description("유튜브3 설명")
                .thumbnailUrl("thumb3").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build()
		);

        youtubePostSources = Arrays.asList(
            PostSource.builder().id(104L).post(dummyPost).source(youtubeSources.get(0)).build(),
            PostSource.builder().id(105L).post(dummyPost).source(youtubeSources.get(1)).build(),
            PostSource.builder().id(106L).post(dummyPost).source(youtubeSources.get(2)).build()
        );
	}

    @Test
    @DisplayName("뉴스 출처 10개 조회 - 성공")
    void getTop10NewsSourcesByPostId_success() {
        // given
        Platform platform = Platform.NAVER_NEWS;
        PageRequest pageRequest = PageRequest.of(0, 10);

        given(postService.getPostById(eq(postId))).willReturn(dummyPost);
        given(postSourceRepository.findAllWithSourceByPostIdAndPlatform(eq(postId), eq(platform), eq(pageRequest)))
                .willReturn(newsPostSources);

        // when
        List<SourceDto> result = sourceService.getTop10NewsSourcesByPostId(postId);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).url()).isEqualTo("https://news1");
        assertThat(result.get(1).title()).isEqualTo("뉴스2");
    }

    @Test
    @DisplayName("뉴스 출처 10개 조회 - 데이터 없음")
    void getTop10NewsSourcesByPostId_emptySourceList() {
        // given
        Platform platform = Platform.NAVER_NEWS;
        PageRequest pageRequest = PageRequest.of(0, 10);

        given(postService.getPostById(eq(postId))).willReturn(dummyPost);
        given(postSourceRepository.findAllWithSourceByPostIdAndPlatform(eq(postId), eq(platform), eq(pageRequest)))
                .willReturn(Collections.emptyList());

        // when
        List<SourceDto> result = sourceService.getTop10NewsSourcesByPostId(postId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("뉴스 출처 10개 조회 - 포스트 없음")
    void getTop10NewsSourcesByPostId_postNotFound() {
        // given
        Long postId = 999L;

        given(postService.getPostById(postId))
                .willThrow(new ServiceException("400", "해당 포스트를 찾을 수 없습니다."));

        // when & then
        assertThatThrownBy(() -> sourceService.getTop10NewsSourcesByPostId(postId))
                .isInstanceOf(ServiceException.class)
                .hasMessage("해당 포스트를 찾을 수 없습니다.");
    }


    @Test
    @DisplayName("영상 출처 10개 조회 - 성공")
    void getTop10VideoSourcesByPostId_success() {
        // given
        Platform platform = Platform.YOUTUBE;
        PageRequest pageRequest = PageRequest.of(0, 10);

        given(postService.getPostById(eq(postId))).willReturn(dummyPost);
        given(postSourceRepository.findAllWithSourceByPostIdAndPlatform(eq(postId), eq(platform), eq(pageRequest)))
                .willReturn(youtubePostSources);

        // when
        List<SourceDto> result = sourceService.getTop10VideoSourcesByPostId(postId);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).url()).isEqualTo("https://youtube1");
        assertThat(result.get(1).title()).isEqualTo("유튜브2");
    }

    @Test
    @DisplayName("키워드 검색 소스 조회 - 성공")
    void getTop10VideoSourcesByPostId_Success() {
        PostDto post1 = new PostDto(1L, "키워드", "제목1", "설명1", "url1");
        PostDto post2 = new PostDto(2L, "키워드", "제목2", "설명2", "url2");

        List<PostDto> postDtos = List.of(post1, post2);

        given(sourceRepository.findByPostIdsOrderByPublishedAtDesc(eq(List.of(1L, 2L)), any(PageRequest.class)))
                .willReturn(newsSources);

        // when
        List<SourceDto> result = sourceService.getTop5SourcesByPosts(postDtos);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).title()).isEqualTo("뉴스1");
        assertThat(result.get(1).title()).isEqualTo("뉴스2");

        verify(sourceRepository).findByPostIdsOrderByPublishedAtDesc(eq(List.of(1L, 2L)), any(PageRequest.class));
    }


    @Test
    @DisplayName("키워드 검색 소스 조회 - 빈 데이터")
    void getTop10VideoSourcesByPostId_Empty() {
        // when
        List<SourceDto> result = sourceService.getTop5SourcesByPosts(Collections.emptyList());

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(sourceRepository);
    }


    @Test
    @DisplayName("뉴스 검색 - 성공")
    void searchNews_success() {
        // given
        Long keywordId = 1L;
        String keywordText = "키워드";
        KeywordMetricHourlyDto metric = new KeywordMetricHourlyDto(keywordId, keywordText, Platform.GOOGLE_TREND, LocalDateTime.now(), 0, 0, false);
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(List.of(metric));

        NewsDto dto = NewsDto.builder()
            .title("뉴스 제목")
            .link("https://example.com/news")
            .originalLink("https://example.com/origin")
            .description("뉴스 설명")
            .pubDate(LocalDateTime.now())
            .build();

        given(newsApi.fetchNews(eq(keywordText), anyInt(), anyInt(), eq("sim")))
            .willReturn(Mono.just(List.of(dto)));

        // when
        sourceService.searchNews();

        // then
        then(sourceRepository).should().insertIgnoreAll(argThat(sources ->
            sources.size() == 1 &&
            sources.getFirst().getTitle().equals("뉴스 제목")
        ));
        then(keywordSourceRepository).should().insertIgnoreAll(argThat(ksList ->
            ksList.size() == 1 &&
            ksList.getFirst().getKeyword().getId().equals(keywordId)
        ));
        then(openGraphService).should().enrichAsync(any(Source.class));
    }

    @Test
    @DisplayName("뉴스 검색 - Empty 데이터")
    void searchNews_empty() {
        // given
        KeywordMetricHourlyDto metric = new KeywordMetricHourlyDto(1L, "키워드",
            Platform.NAVER_NEWS, LocalDateTime.now(), 0, 0, false);
        given(keywordMetricHourlyService.findHourlyMetrics())
                .willReturn(List.of(metric));

        given(newsApi.fetchNews(anyString(), anyInt(), anyInt(), anyString()))
                .willReturn(Mono.empty());

        // when
        sourceService.searchNews();

        // then
        then(sourceRepository).shouldHaveNoInteractions();
        then(keywordSourceRepository).shouldHaveNoInteractions();
        then(openGraphService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("뉴스 검색 - 에러 발생")
    void searchNews_error() {
        // given
        KeywordMetricHourlyDto metric1 = new KeywordMetricHourlyDto(1L, "실패키워드",
            Platform.NAVER_NEWS, LocalDateTime.now(), 0, 0, false);
        KeywordMetricHourlyDto metric2 = new KeywordMetricHourlyDto(2L, "정상키워드",
            Platform.NAVER_NEWS, LocalDateTime.now(), 0, 0, false);
        given(keywordMetricHourlyService.findHourlyMetrics())
                .willReturn(List.of(metric1, metric2));

        NewsDto dto = NewsDto.builder()
                .title("제목1")
                .link("http://example.com/article")
                .originalLink("http://example.com/original")
                .description("설명")
                .pubDate(LocalDateTime.of(2025, 4, 29, 12, 0))
                .build();

        given(newsApi.fetchNews(eq("실패키워드"), anyInt(), anyInt(), anyString()))
            .willReturn(Mono.error(new RetryableExternalApiException(503, "서버 오류")));

        given(newsApi.fetchNews(eq("정상키워드"), anyInt(), anyInt(), anyString()))
            .willReturn(Mono.just(List.of(dto)));

        // when
        sourceService.searchNews();

        // then
        then(sourceRepository).should().insertIgnoreAll(argThat(sources -> {
            assertThat(sources).hasSize(1);
            assertThat(sources.getFirst().getTitle()).isEqualTo("제목1");
            return true;
        }));
    }

    @Test
    @DisplayName("실시간 인기 유튜브 Source 조회 - 성공")
    void getTopYoutubeSources_Success() {

        /// given
        // Mocking할 인기 키워드 목록 생성
        List<KeywordMetricHourlyDto> mockTopKeywords = Arrays.asList(
                new KeywordMetricHourlyDto(1L, "키워드 1", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 1000, 90, false),
                new KeywordMetricHourlyDto(2L, "키워드 2", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 800, 70, false),
                new KeywordMetricHourlyDto(3L, "키워드 3", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 300, 30, false),
                new KeywordMetricHourlyDto(4L, "키워드 4", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 50, 77, false),
                new KeywordMetricHourlyDto(5L, "키워드 5", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 650, 70, false),
                new KeywordMetricHourlyDto(6L, "키워드 6", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 70, 86, false),
                new KeywordMetricHourlyDto(7L, "키워드 7", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 120, 43, false),
                new KeywordMetricHourlyDto(8L, "키워드 8", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 665, 86, false),
                new KeywordMetricHourlyDto(9L, "키워드 9", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 505, 42, false),
                new KeywordMetricHourlyDto(10L, "키워드 10", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 404, 55, false)
        );

        // Mocking할 Repository의 반환 값 (Page<TopSourceItemDto>) 생성
        List<TopSourceItemDto> mockSourceItemDtoList = Arrays.asList(
                TopSourceItemDto.builder().url("http://youtube.com/v1").title("영상제목1")
                        .thumbnailUrl("thumb1").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).score(90).build(),
                TopSourceItemDto.builder().url("http://youtube.com/v2").title("영상제목2")
                        .thumbnailUrl("thumb2").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).score(70).build(),
                TopSourceItemDto.builder().url("http://youtube.com/v3").title("영상제목3")
                        .thumbnailUrl("thumb3").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).score(30).build(),
                TopSourceItemDto.builder().url("http://youtube.com/v4").title("영상제목4")
                        .thumbnailUrl("thumb4").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).score(77).build(),
                TopSourceItemDto.builder().url("http://youtube.com/v5").title("영상제목5")
                        .thumbnailUrl("thumb5").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).score(70).build(),
                TopSourceItemDto.builder().url("http://youtube.com/v6").title("영상제목6")
                        .thumbnailUrl("thumb6").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).score(86).build(),
                TopSourceItemDto.builder().url("http://youtube.com/v7").title("영상제목7")
                        .thumbnailUrl("thumb7").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).score(43).build(),
                TopSourceItemDto.builder().url("http://youtube.com/v8").title("영상제목8")
                        .thumbnailUrl("thumb8").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).score(86).build(),
                TopSourceItemDto.builder().url("http://youtube.com/v9").title("영상제목9")
                        .thumbnailUrl("thumb9").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).score(42).build(),
                TopSourceItemDto.builder().url("http://youtube.com/v10").title("영상제목10")
                        .thumbnailUrl("thumb10").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).score(55).build()
        );

        // Service 메소드에 전달될 Pageable 객체 (컨트롤러에서 넘어올 형태)
        Pageable pageable = PageRequest.of(0, 10, Sort.by("score").descending());

        // Repository가 반환할 Mock Page<TopSourceItemDto> 객체 생성
        Page<TopSourceItemDto> mockSourceItemDtoPage = new PageImpl<>(
                mockSourceItemDtoList, pageable, 50);

        // keywordMetricHourlyService.findHourlyMetrics() 호출 시 mockTopKeywords 반환
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(mockTopKeywords);

        // findTopSourcesByKeywordIdsAndPlatformOrderedByScore()를 Mocking
        given(postSourceRepository.findTopSourcesByKeywordIdsAndPlatformOrderedByScore(
                anyList(),
                eq(Platform.YOUTUBE),
                eq(pageable)
        )).willReturn(mockSourceItemDtoPage);

        /// when
        // 테스트 대상 메소드 호출
        TopSourceListResponse result = sourceService.getTopYoutubeSources(pageable);

        /// then
        assertThat(result).isNotNull();
        assertThat(result.list()).isNotNull().hasSize(mockTopKeywords.size());
        assertThat(result.meta()).isNotNull();

        // list 안의 TopSourceItemDto 객체들이 올바르게 변환되었는지 검증
        assertThat(result.list().getFirst().url())
                .isEqualTo(mockSourceItemDtoList.getFirst().url());
        assertThat(result.list().getFirst().title())
                .isEqualTo(mockSourceItemDtoList.getFirst().title());
        assertThat(result.list().getFirst().thumbnailUrl())
                .isEqualTo(mockSourceItemDtoList.getFirst().thumbnailUrl());
        assertThat(result.list().getFirst().publishedAt())
                .isEqualTo(mockSourceItemDtoList.getFirst().publishedAt());
        assertThat(result.list().getFirst().platform())
                .isEqualTo(mockSourceItemDtoList.getFirst().platform());
        assertThat(result.list().getFirst().score())
                .isEqualTo(mockSourceItemDtoList.getFirst().score());

        // Mock SourceList의 두 번째 항목도 검증
        assertThat(result.list().get(1).url())
                .isEqualTo(mockSourceItemDtoList.get(1).url());
        assertThat(result.list().get(1).title())
                .isEqualTo(mockSourceItemDtoList.get(1).title());
        assertThat(result.list().get(1).thumbnailUrl())
                .isEqualTo(mockSourceItemDtoList.get(1).thumbnailUrl());
        assertThat(result.list().get(1).publishedAt())
                .isEqualTo(mockSourceItemDtoList.get(1).publishedAt());
        assertThat(result.list().get(1).platform())
                .isEqualTo(mockSourceItemDtoList.get(1).platform());

        // meta 정보가 Mock Page에서 올바르게 변환되었는지 검증
        assertThat(result.meta().page()).isEqualTo(mockSourceItemDtoPage.getNumber());
        assertThat(result.meta().size()).isEqualTo(mockSourceItemDtoPage.getSize());
        assertThat(result.meta().totalElements()).isEqualTo(mockSourceItemDtoPage.getTotalElements());
        assertThat(result.meta().totalPages()).isEqualTo(mockSourceItemDtoPage.getTotalPages());
        assertThat(result.meta().hasNext()).isEqualTo(mockSourceItemDtoPage.hasNext());
        assertThat(result.meta().hasPrevious()).isEqualTo(mockSourceItemDtoPage.hasPrevious());

        // Service 메소드가 의존하는 다른 메소드들을 올바르게 호출했는지 검증
        verify(keywordMetricHourlyService, times(1))
                .findHourlyMetrics();
        verify(postSourceRepository, times(1))
                .findTopSourcesByKeywordIdsAndPlatformOrderedByScore(
                        anyList(),
                        eq(Platform.YOUTUBE),
                        eq(pageable)
                );
    }

    @Test
    @DisplayName("실시간 인기 유튜브 Source 조회 - 인기 키워드 목록이 비어있을 때")
    void getTopYoutubeSources_EmptyKeywords() {

        /// given
        // keywordMetricHourlyService.findHourlyMetrics() 호출 시 빈 리스트 반환하도록 Mocking
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(Collections.emptyList());

        // Service 메소드에 전달될 Pageable 객체
        Pageable pageable = PageRequest.of(0, 10, Sort.by("score").descending());

        /// when
        // 테스트 대상 메소드 호출
        TopSourceListResponse result = sourceService.getTopYoutubeSources(pageable);

        /// then
        assertThat(result).isNotNull();
        assertThat(result.list()).isNotNull().isEmpty();

        // Page.empty(pageable)이 생성하는 Page 객체의 메타 정보를 기준으로 검증
        Page<TopSourceItemDto> emptyPage = Page.empty(pageable);

        assertThat(result.meta()).isNotNull();
        assertThat(result.meta().page()).isEqualTo(emptyPage.getNumber());
        assertThat(result.meta().size()).isEqualTo(emptyPage.getSize());
        assertThat(result.meta().totalElements()).isEqualTo(emptyPage.getTotalElements());
        assertThat(result.meta().totalPages()).isEqualTo(emptyPage.getTotalPages());
        assertThat(result.meta().hasNext()).isEqualTo(emptyPage.hasNext());
        assertThat(result.meta().hasPrevious()).isEqualTo(emptyPage.hasPrevious());

        // Service가 Repository 메소드를 호출하지 않았는지 검증
        verify(postSourceRepository, never()).findSourcesByTopKeywordIdsAndPlatform(
                anyList(),
                any(Platform.class),
                any(Pageable.class)
        );

        // keywordMetricHourlyService.findHourlyMetrics()는 1번 호출되었는지 검증
        verify(keywordMetricHourlyService, times(1)).findHourlyMetrics();
    }

    @Test
    @DisplayName("실시간 인기 유튜브 Source 조회 - Repository가 빈 Page를 반환할 때")
    void getTopYoutubeSources_RepositoryReturnsEmptyPage() {

        /// given
        // Mocking할 인기 키워드 목록 생성
        List<KeywordMetricHourlyDto> mockTopKeywords = List.of(
                new KeywordMetricHourlyDto(1L, "키워드 1", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 1000, 90, false)
        );

        // Mocking할 Repository의 반환 값 (빈 Page<TopSourceItemDto>) 생성
        List<TopSourceItemDto> mockEmptySourceList = Collections.emptyList();
        Pageable pageable = PageRequest.of(
                0, 10, Sort.by("score").descending());

        // Repository가 반환할 Mock 빈 Page<TopSourceItemDto> 객체 생성
        Page<TopSourceItemDto> mockEmptySourceItemDtoPage = new PageImpl<>(
                mockEmptySourceList, pageable, 0);

        // keywordMetricHourlyService.findHourlyMetrics() 호출 시 비어있지 않은 목록 반환
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(mockTopKeywords);

        // findTopSourcesByKeywordIdsAndPlatformOrderedByScore()를 Mocking
        given(postSourceRepository.findTopSourcesByKeywordIdsAndPlatformOrderedByScore(
                anyList(),
                eq(Platform.YOUTUBE),
                eq(pageable)
        )).willReturn(mockEmptySourceItemDtoPage);

        /// when
        // 테스트 대상 메소드 호출
        TopSourceListResponse result = sourceService.getTopYoutubeSources(pageable);

        /// then
        assertThat(result).isNotNull();
        assertThat(result.list()).isNotNull().isEmpty();
        assertThat(result.meta()).isNotNull();
        assertThat(result.meta().page()).isEqualTo(mockEmptySourceItemDtoPage.getNumber());
        assertThat(result.meta().size()).isEqualTo(mockEmptySourceItemDtoPage.getSize());
        assertThat(result.meta().totalElements()).isEqualTo(mockEmptySourceItemDtoPage.getTotalElements());
        assertThat(result.meta().totalPages()).isEqualTo(mockEmptySourceItemDtoPage.getTotalPages());
        assertThat(result.meta().hasNext()).isEqualTo(mockEmptySourceItemDtoPage.hasNext());
        assertThat(result.meta().hasPrevious()).isEqualTo(mockEmptySourceItemDtoPage.hasPrevious());

        // Service가 Repository 메소드를 호출했는지 검증 (인기 키워드가 있으므로 호출되어야 함)
        verify(postSourceRepository, times(1))
                .findTopSourcesByKeywordIdsAndPlatformOrderedByScore(
                        anyList(),
                        eq(Platform.YOUTUBE),
                        eq(pageable)
                );

        // keywordMetricHourlyService.findHourlyMetrics()는 1번 호출되었는지 검증
        verify(keywordMetricHourlyService, times(1)).findHourlyMetrics();
    }

    @Test
    @DisplayName("실시간 인기 네이버 뉴스 Source 조회 - 성공")
    void getTopNaverNewsSources_Success() {

        /// given
        // Mocking할 인기 키워드 목록 생성
        List<KeywordMetricHourlyDto> mockTopKeywords = Arrays.asList(
                new KeywordMetricHourlyDto(1L, "키워드 1", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 1000, 90, false),
                new KeywordMetricHourlyDto(2L, "키워드 2", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 800, 70, false),
                new KeywordMetricHourlyDto(3L, "키워드 3", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 300, 30, false),
                new KeywordMetricHourlyDto(4L, "키워드 4", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 50, 77, false),
                new KeywordMetricHourlyDto(5L, "키워드 5", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 650, 70, false),
                new KeywordMetricHourlyDto(6L, "키워드 6", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 70, 86, false),
                new KeywordMetricHourlyDto(7L, "키워드 7", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 120, 43, false),
                new KeywordMetricHourlyDto(8L, "키워드 8", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 665, 86, false),
                new KeywordMetricHourlyDto(9L, "키워드 9", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 505, 42, false),
                new KeywordMetricHourlyDto(10L, "키워드 10", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 404, 55, false)
        );

        // Mocking할 Repository의 반환 값 (Page<TopSourceItemDto>) 생성
        List<TopSourceItemDto> mockSourceItemDtoList = Arrays.asList(
                TopSourceItemDto.builder().url("http://news.naver.com/article/1").title("뉴스제목1")
                        .thumbnailUrl("thumb1").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).score(90).build(),
                TopSourceItemDto.builder().url("http://news.naver.com/article/2").title("뉴스제목2")
                        .thumbnailUrl("thumb2").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).score(70).build(),
                TopSourceItemDto.builder().url("http://news.naver.com/article/3").title("뉴스제목3")
                        .thumbnailUrl("thumb3").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).score(30).build(),
                TopSourceItemDto.builder().url("http://news.naver.com/article/4").title("뉴스제목4")
                        .thumbnailUrl("thumb4").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).score(77).build(),
                TopSourceItemDto.builder().url("http://news.naver.com/article/5").title("뉴스제목5")
                        .thumbnailUrl("thumb5").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).score(70).build(),
                TopSourceItemDto.builder().url("http://news.naver.com/article/6").title("뉴스제목6")
                        .thumbnailUrl("thumb6").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).score(86).build(),
                TopSourceItemDto.builder().url("http://news.naver.com/article/7").title("뉴스제목7")
                        .thumbnailUrl("thumb7").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).score(43).build(),
                TopSourceItemDto.builder().url("http://news.naver.com/article/8").title("뉴스제목8")
                        .thumbnailUrl("thumb8").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).score(86).build(),
                TopSourceItemDto.builder().url("http://news.naver.com/article/9").title("뉴스제목9")
                        .thumbnailUrl("thumb9").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).score(42).build(),
                TopSourceItemDto.builder().url("http://news.naver.com/article/10").title("뉴스제목10")
                        .thumbnailUrl("thumb10").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).score(55).build()
        );

        // Service 메소드에 전달될 Pageable 객체 (컨트롤러에서 넘어올 형태)
        Pageable pageable = PageRequest.of(0, 10, Sort.by("score").descending());

        // Repository가 반환할 Mock Page<TopSourceItemDto> 객체 생성
        Page<TopSourceItemDto> mockSourceItemDtoPage = new PageImpl<>(
                mockSourceItemDtoList, pageable, 50);

        // keywordMetricHourlyService.findHourlyMetrics() 호출 시 mockTopKeywords 반환
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(mockTopKeywords);

        // findTopSourcesByKeywordIdsAndPlatformOrderedByScore()를 Mocking
        given(postSourceRepository.findTopSourcesByKeywordIdsAndPlatformOrderedByScore(
                anyList(),
                eq(Platform.NAVER_NEWS),
                eq(pageable)
        )).willReturn(mockSourceItemDtoPage);

        /// when
        // 테스트 대상 메소드 호출
        TopSourceListResponse result = sourceService.getTopNaverNewsSources(pageable);

        /// then
        assertThat(result).isNotNull();
        assertThat(result.list()).isNotNull().hasSize(mockTopKeywords.size());
        assertThat(result.meta()).isNotNull();

        // list 안의 TopSourceItemDto 객체들이 올바르게 변환되었는지 검증
        assertThat(result.list().getFirst().url())
                .isEqualTo(mockSourceItemDtoList.getFirst().url());
        assertThat(result.list().getFirst().title())
                .isEqualTo(mockSourceItemDtoList.getFirst().title());
        assertThat(result.list().getFirst().thumbnailUrl())
                .isEqualTo(mockSourceItemDtoList.getFirst().thumbnailUrl());
        assertThat(result.list().getFirst().publishedAt())
                .isEqualTo(mockSourceItemDtoList.getFirst().publishedAt());
        assertThat(result.list().getFirst().platform())
                .isEqualTo(mockSourceItemDtoList.getFirst().platform());
        assertThat(result.list().getFirst().score())
                .isEqualTo(mockSourceItemDtoList.getFirst().score());

        // Mock SourceList의 두 번째 항목도 검증
        assertThat(result.list().get(1).url())
                .isEqualTo(mockSourceItemDtoList.get(1).url());
        assertThat(result.list().get(1).title())
                .isEqualTo(mockSourceItemDtoList.get(1).title());
        assertThat(result.list().get(1).thumbnailUrl())
                .isEqualTo(mockSourceItemDtoList.get(1).thumbnailUrl());
        assertThat(result.list().get(1).publishedAt())
                .isEqualTo(mockSourceItemDtoList.get(1).publishedAt());
        assertThat(result.list().get(1).platform())
                .isEqualTo(mockSourceItemDtoList.get(1).platform());
        assertThat(result.list().get(1).score())
                .isEqualTo(mockSourceItemDtoList.get(1).score());

        // meta 정보가 Mock Page에서 올바르게 변환되었는지 검증
        assertThat(result.meta().page()).isEqualTo(mockSourceItemDtoPage.getNumber());
        assertThat(result.meta().size()).isEqualTo(mockSourceItemDtoPage.getSize());
        assertThat(result.meta().totalElements()).isEqualTo(mockSourceItemDtoPage.getTotalElements());
        assertThat(result.meta().totalPages()).isEqualTo(mockSourceItemDtoPage.getTotalPages());
        assertThat(result.meta().hasNext()).isEqualTo(mockSourceItemDtoPage.hasNext());
        assertThat(result.meta().hasPrevious()).isEqualTo(mockSourceItemDtoPage.hasPrevious());

        // Service 메소드가 의존하는 다른 메소드들을 올바르게 호출했는지 검증
        verify(keywordMetricHourlyService, times(1))
                .findHourlyMetrics();
        verify(postSourceRepository, times(1))
                .findTopSourcesByKeywordIdsAndPlatformOrderedByScore(
                        anyList(),
                        eq(Platform.NAVER_NEWS),
                        eq(pageable)
                );
    }

    @Test
    @DisplayName("실시간 인기 네이버 뉴스 Source 조회 - 인기 키워드 목록이 비어있을 때")
    void getTopNaverNewsSources_EmptyKeywords() {

        /// given
        // keywordMetricHourlyService.findHourlyMetrics() 호출 시 빈 리스트 반환하도록 Mocking
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(Collections.emptyList());

        // Service 메소드에 전달될 Pageable 객체
        Pageable pageable = PageRequest.of(0, 10, Sort.by("score").descending());

        /// when
        // 테스트 대상 메소드 호출
        TopSourceListResponse result = sourceService.getTopNaverNewsSources(pageable);

        /// then
        assertThat(result).isNotNull();
        assertThat(result.list()).isNotNull().isEmpty();

        // Page.empty(pageable)이 생성하는 Page 객체의 메타 정보를 기준으로 검증
        Page<TopSourceItemDto> emptyPage = Page.empty(pageable);

        assertThat(result.meta()).isNotNull();
        assertThat(result.meta().page()).isEqualTo(emptyPage.getNumber());
        assertThat(result.meta().size()).isEqualTo(emptyPage.getSize());
        assertThat(result.meta().totalElements()).isEqualTo(emptyPage.getTotalElements());
        assertThat(result.meta().totalPages()).isEqualTo(emptyPage.getTotalPages());
        assertThat(result.meta().hasNext()).isEqualTo(emptyPage.hasNext());
        assertThat(result.meta().hasPrevious()).isEqualTo(emptyPage.hasPrevious());

        // Service가 Repository 메소드를 호출하지 않았는지 검증
        verify(postSourceRepository, never()).findSourcesByTopKeywordIdsAndPlatform(
                anyList(),
                any(Platform.class),
                any(Pageable.class)
        );

        // keywordMetricHourlyService.findHourlyMetrics()는 1번 호출되었는지 검증
        verify(keywordMetricHourlyService, times(1)).findHourlyMetrics();
    }

    @Test
    @DisplayName("실시간 인기 네이버 뉴스 Source 조회 - Repository가 빈 Page를 반환할 때")
    void getTopNaverNewsSources_RepositoryReturnsEmptyPage() {

        /// given
        // Mocking할 인기 키워드 목록 생성
        List<KeywordMetricHourlyDto> mockTopKeywords = List.of(
                new KeywordMetricHourlyDto(1L, "키워드 1", Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 1000, 90, false)
        );

        // Mocking할 Repository의 반환 값 (빈 Page<TopSourceItemDto>) 생성
        List<TopSourceItemDto> mockEmptySourceItemDtoList = Collections.emptyList();
        Pageable pageable = PageRequest.of(
                0, 10, Sort.by("score").descending());

        // Repository가 반환할 Mock 빈 Page<Source> 객체 생성 (내용은 비어있고, 전체 개수는 0)
        Page<TopSourceItemDto> mockEmptySourceItemDtoPage = new PageImpl<>(
                mockEmptySourceItemDtoList,pageable, 0);

        // keywordMetricHourlyService.findHourlyMetrics() 호출 시 비어있지 않은 목록 반환
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(mockTopKeywords);

        // findTopSourcesByKeywordIdsAndPlatformOrderedByScore()를 Mocking
        given(postSourceRepository.findTopSourcesByKeywordIdsAndPlatformOrderedByScore(
                anyList(),
                eq(Platform.NAVER_NEWS),
                eq(pageable)
        )).willReturn(mockEmptySourceItemDtoPage);

        /// when
        // 테스트 대상 메소드 호출
        TopSourceListResponse result = sourceService.getTopNaverNewsSources(pageable);

        /// then
        assertThat(result).isNotNull();
        assertThat(result.list()).isNotNull().isEmpty();
        assertThat(result.meta()).isNotNull();
        assertThat(result.meta().page()).isEqualTo(mockEmptySourceItemDtoPage.getNumber());
        assertThat(result.meta().size()).isEqualTo(mockEmptySourceItemDtoPage.getSize());
        assertThat(result.meta().totalElements()).isEqualTo(mockEmptySourceItemDtoPage.getTotalElements());
        assertThat(result.meta().totalPages()).isEqualTo(mockEmptySourceItemDtoPage.getTotalPages());
        assertThat(result.meta().hasNext()).isEqualTo(mockEmptySourceItemDtoPage.hasNext());
        assertThat(result.meta().hasPrevious()).isEqualTo(mockEmptySourceItemDtoPage.hasPrevious());

        // Service가 Repository 메소드를 호출했는지 검증 (인기 키워드가 있으므로 호출되어야 함)
        verify(postSourceRepository, times(1))
                .findTopSourcesByKeywordIdsAndPlatformOrderedByScore(
                        anyList(),
                        eq(Platform.NAVER_NEWS),
                        eq(pageable)
                );

        // keywordMetricHourlyService.findHourlyMetrics()는 1번 호출되었는지 검증
        verify(keywordMetricHourlyService, times(1)).findHourlyMetrics();
    }

    @Test
    @DisplayName("Youtube 검색 - 성공")
    void searchYoutube_success() {
        /// given
        Long keywordId1 = 1L;
        String keywordText1 = "키워드1";
        Long keywordId2 = 2L;
        String keywordText2 = "키워드2";

        // Mock: keywordMetricHourlyService.findHourlyMetrics()가 키워드 목록을 반환하도록 설정
        List<KeywordMetricHourlyDto> topKeywords = Arrays.asList(
                new KeywordMetricHourlyDto(keywordId1, keywordText1, Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 0, 0, false),
                new KeywordMetricHourlyDto(keywordId2, keywordText2, Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 0, 0, false)
        );

        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(topKeywords);

        // videoApi.fetchVideos()가 각 키워드에 대해 VideoDto 목록을 담은 Mono를 반환하도록 설정
        // 키워드1에 대한 응답
        List<VideoDto> video1 = Arrays.asList(
                VideoDto.builder().id("v1_id_k1").title("영상1 제목 k1").publishedAt(
                        LocalDateTime.now().minusDays(1)).thumbnailUrl("thumb1_k1")
                        .description("desc1_k1").build(),
                VideoDto.builder().id("v2_id_k1").title("영상2 제목 k1").publishedAt(
                        LocalDateTime.now().minusDays(2)).thumbnailUrl("thumb2_k1")
                        .description("desc2_k1").build()
        );

        given(videoApi.fetchVideos(eq(keywordText1), anyInt()))
                .willReturn(Mono.just(video1));

        // 키워드2에 대한 응답 (일부러 중복되는 영상 포함)
        List<VideoDto> video2 = Arrays.asList(
                VideoDto.builder().id("v3_id_k2").title("영상3 제목 k2").publishedAt(
                        LocalDateTime.now().minusDays(3)).thumbnailUrl("thumb3_k2")
                        .description("desc3_k2").build(),
                // 중복 영상
                VideoDto.builder().id("v2_id_k1").title("영상2 제목 k1").publishedAt(
                        LocalDateTime.now().minusDays(2)).thumbnailUrl("thumb2_k1")
                        .description("desc2_k1").build()
        );

        given(videoApi.fetchVideos(eq(keywordText2), anyInt()))
                .willReturn(Mono.just(video2));

        /// when
        // searchYoutube 메소드 실행
        sourceService.searchYoutube();

        /// then
        // 1. videoApi.fetchVideos가 각 키워드에 대해 호출되었는지 검증
        then(videoApi).should().fetchVideos(eq(keywordText1), anyInt());
        then(videoApi).should().fetchVideos(eq(keywordText2), anyInt());

        // 2. sourceRepository.insertIgnoreAll 호출 검증
        // 예상되는 Source 리스트: videos1과 videos2의 모든 VideoDto를 Source로 변환 후 중복 제거된 리스트
        List<Source> expextedSources = new ArrayList<>();

        // VideoDto의 toEntity 메소드가 정확한 Source 객체를 생성하는지 확인 필요
        expextedSources.add(video1.get(0).toEntity(Platform.YOUTUBE));
        expextedSources.add(video1.get(1).toEntity(Platform.YOUTUBE));
        expextedSources.add(video2.get(0).toEntity(Platform.YOUTUBE));

        // argThat을 사용하여 전달된 리스트의 크기와 내용 검증
        then(sourceRepository).should().insertIgnoreAll(argThat(sources -> {
            assertThat(sources).hasSize(3);

            // Source 엔티티의 equals/hashCode 구현이 fingerprint 기반인지 확인 필요
            List<String> fingerprints = sources.stream()
                    .map(Source::getFingerprint).toList();
            assertThat(fingerprints).containsExactlyInAnyOrder(
                    video1.get(0).toEntity(Platform.YOUTUBE).getFingerprint(),
                    video1.get(1).toEntity(Platform.YOUTUBE).getFingerprint(),
                    video2.get(0).toEntity(Platform.YOUTUBE).getFingerprint()
            );
            return true;
        }));

        // 3. keywordSourceRepository.insertIgnoreAll 호출 검증
        then(keywordSourceRepository).should().insertIgnoreAll(argThat(ksList -> {
            assertThat(ksList).hasSize(4);

            // 리스트에 특정 Keyword ID와 Source fingerprint 조합을 가진 KeywordSource가 포함되어 있는지 확인
            List<String> ksCominations = ksList.stream()
                    .map(ks -> ks.getKeyword().getId() + "-" +
                            ks.getSource().getFingerprint())
                    .toList();
            assertThat(ksCominations).containsExactlyInAnyOrder(
                    keywordId1 + "-" + video1.get(0).toEntity(Platform.YOUTUBE).getFingerprint(),
                    keywordId1 + "-" + video1.get(1).toEntity(Platform.YOUTUBE).getFingerprint(),
                    keywordId2 + "-" + video2.get(0).toEntity(Platform.YOUTUBE).getFingerprint(),
                    keywordId2 + "-" + video2.get(1).toEntity(Platform.YOUTUBE).getFingerprint()
            );
            return true;
        }));

        // 4. openGraphService.enrichAsync 호출 검증
        // distinctSources 리스트의 각 Source에 대해 호출되었는지 검증
        then(openGraphService).should(times(3))
                .enrichAsync(any(Source.class));
    }

    @Test
    @DisplayName("Youtube 검색 - API 응답 비어있음")
    void searchYoutube_emptyApiResponse() {
        /// given
        Long keywordId1 = 1L;
        String keywordText = "빈응답키워드";
        KeywordMetricHourlyDto metric = new KeywordMetricHourlyDto(keywordId1, keywordText,
                Platform.GOOGLE_TREND, LocalDateTime.now(), 0, 0, false);
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(List.of(metric));

        // videoApi.fetchVideos()가 빈 목록을 담은 Mono를 반환하도록 설정
        given(videoApi.fetchVideos(eq(keywordText), anyInt())).willReturn(Mono.just(List.of()));

        /// when
        sourceService.searchYoutube();

        /// then
        // insertIgnoreAll 메소드들이 호출되지 않았는지 검증
        then(sourceRepository).shouldHaveNoInteractions();
        then(keywordSourceRepository).shouldHaveNoInteractions();
        // API 응답 비어있으면 OpenGraph도 호출 안됨
        then(openGraphService).shouldHaveNoInteractions();

        // keywordMetricHourlyService.findHourlyMetrics()는 호출되었는지 검증
        then(keywordMetricHourlyService).should(times(1)).findHourlyMetrics();
        // videoApi.fetchVideos()도 호출되었는지 검증
        then(videoApi).should(times(1)).fetchVideos(eq(keywordText), anyInt());
    }

    @Test
    @DisplayName("Youtube 검색 - API 호출 에러 발생")
    void searchYoutube_apiError() {
        /// given
        Long keywordId1 = 1L;
        String keywordText1 = "에러키워드";
        Long keywordId2 = 2L;
        String keywordText2 = "정상키워드";

        List<KeywordMetricHourlyDto> topKeywords = Arrays.asList(
                new KeywordMetricHourlyDto(keywordId1, keywordText1, Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 0, 0, false),
                new KeywordMetricHourlyDto(keywordId2, keywordText2, Platform.GOOGLE_TREND,
                        LocalDateTime.now(), 0, 0, false)
        );
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(topKeywords);

        // 키워드1에 대해 API 호출 에러 발생 설정
        given(videoApi.fetchVideos(eq(keywordText1), anyInt()))
                .willReturn(Mono.error(
                        new RetryableExternalApiException(503, "Youtube API Error")));

        // 키워드2에 대해 정상 응답 설정
        List<VideoDto> video2 = Arrays.asList(
                VideoDto.builder().id("v_id_k2").title("정상 영상 제목 k2")
                        .publishedAt(LocalDateTime.now().minusDays(1))
                        .thumbnailUrl("thumb_k2").description("desc_k2").build()
        );
        given(videoApi.fetchVideos(eq(keywordText2), anyInt())).willReturn(Mono.just(video2));

        /// when
        sourceService.searchYoutube();

        /// then
        // keywordMetricHourlyService.findHourlyMetrics() 호출 검증
        then(keywordMetricHourlyService).should(times(1)).findHourlyMetrics();

        // videoApi.fetchVideos()가 각 키워드에 대해 호출되었는지 검증
        then(videoApi).should().fetchVideos(eq(keywordText1), anyInt());
        then(videoApi).should().fetchVideos(eq(keywordText2), anyInt());

        // 에러 발생한 키워드는 건너뛰고, 정상 키워드에 대한 데이터만 처리되었는지 검증
        then(sourceRepository).should().insertIgnoreAll(argThat(sources -> {
            assertThat(sources).hasSize(1);
            assertThat(sources.getFirst().getFingerprint()).isEqualTo(video2.getFirst()
                    .toEntity(Platform.YOUTUBE).getFingerprint());
            return true;
        }));

        then(keywordSourceRepository).should().insertIgnoreAll(argThat(ksList -> {
            assertThat(ksList).hasSize(1);
            assertThat(ksList.getFirst().getKeyword().getId()).isEqualTo(keywordId2);
            assertThat(ksList.getFirst().getSource().getFingerprint()).isEqualTo(
                    video2.getFirst().toEntity(Platform.YOUTUBE).getFingerprint());
            return true;
        }));

        // OpenGraphService는 정상 처리된 Source 개수만큼 호출 예상 (1개)
        then(openGraphService).should(times(1)).enrichAsync(any(Source.class));
    }
}