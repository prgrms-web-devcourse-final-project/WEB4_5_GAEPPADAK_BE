package site.kkokkio.domain.source.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyInt;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import reactor.core.publisher.Mono;
import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyDto;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.controller.dto.TopSourceListResponse;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.dto.TopSourceItemDto;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.port.out.NewsApiPort;
import site.kkokkio.domain.source.repository.PostSourceRepository;
import site.kkokkio.domain.source.repository.SourceRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
class SourceServiceTest {

    @InjectMocks
    private SourceService sourceService;

    @Mock
    private PostService postService;

    @Mock
    private PostSourceRepository postSourceRepository;

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private NewsApiPort newsApi;

    @Mock
    private KeywordMetricHourlyService keywordMetricHourlyService;


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


    //TODO(2hwayoung): fix
    // @Test
    // @DisplayName("뉴스 검색 - 성공")
    // void searchNews_success() {
    //     // given
    //     KeywordMetricHourlyDto metric = new KeywordMetricHourlyDto(1L, "키워드",
    //         Platform.NAVER_NEWS, LocalDateTime.of(2025, 4, 29, 12, 0), 0, 0, false);
    //     given(keywordMetricHourlyService.findHourlyMetrics())
    //             .willReturn(List.of(metric));
    //
    //     NewsDto dto = NewsDto.builder()
    //             .title("제목")
    //             .link("http://example.com/article")
    //             .originalLink("http://example.com/original")
    //             .description("설명")
    //             .pubDate(LocalDateTime.of(2025, 4, 29, 12, 0))
    //             .build();
    //     given(newsApi.fetchNews(eq("키워드"), anyInt(), anyInt(), anyString()))
    //             .willReturn(Mono.just(List.of(dto)));
    //
    //     // when
    //     sourceService.searchNews();
    //
    //     // then
    //     then(sourceRepository).should().saveAll(argThat(sources -> {
    //         // 검사 로직
    //         assertThat(sources).hasSize(1);
    //         Source src = sources.iterator().next();
    //         assertThat(src.getTitle()).isEqualTo("제목");
    //         assertThat(src.getNormalizedUrl()).isEqualTo("http://example.com/article");
    //         assertThat(src.getPlatform()).isEqualTo(Platform.NAVER_NEWS);
    //         assertThat(src.getDescription()).isEqualTo("설명");
    //         assertThat(src.getPublishedAt()).isEqualTo(LocalDateTime.of(2025, 4, 29, 12, 0));
    //         assertThat(src.getFingerprint()).isNotBlank();
    //         return true;
    //     }));
    // }

    @Test
    @DisplayName("뉴스 검색 - Empty 데이터")
    void searchNews_empty() {
        // given
        KeywordMetricHourlyDto metric = new KeywordMetricHourlyDto(1L, "키워드",
            Platform.NAVER_NEWS, LocalDateTime.of(2025, 4, 29, 12, 0), 0, 0, false);
        given(keywordMetricHourlyService.findHourlyMetrics())
                .willReturn(List.of(metric));

        given(newsApi.fetchNews(anyString(), anyInt(), anyInt(), anyString()))
                .willReturn(Mono.empty());

        // when
        sourceService.searchNews();

        // then
        then(sourceRepository).should(never()).saveAll(any());}



    //TODO(2hwayoung): fix
    // @Test
    // @DisplayName("뉴스 검색 - 에러 발생")
    // void searchNews_error() {
    //     // given
    //     KeywordMetricHourlyDto metric1 = new KeywordMetricHourlyDto(1L, "실패키워드",
    //         Platform.NAVER_NEWS, LocalDateTime.of(2025, 4, 29, 12, 0), 0, 0, false);
    //     KeywordMetricHourlyDto metric2 = new KeywordMetricHourlyDto(2L, "정상키워드",
    //         Platform.NAVER_NEWS, LocalDateTime.of(2025, 4, 29, 12, 0), 0, 0, false);
    //     given(keywordMetricHourlyService.findHourlyMetrics())
    //             .willReturn(List.of(metric1, metric2));
    //
    //     NewsDto dto = NewsDto.builder()
    //             .title("제목1")
    //             .link("http://example.com/article")
    //             .originalLink("http://example.com/original")
    //             .description("설명")
    //             .pubDate(LocalDateTime.of(2025, 4, 29, 12, 0))
    //             .build();
    //
    //     given(newsApi.fetchNews(eq("실패키워드"), anyInt(), anyInt(), anyString()))
    //         .willReturn(Mono.error(new RetryableExternalApiException(503, "서버 오류")));
    //
    //     given(newsApi.fetchNews(eq("정상키워드"), anyInt(), anyInt(), anyString()))
    //         .willReturn(Mono.just(List.of(dto)));
    //
    //     // when
    //     sourceService.searchNews();
    //
    //     // then
    //     then(sourceRepository).should().saveAll(argThat(sources -> {
    //         assertThat(sources).hasSize(1);
    //         assertThat(sources.iterator().next().getTitle()).isEqualTo("제목1");
    //         return true;
    //     }));
    // }

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

        // Mocking할 Repository의 반환 값 생성
        List<Source> mockSourceList = Arrays.asList(
                Source.builder().fingerprint("fp1").normalizedUrl("http://youtube.com/v1").title("영상제목1")
                        .thumbnailUrl("thumb1").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).build(),
                Source.builder().fingerprint("fp2").normalizedUrl("http://youtube.com/v2").title("영상제목2")
                        .thumbnailUrl("thumb2").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).build(),
                Source.builder().fingerprint("fp3").normalizedUrl("http://youtube.com/v3").title("영상제목3")
                        .thumbnailUrl("thumb3").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).build(),
                Source.builder().fingerprint("fp4").normalizedUrl("http://youtube.com/v4").title("영상제목4")
                        .thumbnailUrl("thumb4").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).build(),
                Source.builder().fingerprint("fp5").normalizedUrl("http://youtube.com/v5").title("영상제목5")
                        .thumbnailUrl("thumb5").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).build(),
                Source.builder().fingerprint("fp6").normalizedUrl("http://youtube.com/v6").title("영상제목6")
                        .thumbnailUrl("thumb6").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).build(),
                Source.builder().fingerprint("fp7").normalizedUrl("http://youtube.com/v7").title("영상제목7")
                        .thumbnailUrl("thumb7").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).build(),
                Source.builder().fingerprint("fp8").normalizedUrl("http://youtube.com/v8").title("영상제목8")
                        .thumbnailUrl("thumb8").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).build(),
                Source.builder().fingerprint("fp9").normalizedUrl("http://youtube.com/v9").title("영상제목9")
                        .thumbnailUrl("thumb9").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).build(),
                Source.builder().fingerprint("fp10").normalizedUrl("http://youtube.com/v10").title("영상제목10")
                        .thumbnailUrl("thumb10").publishedAt(LocalDateTime.now()).platform(Platform.YOUTUBE).build()
        );

        // Service 메소드에 전달될 Pageable 객체 (컨트롤러에서 넘어올 형태)
        Pageable pageable = PageRequest.of(0, 10, Sort.by("score").descending());

        // Repository가 반환할 Mock Page<Source> 객체 생성
        Page<Source> mockSourcePage = new PageImpl<>(mockSourceList, pageable, 50);

        // keywordMetricHourlyService.findHourlyMetrics() 호출 시 mockTopKeywords 반환
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(mockTopKeywords);

        // postSourceRepository.findSourcesByTopKeywordIdsAndPlatform() 호출 시 mockSourcePage 반환
        given(postSourceRepository.findSourcesByTopKeywordIdsAndPlatform(
                anyList(),
                eq(Platform.YOUTUBE),
                eq(pageable)
        )).willReturn(mockSourcePage);

        /// when
        // 테스트 대상 메소드 호출
        TopSourceListResponse result = sourceService.getTopYoutubeSources(pageable);

        /// then
        assertThat(result).isNotNull();
        assertThat(result.list()).isNotNull().hasSize(mockTopKeywords.size());
        assertThat(result.meta()).isNotNull();

        // list 안의 TopSourceItemDto 객체들이 올바르게 변환되었는지 검증
        assertThat(result.list().getFirst().url())
                .isEqualTo(mockSourceList.getFirst().getNormalizedUrl());
        assertThat(result.list().getFirst().title())
                .isEqualTo(mockSourceList.getFirst().getTitle());
        assertThat(result.list().getFirst().thumbnailUrl())
                .isEqualTo(mockSourceList.getFirst().getThumbnailUrl());
        assertThat(result.list().getFirst().publishedAt())
                .isEqualTo(mockSourceList.getFirst().getPublishedAt());
        assertThat(result.list().getFirst().platform())
                .isEqualTo(mockSourceList.getFirst().getPlatform());

        // Mock SourceList의 두 번째 항목도 검증
        assertThat(result.list().get(1).url())
                .isEqualTo(mockSourceList.get(1).getNormalizedUrl());
        assertThat(result.list().get(1).title())
                .isEqualTo(mockSourceList.get(1).getTitle());
        assertThat(result.list().get(1).thumbnailUrl())
                .isEqualTo(mockSourceList.get(1).getThumbnailUrl());
        assertThat(result.list().get(1).publishedAt())
                .isEqualTo(mockSourceList.get(1).getPublishedAt());
        assertThat(result.list().get(1).platform())
                .isEqualTo(mockSourceList.get(1).getPlatform());

        // meta 정보가 Mock Page에서 올바르게 변환되었는지 검증
        assertThat(result.meta().page()).isEqualTo(mockSourcePage.getNumber());
        assertThat(result.meta().size()).isEqualTo(mockSourcePage.getSize());
        assertThat(result.meta().totalElements()).isEqualTo(mockSourcePage.getTotalElements());
        assertThat(result.meta().totalPages()).isEqualTo(mockSourcePage.getTotalPages());
        assertThat(result.meta().hasNext()).isEqualTo(mockSourcePage.hasNext());
        assertThat(result.meta().hasPrevious()).isEqualTo(mockSourcePage.hasPrevious());

        // Service 메소드가 의존하는 다른 메소드들을 올바르게 호출했는지 검증
        verify(keywordMetricHourlyService, times(1))
                .findHourlyMetrics();
        verify(postSourceRepository, times(1))
                .findSourcesByTopKeywordIdsAndPlatform(
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

        // Mocking할 Repository의 반환 값 (빈 Page<Source>) 생성
        List<Source> mockEmptySourceList = Collections.emptyList();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("score").descending());

        // Repository가 반환할 Mock 빈 Page<Source> 객체 생성 (내용은 비어있고, 전체 개수는 0)
        Page<Source> mockEmptySourcePage = new PageImpl<>(mockEmptySourceList, pageable, 0);

        // keywordMetricHourlyService.findHourlyMetrics() 호출 시 비어있지 않은 목록 반환
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(mockTopKeywords);

        // postSourceRepository.findSourcesByTopKeywordIdsAndPlatform() 호출 시 빈 mockEmptySourcePage 반환
        given(postSourceRepository.findSourcesByTopKeywordIdsAndPlatform(
                anyList(),
                eq(Platform.YOUTUBE),
                eq(pageable)
        )).willReturn(mockEmptySourcePage);

        /// when
        // 테스트 대상 메소드 호출
        TopSourceListResponse result = sourceService.getTopYoutubeSources(pageable);

        /// then
        assertThat(result).isNotNull();
        assertThat(result.list()).isNotNull().isEmpty();
        assertThat(result.meta()).isNotNull();
        assertThat(result.meta().page()).isEqualTo(mockEmptySourcePage.getNumber());
        assertThat(result.meta().size()).isEqualTo(mockEmptySourcePage.getSize());
        assertThat(result.meta().totalElements()).isEqualTo(mockEmptySourcePage.getTotalElements());
        assertThat(result.meta().totalPages()).isEqualTo(mockEmptySourcePage.getTotalPages());
        assertThat(result.meta().hasNext()).isEqualTo(mockEmptySourcePage.hasNext());
        assertThat(result.meta().hasPrevious()).isEqualTo(mockEmptySourcePage.hasPrevious());

        // Service가 Repository 메소드를 호출했는지 검증 (인기 키워드가 있으므로 호출되어야 함)
        verify(postSourceRepository, times(1))
                .findSourcesByTopKeywordIdsAndPlatform(
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

        // Mocking할 Repository의 반환 값 생성
        List<Source> mockSourceList = Arrays.asList(
                Source.builder().fingerprint("fp1").normalizedUrl("http://news.naver.com/article/1").title("뉴스제목1")
                        .thumbnailUrl("thumb1").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
                Source.builder().fingerprint("fp2").normalizedUrl("http://news.naver.com/article/2").title("뉴스제목2")
                        .thumbnailUrl("thumb2").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
                Source.builder().fingerprint("fp3").normalizedUrl("http://news.naver.com/article/3").title("뉴스제목3")
                        .thumbnailUrl("thumb3").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
                Source.builder().fingerprint("fp4").normalizedUrl("http://news.naver.com/article/4").title("뉴스제목4")
                        .thumbnailUrl("thumb4").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
                Source.builder().fingerprint("fp5").normalizedUrl("http://news.naver.com/article/5").title("뉴스제목5")
                        .thumbnailUrl("thumb5").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
                Source.builder().fingerprint("fp6").normalizedUrl("http://news.naver.com/article/6").title("뉴스제목6")
                        .thumbnailUrl("thumb6").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
                Source.builder().fingerprint("fp7").normalizedUrl("http://news.naver.com/article/7").title("뉴스제목7")
                        .thumbnailUrl("thumb7").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
                Source.builder().fingerprint("fp8").normalizedUrl("http://news.naver.com/article/8").title("뉴스제목8")
                        .thumbnailUrl("thumb8").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
                Source.builder().fingerprint("fp9").normalizedUrl("http://news.naver.com/article/9").title("뉴스제목9")
                        .thumbnailUrl("thumb9").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build(),
                Source.builder().fingerprint("fp10").normalizedUrl("http://news.naver.com/article/10").title("뉴스제목10")
                        .thumbnailUrl("thumb10").publishedAt(LocalDateTime.now()).platform(Platform.NAVER_NEWS).build()
        );

        // Service 메소드에 전달될 Pageable 객체 (컨트롤러에서 넘어올 형태)
        Pageable pageable = PageRequest.of(0, 10, Sort.by("score").descending());

        // Repository가 반환할 Mock Page<Source> 객체 생성
        Page<Source> mockSourcePage = new PageImpl<>(mockSourceList, pageable, 50);

        // keywordMetricHourlyService.findHourlyMetrics() 호출 시 mockTopKeywords 반환
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(mockTopKeywords);

        // postSourceRepository.findSourcesByTopKeywordIdsAndPlatform() 호출 시 mockSourcePage 반환
        given(postSourceRepository.findSourcesByTopKeywordIdsAndPlatform(
                anyList(),
                eq(Platform.NAVER_NEWS),
                eq(pageable)
        )).willReturn(mockSourcePage);

        /// when
        // 테스트 대상 메소드 호출
        TopSourceListResponse result = sourceService.getTopNaverNewsSources(pageable);

        /// then
        assertThat(result).isNotNull();
        assertThat(result.list()).isNotNull().hasSize(mockTopKeywords.size());
        assertThat(result.meta()).isNotNull();

        // list 안의 TopSourceItemDto 객체들이 올바르게 변환되었는지 검증
        assertThat(result.list().getFirst().url())
                .isEqualTo(mockSourceList.getFirst().getNormalizedUrl());
        assertThat(result.list().getFirst().title())
                .isEqualTo(mockSourceList.getFirst().getTitle());
        assertThat(result.list().getFirst().thumbnailUrl())
                .isEqualTo(mockSourceList.getFirst().getThumbnailUrl());
        assertThat(result.list().getFirst().publishedAt())
                .isEqualTo(mockSourceList.getFirst().getPublishedAt());
        assertThat(result.list().getFirst().platform())
                .isEqualTo(mockSourceList.getFirst().getPlatform());

        // Mock SourceList의 두 번째 항목도 검증
        assertThat(result.list().get(1).url())
                .isEqualTo(mockSourceList.get(1).getNormalizedUrl());
        assertThat(result.list().get(1).title())
                .isEqualTo(mockSourceList.get(1).getTitle());
        assertThat(result.list().get(1).thumbnailUrl())
                .isEqualTo(mockSourceList.get(1).getThumbnailUrl());
        assertThat(result.list().get(1).publishedAt())
                .isEqualTo(mockSourceList.get(1).getPublishedAt());
        assertThat(result.list().get(1).platform())
                .isEqualTo(mockSourceList.get(1).getPlatform());

        // meta 정보가 Mock Page에서 올바르게 변환되었는지 검증
        assertThat(result.meta().page()).isEqualTo(mockSourcePage.getNumber());
        assertThat(result.meta().size()).isEqualTo(mockSourcePage.getSize());
        assertThat(result.meta().totalElements()).isEqualTo(mockSourcePage.getTotalElements());
        assertThat(result.meta().totalPages()).isEqualTo(mockSourcePage.getTotalPages());
        assertThat(result.meta().hasNext()).isEqualTo(mockSourcePage.hasNext());
        assertThat(result.meta().hasPrevious()).isEqualTo(mockSourcePage.hasPrevious());

        // Service 메소드가 의존하는 다른 메소드들을 올바르게 호출했는지 검증
        verify(keywordMetricHourlyService, times(1))
                .findHourlyMetrics();
        verify(postSourceRepository, times(1))
                .findSourcesByTopKeywordIdsAndPlatform(
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

        // Mocking할 Repository의 반환 값 (빈 Page<Source>) 생성
        List<Source> mockEmptySourceList = Collections.emptyList();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("score").descending());

        // Repository가 반환할 Mock 빈 Page<Source> 객체 생성 (내용은 비어있고, 전체 개수는 0)
        Page<Source> mockEmptySourcePage = new PageImpl<>(mockEmptySourceList, pageable, 0);

        // keywordMetricHourlyService.findHourlyMetrics() 호출 시 비어있지 않은 목록 반환
        given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(mockTopKeywords);

        // postSourceRepository.findSourcesByTopKeywordIdsAndPlatform() 호출 시 빈 mockEmptySourcePage 반환
        given(postSourceRepository.findSourcesByTopKeywordIdsAndPlatform(
                anyList(),
                eq(Platform.NAVER_NEWS),
                eq(pageable)
        )).willReturn(mockEmptySourcePage);

        /// when
        // 테스트 대상 메소드 호출
        TopSourceListResponse result = sourceService.getTopNaverNewsSources(pageable);

        /// then
        assertThat(result).isNotNull();
        assertThat(result.list()).isNotNull().isEmpty();
        assertThat(result.meta()).isNotNull();
        assertThat(result.meta().page()).isEqualTo(mockEmptySourcePage.getNumber());
        assertThat(result.meta().size()).isEqualTo(mockEmptySourcePage.getSize());
        assertThat(result.meta().totalElements()).isEqualTo(mockEmptySourcePage.getTotalElements());
        assertThat(result.meta().totalPages()).isEqualTo(mockEmptySourcePage.getTotalPages());
        assertThat(result.meta().hasNext()).isEqualTo(mockEmptySourcePage.hasNext());
        assertThat(result.meta().hasPrevious()).isEqualTo(mockEmptySourcePage.hasPrevious());

        // Service가 Repository 메소드를 호출했는지 검증 (인기 키워드가 있으므로 호출되어야 함)
        verify(postSourceRepository, times(1))
                .findSourcesByTopKeywordIdsAndPlatform(
                        anyList(),
                        eq(Platform.NAVER_NEWS),
                        eq(pageable)
                );

        // keywordMetricHourlyService.findHourlyMetrics()는 1번 호출되었는지 검증
        verify(keywordMetricHourlyService, times(1)).findHourlyMetrics();
    }
}