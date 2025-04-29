package site.kkokkio.domain.source.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import reactor.core.publisher.Mono;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.dto.NewsDto;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.port.out.NewsApiPort;
import site.kkokkio.domain.source.repository.PostSourceRepository;
import site.kkokkio.domain.source.repository.SourceRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.infra.common.exception.RetryableExternalApiException;

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

    @Test
	@DisplayName("뉴스 출처 10개 조회 - 성공")
    void getTop10NewsSourcesByPostId_success() {
        // given
        Long postId = 1L;
        Platform platform = Platform.NAVER_NEWS;
        PageRequest pageRequest = PageRequest.of(0, 10);

		Post dummyPost = Post.builder().id(postId).build();

        Source s1 = Source.builder()
                .fingerprint("f1")
                .normalizedUrl("https://news1")
                .title("뉴스1")
                .thumbnailUrl("thumb1")
                .publishedAt(LocalDateTime.now())
                .platform(platform)
                .build();

        Source s2 = Source.builder()
                .fingerprint("f2")
                .normalizedUrl("https://news2")
                .title("뉴스2")
                .thumbnailUrl("thumb2")
                .publishedAt(LocalDateTime.now())
                .platform(platform)
                .build();

        PostSource ps1 = PostSource.builder()
                .id(101L)
                .post(dummyPost)
                .source(s1)
                .build();

        PostSource ps2 = PostSource.builder()
                .id(102L)
                .post(dummyPost)
                .source(s2)
                .build();


        given(postService.getPostById(eq(postId))).willReturn(dummyPost);
        given(postSourceRepository.findAllWithSourceByPostIdAndPlatform(eq(postId), eq(platform), eq(pageRequest)))
                .willReturn(List.of(ps1, ps2));

        // when
        List<SourceDto> result = sourceService.getTop10NewsSourcesByPostId(postId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).url()).isEqualTo("https://news1");
        assertThat(result.get(1).title()).isEqualTo("뉴스2");
    }

    @Test
	@DisplayName("뉴스 출처 10개 조회 - 데이터 없음")
    void getTop10NewsSourcesByPostId_emptySourceList() {
        // given
        Long postId = 1L;
        Platform platform = Platform.NAVER_NEWS;
        PageRequest pageRequest = PageRequest.of(0, 10);

		Post dummyPost = Post.builder().id(postId).build();

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
        Long postId = 1L;
        Platform platform = Platform.YOUTUBE;
        PageRequest pageRequest = PageRequest.of(0, 10);

		Post dummyPost = Post.builder().id(postId).build();

        Source s1 = Source.builder()
                .fingerprint("f1")
                .normalizedUrl("https://youtube1")
                .title("유튜브1")
                .thumbnailUrl("thumb1")
                .publishedAt(LocalDateTime.now())
                .platform(platform)
                .build();

        Source s2 = Source.builder()
                .fingerprint("f2")
                .normalizedUrl("https://youtube2")
                .title("유튜브2")
                .thumbnailUrl("thumb2")
                .publishedAt(LocalDateTime.now())
                .platform(platform)
                .build();

        PostSource ps1 = PostSource.builder()
                .id(101L)
                .post(dummyPost)
                .source(s1)
                .build();

        PostSource ps2 = PostSource.builder()
                .id(102L)
                .post(dummyPost)
                .source(s2)
                .build();


        given(postService.getPostById(eq(postId))).willReturn(dummyPost);
        given(postSourceRepository.findAllWithSourceByPostIdAndPlatform(eq(postId), eq(platform), eq(pageRequest)))
                .willReturn(List.of(ps1, ps2));

        // when
        List<SourceDto> result = sourceService.getTop10VideoSourcesByPostId(postId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).url()).isEqualTo("https://youtube1");
        assertThat(result.get(1).title()).isEqualTo("유튜브2");
    }


    @Test
    @DisplayName("뉴스 검색 - 성공")
    void searchNews_success() {
        // given
        NewsDto dto = NewsDto.builder()
                .title("제목")
                .link("http://example.com/article")
                .originalLink("http://example.com/original")
                .description("설명")
                .pubDate(LocalDateTime.of(2025, 4, 29, 12, 0))
                .build();
        given(newsApi.fetchNews(anyString(), anyInt(), anyInt(), anyString()))
                .willReturn(Mono.just(List.of(dto)));

        // when
        sourceService.searchNews("키워드");

        // then
        then(sourceRepository).should().saveAll(argThat(sources -> {
            // 검사 로직
            assertThat(sources).hasSize(1);
            Source src = sources.iterator().next();
            assertThat(src.getTitle()).isEqualTo("제목");
            assertThat(src.getNormalizedUrl()).isEqualTo("http://example.com/article");
            assertThat(src.getPlatform()).isEqualTo(Platform.NAVER_NEWS);
            assertThat(src.getDescription()).isEqualTo("설명");
            assertThat(src.getPublishedAt()).isEqualTo(LocalDateTime.of(2025, 4, 29, 12, 0));
            assertThat(src.getFingerprint()).isNotBlank();
            return true;
        }));
    }

    @Test
    @DisplayName("뉴스 검색 - Empty 데이터")
    void searchNews_empty() {
        // given
        given(newsApi.fetchNews(anyString(), anyInt(), anyInt(), anyString()))
                .willReturn(Mono.empty());

        // when
        sourceService.searchNews("빈키워드");

        // then
        then(sourceRepository).should(never()).saveAll(any());}


    @Test
    @DisplayName("뉴스 검색 - RetryableExternalApiException 발생")
    void searchNews_retryableApiException() {
        // given
        given(newsApi.fetchNews(anyString(), anyInt(), anyInt(), anyString()))
                .willThrow(new RetryableExternalApiException(500, "서버 오류"));

        Source fallback = Source.builder()
                .fingerprint("fallback-fp")
                .normalizedUrl("http://fallback.com")
                .title("Fallback 제목")
                .description("Fallback 설명")
                .thumbnailUrl(null)
                .publishedAt(LocalDateTime.of(2025, 4, 29, 0, 0))
                .platform(Platform.NAVER_NEWS)
                .build();

        given(sourceRepository.findLatest10ByPlatformAndKeyword(
                eq(Platform.NAVER_NEWS),
                anyString(),
                any(PageRequest.class)))
            .willReturn(List.of(fallback));

        // when
        sourceService.searchNews("키워드");

        // then
        then(sourceRepository).should().saveAll(List.of(fallback));}

    @Test
    @DisplayName("뉴스 검색 - 이외 에러 발생")
    void searchNews_error() {
        // given
        given(newsApi.fetchNews(anyString(), anyInt(), anyInt(), anyString()))
            .willThrow(new RuntimeException("알 수 없는 오류"));

        // when & then
        assertThatThrownBy(() -> sourceService.searchNews("키워드"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("알 수 없는 오류");

        then(sourceRepository).should(never()).saveAll(any());

    }

}