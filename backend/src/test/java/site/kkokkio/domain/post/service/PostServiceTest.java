package site.kkokkio.domain.post.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyDto;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.entity.PostKeyword;
import site.kkokkio.domain.post.repository.PostKeywordRepository;
import site.kkokkio.domain.post.repository.PostMetricHourlyRepository;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.domain.source.entity.KeywordSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.repository.KeywordSourceRepository;
import site.kkokkio.domain.source.repository.PostSourceRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
	@InjectMocks
	private PostService postService;

	@Mock
	private KeywordMetricHourlyService keywordMetricHourlyService;
	@Mock
	private KeywordSourceRepository keywordSourceRepository;
	@Mock
	private KeywordMetricHourlyRepository keywordMetricHourlyRepository;
	@Mock
	private PostRepository postRepository;
	@Mock
	private KeywordRepository keywordRepository;
	@Mock
	private PostKeywordRepository postKeywordRepository;
	@Mock
	private PostMetricHourlyRepository postMetricHourlyRepository;
	@Mock
	private PostSourceRepository postSourceRepository;
	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ObjectMapper objectMapper;
	@Mock
	private ValueOperations<String, String> valueOps;

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
	void test3() throws IOException {
		// given
		LocalDateTime now = LocalDateTime.of(2025, 4, 29, 18, 0);

		Keyword keyword = Keyword.builder().id(100L).text("테스트 키워드").build();
		Post post = Post.builder().id(1L).title("포스트 제목").summary("요약").bucketAt(now).build();
		KeywordMetricHourly metric = KeywordMetricHourly.builder()
			.id(new KeywordMetricHourlyId(now, Platform.GOOGLE_TREND, 100L))
			.keyword(keyword)
			.volume(100)
			.score(100)
			.post(post)
			.build();

		given(keywordMetricHourlyRepository.findTop10ById_BucketAtOrderByScoreDesc(any())).willReturn(List.of(metric));

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

		// when
		List<PostDto> result = postService.getTopPostsWithKeyword();

		// then
		assertThat(result).hasSize(0);
	}

	@Test
	@DisplayName("포스트 생성 - 성공")
	void generatePosts_success() {
		// given
		Long keywordId = 100L;
		LocalDateTime bucketAt = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
		String KeywordText = "키워드";

		KeywordMetricHourlyDto metric = new KeywordMetricHourlyDto(keywordId, KeywordText, Platform.GOOGLE_TREND,
			bucketAt, 0, 0, false);
		given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(List.of(metric));

		Keyword keyword = Keyword.builder().id(keywordId).text(KeywordText).build();
		Source source1 = createSource("url1");
		Source source2 = createSource("url2");
		Post savedPost = Post.builder().id(100L).title("title").summary("desc").build();

		KeywordSource ks1 = KeywordSource.builder().keyword(keyword).source(source1).build();
		KeywordSource ks2 = KeywordSource.builder().keyword(keyword).source(source2).build();
		given(keywordSourceRepository.findTopSourcesByKeywordIdsLimited(List.of(keywordId), 10))
			.willReturn(List.of(ks1, ks2));

		KeywordMetricHourly metricEntity = KeywordMetricHourly.builder()
			.id(new KeywordMetricHourlyId(bucketAt, Platform.GOOGLE_TREND, keywordId))
			.build();
		given(keywordMetricHourlyRepository.findById(any())).willReturn(Optional.of(metricEntity));

		given(postRepository.save(any())).willReturn(savedPost);
		given(keywordRepository.findById(keywordId)).willReturn(Optional.of(keyword));
		given(redisTemplate.opsForValue()).willReturn(valueOps);
		doNothing().when(valueOps).set(any(), any(), any());

		// when
		postService.generatePosts();

		// then
		then(postRepository).should().save(any());
		then(postSourceRepository).should().insertIgnoreAll(any());
		then(postKeywordRepository).should().insertIgnoreAll(any());
		then(postMetricHourlyRepository).should().save(any());
		then(valueOps).should().set(contains("POST_CARD:"), any(), eq(Duration.ofHours(24)));

	}

	@Test
	@DisplayName("포스트 생성 - lowVariation=true인 경우 기존 포스트 연결")
	void generatePosts_LowVariation() {// given
		Long keywordId = 1L;
		LocalDateTime bucketAt = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
		KeywordMetricHourlyDto metric = new KeywordMetricHourlyDto(keywordId, "chatgpt", Platform.GOOGLE_TREND,
			bucketAt, 0, 0, true);
		Keyword keyword = Keyword.builder().id(keywordId).text("chatgpt").build();
		Source source = createSource("http://example.com");

		given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(List.of(metric));
		given(keywordSourceRepository.findTopSourcesByKeywordIdsLimited(List.of(keywordId), 10))
			.willReturn(List.of(KeywordSource.builder().keyword(keyword).source(source).build()));

		Post existingPost = Post.builder().id(999L).title("기존").summary("요약").build();
		PostKeyword pk = PostKeyword.builder().post(existingPost).build();

		given(postKeywordRepository.findTopByKeywordIdOrderByPost_BucketAtDesc(keywordId))
			.willReturn(Optional.of(pk));

		KeywordMetricHourlyId kmhId = new KeywordMetricHourlyId(bucketAt, Platform.GOOGLE_TREND, keywordId);
		given(keywordMetricHourlyRepository.findById(kmhId)).willReturn(Optional.of(
			KeywordMetricHourly.builder().id(kmhId).build()
		));

		// when
		postService.generatePosts();

		// then
		then(postRepository).should(never()).save(any()); // 신규 포스트 저장 안됨
		then(postSourceRepository).should().insertIgnoreAll(argThat(mappings ->
			mappings.size() == 1 &&
				mappings.getFirst().getPost().getId().equals(999L) &&
				mappings.getFirst().getSource().getFingerprint().equals(source.getFingerprint())
		));
	}

	@Test
	@DisplayName("포스트 생성 - 소스가 없을 시 스킵")
	void generatePosts_skipped() {
		// given
		Long keywordId = 1L;
		LocalDateTime now = LocalDateTime.of(2025, 5, 1, 0, 0);
		KeywordMetricHourlyDto metric = new KeywordMetricHourlyDto(keywordId, "없음", Platform.GOOGLE_TREND, now, 0, 0,
			false);

		given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(List.of(metric));
		given(keywordSourceRepository.findTopSourcesByKeywordIdsLimited(List.of(keywordId), 10)).willReturn(List.of());

		// when
		postService.generatePosts();

		// then
		then(postRepository).shouldHaveNoInteractions();
		then(postSourceRepository).shouldHaveNoInteractions();
		then(postKeywordRepository).shouldHaveNoInteractions();
		then(postMetricHourlyRepository).shouldHaveNoInteractions();
		then(keywordMetricHourlyRepository).should(never()).findById(any());
	}

	private Source createSource(String url) {
		return Source.builder()
			.fingerprint(url)
			.normalizedUrl(url)
			.title("제목")
			.description("설명")
			.thumbnailUrl("썸네일")
			.publishedAt(LocalDateTime.now())
			.platform(Platform.NAVER_NEWS)
			.build();
	}
}
