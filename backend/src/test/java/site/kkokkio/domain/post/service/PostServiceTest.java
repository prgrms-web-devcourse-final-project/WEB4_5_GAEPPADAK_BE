package site.kkokkio.domain.post.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;
import site.kkokkio.domain.keyword.entity.KeywordPostHourly;
import site.kkokkio.domain.keyword.entity.KeywordPostHourlyId;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.domain.keyword.repository.KeywordPostHourlyRepository;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.entity.PostKeyword;
import site.kkokkio.domain.post.repository.PostKeywordRepository;
import site.kkokkio.domain.post.repository.PostMetricHourlyRepository;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
	@InjectMocks
	private PostService postService;

	@Mock
	private PostRepository postRepository;
	@Mock
	private KeywordRepository keywordRepository;
	@Mock
	private KeywordMetricHourlyRepository keywordMetricHourlyRepository;
	@Mock
	private KeywordPostHourlyRepository keywordPostHourlyRepository;
	@Mock
	private PostKeywordRepository postKeywordRepository;
	@Mock
	private PostMetricHourlyRepository postMetricHourlyRepository;

	@Test
	@DisplayName("postId로 포스트 단건 조회 성공")
	void test1() {
		// given
		Long postId = 1L;
		Post post = Post.builder()
			.id(postId)
			.title("제목")
			.summary("요약")
			.thumbnailUrl("https://image.url")
			.bucketAt(LocalDateTime.now())
			.build();

		Keyword keyword = Keyword.builder()
			.id(100L)
			.text("테스트 키워드")
			.build();

		PostKeyword postKeyword = PostKeyword.builder()
			.post(post)
			.keyword(keyword)
			.build();

		given(postRepository.findById(postId)).willReturn(Optional.of(post));
		given(postKeywordRepository.findByPost_Id(postId)).willReturn(Optional.of(postKeyword));

		// when
		PostDto result = postService.getPostWithKeywordById(postId);

		// then
		assertThat(result.postId()).isEqualTo(postId);
		assertThat(result.keyword()).isEqualTo("테스트 키워드");
		assertThat(result.title()).isEqualTo("제목");
		assertThat(result.summary()).isEqualTo("요약");
		assertThat(result.thumbnailUrl()).isEqualTo("https://image.url");
	}

	@Test
	@DisplayName("postId로 포스트 단건 조회 실패 - 포스트 없음")
	void test2() {
		// given
		given(postRepository.findById(1L)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> postService.getPostWithKeywordById(1L))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("포스트를 불러오지 못했습니다.");
	}

	@Test
	@DisplayName("top10 키워드 포스트 조회 성공")
	void test3() {
		// given
		LocalDateTime now = LocalDateTime.of(2025, 4, 29, 15, 0); // 고정된 시간

		Keyword keyword = Keyword.builder().id(100L).text("테스트 키워드").build();
		Post post = Post.builder().id(1L).title("포스트 제목").summary("요약").bucketAt(now).build();
		KeywordMetricHourly metric = KeywordMetricHourly.builder()
			.id(new KeywordMetricHourlyId(now, Platform.GOOGLE_TREND, 100L))
			.keyword(keyword)
			.volume(100)
			.score(100)
			.build();
		KeywordPostHourly keywordPostHourly = KeywordPostHourly.builder()
			.id(new KeywordPostHourlyId(now, Platform.GOOGLE_TREND, 100L, 1L))
			.keywordMetricHourly(metric)
			.post(post)
			.build();

		given(keywordMetricHourlyRepository.findTop10ById_BucketAtOrderByScoreDesc(now)).willReturn(List.of(metric));
		given(keywordPostHourlyRepository.findById_KeywordIdAndId_BucketAt(100L, now)).willReturn(
			Optional.of(keywordPostHourly));

		// when
		List<PostDto> result = postService.getTopPostsWithKeyword();

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).keyword()).isEqualTo("테스트 키워드");
	}

	@Test
	@DisplayName("top10 키워드 포스트 조회 실패 - 포스트 없음")
	void test4() {
		// given
		given(keywordMetricHourlyRepository.findTop10ById_BucketAtOrderByScoreDesc(any()))
			.willReturn(Collections.emptyList());

		// when & then
		assertThatThrownBy(() -> postService.getTopPostsWithKeyword())
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("포스트를 불러오지 못했습니다.");
	}

	@Test
	@DisplayName("포스트 생성 성공")
	void test5() {
		// given
		Long keywordId = 100L;
		LocalDateTime bucketAt = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
		String title = "포스트 제목";
		String summary = "요약";
		String thumbnailUrl = "https://img.url";

		Keyword keyword = Keyword.builder().id(keywordId).text("테스트 키워드").build();
		KeywordMetricHourly metric = KeywordMetricHourly.builder()
			.id(new KeywordMetricHourlyId(bucketAt, Platform.GOOGLE_TREND, keywordId))
			.keyword(keyword)
			.volume(100)
			.score(100)
			.build();
		Post savedPost = Post.builder()
			.id(1L)
			.title(title)
			.summary(summary)
			.thumbnailUrl(thumbnailUrl)
			.bucketAt(bucketAt)
			.build();

		given(postRepository.save(any(Post.class))).willReturn(savedPost);
		given(keywordMetricHourlyRepository.findById(any())).willReturn(Optional.of(metric));
		given(keywordRepository.findById(keywordId)).willReturn(Optional.of(keyword));

		// when
		Post result = postService.createPost(keywordId, bucketAt, title, summary, thumbnailUrl);

		// then
		assertThat(result.getTitle()).isEqualTo(title);
	}

	@Test
	@DisplayName("포스트 생성 실패 - KeywordMetricHourly 없음")
	void test6() {
		// given
		Long keywordId = 100L;
		LocalDateTime bucketAt = LocalDateTime.now();

		given(postRepository.save(any(Post.class))).willReturn(Post.builder().id(1L).build());
		given(keywordMetricHourlyRepository.findById(any())).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> postService.createPost(keywordId, bucketAt, "제목", "요약", null))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("KeywordMetricHourly를 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("포스트 생성 실패 - Keyword 없음")
	void test7() {
		// given
		Long keywordId = 100L;
		LocalDateTime bucketAt = LocalDateTime.now();

		Keyword keyword = Keyword.builder().id(keywordId).text("테스트 키워드").build();
		KeywordMetricHourly metric = KeywordMetricHourly.builder()
			.id(new KeywordMetricHourlyId(bucketAt, Platform.GOOGLE_TREND, keywordId))
			.keyword(keyword)
			.volume(100)
			.score(100)
			.build();

		given(postRepository.save(any(Post.class))).willReturn(Post.builder().id(1L).build());
		given(keywordMetricHourlyRepository.findById(any())).willReturn(Optional.of(metric));
		given(keywordRepository.findById(keywordId)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> postService.createPost(keywordId, bucketAt, "제목", "요약", null))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("Keyword를 찾을 수 없습니다.");
	}
}
