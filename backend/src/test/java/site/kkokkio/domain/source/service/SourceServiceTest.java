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

import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.entity.PostSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.repository.PostSourceRepository;
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
}