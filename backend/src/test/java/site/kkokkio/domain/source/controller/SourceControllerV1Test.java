package site.kkokkio.domain.source.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import site.kkokkio.domain.source.controller.dto.TopSourceListResponse;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.dto.TopSourceItemDto;
import site.kkokkio.domain.source.service.SourceService;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.ServiceException;
import site.kkokkio.global.security.CustomUserDetailsService;
import site.kkokkio.global.util.JwtUtils;

@WebMvcTest(controllers = SourceControllerV1.class)
@AutoConfigureMockMvc(addFilters = false)
class SourceControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SourceService sourceService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("포스트의 출처 뉴스 조회 - 성공")
    void getNewsSources_Success() throws Exception {
        // given
        List<SourceDto> mockNewsList = List.of(
                new SourceDto("source-1", "https://news.com", "https://image.jpg", "뉴스 제목1", LocalDateTime.parse("2024-04-29T15:30:00"),
                        Platform.NAVER_NEWS),
                new SourceDto("source-2", "https://news.com", "https://image.jpg", "뉴스 제목2", LocalDateTime.parse("2024-04-29T15:30:00"),
                        Platform.NAVER_NEWS),
                new SourceDto("source-3", "https://news.com", "https://image.jpg", "뉴스 제목3", LocalDateTime.parse("2024-04-29T15:30:00"),
                        Platform.NAVER_NEWS),
                new SourceDto("source-4", "https://news.com", "https://image.jpg", "뉴스 제목4", LocalDateTime.parse("2024-04-29T15:30:00"),
                        Platform.NAVER_NEWS),
                new SourceDto("source-5", "https://news.com", "https://image.jpg", "뉴스 제목5", LocalDateTime.parse("2024-04-29T15:30:00"),
                        Platform.NAVER_NEWS)
        );
        given(sourceService.getTop10NewsSourcesByPostId(anyLong())).willReturn(mockNewsList);

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/news", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("성공적으로 조회되었습니다."))
                .andExpect(jsonPath("$.data.list[0].sourceId").value("source-1"))
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

    @Test
    @DisplayName("포스트의 출처 영상 조회 - 성공")
    void getVideoSources_Success() throws Exception {
        // given
        List<SourceDto> mockVideoList = List.of(
                new SourceDto("source-1", "https://youtube.com", "https://image.jpg", "영상 제목1",
                        LocalDateTime.parse("2024-04-29T15:30:00"), Platform.YOUTUBE),
                new SourceDto("source-2", "https://youtube.com", "https://image.jpg", "영상 제목2",
                        LocalDateTime.parse("2024-04-29T15:30:00"), Platform.YOUTUBE),
                new SourceDto("source-3", "https://youtube.com", "https://image.jpg", "영상 제목3",
                        LocalDateTime.parse("2024-04-29T15:30:00"), Platform.YOUTUBE),
                new SourceDto("source-4", "https://youtube.com", "https://image.jpg", "영상 제목4",
                        LocalDateTime.parse("2024-04-29T15:30:00"), Platform.YOUTUBE),
                new SourceDto("source-5", "https://youtube.com", "https://image.jpg", "영상 제목5",
                        LocalDateTime.parse("2024-04-29T15:30:00"), Platform.YOUTUBE)
        );
        given(sourceService.getTop10VideoSourcesByPostId(anyLong())).willReturn(mockVideoList);

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/videos", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("성공적으로 조회되었습니다."))
                .andExpect(jsonPath("$.data.list[0].sourceId").value("source-1"))
                .andExpect(jsonPath("$.data.list[0].url").value("https://youtube.com"))
                .andExpect(jsonPath("$.data.list[0].thumbnailUrl").value("https://image.jpg"))
                .andExpect(jsonPath("$.data.list[0].title").value("영상 제목1"));
    }

    @Test
    @DisplayName("실시간 인기 유튜브 비디오 목록 조회 - 성공")
    void getTopYoutubeSources_Success() throws Exception {

        /// given
        // Mocking 할 서비스 메소드의 반환 값 생성
        List<TopSourceItemDto> mockItemList = List.of(
                TopSourceItemDto.builder()
                        .sourceId("source-id-1")
                        .url("https://youtube.com/watch?v=video1")
                        .title("유튜브 인기 영상 제목 1")
                        .description(null)
                        .thumbnailUrl("https://image.youtube1.jpg")
                        .publishedAt(LocalDateTime.parse("2024-04-29T15:30:00"))
                        .platform(Platform.YOUTUBE)
                        .build(),
                TopSourceItemDto.builder()
                        .sourceId("source-id-2")
                        .url("https://youtube.com/watch?v=video2")
                        .title("유튜브 인기 영상 제목 2")
                        .description(null)
                        .thumbnailUrl("https://image.youtube2.jpg")
                        .publishedAt(LocalDateTime.parse("2024-02-04T18:30:00"))
                        .platform(Platform.YOUTUBE)
                        .build(),
                TopSourceItemDto.builder()
                        .sourceId("source-id-3")
                        .url("https://youtube.com/watch?v=video3")
                        .title("유튜브 인기 영상 제목 3")
                        .description(null)
                        .thumbnailUrl("https://image.youtube3.jpg")
                        .publishedAt(LocalDateTime.parse("2022-02-06T01:11:30"))
                        .platform(Platform.YOUTUBE)
                        .build(),
                TopSourceItemDto.builder()
                        .sourceId("source-id-4")
                        .url("https://youtube.com/watch?v=video4")
                        .title("유튜브 인기 영상 제목 4")
                        .description(null)
                        .thumbnailUrl("https://image.youtube4.jpg")
                        .publishedAt(LocalDateTime.parse("2024-09-20T19:53:28"))
                        .platform(Platform.YOUTUBE)
                        .build()
        );

        // Mocking할 Page<TopSourceItemDto> 객체 생성
        Pageable mockPageable =
                PageRequest.of(0, 5, Sort.by("score").descending());
        Page<TopSourceItemDto> mockPage = new PageImpl<>(mockItemList, mockPageable, mockItemList.size());

        // Mocking할 최종 반환 객체 TopSourceListResponse 생성
        TopSourceListResponse mockResponse = TopSourceListResponse.from(mockPage);

        // sousourceService.getTopYoutubeSources() 메소드가 어떤 Pageable 객체를 받든
        // 위에서 만든 mockResponse 객체를 반환하도록 Mocking 설정
        given(sourceService.getTopYoutubeSources(any(Pageable.class)))
                .willReturn(mockResponse);

        /// when & then
        // /api/v1/videos/top 엔드포인트로 GET 요청
        mockMvc.perform(get("/api/v1/videos/top"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("정상적으로 호출되었습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list.length()").value(mockItemList.size()))
                .andExpect(jsonPath("$.data.meta").exists())

                // 첫번째 값 검증
                .andExpect(jsonPath("$.data.list[0].sourceId").value("source-id-1"))
                .andExpect(jsonPath("$.data.list[0].url").value("https://youtube.com/watch?v=video1"))
                .andExpect(jsonPath("$.data.list[0].title").value("유튜브 인기 영상 제목 1"))
                .andExpect(jsonPath("$.data.list[0].description").value(nullValue()))
                .andExpect(jsonPath("$.data.list[0].thumbnailUrl").value("https://image.youtube1.jpg"))
                .andExpect(jsonPath("$.data.list[0].publishedAt").value("2024-04-29T15:30:00"))
                .andExpect(jsonPath("$.data.list[0].platform").value("YOUTUBE"))

                // data.meta 필드 값 검증
                .andExpect(jsonPath("$.data.meta.page").value(mockPage.getNumber()))
                .andExpect(jsonPath("$.data.meta.size").value(mockPage.getSize()))
                .andExpect(jsonPath("$.data.meta.totalElements").value(mockPage.getTotalElements()))
                .andExpect(jsonPath("$.data.meta.totalPages").value(mockPage.getTotalPages()))
                .andExpect(jsonPath("$.data.meta.hasNext").value(mockPage.hasNext()))
                .andExpect(jsonPath("$.data.meta.hasPrevious").value(mockPage.hasPrevious()));
    }

    @Test
    @DisplayName("실시간 인기 유튜브 비디오 목록 조회 - 데이터 없음")
    void getTopYoutubeSources_EmptyList() throws Exception {

        /// given
        // Service가 반환할 비어있는 Page<TopSourceItemDto> 객체 생성
        Pageable mockPageable =
                PageRequest.of(0, 5, Sort.by("score").descending());
        Page<TopSourceItemDto> mockEmptyPage = Page.empty(mockPageable);

        // Service가 반환할 최종 응답 객체 (비어있는 목록 포함)
        TopSourceListResponse mockResponse = TopSourceListResponse.from(mockEmptyPage);

        // sourceService.getTopYoutubeSources() 메소드가 어떤 Pageable 객체를 받든
        // 위에서 만든 비어있는 mockResponse 객체를 반환하도록 Mocking 설정
        given(sourceService.getTopYoutubeSources(any(Pageable.class))).willReturn(mockResponse);

        /// when & then
        // /api/v1/videos/top 엔드포인트로 GET 요청
        mockMvc.perform(get("/api/v1/videos/top"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("정상적으로 호출되었습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list").isEmpty())
                .andExpect(jsonPath("$.data.meta").exists())

                // data.met 필드 값 검증
                .andExpect(jsonPath("$.data.meta.page").value(mockEmptyPage.getNumber()))
                .andExpect(jsonPath("$.data.meta.size").value(mockEmptyPage.getSize()))
                .andExpect(jsonPath("$.data.meta.totalElements").value(mockEmptyPage.getTotalElements()))
                .andExpect(jsonPath("$.data.meta.totalPages").value(mockEmptyPage.getTotalPages()))
                .andExpect(jsonPath("$.data.meta.hasNext").value(mockEmptyPage.hasNext()))
                .andExpect(jsonPath("$.data.meta.hasPrevious").value(mockEmptyPage.hasPrevious()));
        ;
    }

    @Test
    @DisplayName("실시간 인기 네이버 뉴스 목록 조회 - 성공")
    void getTopNaverNewsSources_Success() throws Exception {

        /// given
        // Mocking 할 서비스 메소드의 반환 값 생성
        List<TopSourceItemDto> mockItemList = List.of(
                TopSourceItemDto.builder()
                        .sourceId("source-id-1")
                        .url("https://news.naver.com/article/1")
                        .title("네이버 인기 뉴스 제목 1")
                        .description("네이버 요약 뉴스 내용 1")
                        .thumbnailUrl("https://image.naver.com/1.jpg")
                        .publishedAt(LocalDateTime.parse("2024-04-29T15:30:00"))
                        .platform(Platform.NAVER_NEWS)
                        .build(),
                TopSourceItemDto.builder()
                        .sourceId("source-id-2")
                        .url("https://news.naver.com/article/2")
                        .title("네이버 인기 뉴스 제목 2")
                        .description("네이버 요약 뉴스 내용 2")
                        .thumbnailUrl("https://image.naver.com/2.jpg")
                        .publishedAt(LocalDateTime.parse("2024-02-04T18:30:00"))
                        .platform(Platform.NAVER_NEWS)
                        .build(),
                TopSourceItemDto.builder()
                        .sourceId("source-id-3")
                        .url("https://news.naver.com/article/3")
                        .title("네이버 인기 뉴스 제목 3")
                        .description("네이버 요약 뉴스 내용 3")
                        .thumbnailUrl("https://image.naver.com/3.jpg")
                        .publishedAt(LocalDateTime.parse("2022-02-06T01:11:30"))
                        .platform(Platform.NAVER_NEWS)
                        .build(),
                TopSourceItemDto.builder()
                        .sourceId("source-id-4")
                        .url("https://news.naver.com/article/4")
                        .title("네이버 인기 뉴스 제목 4")
                        .description("네이버 요약 뉴스 내용 4")
                        .thumbnailUrl("https://image.naver.com/4.jpg")
                        .publishedAt(LocalDateTime.parse("2024-09-20T19:53:28"))
                        .platform(Platform.NAVER_NEWS)
                        .build()
        );

        // Mocking할 Page<TopSourceItemDto> 객체 생성
        Pageable mockPageable =
                PageRequest.of(0, 5, Sort.by("score").descending());
        Page<TopSourceItemDto> mockPage = new PageImpl<>(mockItemList, mockPageable, mockItemList.size());

        // Mocking할 최종 반환 객체 TopSourceListResponse 생성
        TopSourceListResponse mockResponse = TopSourceListResponse.from(mockPage);

        // sousourceService.getTopNaverNewsSources() 메소드가 어떤 Pageable 객체를 받든
        // 위에서 만든 mockResponse 객체를 반환하도록 Mocking 설정
        given(sourceService.getTopNaverNewsSources(any(Pageable.class)))
                .willReturn(mockResponse);

        /// when & then
        // /api/v1/news/top 엔드포인트로 GET 요청
        mockMvc.perform(get("/api/v1/news/top"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("정상적으로 호출되었습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list.length()").value(mockItemList.size()))
                .andExpect(jsonPath("$.data.meta").exists())

                // 첫번째 값 검증
                .andExpect(jsonPath("$.data.list[0].sourceId").value("source-id-1"))
                .andExpect(jsonPath("$.data.list[0].url").value("https://news.naver.com/article/1"))
                .andExpect(jsonPath("$.data.list[0].title").value("네이버 인기 뉴스 제목 1"))
                .andExpect(jsonPath("$.data.list[0].description").value("네이버 요약 뉴스 내용 1"))
                .andExpect(jsonPath("$.data.list[0].thumbnailUrl").value("https://image.naver.com/1.jpg"))
                .andExpect(jsonPath("$.data.list[0].publishedAt").value("2024-04-29T15:30:00"))
                .andExpect(jsonPath("$.data.list[0].platform").value("NAVER_NEWS"))

                // data.meta 필드 값 검증
                .andExpect(jsonPath("$.data.meta.page").value(mockPage.getNumber()))
                .andExpect(jsonPath("$.data.meta.size").value(mockPage.getSize()))
                .andExpect(jsonPath("$.data.meta.totalElements").value(mockPage.getTotalElements()))
                .andExpect(jsonPath("$.data.meta.totalPages").value(mockPage.getTotalPages()))
                .andExpect(jsonPath("$.data.meta.hasNext").value(mockPage.hasNext()))
                .andExpect(jsonPath("$.data.meta.hasPrevious").value(mockPage.hasPrevious()));
    }

    @Test
    @DisplayName("실시간 인기 네이버 뉴스 목록 조회 - 데이터 없음")
    void getTopNaverNewsSources_EmptyList() throws Exception {

        /// given
        // Service가 반환할 비어있는 Page<TopSourceItemDto> 객체 생성
        Pageable mockPageable =
                PageRequest.of(0, 5, Sort.by("score").descending());
        Page<TopSourceItemDto> mockEmptyPage = Page.empty(mockPageable);

        // Service가 반환할 최종 응답 객체 (비어있는 목록 포함)
        TopSourceListResponse mockResponse = TopSourceListResponse.from(mockEmptyPage);

        // sourceService.getTopNaverNewsSources() 메소드가 어떤 Pageable 객체를 받든
        // 위에서 만든 비어있는 mockResponse 객체를 반환하도록 Mocking 설정
        given(sourceService.getTopNaverNewsSources(any(Pageable.class))).willReturn(mockResponse);

        /// when & then
        // /api/v1/news/top 엔드포인트로 GET 요청
        mockMvc.perform(get("/api/v1/news/top"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("정상적으로 호출되었습니다."))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.list").isEmpty())
                .andExpect(jsonPath("$.data.meta").exists())

                // data.met 필드 값 검증
                .andExpect(jsonPath("$.data.meta.page").value(mockEmptyPage.getNumber()))
                .andExpect(jsonPath("$.data.meta.size").value(mockEmptyPage.getSize()))
                .andExpect(jsonPath("$.data.meta.totalElements").value(mockEmptyPage.getTotalElements()))
                .andExpect(jsonPath("$.data.meta.totalPages").value(mockEmptyPage.getTotalPages()))
                .andExpect(jsonPath("$.data.meta.hasNext").value(mockEmptyPage.hasNext()))
                .andExpect(jsonPath("$.data.meta.hasPrevious").value(mockEmptyPage.hasPrevious()));
    }
}