package site.kkokkio.domain.comment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Collections;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.kkokkio.domain.comment.controller.dto.CommentCreateRequest;
import site.kkokkio.domain.comment.dto.CommentDto;
import site.kkokkio.domain.comment.service.CommentService;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.auth.AuthChecker;
import site.kkokkio.global.auth.CustomUserDetails;
import site.kkokkio.global.auth.CustomUserDetailsService;
import site.kkokkio.global.config.SecurityConfig;
import site.kkokkio.global.enums.MemberRole;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(CommentControllerV1.class)
@WithMockUser(roles = "USER")
@Import(SecurityConfig.class)
class CommentControllerV1Test {

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
	@DisplayName("댓글 목록 조회 성공")
	void test1() throws Exception {
		CommentDto commentDto = new CommentDto(1L, UUID.randomUUID(), "test url", "user",
			"댓글", 0, null, null, LocalDateTime.now());
		Page<CommentDto> page = new PageImpl<>(Collections.singletonList(commentDto),
			PageRequest.of(0, 10), 1);

		Mockito.when(commentService.getCommentListByPostId(eq(1L), any(), any(Pageable.class)))
			.thenReturn(page);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/1/comments")
				.accept(APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.list[0].commentId").value(1))
			.andExpect(jsonPath("$.data.list[0].body").value("댓글"));
	}

	@Test
	@DisplayName("댓글 작성 성공")
	void test2() throws Exception {
		CommentCreateRequest request = new CommentCreateRequest("새 댓글");
		CommentDto commentDto = new CommentDto(1L, UUID.randomUUID(), "test url", "user", "새 댓글", 0,
			null, null, LocalDateTime.now());

		Member member = mock(Member.class);
		when(member.getRole()).thenReturn(MemberRole.USER);

		Mockito.when(commentService.createComment(eq(1L), any(UserDetails.class), any(CommentCreateRequest.class)))
			.thenReturn(commentDto);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts/1/comments")
				.with(user(new CustomUserDetails("test@email.com", member.getRole().toString(), true)))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.commentId").value(1))
			.andExpect(jsonPath("$.data.body").value("새 댓글"));
	}

	@Test
	@DisplayName("댓글 작성 실패 - 블랙 처리된 유저")
	@WithMockUser(username = "user@example.com", roles = {"BLACK"})
	void test2_1() throws Exception {
		CommentCreateRequest request = new CommentCreateRequest("새로운 댓글");

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts/1/comments")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("댓글 작성 실패 - body 비어 있음")
	void test2_2() throws Exception {
		CommentCreateRequest request = new CommentCreateRequest(""); // 비어있는 body
		Member member = mock(Member.class);
		when(member.getRole()).thenReturn(MemberRole.USER);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts/1/comments")
				.with(user(new CustomUserDetails("test@email.com", member.getRole().toString(), true)))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("댓글 수정 성공")
	void test3() throws Exception {
		CommentCreateRequest request = new CommentCreateRequest("수정된 댓글");
		UUID memberId = UUID.randomUUID();
		CommentDto commentDto = new CommentDto(1L, memberId, "test url", "user",
			"수정된 댓글", 0, null, null, LocalDateTime.now());

		Member member = mock(Member.class);
		when(member.getId()).thenReturn(memberId);
		when(member.getEmail()).thenReturn("user@example.com");
		when(member.getRole()).thenReturn(MemberRole.USER);

		when(authChecker.isOwner(eq("comment"), eq(1L), any(Authentication.class)))
			.thenReturn(true);

		when(commentService.updateComment(eq(1L), any(CommentCreateRequest.class)))
			.thenReturn(commentDto);

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/comments/1")
				.with(user(new CustomUserDetails(member.getEmail(), member.getRole().toString(), true)))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.commentId").value(1))
			.andExpect(jsonPath("$.data.body").value("수정된 댓글"));
	}

	@Test
	@DisplayName("댓글 수정 실패 - body 비어 있음")
	void test3_1() throws Exception {
		CommentCreateRequest request = new CommentCreateRequest(""); // 비어있는 body
		Member member = mock(Member.class);
		when(member.getRole()).thenReturn(MemberRole.USER);

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/comments/1")
				.with(user(new CustomUserDetails("test@email.com", member.getRole().toString(), true)))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("댓글 수정 실패 - 본인 아님")
	void test3_2() throws Exception {
		CommentCreateRequest request = new CommentCreateRequest("수정된 댓글");
		UUID memberId = UUID.randomUUID();
		CommentDto commentDto = new CommentDto(1L, memberId, "test url", "user",
			"수정된 댓글", 0, null, null, LocalDateTime.now());

		Member member = mock(Member.class);
		when(member.getEmail()).thenReturn("other@example.com");
		when(member.getRole()).thenReturn(MemberRole.USER);

		Mockito.when(commentService.updateComment(eq(1L), any(CommentCreateRequest.class)))
			.thenReturn(commentDto);

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/comments/1")
				.with(user(new CustomUserDetails(member.getEmail(), member.getRole().toString(), true)))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("403"))
			.andExpect(jsonPath("$.message").value("권한이 없습니다."));
	}

	@Test
	@DisplayName("댓글 삭제 성공")
	void test4() throws Exception {
		Member member = mock(Member.class);
		when(member.getRole()).thenReturn(MemberRole.USER);

		when(authChecker.isOwner(eq("comment"), eq(1L), any(Authentication.class)))
			.thenReturn(true);

		mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/comments/1")
				.with(user(new CustomUserDetails("test@email.com", member.getRole().toString(), true)))
				.with(csrf())
				.accept(APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("댓글이 삭제되었습니다."));
	}

	@Test
	@DisplayName("댓글 좋아요 성공")
	void test5() throws Exception {
		CommentDto commentDto = new CommentDto(1L, UUID.randomUUID(), "test url", "user",
			"댓글", 1, null, null, LocalDateTime.now());

		Member member = mock(Member.class);
		when(member.getRole()).thenReturn(MemberRole.USER);

		Mockito.when(commentService.likeComment(eq(1L), any(UserDetails.class)))
			.thenReturn(commentDto);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/comments/1/like")
				.with(user(new CustomUserDetails("test@email.com", member.getRole().toString(), true)))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.commentId").value(1))
			.andExpect(jsonPath("$.data.likeCount").value(1));
	}

	@Test
	@DisplayName("댓글 좋아요 취소 성공")
	void test6() throws Exception {
		CommentDto commentDto = new CommentDto(1L, UUID.randomUUID(), "test url", "user",
			"댓글", 0, null, null, LocalDateTime.now());

		Member member = mock(Member.class);
		when(member.getRole()).thenReturn(MemberRole.USER);
		UserDetails userDetails = new CustomUserDetails("test@email.com", member.getRole().toString(), true);

		Mockito.when(commentService.unlikeComment(eq(1L), eq(userDetails)))
			.thenReturn(commentDto);

		mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/comments/1/like")
				.with(user(userDetails))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.commentId").value(1))
			.andExpect(jsonPath("$.data.likeCount").value(0));
	}
}
