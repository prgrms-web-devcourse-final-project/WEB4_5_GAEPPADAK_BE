package site.kkokkio.domain.source.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.source.controller.dto.SourceListResponse;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.service.SourceService;
import site.kkokkio.global.dto.RsData;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Source API V1", description = "출처 관련 API 엔드포인트 V1")
public class SourceControllerV1 {
	private final SourceService sourceService;

	@Operation(summary = "포스트의 출처 뉴스 Top 10 조회")
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
}
