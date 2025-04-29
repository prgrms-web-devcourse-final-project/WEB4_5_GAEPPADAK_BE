package site.kkokkio.domain.post.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.global.exception.ServiceException;

@WebMvcTest(PostControllerV1.class)
@AutoConfigureMockMvc(addFilters = false)
public class PostControllerV1Test {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PostService postService;

	@Test
	@DisplayName("포스트 단건 조회 - 성공")
	void getPostById_success() throws Exception {
		// given
		PostDto postDto = PostDto.builder()
			.postId(1L)
			.keyword("테스트 키워드")
			.title("포스트 제목")
			.summary("포스트 요약")
			.thumbnailUrl("https://image.url")
			.build();

		given(postService.getPostWithKeywordById(1L)).willReturn(postDto);

		// when & then
		mockMvc.perform(get("/api/v1/posts/{postId}", 1L))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.postId").value(1L))
			.andExpect(jsonPath("$.data.keyword").value("테스트 키워드"))
			.andExpect(jsonPath("$.data.title").value("포스트 제목"))
			.andExpect(jsonPath("$.data.summary").value("포스트 요약"))
			.andExpect(jsonPath("$.data.thumbnailUrl").value("https://image.url"));
	}

	@Test
	@DisplayName("포스트 단건 조회 - 실패 (포스트 없음)")
	void getPostById_fail() throws Exception {
		// given
		given(postService.getPostWithKeywordById(1L))
			.willThrow(new ServiceException("400", "포스트를 불러오지 못했습니다."));

		// when & then
		mockMvc.perform(get("/api/v1/posts/{postId}", 1L))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message").value("포스트를 불러오지 못했습니다."));
	}

	@Test
	@DisplayName("Top10 포스트 조회 - 성공")
	void getTopKeywordPosts_success() throws Exception {
		// given
		List<PostDto> topPosts = List.of(
			PostDto.builder()
				.postId(1L)
				.keyword("키워드1")
				.title("제목1")
				.summary("요약1")
				.thumbnailUrl("https://image1.url")
				.build(),
			PostDto.builder()
				.postId(2L)
				.keyword("키워드2")
				.title("제목2")
				.summary("요약2")
				.thumbnailUrl("https://image2.url")
				.build()
		);

		given(postService.getTopPostsWithKeyword()).willReturn(topPosts);

		// when & then
		mockMvc.perform(get("/api/v1/posts/top"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data[0].keyword").value("키워드1"))
			.andExpect(jsonPath("$.data[1].keyword").value("키워드2"));
	}

	@Test
	@DisplayName("Top10 포스트 조회 - 실패 (포스트 없음)")
	void getTopKeywordPosts_fail() throws Exception {
		// given
		given(postService.getTopPostsWithKeyword())
			.willThrow(new ServiceException("400", "포스트를 불러오지 못했습니다."));

		// when & then
		mockMvc.perform(get("/api/v1/posts/top"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.message").value("포스트를 불러오지 못했습니다."));
	}
}
