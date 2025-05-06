package site.kkokkio.domain.keyword.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import site.kkokkio.domain.keyword.service.KeywordService;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.service.SourceService;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.security.CustomUserDetailsService;
import site.kkokkio.global.util.JwtUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = KeywordControllerV1.class)
@AutoConfigureMockMvc(addFilters = false)
public class KeywordControllerTest {
	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private KeywordService keywordService;
	@MockitoBean
	private SourceService sourceService;

	@MockitoBean
	private CustomUserDetailsService customUserDetailsService;

	@MockitoBean
	private RedisTemplate<String, String> redisTemplate;

	@MockitoBean
	private JwtUtils jwtUtils;

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
		mvc.perform(MockMvcRequestBuilders.get("/api/v1/keywords/search")
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
		mvc.perform(MockMvcRequestBuilders.get("/api/v1/keywords/search")
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
		mvc.perform(MockMvcRequestBuilders.get("/api/v1/keywords/search")
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
				i -> new SourceDto("url-" + i, "thumb-" + i, "title-" + i, LocalDateTime.now(), Platform.NAVER_NEWS, null))
			.toList();
		when(keywordService.getPostListByKeyword(eq(keywordText), any(PageRequest.class)))
			.thenReturn(new PageImpl<>(postDtos));
		when(sourceService.getTop5SourcesByPosts(anyList()))
			.thenReturn(mockSources);

		// when & then
		mvc.perform(MockMvcRequestBuilders.get("/api/v1/keywords/search/sources/top")
				.param("keyword", keywordText)
				.param("page", String.valueOf(page))
				.param("size", String.valueOf(size))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.code").value("200"))
			.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("성공적으로 조회되었습니다."))
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.list").isArray())
			.andExpect(MockMvcResultMatchers.jsonPath("$.data.list.length()").value(5))
			.andDo(print());
	}
}
