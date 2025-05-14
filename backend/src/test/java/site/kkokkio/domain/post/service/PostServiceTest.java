package site.kkokkio.domain.post.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import site.kkokkio.domain.keyword.dto.KeywordMetricHourlyDto;
import site.kkokkio.domain.keyword.entity.Keyword;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourly;
import site.kkokkio.domain.keyword.entity.KeywordMetricHourlyId;
import site.kkokkio.domain.keyword.repository.KeywordMetricHourlyRepository;
import site.kkokkio.domain.keyword.repository.KeywordRepository;
import site.kkokkio.domain.keyword.service.KeywordMetricHourlyService;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.dto.PostReportRequestDto;
import site.kkokkio.domain.post.entity.Post;
import site.kkokkio.domain.post.entity.PostKeyword;
import site.kkokkio.domain.post.entity.PostReport;
import site.kkokkio.domain.post.repository.PostKeywordRepository;
import site.kkokkio.domain.post.repository.PostMetricHourlyRepository;
import site.kkokkio.domain.post.repository.PostReportRepository;
import site.kkokkio.domain.post.repository.PostRepository;
import site.kkokkio.domain.source.entity.KeywordSource;
import site.kkokkio.domain.source.entity.Source;
import site.kkokkio.domain.source.repository.KeywordSourceRepository;
import site.kkokkio.domain.source.repository.PostSourceRepository;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.enums.ReportReason;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.infra.ai.adapter.AiSummaryClient;
import site.kkokkio.infra.ai.gemini.GeminiProperties;

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
	private PostReportRepository postReportRepository;
	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ObjectMapper objectMapper;
	@Mock
	private ValueOperations<String, String> valueOps;
	@Mock
	private AiSummaryClient aiSummaryClient;
	@Mock
	private GeminiProperties geminiProperties;

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

		given(keywordMetricHourlyRepository.findTop10HourlyMetricsClosestToNowNative(any())).willReturn(List.of(metric));

		// when
		List<PostDto> result = postService.getTopPostsWithKeyword();

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).keyword()).isEqualTo("테스트 키워드");
	}

	@Test
	@DisplayName("top10 키워드 포스트 조회 - 포스트 없음")
	void test4() throws IOException {
		// given
		given(keywordMetricHourlyRepository.findTop10HourlyMetricsClosestToNowNative(any()))
			.willReturn(Collections.emptyList());

		// when
		List<PostDto> result = postService.getTopPostsWithKeyword();

		// then
		assertThat(result).hasSize(0);
	}

	@Test
	@DisplayName("포스트 생성 - 성공")
	void generatePosts_success() throws Exception {
		// given
		Long keywordId = 100L;
		LocalDateTime bucketAt = LocalDateTime.now();
		String KeywordText = "키워드";

		KeywordMetricHourlyDto metric = new KeywordMetricHourlyDto(keywordId, KeywordText, Platform.GOOGLE_TREND,
			bucketAt, 0, 0, false, null);
		given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(List.of(metric));

		Keyword keyword = Keyword.builder().id(keywordId).text(KeywordText).build();
		Source source1 = createSource("url1");
		Source source2 = createSource("url2");

		KeywordSource ks1 = KeywordSource.builder().keyword(keyword).source(source1).build();
		KeywordSource ks2 = KeywordSource.builder().keyword(keyword).source(source2).build();
		given(keywordSourceRepository.findTopSourcesByKeywordIdsLimited(List.of(keywordId), 10))
			.willReturn(List.of(ks1, ks2));

		KeywordMetricHourly metricEntity = KeywordMetricHourly.builder()
			.id(new KeywordMetricHourlyId(bucketAt, Platform.GOOGLE_TREND, keywordId))
			.build();
		given(keywordMetricHourlyRepository.findById(any())).willReturn(Optional.of(metricEntity));

		given(geminiProperties.getSummaryPrompt()).willReturn("시스템 프롬프트");

		String fakeJson = """
      {"title":"테스트제목","summary":"이것은 테스트 요약입니다."}
      """;

		given(aiSummaryClient.requestSummary(anyString(), anyString()))
			.willReturn(fakeJson);

		ObjectNode fakeNode = new ObjectNode(new ObjectMapper().getNodeFactory())
			.put("title", "테스트제목")
			.put("summary", "이것은 테스트 요약입니다.");
		given(objectMapper.readTree(anyString()))
			.willReturn(fakeNode);

		// Redis 캐시용 JSON 직렬화 모킹
		given(objectMapper.writeValueAsString(any()))
			.willReturn(fakeJson);

		// PostRepository 저장 시 리턴할 엔티티
		Post savedPost = Post.builder()
			.id(100L)
			.title("테스트제목")
			.summary("이것은 테스트 요약입니다.")
			.build();

		given(postRepository.save(any())).willReturn(savedPost);
		given(keywordRepository.findById(keywordId)).willReturn(Optional.of(keyword));
		given(redisTemplate.opsForValue()).willReturn(valueOps);
		doNothing().when(valueOps).set(any(), any(), any());

		// when
		postService.generatePosts();

		// then
		then(postRepository).should().save(argThat(p ->
			"테스트제목".equals(p.getTitle()) &&
			// 서비스 로직에서 붙이는 프리픽스까지 함께 검증
			"AI가 찾아낸 핵심\n\n이것은 테스트 요약입니다.".equals(p.getSummary())
		));
		// then(postRepository).should().save(any());
		then(postSourceRepository).should().insertIgnoreAll(any());
		then(postKeywordRepository).should().insertIgnoreAll(any());
		then(postMetricHourlyRepository).should().save(any());
		then(valueOps).should().set(startsWith("POST_CARD:"), contains("테스트제목"), eq(Duration.ofHours(24)));

	}

	@Test
	@DisplayName("포스트 생성 - lowVariation=true인 경우 기존 포스트 연결")
	void generatePosts_LowVariation() {// given
		Long keywordId = 1L;
		LocalDateTime bucketAt = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
		KeywordMetricHourlyDto metric = new KeywordMetricHourlyDto(keywordId, "chatgpt", Platform.GOOGLE_TREND,
			bucketAt, 0, 0, true, 999L);
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
			false, null);

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

	@Test
	@DisplayName("포스트 신고 성공")
	void reportPost_Success() {
		Long postId = 1L;
		UUID reporterId = UUID.randomUUID();
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		PostReportRequestDto request = new PostReportRequestDto(reportReason);

		// 신고 대상 포스트 실제 객체 생성 및 필드 설정
		Post post = Post.builder().build();
		ReflectionTestUtils.setField(post, "id", postId);
		ReflectionTestUtils.setField(post, "deletedAt", null);
		ReflectionTestUtils.setField(post, "reportCount", 0);

		// 신고하는 사용자 Member 실제 객체 생성
		Member reporter = Member.builder().build();
		ReflectionTestUtils.setField(reporter, "id", reporterId);

		// postRepository.findById 호출 시 실제 post 객체 반환
		given(postRepository.findById(postId)).willReturn(Optional.of(post));

		// postRepository.save 호출 시 실제 post 객체를 인자로 받아서 실제 post 객체 반환
		given(postRepository.save(post)).willReturn(post);

		/// when
		// Service 메소드 호출 시 ReportReason Enum 값을 직접 전달
		postService.reportPost(postId, reporter, reportReason);

		/// 검증
		verify(postRepository).findById(postId);
		verify(postReportRepository).existsByPostAndReporter(post, reporter);
		verify(postReportRepository).save(any(PostReport.class));
		verify(postRepository).save(post);

		assertEquals(1, post.getReportCount());
	}

	@Test
	@DisplayName("포스트 신고 실패 - 포스트 찾을 수 없음")
	void reportPost_PostNotFound() {
		Long postId = 999L;
		UUID reporterId = UUID.randomUUID();
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		Member reporter = Member.builder().build();
		ReflectionTestUtils.setField(reporter, "id", reporterId);

		// postRepository.findById 호출 시 Optional.empty() 반환
		given(postRepository.findById(postId)).willReturn(Optional.empty());

		/// when & then
		// ServiceException 발생 예상 및 검증
		assertThatThrownBy(() -> postService.reportPost(postId, reporter, reportReason))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("존재하지 않는 포스트입니다.");

		/// 검증
		verify(postRepository).findById(postId);
		verify(postReportRepository, never()).existsByPostAndReporter(any(), any());
		verify(postReportRepository, never()).save(any());
		verify(postRepository, never()).save(any());
	}

	@Test
	@DisplayName("포스트 신고 실패 - 삭제된 포스트")
	void reportPost_DeletedPost() {
		Long postId = 2L;
		UUID reporterId = UUID.randomUUID();
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		Member reporter = Member.builder().build();
		ReflectionTestUtils.setField(reporter, "id", reporterId);

		// 신고 대상 포스트 실제 객체 생성 및 필드 설정
		Post post = Post.builder().build();
		ReflectionTestUtils.setField(post, "id", postId);
		ReflectionTestUtils.setField(post, "deletedAt", LocalDateTime.now());
		ReflectionTestUtils.setField(post, "reportCount", 0);

		// postRepository.findById 호출 시 실제 삭제된 post 객체 반환
		given(postRepository.findById(postId)).willReturn(Optional.of(post));

		/// when & then
		assertThatThrownBy(() -> postService.reportPost(postId, reporter, reportReason))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("삭제된 포스트는 신고할 수 없습니다.");

		/// 검증
		verify(postRepository).findById(postId);
		verify(postReportRepository, never()).existsByPostAndReporter(any(), any());
		verify(postReportRepository, never()).save(any());
		verify(postRepository, never()).save(any());
	}

	@Test
	@DisplayName("포스트 신고 실패 - 중복 신고")
	void reportPost_DuplicateReport() {
		Long postId = 3L;
		UUID reporterId = UUID.randomUUID();
		ReportReason reportReason = ReportReason.BAD_CONTENT;

		// 신고 대상 포스트 실제 객체 생성
		Post post = Post.builder().build();
		ReflectionTestUtils.setField(post, "id", postId);
		ReflectionTestUtils.setField(post, "deletedAt", null);
		ReflectionTestUtils.setField(post, "reportCount", 0);

		// postRepository.findById 호출 시 실제 post 객체 반환
		given(postRepository.findById(postId)).willReturn(Optional.of(post));

		// 신고하는 사용자 실제 Member 객체 생성
		Member reporter = Member.builder().build();
		ReflectionTestUtils.setField(reporter, "id", reporterId);

		// postReportRepository.existsByPostAndReporter 호출 시 true 반환
		given(postReportRepository.existsByPostAndReporter(post, reporter)).willReturn(true);

		/// when & then
		// ServiceException 발생 예상 및 검증
		assertThatThrownBy(() -> postService.reportPost(postId, reporter, reportReason))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("이미 신고한 포스트입니다.");

		/// 검증
		verify(postRepository).findById(postId);
		verify(postReportRepository).existsByPostAndReporter(post, reporter);
		verify(postReportRepository, never()).save(any());
		verify(postRepository, never()).save(any());
	}
}
