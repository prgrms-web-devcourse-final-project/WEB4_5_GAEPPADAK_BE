package site.kkokkio.domain.comment.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Collections;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import site.kkokkio.domain.comment.controller.dto.CommentCreateRequest;
import site.kkokkio.domain.comment.dto.CommentDto;
import site.kkokkio.domain.comment.service.CommentService;
import site.kkokkio.domain.member.entity.Member;
import site.kkokkio.global.config.SecurityConfig;
import site.kkokkio.global.security.CustomUserDetailsService;

@WebMvcTest(CommentControllerV1.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = {"USER"})
class CommentControllerV1Test {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CommentService commentService;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@Test
	@DisplayName("댓글 목록 조회 성공")
	void test1() throws Exception {
		CommentDto commentDto = new CommentDto(1L, UUID.randomUUID(), "댓글", 0, LocalDateTime.now());
		Page<CommentDto> page = new PageImpl<>(Collections.singletonList(commentDto),
			PageRequest.of(0, 10), 1);

		Mockito.when(commentService.getCommentListByPostId(eq(1L), any(Pageable.class)))
			.thenReturn(page);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/1/comments")
				.accept(APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.list[0].body").value("댓글"));
	}

	@Test
	@DisplayName("댓글 작성 성공")
	void test2() throws Exception {
		CommentCreateRequest request = new CommentCreateRequest("새 댓글");
		CommentDto commentDto = new CommentDto(1L, UUID.randomUUID(), "새 댓글", 0, LocalDateTime.now());

		Mockito.when(commentService.createComment(eq(1L), any(Member.class), any(CommentCreateRequest.class)))
			.thenReturn(commentDto);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts/1/comments")
				.with(authentication(
					new UsernamePasswordAuthenticationToken(
						mock(Member.class),
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
					)
				))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.body").value("새 댓글"));
	}

	@Test
	@DisplayName("댓글 작성 실패 - body 비어 있음")
	void test2_1() throws Exception {
		CommentCreateRequest request = new CommentCreateRequest(""); // 비어있는 body

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/posts/1/comments")
				.with(authentication(
					new UsernamePasswordAuthenticationToken(
						mock(Member.class),
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
					)
				))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("댓글 수정 성공")
	void test3() throws Exception {
		CommentCreateRequest request = new CommentCreateRequest("수정된 댓글");
		CommentDto commentDto = new CommentDto(1L, UUID.randomUUID(), "수정된 댓글", 0, LocalDateTime.now());

		Mockito.when(commentService.updateComment(eq(1L), any(Member.class), any(CommentCreateRequest.class)))
			.thenReturn(commentDto);

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/comments/1")
				.with(authentication(
					new UsernamePasswordAuthenticationToken(
						mock(Member.class),
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
					)
				))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.body").value("수정된 댓글"));
	}

	@Test
	@DisplayName("댓글 수정 실패 - body 비어 있음")
	void test3_1() throws Exception {
		CommentCreateRequest request = new CommentCreateRequest(""); // 비어있는 body

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/comments/1")
				.with(authentication(
					new UsernamePasswordAuthenticationToken(
						mock(Member.class),
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
					)
				))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("댓글 삭제 성공")
	void test4() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/comments/1")
				.with(authentication(
					new UsernamePasswordAuthenticationToken(
						mock(Member.class),
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
					)
				))
				.with(csrf())
				.accept(APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("204"))
			.andExpect(jsonPath("$.message").value("댓글이 삭제되었습니다."));
	}

	@Test
	@DisplayName("댓글 좋아요 성공")
	void test5() throws Exception {
		CommentDto commentDto = new CommentDto(1L, UUID.randomUUID(), "댓글", 1, LocalDateTime.now());

		Mockito.when(commentService.likeComment(eq(1L), any(Member.class)))
			.thenReturn(commentDto);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/comments/1/like")
				.with(authentication(
					new UsernamePasswordAuthenticationToken(
						mock(Member.class),
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER"))
					)
				))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.likeCount").value(1));
	}
}
