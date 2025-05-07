package site.kkokkio.domain.post.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import site.kkokkio.domain.keyword.service.KeywordService;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.service.PostService;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.service.SourceService;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.security.CustomUserDetailsService;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(PostControllerV1.class)
@AutoConfigureMockMvc(addFilters = false)
public class PostControllerV1Test {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PostService postService;

	@MockitoBean
	private SourceService sourceService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private RedisTemplate<String, String> redisTemplate;

	@MockitoBean
	private RedisMessageListenerContainer redisMessageListenerContainer;

	@MockitoBean
	private JwtUtils jwtUtils;

	@MockitoBean
	private KeywordService keywordService;

	private List<PostDto> postDtos;
	private String keywordText = "테스트 키워드";
	private int page = 0;
	private int size = 10;

	@BeforeEach
	void setUp() {
		postDtos = Arrays.asList(
			new PostDto(3L, keywordText, "제목3", "요약3", ""),
			new PostDto(1L, keywordText, "제목1", "요약1", ""),
			new PostDto(2L, keywordText, "제목2", "요약2", "")
		);
	}

	@Test
	@DisplayName("포스트 단건 조회 - 성공")
	void test1() throws Exception {
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
			.andExpect(jsonPath("$.message").value("정상적으로 호출되었습니다."))
			.andExpect(jsonPath("$.data.postId").value(1L))
			.andExpect(jsonPath("$.data.keyword").value("테스트 키워드"))
			.andExpect(jsonPath("$.data.title").value("포스트 제목"))
			.andExpect(jsonPath("$.data.summary").value("포스트 요약"))
			.andExpect(jsonPath("$.data.thumbnailUrl").value("https://image.url"));
	}

	@Test
	@DisplayName("포스트 단건 조회 - 실패 (포스트 없음)")
	void test2() throws Exception {
		// given
		given(postService.getPostWithKeywordById(1L))
			.willThrow(new ServiceException("404", "포스트를 불러오지 못했습니다."));

		// when & then
		mockMvc.perform(get("/api/v1/posts/{postId}", 1L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("404"))
			.andExpect(jsonPath("$.message").value("포스트를 불러오지 못했습니다."));
	}

	@Test
	@DisplayName("Top10 포스트 조회 - 성공")
	void test3() throws Exception {
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
			.andExpect(jsonPath("$.message").value("정상적으로 호출되었습니다."))
			.andExpect(jsonPath("$.data[0].postId").value(1))
			.andExpect(jsonPath("$.data[0].keyword").value("키워드1"))
			.andExpect(jsonPath("$.data[0].title").value("제목1"))
			.andExpect(jsonPath("$.data[0].summary").value("요약1"))
			.andExpect(jsonPath("$.data[0].thumbnailUrl").value("https://image1.url"))
			.andExpect(jsonPath("$.data[1].postId").value(2))
			.andExpect(jsonPath("$.data[1].keyword").value("키워드2"))
			.andExpect(jsonPath("$.data[1].title").value("제목2"))
			.andExpect(jsonPath("$.data[1].summary").value("요약2"))
			.andExpect(jsonPath("$.data[1].thumbnailUrl").value("https://image2.url"));
	}

	@Test
	@DisplayName("Top10 포스트 조회 - 빈 배열 반환 (정상 응답)")
	void test4() throws Exception {
		// given
		given(postService.getTopPostsWithKeyword()).willReturn(List.of());

		// when & then
		mockMvc.perform(get("/api/v1/posts/top"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.message").value("정상적으로 호출되었습니다."))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	@DisplayName("키워드 조회 - 최신순 (기본)")
	public void findKeywordTest_Default() throws Exception {
		// Given
		Sort sort = Sort.by("createdAt").descending();
		PageRequest pageRequest = PageRequest.of(page, size, sort);

		// createdAt 기준으로 정렬된 Mock 데이터
		List<PostDto> sortedByCreatedAt = new ArrayList<>(postDtos);
		sortedByCreatedAt.sort((p1, p2) -> p2.postId().compareTo(p1.postId())); // 임의의 최신순 가정
		Page<PostDto> postDtoPage = new PageImpl<>(sortedByCreatedAt, pageRequest, postDtos.size());

		when(keywordService.getPostListByKeyword(eq(keywordText), any(PageRequest.class)))
			.thenReturn(postDtoPage);

		// When & Then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/search")
				.param("keyword", keywordText)
				.param("page", String.valueOf(page))
				.param("size", String.valueOf(size))
				.param("sort", "createdAt,desc")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.list[0].postId").value(3)) // 정렬된 Mock 데이터 확인
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.list[1].postId").value(2))
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.list[2].postId").value(1))
			.andDo(print());
	}

	@Test
	@DisplayName("키워드 조회 - 제목 오름차순")
	public void findKeywordTest_TitleAsc() throws Exception {
		// Given
		Sort sort = Sort.by("title").ascending();
		PageRequest pageRequest = PageRequest.of(page, size, sort);

		// 제목 오름차순으로 정렬된 Mock 데이터
		List<PostDto> sortedByTitle = new ArrayList<>(postDtos);
		sortedByTitle.sort((p1, p2) -> p1.title().compareTo(p2.title()));
		Page<PostDto> postDtoPage = new PageImpl<>(sortedByTitle, pageRequest, postDtos.size());

		when(keywordService.getPostListByKeyword(eq(keywordText), any(PageRequest.class)))
			.thenReturn(postDtoPage);

		// When & Then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/search")
				.param("keyword", keywordText)
				.param("page", String.valueOf(page))
				.param("size", String.valueOf(size))
				.param("sort", "title,asc")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.list[0].postId").value(1)) // 정렬된 Mock 데이터 확인
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.list[1].postId").value(2))
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.list[2].postId").value(3))
			.andDo(print());
	}

	@Test
	@DisplayName("키워드 조회 - 조회 실패")
	public void findKeywordTest_Fail() throws Exception {
		// Given
		when(keywordService.getPostListByKeyword(eq(keywordText), any(PageRequest.class)))
			.thenThrow(new ServiceException("404", "포스트가 존재하지 않습니다."));

		// When & Then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/search")
				.param("keyword", keywordText)
				.param("page", String.valueOf(page))
				.param("size", String.valueOf(size))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isNotFound())
			.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("404"))
			.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("포스트가 존재하지 않습니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("키워드 검색 소스 - 성공")
	public void getKeywordSearchSources_Success() throws Exception {
		// given
		List<SourceDto> mockSources = IntStream.range(0, 5)
			.mapToObj(
				i -> new SourceDto("id-" + i, "url-" + i, "thumb-" + i, "title-" + i, LocalDateTime.now(),
					Platform.NAVER_NEWS))
			.toList();
		when(keywordService.getPostListByKeyword(eq(keywordText), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(postDtos));
		when(sourceService.getTop5SourcesByPosts(anyList()))
			.thenReturn(mockSources);

		// when & then
		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/posts/search/sources")
				.param("keyword", keywordText)
				.param("page", String.valueOf(page))
				.param("size", String.valueOf(size))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
			.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("성공적으로 조회되었습니다."))
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.list").isArray())
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.list.length()").value(5))
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.meta.page").value(0))
			.andDo(print());
	}
}
