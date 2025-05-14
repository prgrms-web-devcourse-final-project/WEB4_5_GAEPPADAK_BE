package site.kkokkio.domain.comment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.kkokkio.domain.comment.controller.dto.CommentReportRequest;
import site.kkokkio.domain.comment.controller.dto.ReportedCommentHideRequest;
import site.kkokkio.domain.comment.dto.ReportedCommentSummary;
import site.kkokkio.domain.comment.service.CommentService;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.auth.AuthChecker;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.auth.CustomUserDetailsService;
import site.kkokkio.global.config.SecurityConfig;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.enums.ReportProcessingStatus;
import site.kkokkio.global.enums.ReportReason;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.exception.doc.ErrorCode;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(CommentControllerV2.class)
@Import(SecurityConfig.class)
class CommentControllerV2Test {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean(name = "authChecker")
	AuthChecker authChecker;

	@MockitoBean
	private CommentService commentService;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private RedisTemplate<String, String> redisTemplate;

	@MockitoBean
	private JwtUtils jwtUtils;

	@Test
	@DisplayName("댓글 신고 - 성공")
	@WithMockUser(roles = "USER")
	void test7() throws Exception {
		Long commentId = 1L;
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		CommentReportRequest request = new CommentReportRequest(reportReason);

		// commentService.reportComment 메소드는 void 이므로 doNothing() 모킹
		Mockito.doNothing()
			.when(commentService).reportComment(eq(commentId), any(Member.class), eq(request.reason()));

		// 인증된 사용자 모킹
		Member mockReporter = mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/comments/{commentId}", commentId)
				.with(user(new CustomUserDetails(mockReporter)))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("정상적으로 댓글 신고가 접수 되었습니다."));

	}

	@Test
	@DisplayName("댓글 신고 실패 - 댓글 찾을 수 없음")
	@WithMockUser(roles = "USER")
	void test7_1() throws Exception {
		Long commentId = 999L;
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		CommentReportRequest request = new CommentReportRequest(reportReason);

		// ServiceException 발생 모킹 (ErrorCode.COMMENT_NOT_FOUND 사용)
		Mockito.doThrow(
				new ServiceException(ErrorCode.COMMENT_NOT_FOUND.getCode(), ErrorCode.COMMENT_NOT_FOUND.getMessage()))
			.when(commentService).reportComment(eq(commentId), any(Member.class), eq(request.reason()));

		// 인증된 사용자 모킹
		Member mockReporter = mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/comments/{commentId}", commentId)
				.with(user(new CustomUserDetails(mockReporter)))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(ErrorCode.COMMENT_NOT_FOUND.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorCode.COMMENT_NOT_FOUND.getMessage()));
	}

	@Test
	@DisplayName("댓글 신고 실패 - 삭제된 댓글")
	@WithMockUser(roles = "USER")
	void test7_2() throws Exception {
		Long commentId = 2L;
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		CommentReportRequest request = new CommentReportRequest(reportReason);

		// ServiceException 발생 모킹
		Mockito.doThrow(new ServiceException("400", "삭제된 댓글은 신고할 수 없습니다."))
			.when(commentService).reportComment(eq(commentId), any(Member.class), eq(request.reason()));

		// 인증된 사용자 모킹
		Member mockReporter = mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/comments/{commentId}", commentId)
				.with(user(new CustomUserDetails(mockReporter)))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message").value("삭제된 댓글은 신고할 수 없습니다."));
	}

	@Test
	@DisplayName("댓글 신고 실패 - 본인 댓글 신고")
	@WithMockUser(roles = "USER")
	void test7_3() throws Exception {
		Long commentId = 3L;
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		CommentReportRequest request = new CommentReportRequest(reportReason);

		// ServiceException 발생 모킹
		Mockito.doThrow(new ServiceException("403", "본인의 댓글은 신고할 수 없습니다.")) // Service에서 던지는 예외와 일치시켜야 함
			.when(commentService).reportComment(eq(commentId), any(Member.class), eq(request.reason()));

		// 인증된 사용자 모킹
		Member mockReporter = mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/comments/{commentId}", commentId)
				.with(user(new CustomUserDetails(mockReporter)))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("403"))
			.andExpect(jsonPath("$.message").value("본인의 댓글은 신고할 수 없습니다."));
	}

	@Test
	@DisplayName("댓글 신고 실패 - 중복 신고")
	@WithMockUser(roles = "USER")
	void test7_4() throws Exception {
		Long commentId = 4L;
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		CommentReportRequest request = new CommentReportRequest(reportReason);

		// ServiceException 발생 모킹
		Mockito.doThrow(new ServiceException("400", "이미 신고한 댓글입니다."))
			.when(commentService).reportComment(eq(commentId), any(Member.class), eq(request.reason()));

		// 인증된 사용자 모킹
		Member mockReporter = mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/comments/{commentId}", commentId)
				.with(user(new CustomUserDetails(mockReporter)))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message").value("이미 신고한 댓글입니다."));
	}

	@Test
	@DisplayName("댓글 신고 실패 - 요청 본문 유효성 검증 실패")
	@WithMockUser(roles = "USER")
	void test7_5() throws Exception {
		Long commentId = 5L;

		// reason 필드가 누락된 JSON 문자열 생성
		String invalidRequestBodyJson = "{}";

		// 인증된 사용자 모킹
		Member mockReporter = mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/comments/{commentId}", commentId)
				.with(user(new CustomUserDetails(mockReporter)))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(invalidRequestBodyJson))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("신고된 댓글 목록 조회 - 성공")
	@WithMockUser(roles = "ADMIN")
	void test8() throws Exception {
		/// given
		Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "reportedAt"));

		// 서비스가 반환할 Mock 데이터 생성
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

		ReportedCommentSummary summary1 = new ReportedCommentSummary(
			1L, UUID.randomUUID(), "신고자", false, 10L, "포스트 제목 1",
			"댓글 내용 1", "BAD_CONTENT,SPAM", now.minusHours(1), 5, ReportProcessingStatus.PENDING
		);

		ReportedCommentSummary summary2 = new ReportedCommentSummary(
			2L, UUID.randomUUID(), "탈퇴 예정자", true, 11L, "포스트 제목 2",
			"댓글 내용 2", "RUDE_LANGUAGE", now.minusHours(2), 3, ReportProcessingStatus.ACCEPTED
		);

		List<ReportedCommentSummary> summaryList = Arrays.asList(summary1, summary2);

		// 서비스 Mock이 반환할 Page 객체 생성
		Page<ReportedCommentSummary> mockPage = new PageImpl<>(summaryList, expectedPageable, 100);

		// commentService.getReportedCommentsList 메서드 호출 시 mockPage를 반환하도록 Mocking
		// 파라미터 없음 -> 서비스 메서드는 Pageable과 search 인자 null 4개를 받음
		given(commentService.getReportedCommentsList(
			eq(expectedPageable),
			eq(null),
			eq(null)
		)).willReturn(mockPage);

		/// when & then
		mockMvc.perform(get("/api/v2/admin/reports/comments"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("신고된 댓글 목록이 조회되었습니다."))
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.data.list").isArray())
			.andExpect(jsonPath("$.data.list.length()").value(summaryList.size()))

			// summary1 데이터 검증
			.andExpect(jsonPath("$.data.list[0].commentId").value(summary1.commentId()))
			.andExpect(jsonPath("$.data.list[0].nickname").value(summary1.nickname()))
			.andExpect(jsonPath("$.data.list[0].postId").value(summary1.postId()))
			.andExpect(jsonPath("$.data.list[0].title").value(summary1.postTitle()))
			.andExpect(jsonPath("$.data.list[0].body").value(summary1.commentBody()))
			.andExpect(jsonPath("$.data.list[0].reportReason").isArray())
			.andExpect(jsonPath("$.data.list[0].reportReason.length()").value(2))
			.andExpect(jsonPath("$.data.list[0].reportReason[0]").value("BAD_CONTENT"))
			.andExpect(jsonPath("$.data.list[0].reportReason[1]").value("SPAM"))
			.andExpect(jsonPath("$.data.list[0].reportedAt").value(summary1.latestReportedAt().format(formatter)))
			.andExpect(jsonPath("$.data.list[0].status").value(summary1.status().name()))

			// summary2 데이터 검증
			.andExpect(jsonPath("$.data.list[1].commentId").value(summary2.commentId()))
			.andExpect(jsonPath("$.data.list[1].nickname").value("탈퇴한 회원"))
			.andExpect(jsonPath("$.data.list[1].postId").value(summary2.postId()))
			.andExpect(jsonPath("$.data.list[1].title").value(summary2.postTitle()))
			.andExpect(jsonPath("$.data.list[1].body").value(summary2.commentBody()))
			.andExpect(jsonPath("$.data.list[1].reportReason").isArray())
			.andExpect(jsonPath("$.data.list[1].reportReason.length()").value(1))
			.andExpect(jsonPath("$.data.list[1].reportReason[0]").value("RUDE_LANGUAGE"))
			.andExpect(jsonPath("$.data.list[1].reportedAt").value(summary2.latestReportedAt().format(formatter)))
			.andExpect(jsonPath("$.data.list[1].status").value(summary2.status().name()))

			// data.meta 검증
			.andExpect(jsonPath("$.data.meta.page").value(expectedPageable.getPageNumber()))
			.andExpect(jsonPath("$.data.meta.size").value(expectedPageable.getPageSize()))
			.andExpect(jsonPath("$.data.meta.totalElements").value(mockPage.getTotalElements()))
			.andExpect(jsonPath("$.data.meta.totalPages").value(mockPage.getTotalPages()))
			.andExpect(jsonPath("$.data.meta.hasNext").value(mockPage.hasNext()))
			.andExpect(jsonPath("$.data.meta.hasPrevious").value(mockPage.hasPrevious()));

		// 서비스 메서드가 예상된 인자로 한 번 호출되었는지 검증
		Mockito.verify(commentService)
			.getReportedCommentsList(eq(expectedPageable), eq(null), eq(null));
	}

	@Test
	@DisplayName("신고된 댓글 목록 조회 - 성공 (신고된 댓글 없음)")
	@WithMockUser(roles = "ADMIN")
	void test9() throws Exception {
		/// given
		Pageable expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "reportedAt"));

		// 서비스가 빈 Page 객체를 반환하도록 Mocking
		Page<ReportedCommentSummary> mockPage = Page.empty(expectedPageable);

		given(commentService.getReportedCommentsList(
			eq(expectedPageable), eq(null), eq(null)
		)).willReturn(mockPage);

		/// when & then
		mockMvc.perform(get("/api/v2/admin/reports/comments"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("신고된 댓글 목록이 조회되었습니다."))
			.andExpect(jsonPath("$.data").isMap())
			.andExpect(jsonPath("$.data.list").isArray())
			.andExpect(jsonPath("$.data.list.length()").value(0))

			// data.meta (페이지네이션 메타데이터) 검증
			.andExpect(jsonPath("$.data.meta.page").value(expectedPageable.getPageNumber()))
			.andExpect(jsonPath("$.data.meta.size").value(expectedPageable.getPageSize()))
			.andExpect(jsonPath("$.data.meta.totalElements").value(0))
			.andExpect(jsonPath("$.data.meta.totalPages").value(0))
			.andExpect(jsonPath("$.data.meta.hasNext").value(false))
			.andExpect(jsonPath("$.data.meta.hasPrevious").value(false));
	}

	@Test
	@DisplayName("신고된 댓글 목록 조회 - 실패 (USER 권한)")
	@WithMockUser(roles = "USER")
	void test9_1() throws Exception {
		/// when & then
		mockMvc.perform(get("/api/v2/admin/reports/comments"))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("신고된 댓글 목록 조회 - 실패 (ADMIN 권한 없음)")
	void test9_2() throws Exception {
		/// when & then
		mockMvc.perform(get("/api/v2/admin/reports/comments"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("신고된 댓글 목록 조회 - 실패 (부적절한 검색 옵션)")
	@WithMockUser(roles = "ADMIN")
	void test9_3() throws Exception {
		/// given
		String invalidSearchTarget = "invalidSearchTarget";
		String searchValue = "searchValue";

		// 서비스 메서드가 ServiceException을 던지도록 Mocking
		given(commentService.getReportedCommentsList(
			any(Pageable.class),
			eq(invalidSearchTarget),
			eq(searchValue)
		)).willThrow(new ServiceException(ErrorCode.BAD_SEARCH_OPTION.getCode(),
			ErrorCode.BAD_SEARCH_OPTION.getMessage()));

		/// when & then
		mockMvc.perform(get("/api/v2/admin/reports/comments")
				.param("searchTarget", invalidSearchTarget)
				.param("searchValue", searchValue))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.BAD_SEARCH_OPTION.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorCode.BAD_SEARCH_OPTION.getMessage()));
	}

	@Test
	@DisplayName("신고된 댓글 목록 조회 - 실패 (부적절한 정렬 옵션)")
	@WithMockUser(roles = "ADMIN")
	void test9_4() throws Exception {
		/// given
		String invalidSortParam = "invalidSortParam,asc";

		// 서비스 메서드가 ServiceException을 던지도록 Mocking
		given(commentService.getReportedCommentsList(
			any(Pageable.class),
			eq(null),
			eq(null)
		)).willThrow(new ServiceException(ErrorCode.BAD_SORT_OPTION.getCode(),
			ErrorCode.BAD_SORT_OPTION.getMessage()));

		/// when & then
		mockMvc.perform(get("/api/v2/admin/reports/comments")
				.param("sort", invalidSortParam))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.BAD_SORT_OPTION.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorCode.BAD_SORT_OPTION.getMessage()));
	}

	@Test
	@DisplayName("신고된 댓글 숨김 처리 - 성공")
	@WithMockUser(roles = "ADMIN")
	void test10() throws Exception {
		/// given
		List<Long> commentIdsToHide = Arrays.asList(1L, 5L, 11L);
		ReportedCommentHideRequest requestBody = new ReportedCommentHideRequest(commentIdsToHide);

		// commentService.hideReportedComment 메서드는 void를 반환하므로 doNothing() 모킹
		doNothing().when(commentService).hideReportedComment(eq(commentIdsToHide));

		/// when & then
		mockMvc.perform(post("/api/v2/admin/reports/comments")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("선택하신 댓글이 숨김 처리되었습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		// 서비스 메서드가 예상된 인자로 한 번 호출되었는지 검증
		verify(commentService).hideReportedComment(eq(commentIdsToHide));
	}

	@Test
	@DisplayName("신고된 댓글 숨김 처리 - 실패 (USER 권한)")
	@WithMockUser(roles = "USER")
	void test10_1() throws Exception {
		/// given
		List<Long> commentIdsToHide = Arrays.asList(1L, 2L);
		ReportedCommentHideRequest requestBody = new ReportedCommentHideRequest(commentIdsToHide);

		/// when & then
		mockMvc.perform(post("/api/v2/admin/reports/comments")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("신고된 댓글 숨김 처리 - 실패 (요청 본문 유효성 검증 실패 - 빈 목록)")
	@WithMockUser(roles = "ADMIN")
	void test10_2() throws Exception {
		/// given
		List<Long> commentIdsToHide = Arrays.asList();
		ReportedCommentHideRequest requestBody = new ReportedCommentHideRequest(commentIdsToHide);

		/// when & then
		mockMvc.perform(post("/api/v2/admin/reports/comments")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.COMMENT_IDS_NOT_PROVIDED.getCode()))
			.andExpect(jsonPath("$.message").value("commentIds : NotEmpty : 댓글이 선택되지 않았습니다."));
	}

	@Test
	@DisplayName("신고된 댓글 숨김 처리 - 실패 (댓글 없음)")
	@WithMockUser(roles = "ADMIN")
	void test10_3() throws Exception {
		/// given
		List<Long> commentIdsToHide = Arrays.asList(999L, 10000000L);
		ReportedCommentHideRequest requestBody = new ReportedCommentHideRequest(commentIdsToHide);

		// 서비스 메서드가 ServiceException (404 댓글 없음)을 던지도록 Mocking
		doThrow(
			new ServiceException(ErrorCode.COMMENT_NOT_INCLUDE.getCode(), ErrorCode.COMMENT_NOT_INCLUDE.getMessage()))
			.when(commentService).hideReportedComment(anyList());

		/// when & then
		mockMvc.perform(post("/api/v2/admin/reports/comments")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(ErrorCode.COMMENT_NOT_INCLUDE.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorCode.COMMENT_NOT_INCLUDE.getMessage()));
	}

	@Test
	@DisplayName("신고된 댓글 신고 거부 처리 - 성공")
	@WithMockUser(roles = "ADMIN")
	void test11() throws Exception {
		/// given
		List<Long> commentIdsToReject = Arrays.asList(1L, 5L, 11L);
		ReportedCommentHideRequest requestBody = new ReportedCommentHideRequest(commentIdsToReject);

		// commentService.rejectReportedComment 메서드는 void를 반환하므로 doNothing() 모킹
		doNothing().when(commentService).rejectReportedComment(eq(commentIdsToReject));

		/// when & then
		mockMvc.perform(delete("/api/v2/admin/reports/comments")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("선택하신 신고가 거부 처리되었습니다."))
			.andExpect(jsonPath("$.data").doesNotExist());

		// 서비스 메서드가 예상된 인자로 한 번 호출되었는지 검증
		verify(commentService).rejectReportedComment(eq(commentIdsToReject));
	}

	@Test
	@DisplayName("신고된 댓글 신고 거부 처리 - 실패 (USER 권한)")
	@WithMockUser(roles = "USER")
	void test11_1() throws Exception {
		/// given
		List<Long> commentIdsToReject = Arrays.asList(1L, 3L);
		ReportedCommentHideRequest requestBody = new ReportedCommentHideRequest(commentIdsToReject);

		/// when & then
		mockMvc.perform(delete("/api/v2/admin/reports/comments")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("신고된 댓글 신고 거부 처리 - 실패 (인증되지 않음)")
	void test11_2() throws Exception {
		/// given
		List<Long> commentIdsToReject = Arrays.asList(2L, 3L);
		ReportedCommentHideRequest requestBody = new ReportedCommentHideRequest(commentIdsToReject);

		/// when & then
		mockMvc.perform(delete("/api/v2/admin/reports/comments")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("신고된 댓글 신고 거부 처리 - 실패 (인증되지 않음)")
	@WithMockUser(roles = "ADMIN")
	void test11_3() throws Exception {
		/// given
		List<Long> commentIdsToReject = Arrays.asList();
		ReportedCommentHideRequest requestBody = new ReportedCommentHideRequest(commentIdsToReject);

		/// when & then
		mockMvc.perform(delete("/api/v2/admin/reports/comments")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value(ErrorCode.COMMENT_IDS_NOT_PROVIDED.getCode()))
			.andExpect(jsonPath("$.message").value("commentIds : NotEmpty : 댓글이 선택되지 않았습니다."));
	}

	@Test
	@DisplayName("신고된 댓글 신고 거부 처리 - 실패 (댓글 없음)")
	@WithMockUser(roles = "ADMIN")
	void test11_4() throws Exception {
		/// given
		List<Long> commentIdsToReject = Arrays.asList(999L, 10000000L);
		ReportedCommentHideRequest requestBody = new ReportedCommentHideRequest(commentIdsToReject);

		// 서비스 메서드가 ServiceException을 던지도록 Mocking
		doThrow(
			new ServiceException(ErrorCode.COMMENT_NOT_INCLUDE.getCode(), ErrorCode.COMMENT_NOT_INCLUDE.getMessage()))
			.when(commentService).rejectReportedComment(anyList());

		/// when & then
		mockMvc.perform(delete("/api/v2/admin/reports/comments")
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value(ErrorCode.COMMENT_NOT_INCLUDE.getCode()))
			.andExpect(jsonPath("$.message").value(ErrorCode.COMMENT_NOT_INCLUDE.getMessage()));
	}
}