package site.kkokkio.domain.post.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.controller.dto.PostReportRequest;
import site.kkokkio.domain.post.controller.dto.ReportedPostHideRequest;
import site.kkokkio.domain.post.dto.ReportedPostSummary;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.global.auth.AuthChecker;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.auth.CustomUserDetailsService;
import site.kkokkio.global.config.SecurityConfig;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.enums.ReportReason;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(PostControllerV2.class)
@WithMockUser(roles = "USER")
@Import(SecurityConfig.class)
public class PostControllerV2Test {

	private static final Logger log = LoggerFactory.getLogger(PostControllerV2Test.class);
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean(name = "authChecker")
	AuthChecker authChecker;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PostService postService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private RedisTemplate<String, String> redisTemplate;

	@MockitoBean
	private JwtUtils jwtUtils;

	@Test
	@DisplayName("포스트 신고 - 성공")
	void reportPost_Success() throws Exception {
		Long postId = 1L;
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		PostReportRequest request = new PostReportRequest(reportReason, null);

		// postService.reportPost 메소드는 void 이므로 doNothing() 모킹
		Mockito.doNothing()
			.when(postService).reportPost(eq(postId), any(UserDetails.class), eq(request));

		// 인증된 사용자 모킹
		Member mockReporter = Mockito.mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		// MockMvc를 사용하여 POST 요청 수행
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/posts/{postId}", postId)
				.with(user(new CustomUserDetails("test@email.com", mockReporter.getRole().toString(), true)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("정상적으로 포스트 신고가 접수 되었습니다."));

		/// 검증
		verify(postService).reportPost(eq(postId), any(UserDetails.class), eq(request));
	}

	@Test
	@DisplayName("포스트 신고 실패 - 포스트 찾을 수 없음")
	void reportPost_PostNotFound() throws Exception {
		Long postId = 999L;
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		PostReportRequest request = new PostReportRequest(reportReason, null);

		// ServiceException 발생 모킹
		Mockito.doThrow(new ServiceException("404", "존재하지 않는 포스트입니다."))
			.when(postService).reportPost(eq(postId), any(UserDetails.class), eq(request));

		// 인증된 사용자 모킹
		Member mockReporter = Mockito.mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		// MockMvc를 사용하여 POST 요청 수행
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/posts/{postId}", postId)
				.with(user(new CustomUserDetails("test@email.com", mockReporter.getRole().toString(), true)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404"))
			.andExpect(jsonPath("$.message").value("존재하지 않는 포스트입니다."));
	}

	@Test
	@DisplayName("포스트 신고 실패 - 중복 신고")
	void reportPost_DuplicateReport() throws Exception {
		Long postId = 2L;
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		PostReportRequest request = new PostReportRequest(reportReason, null);

		// ServiceException 발생 모킹
		Mockito.doThrow(new ServiceException("400", "이미 신고한 포스트입니다."))
			.when(postService).reportPost(eq(postId), any(UserDetails.class), eq(request));

		// 인증된 사용자 모킹
		Member mockReporter = Mockito.mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		// MockMvc를 사용하여 POST 요청 수행
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/posts/{postId}", postId)
				.with(user(new CustomUserDetails("test@email.com", mockReporter.getRole().toString(), true)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message").value("이미 신고한 포스트입니다."));
	}

	@Test
	@DisplayName("포스트 신고 실패 - 요청 본문 유효성 검증 실패")
	void reportPost_InvalidRequestBody() throws Exception {
		Long postId = 3L;

		// reason 필드가 누락된 JSON 문자열 생성
		String invalidRequestBodyJson = "{}";

		// 인증된 사용자 모킹
		Member mockReporter = Mockito.mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		// MockMvc를 사용하여 POST 요청 수행
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/posts/{postId}", postId)
				.with(user(new CustomUserDetails("test@email.com", mockReporter.getRole().toString(), true)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidRequestBodyJson))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("신고된 포스트 목록 조회 - 성공 (기본 페이징 및 정렬)")
	@WithMockUser(roles = "ADMIN")
	void getReportedPosts_Success_Basic() throws Exception {
		/// given
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

		Timestamp formattedDateTime1 = Timestamp.valueOf(now.minusDays(5));
		String reportedTime1Formated = formattedDateTime1.toLocalDateTime().format(formatter);
		ReportedPostSummary summary1 = new ReportedPostSummary(
			1L, "제목1", "요약1", 10L, "키워드1", "BAD_CONTENT",
			formattedDateTime1, 3L, "PENDING"
		);

		Timestamp formattedDateTime2 = Timestamp.valueOf(now.minusDays(1));
		String reportedTime2Formated = formattedDateTime2.toLocalDateTime().format(formatter);
		ReportedPostSummary summary2 = new ReportedPostSummary(
			2L, "제목2", "요약2", 11L, "키워드2", "BAD_CONTENT,FALSE_INFO",
			formattedDateTime2, 5L, "PENDING"
		);

		List<ReportedPostSummary> summaryList = Arrays.asList(summary1, summary2);

		// Service가 반환할 Page<ReportedPostSummary> Mocking
		Pageable contollerPageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("reportedAt")));

		Page<ReportedPostSummary> mockServicePage = new PageImpl<>(summaryList, contollerPageable, summaryList.size());

		// PostService의 getReportedPostsList 메서드 Mocking
		when(postService.getReportedPostsList(eq(contollerPageable), eq(null), eq(null)))
			.thenReturn(mockServicePage);

		/// when & then
		mockMvc.perform(get("/api/v2/admin/reports/posts")
				.param("page", "0")
				.param("size", "10")
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("신고된 포스트 목록입니다."))
			.andExpect(jsonPath("$.data.list").isArray())
			.andExpect(jsonPath("$.data.list.length()").value(summaryList.size()))

			// 첫 번째 항목 상세 검증
			.andExpect(jsonPath("$.data.list[0].postId").value(summary1.postId()))
			.andExpect(jsonPath("$.data.list[0].title").value(summary1.title()))
			.andExpect(jsonPath("$.data.list[0].summary").value(summary1.summary()))
			.andExpect(jsonPath("$.data.list[0].keywordId").value(summary1.keywordId()))
			.andExpect(jsonPath("$.data.list[0].keyword").value(summary1.keyword()))
			.andExpect(jsonPath("$.data.list[0].reportReason").isArray())
			.andExpect(jsonPath("$.data.list[0].reportReason.length()").value(1))
			.andExpect(jsonPath("$.data.list[0].reportedAt").value(reportedTime1Formated))
			.andExpect(jsonPath("$.data.list[0].reportCount").value(summary1.reportCount()))
			.andExpect(jsonPath("$.data.list[0].status").value(summary1.status()))

			// 두 번째 항목 상세 검증
			.andExpect(jsonPath("$.data.list[1].postId").value(summary2.postId()))
			.andExpect(jsonPath("$.data.list[1].title").value(summary2.title()))
			.andExpect(jsonPath("$.data.list[1].summary").value(summary2.summary()))
			.andExpect(jsonPath("$.data.list[1].keywordId").value(summary2.keywordId()))
			.andExpect(jsonPath("$.data.list[1].keyword").value(summary2.keyword()))
			.andExpect(jsonPath("$.data.list[1].reportReason").isArray())
			.andExpect(jsonPath("$.data.list[1].reportReason.length()").value(2))
			.andExpect(jsonPath("$.data.list[1].reportedAt").value(reportedTime2Formated))
			.andExpect(jsonPath("$.data.list[1].reportCount").value(summary2.reportCount()))
			.andExpect(jsonPath("$.data.list[1].status").value(summary2.status()))

			// 페이징 메타데이터 검증
			.andExpect(jsonPath("$.data.meta.page").value(mockServicePage.getNumber()))
			.andExpect(jsonPath("$.data.meta.size").value(mockServicePage.getSize()))
			.andExpect(jsonPath("$.data.meta.totalElements").value(mockServicePage.getTotalElements()))
			.andExpect(jsonPath("$.data.meta.totalPages").value(mockServicePage.getTotalPages()))
			.andExpect(jsonPath("$.data.meta.hasNext").value(mockServicePage.hasNext()))
			.andExpect(jsonPath("$.data.meta.hasPrevious").value(mockServicePage.hasPrevious()));

		verify(postService).getReportedPostsList(eq(contollerPageable), eq(null), eq(null));
	}

	@Test
	@DisplayName("신고된 포스트 목록 조회 - 성공 (검색 및 정렬 적용)")
	@WithMockUser(roles = "ADMIN")
	void getReportedPosts_Success_SearchAndSort() throws Exception {
		/// given
		String searchTarget = "post_title";
		String searchValue = "테스트";
		String sortParam = "reportCount,asc";

		// Controller가 받을 예상 Pageable
		Pageable contollerPageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("reportCount")));

		// Service가 반환할 Page<ReportedPostSummary> Mocking
		Page<ReportedPostSummary> mockServicePage = new PageImpl<>(List.of(), contollerPageable, 0);

		// PostService의 getReportedPostsList 메서드 Mocking
		when(postService.getReportedPostsList(eq(contollerPageable), eq(searchTarget), eq(searchValue)))
			.thenReturn(mockServicePage);

		/// when & then
		mockMvc.perform(get("/api/v2/admin/reports/posts")
				.param("page", "0")
				.param("size", "10")
				.param("sort", sortParam)
				.param("searchTarget", searchTarget)
				.param("searchValue", searchValue)
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("신고된 포스트 목록입니다."))
			.andExpect(jsonPath("$.data.list").isArray())
			.andExpect(jsonPath("$.data.meta").exists());

		verify(postService).getReportedPostsList(eq(contollerPageable), eq(searchTarget), eq(searchValue));
	}

	@Test
	@DisplayName("신고된 포스트 목록 조회 - 실패 (부적절한 검색 옵션)")
	@WithMockUser(roles = "ADMIN")
	void getReportedPosts_Fail_InvalidSearchTarget() throws Exception {
		/// given
		String invalidSearchTarget = "invalidSearchTarget";
		String searchValue = "value";

		// Service에서 ServiceException 발생 Mocking
		when(postService.getReportedPostsList(any(Pageable.class), eq(invalidSearchTarget), eq(searchValue)))
			.thenThrow(new ServiceException("400", "부적절한 검색 옵션입니다."));

		/// when & then
		mockMvc.perform(get("/api/v2/admin/reports/posts")
				.param("searchTarget", invalidSearchTarget)
				.param("searchValue", searchValue)
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message").value("부적절한 검색 옵션입니다."));

		verify(postService).getReportedPostsList(any(Pageable.class), eq(invalidSearchTarget), eq(searchValue));
	}

	@Test
	@DisplayName("신고된 포스트 목록 조회 - 실패 (부적절한 정렬 옵션)")
	@WithMockUser(roles = "ADMIN")
	void getReportedPosts_Fail_InvalidSortProperty() throws Exception {
		/// given
		String invalidSortParam = "invalid_sort";

		// Controller가 받을 예상 Pageable
		Pageable controllerPageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("invalid_sort")));

		// Service에서 ServiceException 발생 Mocking
		when(postService.getReportedPostsList(any(Pageable.class), eq(null), eq(null)))
			.thenThrow(new ServiceException("400", "부적절한 정렬 옵션입니다."));

		/// when & then
		mockMvc.perform(get("/api/v2/admin/reports/posts")
				.param("sort", invalidSortParam)
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message").value("부적절한 정렬 옵션입니다."));

		verify(postService).getReportedPostsList(any(Pageable.class), eq(null), eq(null));
	}

	@Test
	@DisplayName("신고된 포스트 목록 조회 - 실패 (관리자 권한 없음)")
	void getReportedPosts_Fail_NotAdmin() throws Exception {
		/// when & then
		mockMvc.perform(get("/api/v2/admin/reports/posts")
				.with(csrf()))
			.andExpect(status().isForbidden());

		verify(postService, never()).getReportedPostsList(any(Pageable.class), any(), any());
	}

	@Test
	@DisplayName("신고된 포스트 숨김 처리 - 성공")
	@WithMockUser(roles = "ADMIN")
	void hideReportedPosts_Success() throws Exception {
		/// given
		List<Long> postIdsToHide = Arrays.asList(1L, 2L, 3L);
		ReportedPostHideRequest requestBody = new ReportedPostHideRequest(postIdsToHide);

		// PostService의 hideReportedPost 메서드 Mocking
		Mockito.doNothing().when(postService).hideReportedPost(eq(postIdsToHide));

		/// when & then
		mockMvc.perform(post("/api/v2/admin/reports/posts")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("포스트가 정상적으로 삭제되었습니다."));

		verify(postService).hideReportedPost(eq(postIdsToHide));
	}

	@Test
	@DisplayName("신고된 포스트 숨김 처리 - 실패 (요청 본문 유효성 검증 실패 빈 목록)")
	@WithMockUser(roles = "ADMIN")
	void hideReportedPosts_Failure_EmptyList() throws Exception {
		/// given
		List<Long> postIdsToHide = List.of();
		ReportedPostHideRequest requestBody = new ReportedPostHideRequest(postIdsToHide);

		/// when & then
		mockMvc.perform(post("/api/v2/admin/reports/posts")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isBadRequest());

		verify(postService, never()).hideReportedPost(anyList());
	}

	@Test
	@DisplayName("신고된 포스트 숨김 처리 - 실패 (요청 본문 유효성 검증 실패 null 목록)")
	@WithMockUser(roles = "ADMIN")
	void hideReportedPosts_Failure_NullList() throws Exception {
		/// given
		String requestBodyJson = "{\"postIds\": null}";

		/// when & then
		mockMvc.perform(post("/api/v2/admin/reports/posts")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBodyJson))
			.andExpect(status().isBadRequest());

		verify(postService, never()).hideReportedPost(anyList());
	}

	@Test
	@DisplayName("신고된 포스트 숨김 처리 - 실패 (포스트 찾을 수 없음)")
	@WithMockUser(roles = "ADMIN")
	void hideReportedPosts_Failure_NotFound() throws Exception {
		/// given
		List<Long> postIdsToHide = Arrays.asList(1L, 999L, 3L);
		ReportedPostHideRequest requestBody = new ReportedPostHideRequest(postIdsToHide);

		// Service에서 ServiceException 발생 Mocking
		doThrow(new ServiceException("404", "존재하지 않는 포스트가 포함되어 있습니다."))
			.when(postService).hideReportedPost(eq(postIdsToHide));

		/// when & then
		mockMvc.perform(post("/api/v2/admin/reports/posts")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404"))
			.andExpect(jsonPath("$.message").value("존재하지 않는 포스트가 포함되어 있습니다."));

		verify(postService).hideReportedPost(eq(postIdsToHide));
	}

	@Test
	@DisplayName("신고된 포스트 숨김 처리 - 실패 (이미 삭제된 포스트 포함)")
	@WithMockUser(roles = "ADMIN")
	void hideReportedPosts_Failure_AlreadyDeleted() throws Exception {
		/// given
		List<Long> postIdsToHide = Arrays.asList(1L, 2L);
		ReportedPostHideRequest requestBody = new ReportedPostHideRequest(postIdsToHide);
		Long deletedPostId = 2L;

		// Service에서 ServiceException
		doThrow(new ServiceException("400", "ID [" + deletedPostId + "] 포스트는 이미 삭제되었습니다."))
			.when(postService).hideReportedPost(eq(postIdsToHide));

		/// when
		mockMvc.perform(post("/api/v2/admin/reports/posts")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message").value("ID [" + deletedPostId + "] 포스트는 이미 삭제되었습니다."));

		verify(postService).hideReportedPost(eq(postIdsToHide));
	}

	@Test
	@DisplayName("신고된 포스트 숨김 처리 - 실패 (관리자 권한 없음)")
	void hideReportedPosts_Failure_NotAdmin() throws Exception {
		/// given
		List<Long> postIdsToHide = Arrays.asList(1L, 2L);
		ReportedPostHideRequest requestBody = new ReportedPostHideRequest(postIdsToHide);

		/// when
		mockMvc.perform(post("/api/v2/admin/reports/posts")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isForbidden());

		verify(postService, never()).hideReportedPost(anyList());
	}

	@Test
	@DisplayName("신고된 포스트 신고 거부 처리 - 성공")
	@WithMockUser(roles = "ADMIN")
	void rejectReportedPosts_Success() throws Exception {
		/// given
		List<Long> postIdsToReject = Arrays.asList(4L, 5L);
		ReportedPostHideRequest requestBody = new ReportedPostHideRequest(postIdsToReject);

		// PostService의 rejectReportedPost 메서드 Mocking
		Mockito.doNothing().when(postService).rejectReportedPost(eq(postIdsToReject));

		/// when
		mockMvc.perform(delete("/api/v2/admin/reports/posts")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("선택하신 신고가 거부 처리되었습니다."));

		verify(postService).rejectReportedPost(eq(postIdsToReject));
	}

	@Test
	@DisplayName("신고된 포스트 신고 거부 처리 - 실패 (요청 본문 유효성 검증 실패 빈 목록)")
	@WithMockUser(roles = "ADMIN")
	void rejectReportedPosts_Fail_EmptyList() throws Exception {
		/// given
		List<Long> postIdsToReject = List.of();
		ReportedPostHideRequest requestBody = new ReportedPostHideRequest(postIdsToReject);

		/// when
		mockMvc.perform(delete("/api/v2/admin/reports/posts")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isBadRequest());

		verify(postService, never()).rejectReportedPost(anyList());
	}

	@Test
	@DisplayName("신고된 포스트 신고 거부 처리 - 실패 (요청 본문 유효성 검증 실패 null 목록)")
	@WithMockUser(roles = "ADMIN")
	void rejectReportedPosts_Fail_NullList() throws Exception {
		/// given
		String requestBodyJson = "{\"postIds\": null}";

		/// when
		mockMvc.perform(delete("/api/v2/admin/reports/posts")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBodyJson))
			.andExpect(status().isBadRequest());

		verify(postService, never()).rejectReportedPost(anyList());
	}

	@Test
	@DisplayName("신고된 포스트 신고 거부 처리 - 실패 (포스트 찾을 수 없음)")
	@WithMockUser(roles = "ADMIN")
	void rejectReportedPosts_Fail_NotFound() throws Exception {
		/// when
		List<Long> postIdsToReject = Arrays.asList(4L, 999L, 5L);
		ReportedPostHideRequest requestBody = new ReportedPostHideRequest(postIdsToReject);

		// Service에서 ServiceException 발생 Mocking
		doThrow(new ServiceException("404", "존재하지 않는 포스트가 포함되어 있습니다."))
			.when(postService).rejectReportedPost(eq(postIdsToReject));

		/// when
		mockMvc.perform(delete("/api/v2/admin/reports/posts")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404"))
			.andExpect(jsonPath("$.message").value("존재하지 않는 포스트가 포함되어 있습니다."));

		verify(postService).rejectReportedPost(eq(postIdsToReject));

	}
}
