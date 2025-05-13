package site.kkokkio.domain.post.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.domain.post.dto.PostReportRequestDto;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.enums.ReportReason;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.security.CustomUserDetails;
import site.kkokkio.global.security.CustomUserDetailsService;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(PostControllerV2.class)
@WithMockUser(roles = "USER")
public class PostControllerV2Test {

	@Autowired
	private MockMvc mockMvc;

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
		PostReportRequestDto request = new PostReportRequestDto(reportReason);

		// postService.reportPost 메소드는 void 이므로 doNothing() 모킹
		Mockito.doNothing()
			.when(postService).reportPost(eq(postId), any(Member.class), eq(request.reason()));

		// 인증된 사용자 모킹
		Member mockReporter = Mockito.mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		// MockMvc를 사용하여 POST 요청 수행
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/posts/{postId}", postId)
				.with(user(new CustomUserDetails(mockReporter)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("정상적으로 포스트 신고가 접수 되었습니다."));

		/// 검증
		verify(postService).reportPost(eq(postId), any(Member.class), eq(request.reason()));
	}

	@Test
	@DisplayName("포스트 신고 실패 - 포스트 찾을 수 없음")
	void reportPost_PostNotFound() throws Exception {
		Long postId = 999L;
		ReportReason reportReason = ReportReason.BAD_CONTENT;
		PostReportRequestDto request = new PostReportRequestDto(reportReason);

		// ServiceException 발생 모킹
		Mockito.doThrow(new ServiceException("404", "존재하지 않는 포스트입니다."))
			.when(postService).reportPost(eq(postId), any(Member.class), eq(request.reason()));

		// 인증된 사용자 모킹
		Member mockReporter = Mockito.mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		// MockMvc를 사용하여 POST 요청 수행
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/posts/{postId}", postId)
				.with(user(new CustomUserDetails(mockReporter)))
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
		PostReportRequestDto request = new PostReportRequestDto(reportReason);

		// ServiceException 발생 모킹
		Mockito.doThrow(new ServiceException("400", "이미 신고한 포스트입니다."))
			.when(postService).reportPost(eq(postId), any(Member.class), eq(request.reason()));

		// 인증된 사용자 모킹
		Member mockReporter = Mockito.mock(Member.class);
		when(mockReporter.getId()).thenReturn(UUID.randomUUID());
		when(mockReporter.getRole()).thenReturn(MemberRole.USER);

		// MockMvc를 사용하여 POST 요청 수행
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/reports/posts/{postId}", postId)
				.with(user(new CustomUserDetails(mockReporter)))
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
				.with(user(new CustomUserDetails(mockReporter)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidRequestBodyJson))
			.andExpect(status().isBadRequest());
	}
}
