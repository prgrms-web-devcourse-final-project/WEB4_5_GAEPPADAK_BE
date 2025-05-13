package site.kkokkio.domain.comment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.kkokkio.domain.comment.dto.CommentReportRequestDto;
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
}
