package site.kkokkio.domain.post.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.UserDetails;
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
import site.kkokkio.domain.member.service.MemberService;
import site.kkokkio.domain.post.controller.dto.PostReportRequest;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.dto.ReportedPostSummary;
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
import site.kkokkio.global.enums.ReportProcessingStatus;
import site.kkokkio.global.enums.ReportReason;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.infra.ai.adapter.AiSummaryPortRouter;

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
	private MemberService memberService;
	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ObjectMapper objectMapper;
	@Mock
	private ValueOperations<String, String> valueOps;
	@Mock
	private AiSummaryPortRouter aiSummaryAdapterRouter;

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

		given(keywordMetricHourlyRepository.findTop10HourlyMetricsClosestToNowNative(any())).willReturn(
			List.of(metric));

		// when
		List<PostDto> result = postService.getTopPostsWithKeyword();

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).keyword()).isEqualTo("테스트 키워드");
	}

	@Test
	@DisplayName("top10 키워드 포스트 조회 - 포스트 없음")
	void test4() {
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
		String keywordText = "키워드";

		KeywordMetricHourlyDto metric = new KeywordMetricHourlyDto(keywordId, keywordText, Platform.GOOGLE_TREND,
			bucketAt, 0, 0, false, null);
		given(keywordMetricHourlyService.findHourlyMetrics()).willReturn(List.of(metric));

		Keyword keyword = Keyword.builder().id(keywordId).text(keywordText).build();
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

		String fakeJson = """
			{"title":"테스트제목","summary":"이것은 테스트 요약입니다."}
			""";

		given(aiSummaryAdapterRouter.summarize(eq(null), anyString()))
			.willReturn(CompletableFuture.completedFuture(fakeJson));

		ObjectNode fakeNode = new ObjectNode(new ObjectMapper().getNodeFactory())
			.put("title", "테스트제목")
			.put("summary", "이것은 테스트 요약입니다.");
		given(objectMapper.readTree(anyString()))
			.willReturn(fakeNode);

		// PostRepository 저장 시 리턴할 엔티티
		Post savedPost = Post.builder()
			.id(100L)
			.title("테스트제목")
			.summary("이것은 테스트 요약입니다.")
			.build();

		given(postRepository.save(any())).willReturn(savedPost);
		given(keywordRepository.findById(keywordId)).willReturn(Optional.of(keyword));

		// when
		postService.generatePosts(List.of(keywordId));

		// then
		then(postRepository).should().save(argThat(p ->
			p.getTitle().equals("테스트제목") && p.getSummary().startsWith("AI가 찾아낸 핵심")
			&& p.getSummary().contains("이것은 테스트 요약입니다.")
		));
		// then(postRepository).should().save(any());
		then(postSourceRepository).should().insertIgnoreAll(any());
		then(postKeywordRepository).should().insertIgnoreAll(any());
		then(postMetricHourlyRepository).should().save(any());
	}

	@Test
	@DisplayName("포스트 생성 - lowVariation=true인 경우 기존 포스트 연결")
	void generatePosts_LowVariation() {
		// given
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
		postService.generatePosts(List.of(keywordId));

		// then
		then(postRepository).should(never()).save(any()); // 신규 포스트 저장 안됨
		then(postSourceRepository).should().insertIgnoreAll(argThat(mappings ->
			mappings.size() == 1 && mappings.getFirst().getPost().getId().equals(999L)
			&& mappings.getFirst().getSource().getFingerprint().equals(source.getFingerprint())
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
		postService.generatePosts(List.of(keywordId));

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

		// 신고 요청 DTO 객체 생성
		PostReportRequest request = new PostReportRequest(reportReason, null);

		// 신고 대상 포스트 실제 객체 생성 및 필드 설정
		Post post = Post.builder().build();
		ReflectionTestUtils.setField(post, "id", postId);
		ReflectionTestUtils.setField(post, "deletedAt", null);
		ReflectionTestUtils.setField(post, "reportCount", 0);

		// 신고하는 사용자 Member 실제 객체 생성
		Member reporter = Member.builder().build();
		ReflectionTestUtils.setField(reporter, "id", reporterId);
		given(memberService.findByEmail(any())).willReturn(reporter);
		UserDetails userDetails = mock(UserDetails.class);
		given(userDetails.getUsername()).willReturn("test@email.com");

		// postRepository.findById 호출 시 실제 post 객체 반환
		given(postRepository.findById(postId)).willReturn(Optional.of(post));

		// postRepository.save 호출 시 실제 post 객체를 인자로 받아서 실제 post 객체 반환
		given(postRepository.save(post)).willReturn(post);

		/// when
		// Service 메소드 호출 시 ReportReason Enum 값을 직접 전달
		postService.reportPost(postId, userDetails, request);

		/// 검증
		verify(postRepository).findById(postId);
		verify(postReportRepository).existsByPostAndReporter(post, reporter);
		verify(postReportRepository).save(any(PostReport.class));
		verify(postRepository).save(post);

		assertEquals(1, post.getReportCount());
	}

	@Test
	@DisplayName("포스트 신고 성공 - 기타 사유")
	void reportPost_Success_Etc() {
		Long postId = 5L;
		UUID reporterId = UUID.randomUUID();
		ReportReason reportReason = ReportReason.ETC;
		String etcReasonText = "기타 상세 사유";

		// 신고 요청 DTO 객체 생성
		PostReportRequest request = new PostReportRequest(reportReason, etcReasonText);

		// 신고 대상 포스트 실제 객체 생성 및 필드 설정
		Post post = Post.builder().build();
		ReflectionTestUtils.setField(post, "id", postId);
		ReflectionTestUtils.setField(post, "deletedAt", null);
		ReflectionTestUtils.setField(post, "reportCount", 0);

		// 신고하는 사용자 Member 실제 객체 생성
		Member reporter = Member.builder().build();
		ReflectionTestUtils.setField(reporter, "id", reporterId);
		given(memberService.findByEmail(any())).willReturn(reporter);
		UserDetails userDetails = mock(UserDetails.class);
		given(userDetails.getUsername()).willReturn("test@email.com");

		// postRepository.findById 호출 시 실제 post 객체 반환
		given(postRepository.findById(postId)).willReturn(Optional.of(post));

		// postRepository.save 호출 시 실제 post 객체를 인자로 받아서 실제 post 객체 반환
		given(postRepository.save(post)).willReturn(post);

		/// when
		// Service 메소드 호출 시 ReportReason Enum 값을 직접 전달
		postService.reportPost(postId, any(), request);

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

		PostReportRequest request = new PostReportRequest(reportReason, null);

		// postRepository.findById 호출 시 Optional.empty() 반환
		given(postRepository.findById(postId)).willReturn(Optional.empty());

		/// when & then
		// ServiceException 발생 예상 및 검증
		assertThatThrownBy(() -> postService.reportPost(postId, any(), request))
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
		given(memberService.findByEmail(any())).willReturn(reporter);
		UserDetails userDetails = mock(UserDetails.class);
		given(userDetails.getUsername()).willReturn("test@email.com");

		PostReportRequest request = new PostReportRequest(reportReason, null);

		// 신고 대상 포스트 실제 객체 생성 및 필드 설정
		Post post = Post.builder().build();
		ReflectionTestUtils.setField(post, "id", postId);
		ReflectionTestUtils.setField(post, "deletedAt", LocalDateTime.now());
		ReflectionTestUtils.setField(post, "reportCount", 0);

		// postRepository.findById 호출 시 실제 삭제된 post 객체 반환
		given(postRepository.findById(postId)).willReturn(Optional.of(post));

		/// when & then
		assertThatThrownBy(() -> postService.reportPost(postId, userDetails, request))
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

		PostReportRequest request = new PostReportRequest(reportReason, null);

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
		given(memberService.findByEmail(any())).willReturn(reporter);
		UserDetails userDetails = mock(UserDetails.class);
		given(userDetails.getUsername()).willReturn("test@email.com");

		// postReportRepository.existsByPostAndReporter 호출 시 true 반환
		given(postReportRepository.existsByPostAndReporter(post, reporter)).willReturn(true);

		/// when & then
		// ServiceException 발생 예상 및 검증
		assertThatThrownBy(() -> postService.reportPost(postId, userDetails, request))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("이미 신고한 포스트입니다.");

		/// 검증
		verify(postRepository).findById(postId);
		verify(postReportRepository).existsByPostAndReporter(post, reporter);
		verify(postReportRepository, never()).save(any());
		verify(postRepository, never()).save(any());
	}

	@Test
	@DisplayName("신고된 포스트 목록 조회 - 성공 (기본 페이징)")
	void getReportedPostsList_Success_Basic() {
		/// given
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("reportedAt")));

		// Service 메서드는 Repository 호출 시 Repository 별칭 기준의 Pageable을 생성하여 전달
		Pageable expectedRepoPageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("latestReportedAt")));

		// Repository가 반환할 ReportedPostSummary 데이터 및 Page 객체 Mocking
		ReportedPostSummary summary1 = new ReportedPostSummary(
			1L, "제목1", "요약1", 10L, "키워드A",
			"BAD_CONTENT", LocalDateTime.now(), 3, ReportProcessingStatus.PENDING
		);
		List<ReportedPostSummary> summaryList = List.of(summary1);
		Page<ReportedPostSummary> mockRepoPage = new PageImpl<>(summaryList, expectedRepoPageable, summaryList.size());

		// PostReportRepository의 findReportedPostSummary 메서드 Mocking
		when(postReportRepository.findReportedPostSummary(eq(null), eq(null), eq(null), eq(null),
			eq(expectedRepoPageable)))
			.thenReturn(mockRepoPage);

		/// when
		Page<ReportedPostSummary> resultPage = postService.getReportedPostsList(pageable, null, null);

		/// then
		verify(postReportRepository)
			.findReportedPostSummary(eq(null), eq(null), eq(null), eq(null), eq(expectedRepoPageable));

		assertThat(resultPage).isEqualTo(mockRepoPage);
		assertThat(resultPage.getContent()).isEqualTo(mockRepoPage.getContent());
		assertThat(resultPage.getTotalElements()).isEqualTo(mockRepoPage.getTotalElements());
	}

	@Test
	@DisplayName("신고된 포스트 목록 조회 - 성공 (검색 적용)")
	void getReportedPostsList_Success_Search() {
		/// given
		Pageable pageable = Pageable.unpaged();
		String searchTarget = "post_title";
		String searchValue = "테스트";

		// Service의 검색 매핑 로직에 따라 Repository에 전달될 예상 검색 파라미터 값
		String expectedSearchTitle = "테스트";

		// Service 메서드가 Unpaged Pageable을 받아 Repository에 전달할 예상 Pageable 객체
		Pageable expectedRepoPageable = PageRequest.of(0, Integer.MAX_VALUE,
			Sort.by(Sort.Order.desc("latestReportedAt")));

		// Repository가 반환할 Page 객체 Mocking
		Page<ReportedPostSummary> mockRepoPage = new PageImpl<>(List.of(), pageable, 0);

		// PostReportRepository의 findReportedPostSummary 메서드 Mocking
		when(postReportRepository.findReportedPostSummary(eq(expectedSearchTitle), eq(null), eq(null), eq(null),
			eq(expectedRepoPageable)))
			.thenReturn(mockRepoPage);

		/// when
		Page<ReportedPostSummary> resultPage = postService.getReportedPostsList(pageable, searchTarget, searchValue);

		/// then
		verify(postReportRepository).findReportedPostSummary(eq(expectedSearchTitle), eq(null), eq(null), eq(null),
			eq(expectedRepoPageable));

		assertThat(resultPage).isEqualTo(mockRepoPage);
	}

	@Test
	@DisplayName("신고된 포스트 목록 조회 - 성공 (정렬 적용)")
	void getReportedPostsList_Success_Sort() {
		/// given
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("reportCount")));

		// Service 메서드는 Repository 호출 시 Repository 별칭 기준의 Pageable을 생성하여 전달
		Pageable expectedRepoPageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("reportCount")));

		// Repository가 반환할 Page 객체 Mocking
		Page<ReportedPostSummary> mockRepoPage = new PageImpl<>(List.of(), expectedRepoPageable, 0);

		// PostReportRepository의 findReportedPostSummary 메서드 Mocking
		when(postReportRepository.findReportedPostSummary(eq(null), eq(null), eq(null), eq(null),
			eq(expectedRepoPageable)))
			.thenReturn(mockRepoPage);

		/// when
		Page<ReportedPostSummary> resultPage = postService.getReportedPostsList(pageable, null, null);

		/// then
		verify(postReportRepository).findReportedPostSummary(eq(null), eq(null), eq(null), eq(null),
			eq(expectedRepoPageable));

		assertThat(resultPage).isEqualTo(mockRepoPage);
	}

	@Test
	@DisplayName("신고된 포스트 목록 조회 - 실패 (부적절한 검색 대상)")
	void getReportedPostsList_Fail_InvalidSearchTarget() {
		/// given
		Pageable pageable = Pageable.unpaged();
		String invalidSearchTarget = "invalidSearchTarget";
		String searchValue = "test";

		assertThatThrownBy(() -> postService.getReportedPostsList(pageable, invalidSearchTarget, searchValue))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("부적절한 검색 옵션입니다.");

		/// when & then
		verify(postReportRepository, never()).findReportedPostSummary(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("신고된 포스트 목록 조회 - 실패 (부적절한 정렬 속성)")
	void getReportedPostsList_Fail_InvalidSortProperty() {
		/// given
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("invalid_sort")));

		/// when & then
		assertThatThrownBy(() -> postService.getReportedPostsList(pageable, null, null))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("부적절한 정렬 옵션입니다.");

		verify(postReportRepository, never()).findReportedPostSummary(any(), any(), any(), any(), any());
	}

	@Test
	@DisplayName("신고된 포스트 숨김 처리 - 성공")
	void hideReportedPost_Success() {
		/// given
		List<Long> postIdsToHide = Arrays.asList(1L, 2L);

		// 숨김 처리될 Post 엔티티 Mocking
		Post post1 = Post.builder().build();
		ReflectionTestUtils.setField(post1, "id", 1L);
		ReflectionTestUtils.setField(post1, "deletedAt", null);

		Post post2 = Post.builder().build();
		ReflectionTestUtils.setField(post2, "id", 2L);
		ReflectionTestUtils.setField(post2, "deletedAt", null);

		when(postRepository.findById(1L)).thenReturn(Optional.of(post1));
		when(postRepository.findById(2L)).thenReturn(Optional.of(post2));

		when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// PostReportRepository의 updateStatusByPostIdIn 메서드 Mocking
		doNothing().when(postReportRepository)
			.updateStatusByPostIdIn(eq(postIdsToHide), eq(ReportProcessingStatus.ACCEPTED));

		/// when
		postService.hideReportedPost(postIdsToHide);

		/// then
		verify(postRepository).findById(1L);
		verify(postRepository).findById(2L);

		assertThat(post1.isDeleted()).isTrue();
		assertThat(post2.isDeleted()).isTrue();

		verify(postRepository).save(eq(post1));
		verify(postRepository).save(eq(post2));

		verify(postReportRepository).updateStatusByPostIdIn(eq(postIdsToHide), eq(ReportProcessingStatus.ACCEPTED));
	}

	@Test
	@DisplayName("신고된 포스트 숨김 처리 - 실패 (포스트 찾을 수 없음)")
	void hideReportedPost_Fail_NotFound() {
		/// given
		List<Long> postIdsToHide = Arrays.asList(1L, 999L, 3L);

		// PostRepository의 findById 메서드 Mocking
		Post post1 = Post.builder().build();
		ReflectionTestUtils.setField(post1, "id", 1L);
		ReflectionTestUtils.setField(post1, "deletedAt", null);

		Post post3 = Post.builder().build();
		ReflectionTestUtils.setField(post3, "id", 3L);
		ReflectionTestUtils.setField(post3, "deletedAt", null);

		when(postRepository.findById(1L)).thenReturn(Optional.of(post1));

		// 존재하지 않는 ID (999L)에 대해서는 Optional.empty() 반환
		when(postRepository.findById(999L)).thenReturn(Optional.empty());

		// PostRepository의 save 메서드 Mocking
		when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

		/// when & then
		assertThatThrownBy(() -> postService.hideReportedPost(postIdsToHide))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("존재하지 않는 포스트가 포함되어 있습니다.");

		// PostRepository의 findById 메서드가 예외가 발생하기 전까지 호출되었는지 검증
		verify(postRepository).findById(1L);
		verify(postRepository).findById(999L);
		verify(postRepository, never()).findById(3L);

		verify(postRepository).save(eq(post1));
		verify(postRepository, never()).save(eq(post3));
		verify(postReportRepository, never()).updateStatusByPostIdIn(anyList(), any(ReportProcessingStatus.class));
		assertThat(post1.isDeleted()).isTrue();
		assertThat(post3.isDeleted()).isFalse();
	}

	@Test
	@DisplayName("신고된 포스트 숨김 처리 - 실패 (이미 삭제된 포스트 포함)")
	void hideReportedPost_Fail_AlreadyDeleted() {
		/// given
		List<Long> postIdsToHide = Arrays.asList(1L, 2L);
		Long deletedPostId = 2L;

		// 숨김 처리될 Post 엔티티 Mocking
		Post post1 = Post.builder().build();
		ReflectionTestUtils.setField(post1, "id", 1L);
		ReflectionTestUtils.setField(post1, "deletedAt", null);

		Post post2 = Post.builder().build();
		ReflectionTestUtils.setField(post2, "id", 2L);
		ReflectionTestUtils.setField(post2, "deletedAt", LocalDateTime.now());

		when(postRepository.findById(1L)).thenReturn(Optional.of(post1));
		when(postRepository.findById(deletedPostId)).thenReturn(Optional.of(post2));

		/// when & then
		assertThatThrownBy(() -> postService.hideReportedPost(postIdsToHide))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("ID [" + deletedPostId + "]포스트는 이미 삭제되었습니다.");

		verify(postRepository).findById(1L);
		verify(postRepository).findById(deletedPostId);
		assertThat(post1.isDeleted()).isTrue();
		verify(postRepository).save(eq(post1));
		verify(postRepository, never()).save(eq(post2));
		verify(postReportRepository, never()).updateStatusByPostIdIn(anyList(), any(ReportProcessingStatus.class));
	}

	@Test
	@DisplayName("신고된 포스트 신고 거부 처리 - 성공")
	void rejectReportedPost_Success() {
		/// given
		List<Long> postIdsToReject = Arrays.asList(4L, 5L);

		// 신고 거부될 Post 엔티티 Mocking
		Post post1 = Post.builder().build();
		ReflectionTestUtils.setField(post1, "id", 1L);

		Post post2 = Post.builder().build();
		ReflectionTestUtils.setField(post2, "id", 2L);

		List<Post> foundPosts = Arrays.asList(post1, post2);

		// PostRepository의 findAllById 메서드 Mocking
		when(postRepository.findAllById(eq(postIdsToReject))).thenReturn(foundPosts);

		// PostReportRepository의 updateStatusByPostIdIn 메서드 Mocking
		doNothing().when(postReportRepository)
			.updateStatusByPostIdIn(eq(postIdsToReject), eq(ReportProcessingStatus.REJECTED));

		/// when
		postService.rejectReportedPost(postIdsToReject);

		/// then
		verify(postRepository).findAllById(eq(postIdsToReject));
		verify(postReportRepository).updateStatusByPostIdIn(eq(postIdsToReject), eq(ReportProcessingStatus.REJECTED));
		verify(postRepository, never()).save(any(Post.class));
	}

	@Test
	@DisplayName("신고된 포스트 신고 거부 처리 - 실패 (포스트 찾을 수 없음)")
	void rejectReportedPost_Fail_NotFound() {
		/// given
		List<Long> postIdsToReject = Arrays.asList(1L, 999L, 2L);

		// PostRepository의 findAllById 메서드 Mocking
		Post post1 = Post.builder().build();
		ReflectionTestUtils.setField(post1, "id", 1L);

		Post post2 = Post.builder().build();
		ReflectionTestUtils.setField(post2, "id", 2L);
		List<Post> foundPosts = Arrays.asList(post1, post2);

		/// when & then
		assertThatThrownBy(() -> postService.rejectReportedPost(postIdsToReject))
			.isInstanceOf(ServiceException.class)
			.hasMessageContaining("존재하지 않는 포스트가 포함되어 있습니다.");

		verify(postRepository).findAllById(eq(postIdsToReject));
		verify(postReportRepository, never()).updateStatusByPostIdIn(anyList(), any(ReportProcessingStatus.class));
		verify(postRepository, never()).save(any(Post.class));
	}
}
