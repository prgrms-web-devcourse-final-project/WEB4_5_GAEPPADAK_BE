package site.kkokkio.domain.source.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.source.controller.dto.SourceListResponse;
import site.kkokkio.domain.source.controller.dto.TopSourceListResponse;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.dto.TopSourceItemDto;
import site.kkokkio.domain.source.service.SourceService;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.enums.Platform;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Source API V1", description = "출처 관련 API 엔드포인트 V1")
public class SourceControllerV1 {
	private final SourceService sourceService;

	@Operation(summary = "포스트의 출처 뉴스 Top 10 조회")
	@ApiErrorCodeExamples({ErrorCode.POST_NOT_FOUND_1})
	@GetMapping("/posts/{postId}/news")
	public RsData<SourceListResponse> getNewsSourceList(@PathVariable("postId") Long postId) {
		List<SourceDto> sources = sourceService.getTop10NewsSourcesByPostId(postId);
		SourceListResponse sourceListResponse = SourceListResponse.from(sources);
		return new RsData<>(
			"200",
			"성공적으로 조회되었습니다.",
			sourceListResponse
		);
	}

	@Operation(summary = "포스트의 출처 영상 Top 10 조회")
	@ApiErrorCodeExamples({ErrorCode.POST_NOT_FOUND_1})
	@GetMapping("/posts/{postId}/videos")
	public RsData<SourceListResponse> getVideoSourceList(@PathVariable("postId") Long postId) {
		List<SourceDto> sources = sourceService.getTop10VideoSourcesByPostId(postId);
		SourceListResponse sourceListResponse = SourceListResponse.from(sources);
		return new RsData<>(
			"200",
			"성공적으로 조회되었습니다.",
			sourceListResponse
		);
	}

	@Operation(
		summary = "실시간 인기 유튜브 비디오 목록 조회 (페이지네이션)",
		description = "키워드를 통해 YouTube Data API를 호출하여 현재 한국에서 인기 있는 YouTube 비디오 목록을 가져옵니다."
	)
	@ApiErrorCodeExamples({ErrorCode.SOURCE_NOT_FOUND_1})
	@GetMapping("/videos/top")
	public RsData<TopSourceListResponse> getTopYoutubeSources(
		@ParameterObject @PageableDefault(
			size = 5,
			sort = "score",
			direction = Sort.Direction.DESC
		) Pageable pageable
	) {
		Page<TopSourceItemDto> topYoutubeSources = sourceService.getTopSourcesByPlatform(pageable, Platform.YOUTUBE);

		return new RsData<>(
			"200",
			"정상적으로 호출되었습니다.",
			TopSourceListResponse.from(topYoutubeSources)
		);
	}

	@Operation(
		summary = "실시간 인기 네이버 뉴스 목록 조회 (페이지네이션)",
		description = "키워드를 통해 네이버 뉴스 API를 호출하여 현재 한국에서 인기 있는 네이버 뉴스 목록을 가져옵니다."
	)
	@ApiErrorCodeExamples({ErrorCode.SOURCE_NOT_FOUND_2})
	@GetMapping("/news/top")
	public RsData<TopSourceListResponse> getTopNaverNewsSources(
		@ParameterObject @PageableDefault(
			size = 5,
			sort = "score",
			direction = Sort.Direction.DESC
		) Pageable pageable
	) {
		Page<TopSourceItemDto> topNewsSources = sourceService.getTopSourcesByPlatform(pageable, Platform.NAVER_NEWS);

		return new RsData<>(
			"200",
			"정상적으로 호출되었습니다.",
			TopSourceListResponse.from(topNewsSources)
		);
	}
}
