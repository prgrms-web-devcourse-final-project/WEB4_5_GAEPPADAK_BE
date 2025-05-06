package site.kkokkio.domain.keyword.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import site.kkokkio.domain.keyword.controller.dto.KeywordSearchSourceListResponse;
import site.kkokkio.domain.keyword.service.KeywordService;
import site.kkokkio.domain.post.dto.PostDto;
import site.kkokkio.domain.post.dto.PostListResponse;
import site.kkokkio.domain.source.dto.SourceDto;
import site.kkokkio.domain.source.service.SourceService;
import site.kkokkio.global.dto.RsData;
import site.kkokkio.global.exception.doc.ApiErrorCodeExamples;
import site.kkokkio.global.exception.doc.ErrorCode;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/keywords")
@Tag(name = "Keyword API", description = "Keyword 관련 API")
public class KeywordControllerV1 {

	private final KeywordService keywordService;
	private final SourceService sourceService;

    @Operation(
        summary = "키워드 검색 출처 5개 최신순 조회 (페이지네이션)",
        description = "키워드 검색 결과인 포스트 목록 기준으로 최신순 출처 목록을 조회힙니다. (없을 경우 빈 배열 반환)"
    )
    @ApiErrorCodeExamples({})
    @GetMapping("/search/sources/top")
    public RsData<KeywordSearchSourceListResponse> getKeywordSearchSources(
		@RequestParam String keyword,
        @ParameterObject @PageableDefault() Pageable pageable
    ) {
		Page<PostDto> postDtoList = keywordService.getPostListByKeyword(keyword, pageable);
        List<SourceDto> searchSourceList = sourceService.getTop5SourcesByPosts(postDtoList.toList());
		KeywordSearchSourceListResponse response = KeywordSearchSourceListResponse.from(searchSourceList);
		return new RsData<>(
			"200",
			"성공적으로 조회되었습니다.",
			response
		);
    }
}
