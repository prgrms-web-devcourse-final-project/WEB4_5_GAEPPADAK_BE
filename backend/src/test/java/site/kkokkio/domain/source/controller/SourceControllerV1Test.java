package site.kkokkio.domain.source.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.service.SourceService;
import site.kkokkio.global.exception.ServiceException;

@WebMvcTest(controllers = SourceControllerV1.class)
@AutoConfigureMockMvc(addFilters = false)
class SourceControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SourceService sourceService;

    @Test
	@DisplayName("포스트의 출처 뉴스 조회 - 성공")
    void getNewsSources_Success() throws Exception {
        // given
        List<SourceDto> mockNewsList = List.of(
            new SourceDto("https://news.com", "https://image.jpg", "뉴스 제목1", LocalDateTime.parse("2024-04-29T15:30:00")),
            new SourceDto("https://news.com", "https://image.jpg", "뉴스 제목2", LocalDateTime.parse("2024-04-29T15:30:00")),
            new SourceDto("https://news.com", "https://image.jpg", "뉴스 제목3", LocalDateTime.parse("2024-04-29T15:30:00")),
            new SourceDto("https://news.com", "https://image.jpg", "뉴스 제목4", LocalDateTime.parse("2024-04-29T15:30:00")),
            new SourceDto("https://news.com", "https://image.jpg", "뉴스 제목5", LocalDateTime.parse("2024-04-29T15:30:00"))
        );
        given(sourceService.getTop10NewsSourcesByPostId(anyLong())).willReturn(mockNewsList);

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/news", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("200"))
            .andExpect(jsonPath("$.message").value("성공적으로 조회되었습니다."))
            .andExpect(jsonPath("$.data.list[0].url").value("https://news.com"))
            .andExpect(jsonPath("$.data.list[0].thumbnailUrl").value("https://image.jpg"))
            .andExpect(jsonPath("$.data.list[0].title").value("뉴스 제목1"));
    }

    @Test
	@DisplayName("포스트의 출처 뉴스 조회 - 데이터 없음")
    void getNewsSources_EmptyList() throws Exception {
        // given
        given(sourceService.getTop10NewsSourcesByPostId(anyLong())).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/news", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("200"))
            .andExpect(jsonPath("$.message").value("성공적으로 조회되었습니다."))
            .andExpect(jsonPath("$.data.list").isEmpty());
    }

    @Test
	@DisplayName("포스트의 출처 뉴스 조회 - 잘못된 요청")
    void getNewsSources_InvalidPostId_BadRequest() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/news", "invalidId"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("400"))
            .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
	@DisplayName("포스트의 출처 뉴스 조회 - 포스트 없음")
    void getNewsSources_PostNotFound() throws Exception {
        // given
        given(sourceService.getTop10NewsSourcesByPostId(anyLong()))
            .willThrow(new ServiceException("404", "해당 포스트를 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/news", 999L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("404"))
            .andExpect(jsonPath("$.message").value("해당 포스트를 찾을 수 없습니다."));
    }
}