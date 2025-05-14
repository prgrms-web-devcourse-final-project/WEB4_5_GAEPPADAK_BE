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

import site.kkokkio.domain.comment.dto.CommentReportRequestDto;
import site.kkokkio.domain.comment.dto.ReportedCommentSummary;
import site.kkokkio.domain.comment.service.CommentService;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.auth.CustomUserDetailsService;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.enums.ReportReason;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.exception.doc.ErrorCode;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(CommentControllerV2.class)
@WithMockUser(roles = "USER")
class CommentControllerV2Test {

	@Autowired
	private MockMvc mockMvc;

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
		CommentReportRequestDto request = new CommentReportRequestDto(reportReason);

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
		CommentReportRequestDto request = new CommentReportRequestDto(reportReason);

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
		CommentReportRequestDto request = new CommentReportRequestDto(reportReason);

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
		CommentReportRequestDto request = new CommentReportRequestDto(reportReason);

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
		CommentReportRequestDto request = new CommentReportRequestDto(reportReason);

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
			"댓글 내용 1", "BAD_CONTENT,SPAM", now.minusHours(1), 5
		);

		ReportedCommentSummary summary2 = new ReportedCommentSummary(
			2L, UUID.randomUUID(), "탈퇴 예정자", true, 11L, "포스트 제목 2",
			"댓글 내용 2", "RUDE_LANGUAGE", now.minusHours(2), 3
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
}
